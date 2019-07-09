package com.fiveonevr.apollo.client.internals;

public class HttpResponse<T> {
    private final int statusCode;
    private final T responseBody;

    public HttpResponse(int statusCode, T body) {
        this.statusCode = statusCode;
        this.responseBody = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public T getBody() {
        return responseBody;
    }
}