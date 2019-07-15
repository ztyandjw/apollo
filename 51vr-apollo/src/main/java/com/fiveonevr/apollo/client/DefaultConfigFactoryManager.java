package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.build.ApolloInjector;
import com.fiveonevr.apollo.client.build.spi.DefaultApolloInjector;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

//ConfigFactory管理类的实现类
//通过registry-> configfactory-> config
public class DefaultConfigFactoryManager implements  ConfigFactoryManager{

    private static final Logger logger = LoggerFactory.getLogger(DefaultConfigFactoryManager.class);
    private ConfigRegistry configRegistry;
    private Map<String, ConfigFactory> caches = Maps.newConcurrentMap();

    //
    public DefaultConfigFactoryManager() {
        ApolloInjector.getInstance(ConfigRegistry.class);
    }


    @Override
    public ConfigFactory getConfigFactory(String namespace) {
        ConfigFactory configFactory = configRegistry.getFactory(namespace);
        if(configFactory != null) {
            return configFactory;
        }
        configFactory = caches.get(namespace);

        if(configFactory != null) {
            return configFactory;
        }

        configFactory = ApolloInjector.getInstance(ConfigFactory.class);
        caches.put(namespace, configFactory);
        return configFactory;
    }
}
