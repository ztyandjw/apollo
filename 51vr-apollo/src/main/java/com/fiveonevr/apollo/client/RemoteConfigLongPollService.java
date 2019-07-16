package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.build.ApolloInjector;
import com.fiveonevr.apollo.client.constants.ConfigConsts;
import com.fiveonevr.apollo.client.core.ExponentialSchedulePolicy;
import com.fiveonevr.apollo.client.core.SchedulePolicy;
import com.fiveonevr.apollo.client.enums.ConfigFileFormat;
import com.fiveonevr.apollo.client.exceptions.ApolloConfigException;
import com.fiveonevr.apollo.client.model.ApolloConfigNotification;
import com.fiveonevr.apollo.client.model.ApolloNotificationMessages;
import com.fiveonevr.apollo.client.model.ServiceDTO;
import com.fiveonevr.apollo.client.utils.*;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/15.
 */

public class RemoteConfigLongPollService {
    //是否已经开始了
    private final AtomicBoolean longPollStarted;
    private static final Joiner STRING_JOINER = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR);
    private static final long INIT_NOTIFICATION_ID = ConfigConsts.NOTIFICATION_ID_PLACEHOLDER;
    private SchedulePolicy longPollFailSchedulePolicyInSecond;
    private final ExecutorService longpoolService;
    private static final Logger logger = LoggerFactory.getLogger(RemoteConfigLongPollService.class);
    private final RateLimiter longpollLimiter;
    private static final Escaper queryParamEscaper = UrlEscapers.urlFormParameterEscaper();
    private static final Joiner.MapJoiner MAP_JOINER = Joiner.on("&").withKeyValueSeparator("=");
    private static final String hostPort = "http://10.100.100.20:8080";
    private final ConcurrentMap<String, Long> notifications;
    private final ConcurrentMap<String, ApolloNotificationMessages> remoteNotificationMessages;
    private Gson gson;
    private Type longPollResponseType;
    private final Multimap<String, RemoteConfigRepository> longPollNamespaces;
    private final HttpUtil httpUtil;
    private ConfigUtil configUtil;
    private ConfigServiceLocator configServiceLocator;
    public RemoteConfigLongPollService() {
        longPollFailSchedulePolicyInSecond = new ExponentialSchedulePolicy(1, 120); //in second

        this.configServiceLocator = ApolloInjector.getInstance(ConfigServiceLocator.class);
        this.configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        longPollStarted = new AtomicBoolean(false);
        //single线程池
        longpoolService = Executors.newSingleThreadExecutor();
        longpollLimiter = RateLimiter.create(2);
        gson = new Gson();
        notifications = Maps.newConcurrentMap();
        longPollNamespaces = Multimaps.synchronizedSetMultimap(HashMultimap.<String, RemoteConfigRepository>create());
        httpUtil = ApolloInjector.getInstance(HttpUtil.class);
        longPollResponseType = new TypeToken<List<ApolloConfigNotification>>() {
        }.getType();
        remoteNotificationMessages = Maps.newConcurrentMap();

    }

    public boolean submit(String namespace, RemoteConfigRepository remoteConfigRepository) {
        boolean added = longPollNamespaces.put(namespace, remoteConfigRepository);
        notifications.putIfAbsent(namespace, INIT_NOTIFICATION_ID);
        if (!longPollStarted.get()) {
            startLongPolling();
        }
        return added;
    }

    private void startLongPolling() {
        //原先为false，expect为false，update为true，返回true，反之返回false
        if(!longPollStarted.compareAndSet(false, true)) {
            return;
        }
        try {
            final String appId = configUtil.getAppId();
            final String cluster = configUtil.getCluster();
            final String datacenter = configUtil.getDataCenter();
            final long longPollingInitialDelayInMills = configUtil.getLongPollingInitialDelayInMills(); //2 second
            longpoolService.submit(() -> {
                if(longPollingInitialDelayInMills > 0) {
                    try {
                        logger.debug("Long polling will start in {} ms.", longPollingInitialDelayInMills);
                        TimeUnit.MILLISECONDS.sleep(longPollingInitialDelayInMills);
                    }catch (InterruptedException e) {
                        //ignore
                    }
                }
                doLongPollingRefresh(appId, cluster, datacenter);
            });
        }catch (Throwable ex) {
            longPollStarted.set(false);
            ApolloConfigException exception = new ApolloConfigException("Schedule long polling refresh failed", ex);
            logger.warn(ExceptionUtils.getDetailMessage(exception));
        }
    }

    private List<ServiceDTO> getConfigServices() {
        List<ServiceDTO> services = configServiceLocator.getConfigServices();
        if (services.size() == 0) {
            throw new ApolloConfigException("No available config service");
        }

        return services;
    }

    String assembleLongPollRefreshUrl(String uri, String appId, String cluster, String dataCenter,
                                      Map<String, Long> notificationsMap) {
        Map<String, String> queryParams = Maps.newHashMap();
        queryParams.put("appId", queryParamEscaper.escape(appId));
        queryParams.put("cluster", queryParamEscaper.escape(cluster));
        queryParams
                .put("notifications", queryParamEscaper.escape(assembleNotifications(notificationsMap)));

        if (!Strings.isNullOrEmpty(dataCenter)) {
            queryParams.put("dataCenter", queryParamEscaper.escape(dataCenter));
        }
        String localIp = configUtil.getLocalIp();
        if (!Strings.isNullOrEmpty(localIp)) {
            queryParams.put("ip", queryParamEscaper.escape(localIp));
        }

        String params = MAP_JOINER.join(queryParams);
        if (!uri.endsWith("/")) {
            uri += "/";
        }

        return uri + "notifications/v2?" + params;
    }

    //正式开始poll
    public void doLongPollingRefresh(String appId, String clusterName, String dataCenter) {
        final Random random = new Random();
        ServiceDTO lastServiceDTO  = null;
        while (this.longPollStarted.get() && !Thread.currentThread().isInterrupted()) {
            if(longpollLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
                try {
                    TimeUnit.SECONDS.sleep(5);
                }catch (InterruptedException ex) {
                    //ignore
                }
            }

            String url = null;
            try {
                if(lastServiceDTO  == null) {
                    List<ServiceDTO> configServices = getConfigServices();
                    lastServiceDTO = configServices.get(random.nextInt(configServices.size()));
                }
                url = assembleLongPollRefreshUrl(lastServiceDTO.getHomepageUrl(), appId, clusterName, dataCenter,
                                notifications);
                logger.debug("Long polling from {}", url);
                HttpRequest request = new HttpRequest(url);
                request.setReadTimeout(90 * 1000);
                final HttpResponse<List<ApolloConfigNotification>> response =  httpUtil.doGet(request, longPollResponseType);
                logger.debug("Long polling response: {}, url: {}", response.getStatusCode(), url);

                if(response.getStatusCode() == 200 && response.getBody() != null) {
                    this.updateNotifications(response.getBody());
                    this.updateRemoteNotifications(response.getBody());
                    this.notify(response.getBody());
                }

                if(response.getStatusCode() == 304 && random.nextBoolean()) {
                    lastServiceDTO = null;
                }
                longPollFailSchedulePolicyInSecond.success();
            }catch (Throwable ex) {
                lastServiceDTO = null;
                long sleepTimeInSecond = longPollFailSchedulePolicyInSecond.fail();
                logger.warn(
                        "Long polling failed, will retry in {} seconds. appId: {}, cluster: {}, namespaces: {}, long polling url: {}, reason: {}",
                        sleepTimeInSecond, appId, clusterName, assembleNamespaces(), url, ExceptionUtils.getDetailMessage(ex));
                try {
                    TimeUnit.SECONDS.sleep(sleepTimeInSecond);
                } catch (InterruptedException ie) {
                    //ignore
                }
            }finally {

            }
        }
    }

    private String assembleNamespaces() {
        return STRING_JOINER.join(longPollNamespaces.keySet());
    }

    private void updateRemoteNotifications(List<ApolloConfigNotification> deltaNotifications) {
        for(ApolloConfigNotification notification: deltaNotifications) {
            if(Strings.isNullOrEmpty(notification.getNamespaceName())) {
                continue;
            }
            if(notification.getMessages() == null || notification.getMessages().isEmpty()) {
                continue;
            }
            ApolloNotificationMessages localRemoteMessages = remoteNotificationMessages.get(notification.getNotificationId());
            if(localRemoteMessages == null) {
                localRemoteMessages = new ApolloNotificationMessages();
                remoteNotificationMessages.put(notification.getNamespaceName(), localRemoteMessages);
            }
            localRemoteMessages.mergeFrom(notification.getMessages());
        }
    }

    private void updateNotifications(List<ApolloConfigNotification> deltaNotifications) {
        for (ApolloConfigNotification notification : deltaNotifications) {
            if (Strings.isNullOrEmpty(notification.getNamespaceName())) {
                continue;
            }
            String namespaceName = notification.getNamespaceName();
            if (notifications.containsKey(namespaceName)) {
                notifications.put(namespaceName, notification.getNotificationId());
            }
            String namespaceNameWithPropertiesSuffix =
                    String.format("%s.%s", namespaceName, ConfigFileFormat.Properties.getValue());
            if (notifications.containsKey(namespaceNameWithPropertiesSuffix)) {
                notifications.put(namespaceNameWithPropertiesSuffix, notification.getNotificationId());
            }
        }
    }


    //notify remoteconfigRepository
    private void notify(List<ApolloConfigNotification> deltaNotifications) {
        if(deltaNotifications == null || notifications.isEmpty()) {
            return;
        }
        for(ApolloConfigNotification notification: deltaNotifications) {
            String namespaceName = notification.getNamespaceName();
            List<RemoteConfigRepository> tobeNotified = Lists.newArrayList(longPollNamespaces.get(namespaceName));
            ApolloNotificationMessages originalMessages = remoteNotificationMessages.get(namespaceName);
            ApolloNotificationMessages remoteMessages = originalMessages == null ? null : originalMessages.clone();
            tobeNotified.addAll(longPollNamespaces.get(String.format("%s.%s", namespaceName, ConfigFileFormat.Properties.getValue())));
            for(RemoteConfigRepository remoteConfigRepository: tobeNotified) {
                try {
                    //todo
                }catch (Throwable ex) {
                    logger.error(ex.toString());
                }
            }
        }
    }


    String assembleNotifications(Map<String, Long> notificationsMap) {
        List<ApolloConfigNotification> notifications = Lists.newArrayList();
        for (Map.Entry<String, Long> entry : notificationsMap.entrySet()) {
            ApolloConfigNotification notification = new ApolloConfigNotification(entry.getKey(), entry.getValue());
            notifications.add(notification);
        }
        return gson.toJson(notifications);
    }


}
