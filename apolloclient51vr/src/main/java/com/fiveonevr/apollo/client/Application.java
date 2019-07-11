package com.fiveonevr.apollo.client;


import com.fiveonevr.apollo.client.spring.annotation.TimApollo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@TimApollo
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
