package com.fiveonevr.apollo.client.internals;

import com.fiveonevr.apollo.client.build.ApolloInjector;
import com.fiveonevr.apollo.client.spi.ConfigFactory;
import com.fiveonevr.apollo.client.spi.ConfigFactoryManager;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DefaultConfigManager implements ConfigManager {
  private ConfigFactoryManager configFactoryManager;

  private Map<String, Config> configs = Maps.newConcurrentMap();
  private Map<String, ConfigFile> configFiles = Maps.newConcurrentMap();

  public DefaultConfigManager() {
    configFactoryManager = ApolloInjector.getInstance(ConfigFactoryManager.class);
  }

  @Override
  public Config getConfig(String namespace) {
    Config config = configs.get(namespace);

    if (config == null) {
      synchronized (this) {
        config = configs.get(namespace);

        if (config == null) {
          ConfigFactory factory = configFactoryManager.getFactory(namespace);

          config = factory.create(namespace);
          configs.put(namespace, config);
        }
      }
    }

    return config;
  }

  @Override
  public ConfigFile getConfigFile(String namespace, ConfigFileFormat configFileFormat) {
    String namespaceFileName = String.format("%s.%s", namespace, configFileFormat.getValue());
    ConfigFile configFile = configFiles.get(namespaceFileName);

    if (configFile == null) {
      synchronized (this) {
        configFile = configFiles.get(namespaceFileName);

        if (configFile == null) {
          ConfigFactory factory = configFactoryManager.getFactory(namespaceFileName);

          configFile = factory.createConfigFile(namespaceFileName, configFileFormat);
          configFiles.put(namespaceFileName, configFile);
        }
      }
    }
    return configFile;
  }
}
