package com.fiveonevr.apollo.client;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/12.
 */

public interface ConfigFactory {
    //定义获取Config
    Config create(String namespace);
}
