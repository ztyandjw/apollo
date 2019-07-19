package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.enums.ConfigSourceType;

import java.util.Properties;

public interface ConfigRepository {
    //定义获取Properties
    Properties getProperty();
    //定义设置上游repository
    void setUpstreamRepository(ConfigRepository upstreamRepository);
    //定义add监听器
    void addChangeListener(RepositoryChangeListener repositoryChangeListener);
    //定义移除监听器
    void removeChangeListener(RepositoryChangeListener repositoryChangeListener);
    //定义获取ConfigSourceType
    ConfigSourceType getSourceType();
}
