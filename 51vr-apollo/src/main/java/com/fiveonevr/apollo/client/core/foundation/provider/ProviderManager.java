package com.fiveonevr.apollo.client.core.foundation.provider;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/16.
 */

public interface ProviderManager {

    String getProperty(String name, String defaultValue);

    <T extends Provider> T provider(Class<T> clazz);
}
