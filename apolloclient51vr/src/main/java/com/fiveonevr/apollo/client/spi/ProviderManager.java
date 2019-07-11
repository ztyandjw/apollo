package com.fiveonevr.apollo.client.spi;


import com.fiveonevr.apollo.client.spi.provider.Provider;

public interface ProviderManager {

  String getProperty(String name, String defaultValue);

  <T extends Provider> T provider(Class<T> clazz);
}
