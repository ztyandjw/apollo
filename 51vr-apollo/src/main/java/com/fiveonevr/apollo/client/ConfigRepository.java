package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.enums.ConfigSourceType;
import com.sun.org.omg.CORBA.Repository;

import java.util.Properties;

public interface ConfigRepository {
    Properties getProperty();
    void setUpstreamRepository(ConfigRegistry upstreamRepository);
    void addChangeListener(RepositoryChangeListener repositoryChangeListener);
    void removeChangeListener(RepositoryChangeListener repositoryChangeListener);
    ConfigSourceType getSourceType();
}
