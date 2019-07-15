package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.Config;
import com.fiveonevr.apollo.client.ConfigFactory;
import com.fiveonevr.apollo.client.ConfigFactoryManager;
import com.fiveonevr.apollo.client.ConfigManager;
import com.fiveonevr.apollo.client.build.ApolloInjector;
import com.google.common.collect.Maps;

import java.util.Map;

//ConfigManager实现类
// config通过configmanager调用获取
public class DefaultConfigManager implements ConfigManager {

    private ConfigFactoryManager configFactoryManager;

    private Map<String, Config> configCaches = Maps.newConcurrentMap();

    public DefaultConfigManager() {
        configFactoryManager = ApolloInjector.getInstance(ConfigFactoryManager.class);
    }



    @Override
    public Config getConfig(String namespace) {
        Config config = configCaches.get(namespace);
        if(config == null) {
            synchronized (this) {
                config = configCaches.get(namespace);
                if(config == null) {
                    ConfigFactory factory = configFactoryManager.getConfigFactory(namespace);
                    config = factory.create(namespace);
                    configCaches.put(namespace, config);
                }
            }
        }
        return config;
    }
}
