package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.build.ApolloInjector;
import com.fiveonevr.apollo.client.core.ExponentialSchedulePolicy;
import com.fiveonevr.apollo.client.core.SchedulePolicy;
import com.fiveonevr.apollo.client.enums.ConfigSourceType;
import com.fiveonevr.apollo.client.exceptions.ApolloConfigException;
import com.fiveonevr.apollo.client.exceptions.ApolloConfigStatusCodeException;
import com.fiveonevr.apollo.client.model.ApolloConfig;
import com.fiveonevr.apollo.client.utils.ExceptionUtils;
import com.fiveonevr.apollo.client.utils.HttpRequest;
import com.fiveonevr.apollo.client.utils.HttpResponse;
import com.fiveonevr.apollo.client.utils.HttpUtil;
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

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RemoteConfigRepository extends  AbstractConfigRepository{
    private final AtomicBoolean configNeedForceRefresh;
    private static final Escaper pathEscaper = UrlEscapers.urlPathSegmentEscaper();
    private static final Escaper queryParamEscaper = UrlEscapers.urlFormParameterEscaper();
    private static final Logger logger = LoggerFactory.getLogger(RemoteConfigRepository.class);
    private static final Joiner.MapJoiner MAP_JOINER = Joiner.on("&").withKeyValueSeparator("=");
    private final static ScheduledExecutorService executorService;
    private final HttpUtil httpUtil;
    private volatile AtomicReference<ApolloConfig> configCaches;
    private final String namespace;
    private final RateLimiter loadConfigRateLimiter;
    private final int loadConfigQPS = 2;
    private final Gson gson;
    private final String appId = "001";
    private final String cluster = "default";
    private final String datacenter = null;
    private final String hostAndPort = "10.100.100.20:8080";
    private final SchedulePolicy loadConfigFailSchedulePolicy;

    static {
        executorService = Executors.newScheduledThreadPool(1);
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
        //这里固定住了，理论上应该通过不同provider获取配置参数
        String appId = this.appId;
        String cluster = this.cluster;
        //String datacenter = this.datacenter;
        //就让他重试2次
        int maxRetries = 2;
        //睡眠时间，默认0
        long onErrorSleepTime = 0;
        Throwable exception = null;
        String url = assembleQueryConfigUrl(appId, cluster, this.namespace);
        logger.debug("Loading config from {}", url);
        for (int i = 0; i < maxRetries; i++) {
            if(onErrorSleepTime > 0)  {
                logger.warn("load config failed, will retry in {} {}. appId: {}, cluster: {}, namespace: {}",
                        onErrorSleepTime, TimeUnit.SECONDS, appId, cluster, namespace);
                try {
                    TimeUnit.SECONDS.sleep(onErrorSleepTime);
                }catch (InterruptedException e) {
                    //ignore
                }
            }
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
        String message = String.format(
                "Load Apollo Config failed - appId: %s, cluster: %s, namespace: %s, url: %s",
                appId, cluster, namespace, url);
        throw new ApolloConfigException(message, exception);
    }

    private void schedulePeridicRefresh() {
        logger.debug("Schedule periodc refresh with interval: {} {}", 5, TimeUnit.MINUTES);
        executorService.scheduleAtFixedRate(() -> trySync(),5, 5, TimeUnit.MINUTES);
    }


    public RemoteConfigRepository(String namespace) {
        this.httpUtil = ApolloInjector.getInstance(HttpUtil.class);
        this.namespace = namespace;
        loadConfigRateLimiter = RateLimiter.create(loadConfigQPS);
        configNeedForceRefresh = new AtomicBoolean(true);
        gson = new Gson();
        this.loadConfigFailSchedulePolicy = new ExponentialSchedulePolicy(1L, 8L);
        //获取apolloconfig，如果更新了，刷新本地缓存
        this.trySync();
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
            String message = String.format("appId: %s, cluster:  %s, namespace:  %s, sync remoteRepository error", this.appId, this.cluster, this.namespace);
            logger.error("appId: {}, cluster: {}, namespace: {}, sync remoteRepository error， {}", this.appId, this.cluster, this.namespace, ExceptionUtils.getDetailMessage(ex));
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


    //获取config请求url
    String assembleQueryConfigUrl(String appId, String cluster, String namespace) {
        String path = "http://" + hostAndPort + "/configs/%s/%s/%s";
        List<String> pathParams =
                Lists.newArrayList(pathEscaper.escape(appId), pathEscaper.escape(cluster),
                        pathEscaper.escape(namespace));
        String pathExpanded = String.format(path, pathParams.toArray());
        return pathExpanded;
    }
}
