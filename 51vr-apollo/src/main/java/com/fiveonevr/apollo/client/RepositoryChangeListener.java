package com.fiveonevr.apollo.client;

import java.util.Properties;

public interface RepositoryChangeListener {
    void onRepositoryChange(String namespace, Properties newProperties);
}
