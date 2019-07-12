package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.internals.Config;

public class Local {
    public static void main(String[] args) throws InterruptedException {
        Config config = ConfigService.getAppConfig(); //config instance is singleton for each namespace and is never null
        String someKey = "DEBUG";
        String someDefaultValue = "local";

        String value = config.getProperty(someKey, someDefaultValue);
        System.out.println(value);
        String value1 = config.getProperty(someKey, someDefaultValue);
        System.out.println(value1);
    }


}
