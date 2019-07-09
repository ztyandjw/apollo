package com.fiveonevr.apollo.client.internals;
import java.util.Map;

//每次发布
public class ApolloConfig {

    private String appId;

    private String cluster;

    private String namespaceName;

    private Map<String,String> configurations;

    private String releaseKey;

    public ApolloConfig() {
    }

    public ApolloConfig(String appId, String cluster, String namespaceName, String releaseKey) {
        this.appId = appId;
        this.cluster = cluster;
        this.namespaceName = namespaceName;
        this.releaseKey = releaseKey;
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

    public String getNamespaceName() {
        return namespaceName;
    }

    public void setNamespaceName(String namespaceName) {
        this.namespaceName = namespaceName;
    }

    public Map<String, String> getConfiguration() {
        return configurations;
    }

    public void setConfiguration(Map<String, String> configurations) {
        this.configurations = configurations;
    }

    public String getReleaseKey() {
        return releaseKey;
    }

    public void setReleaseKey(String releaseKey) {
        this.releaseKey = releaseKey;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApolloConfig{");
        sb.append("appId='").append(appId).append('\'');
        sb.append(", cluster='").append(cluster).append('\'');
        sb.append(", namespaceName='").append(namespaceName).append('\'');
        sb.append(", configurations=").append(configurations);
        sb.append(", releaseKey='").append(releaseKey).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
