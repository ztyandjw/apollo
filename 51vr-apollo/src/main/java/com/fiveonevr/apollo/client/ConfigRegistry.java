package com.fiveonevr.apollo.client;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/12.
 */

public interface ConfigRegistry {
    //定义注册namespace，ConfigFactory的映射
    void register(String namespace, ConfigFactory configFactory);
    //定义获取ConfigFactory
    ConfigFactory getFactory(String namespace);
}
