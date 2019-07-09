package com.fiveonevr.apollo.client.internals;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class HttpUtil {
    private int connectTimeout = 1000; //1 second
    private int readTimeout = 5000; //5 seconds
    private Gson gson;

    public HttpUtil() {
        gson = new Gson();
    }
    public <T> HttpResponse<T> doGet(HttpRequest httpRequest, final Class<T> responseType) {
        Function<String, T> convertResponse = (input) -> gson.fromJson(input, responseType);
        return doGetWithSerializeFunction(httpRequest, convertResponse);
    }
    public <T> HttpResponse<T> doGet(HttpRequest httpRequest, final Type responseType) {
        com.google.common.base.Function<String, T> convertResponse = new com.google.common.base.Function<String, T>() {
            @Override
            public T apply(String input) {
                return gson.fromJson(input, responseType);
            }
        };
        return doGetWithSerializeFunction(httpRequest, convertResponse);
    }

    private <T> HttpResponse<T> doGetWithSerializeFunction(HttpRequest httpRequest,
                                                           Function<String, T> serializeFunction) {
        InputStreamReader isr = null;
        InputStreamReader esr = null;
        int statusCode;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(httpRequest.getUrl()).openConnection();
            conn.setRequestMethod("GET");
            int connectTimeout = httpRequest.getConnectTimeout();
            if (connectTimeout < 0) {
                connectTimeout = this.connectTimeout;
            }
            int readTimeout = httpRequest.getReadTimeout();
            if (readTimeout < 0) {
                readTimeout = this.readTimeout;
            }
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.connect();
            statusCode = conn.getResponseCode();
            String response;
            try {
                isr = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                response = CharStreams.toString(isr);
            } catch (IOException ex) {
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    esr = new InputStreamReader(errorStream, StandardCharsets.UTF_8);
                    try {
                        CharStreams.toString(esr);
                    } catch (IOException ioe) {
                    }
                }
                if (statusCode == 200 || statusCode == 304) {
                    throw ex;
                } else {
                    throw new ApolloConfigStatusCodeException(statusCode, ex);
                }
            }
            if (statusCode == 200) {
                return new HttpResponse<>(statusCode, serializeFunction.apply(response));
            }
            if (statusCode == 304) {
                return new HttpResponse<>(statusCode, null);
            }
        } catch (ApolloConfigStatusCodeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new ApolloConfigException("Could not complete get operation", ex);
        } finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException ex) {
                }
            }
            if (esr != null) {
                try {
                    esr.close();
                } catch (IOException ex) {
                }
            }
        }
        throw new ApolloConfigStatusCodeException(statusCode,
                String.format("Get operation failed for %s", httpRequest.getUrl()));
    }


}
