package com.fiveonevr.apollo.client;
/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/12.
 */

public interface ConfigFactoryManager {
    //定义获取ConfigFactory
    ConfigFactory getConfigFactory(String namespace);
}
