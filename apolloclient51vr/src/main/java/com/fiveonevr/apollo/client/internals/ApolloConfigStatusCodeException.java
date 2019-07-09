package com.fiveonevr.apollo.client.internals;

public class ApolloConfigStatusCodeException extends RuntimeException{
    private final int statusCode;

    public ApolloConfigStatusCodeException(int statusCode, String message) {
        super(String.format("[status code: %d] %s", statusCode, message));
        this.statusCode = statusCode;
    }

    public ApolloConfigStatusCodeException(int statusCode, Throwable cause) {
        super(cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
