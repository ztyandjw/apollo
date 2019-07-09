package com.fiveonevr.apollo.client.internals;

public class ApolloConfigException extends RuntimeException {
    public ApolloConfigException(String message) {
        super(message);
    }

    public ApolloConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
