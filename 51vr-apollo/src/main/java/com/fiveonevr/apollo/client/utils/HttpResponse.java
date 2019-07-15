package com.fiveonevr.apollo.client.utils;

//HttpResponse<T> Object
public class HttpResponse<T> {
  private final int statusCode;
  private final T body;

  public HttpResponse(int statusCode, T body) {
    this.statusCode = statusCode;
    this.body = body;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public T getBody() {
    return body;
  }
}
