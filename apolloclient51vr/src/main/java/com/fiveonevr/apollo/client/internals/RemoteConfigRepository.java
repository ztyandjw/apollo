package com.fiveonevr.apollo.client.internals;


import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;
import com.google.common.util.concurrent.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class RemoteConfigRepository {
    private final RateLimiter loadConfigRateLimiter;
    private final ConfigServiceLocator serviceLocator;
    private HttpUtil httpUtil;
    private final String namespace;

    private volatile AtomicReference<ApolloConfig> configCaches;

    public RemoteConfigRepository(String namespace) {
        this.namespace = namespace;
        loadConfigRateLimiter = RateLimiter.create(2);
        serviceLocator = new ConfigServiceLocator();
        httpUtil = new HttpUtil();
    }

    //获取serviceDTO
    private List<ServiceDTO> getConfigServices() {
        List<ServiceDTO> services = serviceLocator.getConfigServices();
        if (services.size() == 0) {
            throw new ApolloConfigException("No available config service");
        }
        return services;
    }

    private ApolloConfig loadApolloConfig() {
        //make simple
        String appId = "001";
        String cluster = "default";
        String dataCenter = null;
        int maxRetries = 2;
        long onErrorSleepTime = 0;
        Throwable exception = null;
        //限流器，5秒内只能通过2个
        if (!loadConfigRateLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {

            }
        }
        List<ServiceDTO> configServices = getConfigServices();
        String url = null;
        for (int i = 0; i < maxRetries; i++) {
            List<ServiceDTO> randomConfigServices = Lists.newLinkedList(configServices);
            Collections.shuffle(randomConfigServices);
            //todo long pool

            for (ServiceDTO configService : randomConfigServices) {
                url = assembleQueryConfigUrl();
                HttpRequest request = new HttpRequest(url);
                try {
                    HttpResponse<ApolloConfig> response = httpUtil.doGet(request, ApolloConfig.class);
                    if (response.getStatusCode() == 304) {
                        return configCaches.get();
                    }
                    ApolloConfig result = response.getBody();
                    return result;
                } catch (ApolloConfigStatusCodeException ex) {
                    ApolloConfigStatusCodeException statusCodeException = ex;
                    //config not found
                    if (ex.getStatusCode() == 404) {
                        String message = String.format(
                                "Could not find config for namespace - appId: %s, cluster: %s, namespace: %s, " +
                                        "please check whether the configs are released in Apollo!",
                                appId, cluster, namespace);
                        statusCodeException = new ApolloConfigStatusCodeException(ex.getStatusCode(),
                                message);
                    }
                    exception = statusCodeException;
                } catch (Throwable ex) {
                    exception = ex;
                } finally {

                }
            }


        }
        String message = String.format(
                "Load Apollo Config failed - appId: %s, cluster: %s, namespace: %s, url: %s",
                appId, cluster, namespace, url);
        throw new ApolloConfigException(message, exception);
    }

    String assembleQueryConfigUrl() {
        return "http://10.100.100.20:8080/configs/001/default/application?ip=10.2.10.11";
    }
}
