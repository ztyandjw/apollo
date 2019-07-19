package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.build.ApolloInjector;
import com.google.common.collect.Maps;
import java.util.Map;


//Config管理类，通过此类获取Config
public class DefaultConfigManager implements ConfigManager {
    //为了获取Config需要ConfigFactory，获取ConfigFactory需要ConfigFactoryManager
    private ConfigFactoryManager configFactoryManager;
    //Config缓存类
    private Map<String, Config> configCaches = Maps.newConcurrentMap();



    public DefaultConfigManager() {
        //通过guava获取ConfigManagerFactory实例
        configFactoryManager = ApolloInjector.getInstance(ConfigFactoryManager.class);
    }

    @Override
    public Config getConfig(String namespace) {
        //首先通过本地缓存获取Config
        Config config = configCaches.get(namespace);
        if(config == null) {
            synchronized (this) {
                config = configCaches.get(namespace);
                if(config == null) {
                    //本地没有这个namespace的缓存，说明第一次进来，先拿到ConfigFactory，通过ConfigFactoryManager获取
                    ConfigFactory factory = configFactoryManager.getConfigFactory(namespace);
                    //调用#ConfigFactory.create获取config
                    config = factory.create(namespace);
                    //将config放入缓存
                    configCaches.put(namespace, config);
                }
            }
        }
        return config;
    }
}
