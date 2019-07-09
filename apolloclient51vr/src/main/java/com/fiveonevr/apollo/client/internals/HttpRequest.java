package com.fiveonevr.apollo.client.internals;

public class HttpRequest {
    private String url;
    private int connectTimeout;
    private int readTimeout;

    public HttpRequest(String url) {
        this.url = url;
        this.connectTimeout = -1;
        this.readTimeout = -1;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
}
