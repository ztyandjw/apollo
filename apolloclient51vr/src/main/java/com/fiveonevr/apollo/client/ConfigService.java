package com.fiveonevr.apollo.client;


import com.fiveonevr.apollo.client.build.ApolloInjector;
import com.fiveonevr.apollo.client.constant.ConfigConsts;
import com.fiveonevr.apollo.client.internals.*;
import com.fiveonevr.apollo.client.spi.ConfigFactory;

/**
 * Entry point for client config use
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class ConfigService {
  private static final ConfigService instance = new ConfigService();

  private volatile ConfigManager configManager;
  private volatile ConfigRegistry configRegistry;

  //单例获取configManager，通过ApolloInjector获取
  private ConfigManager getManager() {
    if (configManager == null) {
      synchronized (this) {
        if (configManager == null) {
          configManager = ApolloInjector.getInstance(ConfigManager.class);
        }
      }
    }
    return configManager;
  }

  private ConfigRegistry getRegistry() {
    if (configRegistry == null) {
      synchronized (this) {
        if (configRegistry == null) {
          configRegistry = ApolloInjector.getInstance(ConfigRegistry.class);
        }
      }
    }
    return configRegistry;
  }


  public static Config getAppConfig() {
    return getConfig(ConfigConsts.NAMESPACE_APPLICATION);
  }


  public static Config getConfig(String namespace) {
    return instance.getManager().getConfig(namespace);
  }

  public static ConfigFile getConfigFile(String namespace, ConfigFileFormat configFileFormat) {
    return instance.getManager().getConfigFile(namespace, configFileFormat);
  }

  static void setConfig(Config config) {
    setConfig(ConfigConsts.NAMESPACE_APPLICATION, config);
  }


  static void setConfig(String namespace, final Config config) {
    instance.getRegistry().register(namespace, new ConfigFactory() {
      @Override
      public Config create(String namespace) {
        return config;
      }

      @Override
      public ConfigFile createConfigFile(String namespace, ConfigFileFormat configFileFormat) {
        return null;
      }

    });
  }

  static void setConfigFactory(ConfigFactory factory) {
    setConfigFactory(ConfigConsts.NAMESPACE_APPLICATION, factory);
  }


  static void setConfigFactory(String namespace, ConfigFactory factory) {
    instance.getRegistry().register(namespace, factory);
  }

  static void reset() {
    synchronized (instance) {
      instance.configManager = null;
      instance.configRegistry = null;
    }
  }
}
