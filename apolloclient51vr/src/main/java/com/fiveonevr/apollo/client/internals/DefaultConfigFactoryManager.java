package com.fiveonevr.apollo.client.internals;

import com.fiveonevr.apollo.client.build.ApolloInjector;
import com.fiveonevr.apollo.client.spi.ConfigFactory;
import com.fiveonevr.apollo.client.spi.ConfigFactoryManager;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
//默认的ConfigFactoryManager
public class DefaultConfigFactoryManager implements ConfigFactoryManager {
  private ConfigRegistry configRegistry;

  private Map<String, ConfigFactory> factories = Maps.newConcurrentMap();

  public DefaultConfigFactoryManager() {
    configRegistry = ApolloInjector.getInstance(ConfigRegistry.class);
  }

  @Override
  public ConfigFactory getFactory(String namespace) {
    // step 1: check hacked factory
    ConfigFactory factory = configRegistry.getFactory(namespace);

    if (factory != null) {
      return factory;
    }

    // step 2: check cache
    factory = factories.get(namespace);

    if (factory != null) {
      return factory;
    }

    // step 3: check declared config factory
    factory = ApolloInjector.getInstance(ConfigFactory.class, namespace);

    if (factory != null) {
      return factory;
    }

    // step 4: check default config factory
    factory = ApolloInjector.getInstance(ConfigFactory.class);

    factories.put(namespace, factory);

    // factory should not be null
    return factory;
  }
}
