package com.fiveonevr.apollo.client.build;

import com.fiveonevr.apollo.client.exceptions.ApolloConfigException;
import com.fiveonevr.apollo.client.internals.ConfigManager;
import com.fiveonevr.apollo.client.internals.ConfigRegistry;
import com.fiveonevr.apollo.client.internals.DefaultConfigFactoryManager;
import com.fiveonevr.apollo.client.internals.DefaultConfigManager;
import com.fiveonevr.apollo.client.spi.ConfigFactory;
import com.fiveonevr.apollo.client.spi.ConfigFactoryManager;
import com.fiveonevr.apollo.client.spi.DefaultConfigFactory;
import com.fiveonevr.apollo.client.spi.DefaultConfigRegistry;
import com.fiveonevr.apollo.client.util.ConfigUtil;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Singleton;

public class DefaultInjector implements Injector {
    private com.google.inject.Injector injector;

  public DefaultInjector() {
    try {
      injector = Guice.createInjector(new ApolloModule());
    } catch (Throwable ex) {
      ApolloConfigException exception = new ApolloConfigException("Unable to initialize Guice Injector!", ex);
//      Tracer.logError(exception);
      throw exception;
    }
  }

  @Override
  public <T> T getInstance(Class<T> clazz) {
    try {
      return injector.getInstance(clazz);
    } catch (Throwable ex) {
//      Tracer.logError(ex);
      throw new ApolloConfigException(
          String.format("Unable to load instance for %s!", clazz.getName()), ex);
    }
  }

  @Override
  public <T> T getInstance(Class<T> clazz, String name) {
    //Guice does not support get instance by type and name
    return null;
  }

  private static class ApolloModule extends AbstractModule {
    @Override
    protected void configure() {
        //Config的管理类，从该类ConfigFactoryManager->ConfigFactory->Config
        bind(ConfigManager.class).to(DefaultConfigManager.class).in(Singleton.class);
        //configFactory的管理类,从该类获取ConfigRegistry->ConfigFactory
        bind(ConfigFactoryManager.class).to(DefaultConfigFactoryManager.class).in(Singleton.class);
        //namespace与configfactory的映射
       bind(ConfigRegistry.class).to(DefaultConfigRegistry.class).in(Singleton.class);
        bind(ConfigFactory.class).to(DefaultConfigFactory.class).in(Singleton.class);
        bind(ConfigUtil.class).in(Singleton.class);
//      bind(HttpUtil.class).in(Singleton.class);
//      bind(ConfigServiceLocator.class).in(Singleton.class);
//      bind(RemoteConfigLongPollService.class).in(Singleton.class);
//      bind(YamlParser.class).in(Singleton.class);
    }
  }
}
