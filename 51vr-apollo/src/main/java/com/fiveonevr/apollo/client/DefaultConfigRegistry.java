package com.fiveonevr.apollo.client;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

//ConfigRegistry实现类
//缓存namespace与configFactory的kv
public class DefaultConfigRegistry implements  ConfigRegistry{

    private static final Logger logger = LoggerFactory.getLogger(DefaultConfigRegistry.class);
    private Map<String, ConfigFactory> caches = Maps.newConcurrentMap();



    @Override
    public void register(String namespace, ConfigFactory configFactory) {
        if(caches.containsKey(namespace)) {
            logger.warn("ConfigFactory({}) is overridden by {}!", namespace, configFactory.getClass());
        }
        caches.put(namespace, configFactory);
    }

    @Override
    public ConfigFactory getFactory(String namespace) {
        return caches.get(namespace);
    }
}
