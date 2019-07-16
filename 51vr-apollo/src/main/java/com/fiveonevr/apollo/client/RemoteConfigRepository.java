package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.build.ApolloInjector;
import com.fiveonevr.apollo.client.constants.ConfigConsts;
import com.fiveonevr.apollo.client.core.ExponentialSchedulePolicy;
import com.fiveonevr.apollo.client.core.SchedulePolicy;
import com.fiveonevr.apollo.client.enums.ConfigSourceType;
import com.fiveonevr.apollo.client.exceptions.ApolloConfigException;
import com.fiveonevr.apollo.client.exceptions.ApolloConfigStatusCodeException;
import com.fiveonevr.apollo.client.model.ApolloConfig;
import com.fiveonevr.apollo.client.model.ApolloNotificationMessages;
import com.fiveonevr.apollo.client.model.ServiceDTO;
import com.fiveonevr.apollo.client.utils.*;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RemoteConfigRepository extends  AbstractConfigRepository{
    private final ConfigUtil configUtil;
    private static final Joiner STRING_JOINER = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR);
    private final AtomicBoolean configNeedForceRefresh;
    private static final Escaper pathEscaper = UrlEscapers.urlPathSegmentEscaper();
    private static final Escaper queryParamEscaper = UrlEscapers.urlFormParameterEscaper();
    private static final Logger logger = LoggerFactory.getLogger(RemoteConfigRepository.class);
    private static final Joiner.MapJoiner MAP_JOINER = Joiner.on("&").withKeyValueSeparator("=");
    private final static ScheduledExecutorService executorService;
    private final HttpUtil httpUtil;
    private volatile AtomicReference<ApolloConfig> configCaches;
    private final ConfigServiceLocator serviceLocator;
    private final String namespace;
    private final RateLimiter loadConfigRateLimiter;
    private final int loadConfigQPS = 2;
    private final Gson gson;
    private final SchedulePolicy loadConfigFailSchedulePolicy;
    private final AtomicReference<ServiceDTO> longPollServiceDTO;
    private final AtomicReference<ApolloNotificationMessages> remoteMessages;
    private final RemoteConfigLongPollService remoteConfigLongPollService;

    static {
        executorService = Executors.newScheduledThreadPool(1);
    }

    public RemoteConfigRepository(String namespace) {
        this.remoteConfigLongPollService = ApolloInjector.getInstance(RemoteConfigLongPollService.class);
        this.remoteMessages = new AtomicReference<>();
        this.longPollServiceDTO  = new AtomicReference<>();
        this.serviceLocator = ApolloInjector.getInstance(ConfigServiceLocator.class);
        this.configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        this.httpUtil = ApolloInjector.getInstance(HttpUtil.class);
        this.namespace = namespace;
        loadConfigRateLimiter = RateLimiter.create(loadConfigQPS);
        configNeedForceRefresh = new AtomicBoolean(true);
        gson = new Gson();
        this.loadConfigFailSchedulePolicy = new ExponentialSchedulePolicy(1L, 8L);
        //获取apolloconfig，如果更新了，刷新本地缓存
        this.trySync();
        this.schedulePeridicRefresh();
        this.scheduleLongPollingRefresh();

    }

    //启动longpoll
    private void scheduleLongPollingRefresh() {
        remoteConfigLongPollService.submit(this.namespace, this);
    }

    //加载远程apolloconfig
    private ApolloConfig loadApolloConfig() {
        //限速器满
        if(!this.loadConfigRateLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
            try {
                TimeUnit.SECONDS.sleep(5);
            }catch (InterruptedException e) {
                //ignore
            }
        }
        //这里通过工具类获取
        String appId = configUtil.getAppId();
        String cluster = configUtil.getCluster();
        String datacenter = configUtil.getDataCenter();
        logger.debug("DEBUG====> Apollo.Client.ConfigMeta", STRING_JOINER.join(appId, cluster, namespace));
        //重试次数，如果为true，就2次
        int maxRetries = configNeedForceRefresh.get()? 2 : 1;
        //睡眠时间，默认0
        long onErrorSleepTime = 0;
        Throwable exception = null;
        List<ServiceDTO> configServices = getConfigServices();
        String url = null;
        //String url = assembleQueryConfigUrl(appId, cluster, this.namespace);
        //logger.debug("Loading config from {}", url);
        for (int i = 0; i < maxRetries; i++) {
            List<ServiceDTO>  randomConfigServies = Lists.newLinkedList(configServices);
            Collections.shuffle(randomConfigServies);
            if(longPollServiceDTO.get() != null) {
                randomConfigServies.add(0, longPollServiceDTO.getAndSet(null));
            }
            for(ServiceDTO configService: randomConfigServies) {
                if(onErrorSleepTime > 0) {
                    logger.warn(
                            "Load config failed, will retry in {} {}. appId: {}, cluster: {}, namespaces: {}",
                            onErrorSleepTime, configUtil.getOnErrorRetryIntervalTimeUnit(), appId, cluster, namespace);
                    try {
                        configUtil.getOnErrorRetryIntervalTimeUnit().sleep(onErrorSleepTime);
                    }catch (InterruptedException ex) {
                        //ignore;
                    }
                }
                url = assembleQueryConfigUrl(configService.getHomepageUrl(), appId, cluster, namespace,
                        datacenter, remoteMessages.get(), configCaches.get());
                HttpRequest request = new HttpRequest(url);
                try {
                    HttpResponse<ApolloConfig> response = httpUtil.doGet(request, ApolloConfig.class);
                    //是否需要强制刷新，因为调用成功了，所以不需要
                    this.configNeedForceRefresh.set(false);
                    this.loadConfigFailSchedulePolicy.success();
                    if(response.getStatusCode() == 304) {
                        logger.debug("Config server responds with 304 HTTP status code.");
                    }
                    ApolloConfig result = response.getBody();
                    logger.debug("Loaded config for {}: {}", namespace, result);
                    return result;
                }catch (ApolloConfigStatusCodeException ex) {
                    ApolloConfigStatusCodeException apolloConfigStatusCodeException = ex;
                    if(ex.getStatusCode() == 404) {
                        String message = String.format(
                                "Could not find config for namespace - appId: %s, cluster: %s, namespace: %s, " +
                                        "please check whether the configs are released in Apollo!",
                                appId, cluster, namespace);
                        apolloConfigStatusCodeException = new ApolloConfigStatusCodeException(ex.getStatusCode(), message);
                    }
                    exception = apolloConfigStatusCodeException;
                }catch (Throwable ex) {
                    exception = ex;
                }
                //httpUtil doGet就发生了错误，configRefresh是true，那么间隔1秒，否则会逐次递增1秒，最大为8s
                onErrorSleepTime = this.configNeedForceRefresh.get() ? 1 : this.loadConfigFailSchedulePolicy.fail();
            }


        }
        String message = String.format(
                "Load Apollo Config failed - appId: %s, cluster: %s, namespace: %s, url: %s",
                appId, cluster, namespace, url);
        throw new ApolloConfigException(message, exception);
    }

    private void schedulePeridicRefresh() {
        logger.debug("Schedule periodc refresh with interval: {} {}", 5, TimeUnit.MINUTES);
        executorService.scheduleAtFixedRate(() -> trySync(),5, 5, TimeUnit.MINUTES);
    }




    //sync同步方法,检查apolloconfig是否与previous相同，不相同缓存起来
    @Override
    protected synchronized void sync() {
        try {
            ApolloConfig previous = configCaches.get();
            ApolloConfig current = loadApolloConfig();
            if (previous != current) {
                logger.debug("Remote Config refreshed");
                configCaches.set(current);
                this.fireRepositoryChange(namespace, this.getProperty());
            }
        }catch (Throwable ex) {
            String message = String.format("appId: %s, cluster:  %s, namespace:  %s, sync remoteRepository error", configUtil.getAppId(), configUtil.getCluster(), this.namespace);
            logger.error("appId: {}, cluster: {}, namespace: {}, sync remoteRepository error， {}", configUtil.getAppId(), configUtil.getCluster(), this.namespace, ExceptionUtils.getDetailMessage(ex));
            ApolloConfigException apolloConfigException = new ApolloConfigException(message, ex);
            throw apolloConfigException;
        }
    }


    @Override
    public Properties getProperty() {
        if(configCaches .get() == null) {
            this.sync();
        }
        return this.transformApolloConfigToProperties(this.configCaches.get());
    }

    //将ApolloConfig转为Properties
    private Properties transformApolloConfigToProperties(ApolloConfig apolloConfig) {
        Properties result = new Properties();
        result.putAll(apolloConfig.getConfigurations());
        return result;
    }


    @Override
    public void setUpstreamRepository(ConfigRegistry upstreamRepository) {

    }

    @Override
    public ConfigSourceType getSourceType() {
        return null;
    }



    private List<ServiceDTO> getConfigServices() {
        List<ServiceDTO> services = serviceLocator.getConfigServices();
        if (services.size() == 0) {
            throw new ApolloConfigException("No available config service");
        }
        return services;

    }

    String assembleQueryConfigUrl(String uri, String appId, String cluster, String namespace,
                                  String dataCenter, ApolloNotificationMessages remoteMessages, ApolloConfig previousConfig) {

        String path = "configs/%s/%s/%s";
        List<String> pathParams =
                Lists.newArrayList(pathEscaper.escape(appId), pathEscaper.escape(cluster),
                        pathEscaper.escape(namespace));
        Map<String, String> queryParams = Maps.newHashMap();

        if (previousConfig != null) {
            queryParams.put("releaseKey", queryParamEscaper.escape(previousConfig.getRelease()));
        }

        if (!Strings.isNullOrEmpty(dataCenter)) {
            queryParams.put("dataCenter", queryParamEscaper.escape(dataCenter));
        }

        String localIp = configUtil.getLocalIp();
        if (!Strings.isNullOrEmpty(localIp)) {
            queryParams.put("ip", queryParamEscaper.escape(localIp));
        }

        if (remoteMessages != null) {
            queryParams.put("messages", queryParamEscaper.escape(gson.toJson(remoteMessages)));
        }

        String pathExpanded = String.format(path, pathParams.toArray());

        if (!queryParams.isEmpty()) {
            pathExpanded += "?" + MAP_JOINER.join(queryParams);
        }
        if (!uri.endsWith("/")) {
            uri += "/";
        }
        return uri + pathExpanded;
    }
}
