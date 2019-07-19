package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.build.ApolloInjector;
import com.fiveonevr.apollo.client.constants.ServiceNameConsts;
import com.fiveonevr.apollo.client.core.foundation.Foundation;
import com.fiveonevr.apollo.client.exceptions.ApolloConfigException;
import com.fiveonevr.apollo.client.model.ServiceDTO;
import com.fiveonevr.apollo.client.utils.*;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/16.
 */



public class ConfigServiceLocator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigServiceLocator.class);
    private HttpUtil httpUtil;
    private ConfigUtil configUtil;
    private AtomicReference<List<ServiceDTO>> configServices;
    private Type responseType;
    private ScheduledExecutorService executorService;
    private static final Joiner.MapJoiner MAP_JOINER = Joiner.on("&").withKeyValueSeparator("=");
    private static final Escaper queryParamEscaper = UrlEscapers.urlFormParameterEscaper();


    public List<ServiceDTO> getConfigServices() {
        if (configServices.get().isEmpty()) {
            updateConfigServices();
        }
        return configServices.get();
    }

    public ConfigServiceLocator() {
        List<ServiceDTO> initial = Lists.newArrayList();
        configServices = new AtomicReference<>(initial);
        responseType = new TypeToken<List<ServiceDTO>>() {}.getType();
        httpUtil = ApolloInjector.getInstance(HttpUtil.class);
        configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        executorService = Executors.newScheduledThreadPool(1,
                ApolloThreadFactory.create("ConfigServiceLocator", true));
        initConfigServices();
    }

    private void initConfigServices() {
        // get from run time configurations
        List<ServiceDTO> customizedConfigServices = getCustomizedConfigService();

        if (customizedConfigServices != null) {
            setConfigServices(customizedConfigServices);
            return;
        }

        // update from meta service
        this.tryUpdateConfigServices();
        this.schedulePeriodicRefresh();
    }

    private void schedulePeriodicRefresh() {
        this.executorService.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        logger.debug("refresh config services");
                        tryUpdateConfigServices();
                    }
                }, configUtil.getRefreshInterval(), configUtil.getRefreshInterval(),
                configUtil.getRefreshIntervalTimeUnit());
    }

    private void setConfigServices(List<ServiceDTO> services) {
        configServices.set(services);
        logConfigServices(services);
    }

    private boolean tryUpdateConfigServices() {
        try {
            updateConfigServices();
            return true;
        } catch (Throwable ex) {
            //ignore
        }
        return false;
    }

    private synchronized void updateConfigServices() {
        //http://10.100.100.20:8080/services/config?appId=001&ip=10.2.10.38
        String url = assembleMetaServiceUrl();
        HttpRequest request = new HttpRequest(url);
        int maxRetries = 2;
        Throwable exception = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                HttpResponse<List<ServiceDTO>> response = httpUtil.doGet(request, responseType);
                List<ServiceDTO> services = response.getBody();
                //返回response为null 或者长度为0
                if (services == null || services.isEmpty()) {
                    logConfigService("Empty response!");
                    continue;
                }
                setConfigServices(services);
                return;
            } catch (Throwable ex) {
                logger.error("ApolloConfigException: ", ExceptionUtils.getDetailMessage(ex));
                exception = ex;
            } finally {
                //ignore
            }
            try {
                configUtil.getOnErrorRetryIntervalTimeUnit().sleep(configUtil.getOnErrorRetryInterval());
            } catch (InterruptedException ex) {
                //ignore
            }
        }
        throw new ApolloConfigException(
                String.format("Get config services failed from %s", url), exception);
    }


    private String assembleMetaServiceUrl() {
        String domainName = configUtil.getMetaServerDomainName();
        String appId = configUtil.getAppId();
        String localIp = configUtil.getLocalIp();

        Map<String, String> queryParams = Maps.newHashMap();
        queryParams.put("appId", queryParamEscaper.escape(appId));
        if (!Strings.isNullOrEmpty(localIp)) {
            queryParams.put("ip", queryParamEscaper.escape(localIp));
        }
        return domainName + "/services/config?" + MAP_JOINER.join(queryParams);
    }

    private void logConfigServices(List<ServiceDTO> serviceDtos) {
        for (ServiceDTO serviceDto : serviceDtos) {
            logConfigService(serviceDto.getHomepageUrl());
        }
    }

    private void logConfigService(String serviceUrl) {
        logger.debug("Apollo.Config.Services", serviceUrl);
    }

    private List<ServiceDTO> getCustomizedConfigService() {
        // 1. Get from System Property
        String configServices = System.getProperty("apollo.configService");
        if (Strings.isNullOrEmpty(configServices)) {
            // 2. Get from OS environment variable
            configServices = System.getenv("APOLLO_CONFIGSERVICE");
        }
        if (Strings.isNullOrEmpty(configServices)) {
            // 3. Get from server.properties
            configServices = Foundation.server().getProperty("apollo.configService", null);
        }

        if (Strings.isNullOrEmpty(configServices)) {
            return null;
        }

        logger.warn("Located config services from apollo.configService configuration: {}, will not refresh config services from remote meta service!", configServices);

        // mock service dto list
        String[] configServiceUrls = configServices.split(",");
        List<ServiceDTO> serviceDTOS = Lists.newArrayList();

        for (String configServiceUrl : configServiceUrls) {
            configServiceUrl = configServiceUrl.trim();
            ServiceDTO serviceDTO = new ServiceDTO();
            serviceDTO.setHomepageUrl(configServiceUrl);
            serviceDTO.setAppName(ServiceNameConsts.APOLLO_CONFIGSERVICE);
            serviceDTO.setInstanceId(configServiceUrl);
            serviceDTOS.add(serviceDTO);
        }

        return serviceDTOS;
    }



}
