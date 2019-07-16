package com.fiveonevr.apollo.client.core.foundation.provider;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/16.
 */

public interface Provider {

    Class<? extends Provider> getType();

    String getProperty(String name, String defaultValue);

    void initialize();
}
