package com.fiveonevr.apollo.client.utils;

//HttpRequest Object
public class HttpRequest {
  private String url;
  private int connectTimeout;
  private int readTimeout;

  /**
   * Create the request for the url.
   * @param url the url
   */
  public HttpRequest(String url) {
    this.url = url;
    connectTimeout = -1;
    readTimeout = -1;
  }

  public String getUrl() {
    return url;
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
