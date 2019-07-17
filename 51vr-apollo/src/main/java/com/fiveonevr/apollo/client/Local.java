package com.fiveonevr.apollo.client;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/17.
 */

public class Local {

    public static void main(String[] args) throws InterruptedException {
        Config config = ConfigService.getAppConfig(); //config instance is singleton for each namespace and is never null
        String someKey = "DEBUG";
        String someDefaultValue = "local";
        String value = config.getProperty(someKey, someDefaultValue);
        System.out.println(value);
        Thread.sleep(20000);
        value = config.getProperty(someKey, someDefaultValue);
        System.out.println(value);

    }



}
