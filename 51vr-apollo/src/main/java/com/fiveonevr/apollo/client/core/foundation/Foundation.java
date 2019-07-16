package com.fiveonevr.apollo.client.core.foundation;


import com.fiveonevr.apollo.client.build.ServiceBootstrap;
import com.fiveonevr.apollo.client.core.foundation.provider.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Foundation {
  private static final Logger logger = LoggerFactory.getLogger(Foundation.class);
  private static Object lock = new Object();

  private static volatile ProviderManager providerManager;

  // Encourage early initialization and fail early if it happens.
  static {
    getManager();
  }

  private static ProviderManager getManager() {
    try {
      if (providerManager == null) {
        // Double locking to make sure only one thread initializes ProviderManager.
        synchronized (lock) {
          if (providerManager == null) {
            providerManager = ServiceBootstrap.loadFirst(ProviderManager.class);
          }
        }
      }
      return providerManager;
    } catch (Throwable ex) {
      providerManager = new NullProviderManager();
      logger.error("Initialize ProviderManager failed.", ex);
      return providerManager;
    }
  }

  public static String getProperty(String name, String defaultValue) {
    try {
      return getManager().getProperty(name, defaultValue);
    } catch (Throwable ex) {
      logger.error("getProperty for {} failed.", name, ex);
      return defaultValue;
    }
  }

  public static NetworkProvider net() {
    try {
      return getManager().provider(NetworkProvider.class);
    } catch (Exception ex) {
      logger.error("Initialize NetworkProvider failed.", ex);
      return NullProviderManager.provider;
    }
  }

  public static ServerProvider server() {
    try {
      return getManager().provider(ServerProvider.class);
    } catch (Exception ex) {
      logger.error("Initialize ServerProvider failed.", ex);
      return NullProviderManager.provider;
    }
  }

  public static ApplicationProvider app() {
    try {
      return getManager().provider(ApplicationProvider.class);
    } catch (Exception ex) {
      logger.error("Initialize ApplicationProvider failed.", ex);
      return NullProviderManager.provider;
    }
  }
}
