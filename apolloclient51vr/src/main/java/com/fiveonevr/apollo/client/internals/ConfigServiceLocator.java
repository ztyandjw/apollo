package com.fiveonevr.apollo.client.internals;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ConfigServiceLocator {
    private AtomicReference<List<ServiceDTO>> configServices;
    private static final Escaper queryParamEscaper = UrlEscapers.urlFormParameterEscaper();
    private static final Joiner.MapJoiner MAP_JOINER = Joiner.on("&").withKeyValueSeparator("=");
    private Type responseType;
    private HttpUtil httpUtil;

    public ConfigServiceLocator() {
        List<ServiceDTO> initial = Lists.newArrayList();
        configServices = new AtomicReference<>(initial);
        responseType = new TypeToken<List<ServiceDTO>>() {}.getType();
        httpUtil = new HttpUtil();
    }
    public List<ServiceDTO> getConfigServices() {
        if (configServices.get().isEmpty()) {
            updateConfigServices();
        }
        return configServices.get();
    }

    private synchronized void updateConfigServices() {
        String url = assembleMetaServiceUrl();
        HttpRequest request = new HttpRequest(url);
        int maxRetries = 2;
        Throwable exception = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                HttpResponse<List<ServiceDTO>> response = httpUtil.doGet(request, responseType);
                List<ServiceDTO> services = response.getBody();
                if (services == null || services.isEmpty()) {
                    continue;
                }
                setConfigServices(services);
                return;
            } catch (Throwable ex) {
                exception = ex;
            } finally {
            }
//            try {
//                m_configUtil.getOnErrorRetryIntervalTimeUnit().sleep(m_configUtil.getOnErrorRetryInterval());
//            } catch (InterruptedException ex) {
//                //ignore
//            }
        }

        throw new ApolloConfigException(
                String.format("Get config services failed from %s", url), exception);
    }

    //获取metaServiceUrl
    private String assembleMetaServiceUrl() {
        //make it easy
        String domainName = "http://10.100.100.20:8080";
        String appId = "001";
        String localIp = "10.2.10.11";
        Map<String, String> queryParams = Maps.newHashMap();
        queryParams.put("appId", queryParamEscaper.escape(appId));
        if (!Strings.isNullOrEmpty(localIp)) {
            queryParams.put("ip", queryParamEscaper.escape(localIp));
        }
        return domainName + "/services/config?" + MAP_JOINER.join(queryParams);
    }

    private void setConfigServices(List<ServiceDTO> services) {
        configServices.set(services);
    }
}
