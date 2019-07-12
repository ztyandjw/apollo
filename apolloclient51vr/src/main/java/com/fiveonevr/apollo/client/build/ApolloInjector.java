package com.fiveonevr.apollo.client.build;
import com.fiveonevr.apollo.client.exceptions.ApolloConfigException;
import com.fiveonevr.apollo.client.foundation.ServiceBootstrap;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ApolloInjector {
  private static volatile Injector injector;
  private static final Object lock = new Object();

  private static Injector getInjector() {
    if (injector == null) {
      synchronized (lock) {
        if (injector == null) {
          try {
            injector = ServiceBootstrap.loadFirst(Injector.class);
          } catch (Throwable ex) {
            ApolloConfigException exception = new ApolloConfigException("Unable to initialize Apollo Injector!", ex);
            //Tracer.logError(exception);
            throw exception;
          }
        }
      }
    }
    return injector;
  }

  public static <T> T getInstance(Class<T> clazz) {
    try {
      return getInjector().getInstance(clazz);
    } catch (Throwable ex) {
//      Tracer.logError(ex);
      throw new ApolloConfigException(String.format("Unable to load instance for type %s!", clazz.getName()), ex);
    }
  }

  public static <T> T getInstance(Class<T> clazz, String name) {
    try {
      return getInjector().getInstance(clazz, name);
    } catch (Throwable ex) {
//      Tracer.logError(ex);
      throw new ApolloConfigException(
          String.format("Unable to load instance for type %s and name %s !", clazz.getName(), name), ex);
    }
  }
}
