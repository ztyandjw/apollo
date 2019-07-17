package com.fiveonevr.apollo.client.model;

import java.util.Map;

//一次发布config
public class ApolloConfig {
    private String appId;
    private String cluster;
    private String namespace;
    private Map<String,String> configurations;
    private String releaseKey;

    public ApolloConfig() {
    }

    public ApolloConfig(String appId, String cluster, String namespace, Map<String, String> configurations, String release) {
        this.appId = appId;
        this.cluster = cluster;
        this.namespace = namespace;
        this.configurations = configurations;
        this.releaseKey = release;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Map<String, String> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Map<String, String> configurations) {
        this.configurations = configurations;
    }

    public String getReleaseKey() {
        return releaseKey;
    }

    public void setRelease(String release) {
        this.releaseKey = release;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApolloConfig{");
        sb.append("appId='").append(appId).append('\'');
        sb.append(", cluster='").append(cluster).append('\'');
        sb.append(", namespaceName='").append(namespace).append('\'');
        sb.append(", configurations=").append(configurations);
        sb.append(", releaseKey='").append(releaseKey).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
