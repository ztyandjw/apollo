package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.build.ApolloInjector;
import com.google.common.collect.Maps;

import java.util.Map;

//通过此类获取ConfigFactory
public class DefaultConfigFactoryManager implements  ConfigFactoryManager{

    //通过ConfigRegistry获取ConfigFactory
    private final ConfigRegistry configRegistry;
    //ConfigFactory的缓存
    private Map<String, ConfigFactory> caches = Maps.newConcurrentMap();


    public DefaultConfigFactoryManager() {
        //通过guava获取configRegistry的实例
        this.configRegistry = ApolloInjector.getInstance(ConfigRegistry.class);
    }


    @Override
    public ConfigFactory getConfigFactory(String namespace) {
        //首先查看registry能否获取到
        ConfigFactory configFactory = configRegistry.getFactory(namespace);
        if(configFactory != null) {
            return configFactory;
        }
        //再次查看本地缓存能否获取到
        configFactory = caches.get(namespace);
        if(configFactory != null) {
            return configFactory;
        }
        //都拿不到，通过guava获取实例
        configFactory = ApolloInjector.getInstance(ConfigFactory.class);
        caches.put(namespace, configFactory);
        return configFactory;
    }
}
