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



public class RemoteConfigRepository extends AbstractConfigRepository {
    private static final Logger logger = LoggerFactory.getLogger(RemoteConfigRepository.class);
    private final ConfigUtil configUtil;
    private static final Joiner STRING_JOINER = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR);
    private final AtomicBoolean configNeedForceRefresh;
    private static final Escaper pathEscaper = UrlEscapers.urlPathSegmentEscaper();
    private static final Escaper queryParamEscaper = UrlEscapers.urlFormParameterEscaper();
    private static final Joiner.MapJoiner MAP_JOINER = Joiner.on("&").withKeyValueSeparator("=");
    //定时启动线程
    private final static ScheduledExecutorService executorService;
    private final HttpUtil httpUtil;
    //原子引用类<ApolloConfig> 存放缓存
    private volatile AtomicReference<ApolloConfig> configCaches;
    private final ConfigServiceLocator serviceLocator;
    private final String namespace;
    //限速器
    private final RateLimiter loadConfigRateLimiter;
    private final Gson gson;
    //加载config失败的policy
    private final SchedulePolicy loadConfigFailSchedulePolicy;
    private final AtomicReference<ServiceDTO> longPollServiceDTO;
    private final AtomicReference<ApolloNotificationMessages> remoteMessages;
    private final RemoteConfigLongPollService remoteConfigLongPollService;

    static {
        executorService = Executors.newScheduledThreadPool(1, ApolloThreadFactory.create("RemoteConfigRepository", true));
    }

    public RemoteConfigRepository(String namespace) {
        this.namespace = namespace;
        this.configCaches = new AtomicReference<>();
        this.remoteConfigLongPollService = ApolloInjector.getInstance(RemoteConfigLongPollService.class);
        this.remoteMessages = new AtomicReference<>();
        this.longPollServiceDTO  = new AtomicReference<>();
        this.configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        this.httpUtil = ApolloInjector.getInstance(HttpUtil.class);
        this.serviceLocator = ApolloInjector.getInstance(ConfigServiceLocator.class);
        loadConfigRateLimiter = RateLimiter.create(configUtil.getLoadConfigQPS());
        configNeedForceRefresh = new AtomicBoolean(true);
        gson = new Gson();
        this.loadConfigFailSchedulePolicy = new ExponentialSchedulePolicy(configUtil.getOnErrorRetryInterval(), configUtil.getOnErrorRetryInterval() * 8);
        //本地刷新缓存，引爆监听器
        this.trySync();
        //定时同步本地缓存
        this.schedulePeridicRefresh();
        //开启poll长轮询，nitify了会本地trySync
        this.scheduleLongPollingRefresh();

    }

    //启动长轮询
    private void scheduleLongPollingRefresh() {
        remoteConfigLongPollService.submit(this.namespace, this);
    }

    //加载远程ApolloConfig
    private ApolloConfig loadApolloConfig() {
        //5s超时时间内看限速器是否可以通过，若false，睡眠5s
        if(!this.loadConfigRateLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
            try {
                TimeUnit.SECONDS.sleep(5);
            }catch (InterruptedException e) {
                //ignore
            }
        }
        //这里通过工具类获取appId，cluster，datacenter
        String appId = configUtil.getAppId();
        String cluster = configUtil.getCluster();
        String datacenter = configUtil.getDataCenter();
        logger.debug("Apollo.Client.ConfigMeta: {}", STRING_JOINER.join(appId, cluster, namespace));
        //configNeedForceRefresh为true，重试次数为2
        int maxRetries = configNeedForceRefresh.get()? 2 : 1;
        //默认不睡眠
        long onErrorSleepTime = 0;
        Throwable exception = null;
        List<ServiceDTO> configServices = getConfigServices();

        String url = null;
        for (int i = 0; i < maxRetries; i++) {
            //这里貌似不需要array转linked
            //List<ServiceDTO>  randomConfigServies = Lists.newLinkedList(configServices);
            //shuffle随机洗牌
            Collections.shuffle(configServices);
            //假设longPoolServiceDTO被notice，将longpollserviceDTO加入list，然后清空longpollserviceDTO
            if(longPollServiceDTO.get() != null) {
                configServices.add(0, longPollServiceDTO.getAndSet(null));
            }
            //循环configServies
            for(ServiceDTO configService: configServices) {
                //线程休眠一段时间
                if(onErrorSleepTime > 0) {
                    logger.warn("Load config failed, will retry in {} {}. appId: {}, cluster: {}, namespaces: {}", onErrorSleepTime, configUtil.getOnErrorRetryIntervalTimeUnit(), appId, cluster, namespace);
                    try {
                        configUtil.getOnErrorRetryIntervalTimeUnit().sleep(onErrorSleepTime);
                    }catch (InterruptedException ex) {
                        //ignore;
                    }
                }
                //http://10.100.100.20:8080/configs/001/default/application?ip=10.2.10.38
                url = assembleQueryConfigUrl(configService.getHomepageUrl(), appId, cluster, namespace, datacenter, remoteMessages.get(), configCaches.get());
                HttpRequest request = new HttpRequest(url);
                try {
                    HttpResponse<ApolloConfig> response = httpUtil.doGet(request, ApolloConfig.class);
                    //doget没有发生错误，强制刷新为false
                    this.configNeedForceRefresh.set(false);
                    //策略为true
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
                        String message = String.format("Could not find config for namespace - appId: %s, cluster: %s, namespace: %s, " + "please check whether the configs are released in Apollo!", appId, cluster, namespace);
                        apolloConfigStatusCodeException = new ApolloConfigStatusCodeException(ex.getStatusCode(), message);
                    }
                    exception = apolloConfigStatusCodeException;
                }catch (Throwable ex) {
                    exception = ex;
                }
                //httpUtil doGet就发生了错误，configRefresh是true，那么间隔1秒，否则根据policy返回间隔时间
                onErrorSleepTime = this.configNeedForceRefresh.get() ? 1 : this.loadConfigFailSchedulePolicy.fail();
            }


        }
        String message = String.format(
                "Load Apollo Config failed - appId: %s, cluster: %s, namespace: %s, url: %s",
                appId, cluster, namespace, url);
        throw new ApolloConfigException(message, exception);
    }

    //定时同步本地缓存
    private void schedulePeridicRefresh() {
        logger.debug("Schedule periodc refresh with interval: {} {}", 5, TimeUnit.MINUTES);
        executorService.scheduleAtFixedRate(() -> trySync(),5, 5, TimeUnit.MINUTES);
    }

    //longpoll线程notify
    public void onLongPollNotified(ServiceDTO longPollNotifiedServiceDto, ApolloNotificationMessages remoteMessages) {
        longPollServiceDTO.set(longPollNotifiedServiceDto);
        this.remoteMessages.set(remoteMessages);
        executorService.submit(() ->{
            configNeedForceRefresh.set(true);
            trySync();
        });
    }


    //同步方法代码块，将最新的apolloconfig缓存 并且引爆监听器
    @Override
    protected synchronized void sync() {
        try {
            //获取previous版本，从缓存中
            ApolloConfig previous = configCaches.get();
            //获取当前版本
            ApolloConfig current = loadApolloConfig();
            if (previous != current) {
                logger.debug("Remote Config refreshed");
                //本地缓存刷新
                configCaches.set(current);
                //点火本地监听器
                this.fireRepositoryChange(namespace, this.getProperty());
            }
        }catch (Throwable ex) {
            String message = String.format("appId: %s, cluster:  %s, namespace:  %s, sync remoteRepository error", configUtil.getAppId(), configUtil.getCluster(), this.namespace);
            logger.error("appId: {}, cluster: {}, namespace: {}, sync remoteRepository error， {}", configUtil.getAppId(), configUtil.getCluster(), this.namespace, ExceptionUtils.getDetailMessage(ex));
            ApolloConfigException apolloConfigException = new ApolloConfigException(message, ex);
            throw apolloConfigException;
        }
    }

    //首先从本地缓存获取，如果没有，进行sync同步，将远程数据同步到本地缓存
    @Override
    public Properties getProperty() {
        if(configCaches .get() == null) {
            //同步并且缓存
            this.sync();
        }
        return this.transformApolloConfigToProperties(this.configCaches.get());
    }

    @Override
    public void setUpstreamRepository(ConfigRepository upstreamRepository) {

    }

    //将ApolloConfig转为Properties
    private Properties transformApolloConfigToProperties(ApolloConfig apolloConfig) {
        Properties result = new Properties();
        result.putAll(apolloConfig.getConfigurations());
        return result;
    }

    @Override
    public ConfigSourceType getSourceType() {
        return ConfigSourceType.REMOTE;
    }


    //获取ServiceDTO的list
    private List<ServiceDTO> getConfigServices() {
        List<ServiceDTO> services = serviceLocator.getConfigServices();
        if (services.size() == 0) {
            throw new ApolloConfigException("No available config service");
        }
        return services;
    }

    //detail url 拼装
    private String assembleQueryConfigUrl(String uri, String appId, String cluster, String namespace, String dataCenter, ApolloNotificationMessages remoteMessages, ApolloConfig previousConfig) {
        String path = "configs/%s/%s/%s";
        List<String> pathParams = Lists.newArrayList(pathEscaper.escape(appId), pathEscaper.escape(cluster), pathEscaper.escape(namespace));
        Map<String, String> queryParams = Maps.newHashMap();
        if (previousConfig != null) {
            queryParams.put("releaseKey", queryParamEscaper.escape(previousConfig.getReleaseKey()));
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
