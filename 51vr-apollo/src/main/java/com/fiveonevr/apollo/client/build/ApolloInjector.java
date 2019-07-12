package com.fiveonevr.apollo.client.build;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/12.
 */

public class ApolloInjector {

    private static volatile  Injector injector;

    private static  final Object lock = new Object();


    private static Injector getInjector() {
        if(injector ==null) {
            synchronized (lock) {

            }
        }
    }

}
