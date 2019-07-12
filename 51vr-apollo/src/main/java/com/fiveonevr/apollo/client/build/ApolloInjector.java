package com.fiveonevr.apollo.client.build;


import com.fiveonevr.apollo.client.exceptions.ApolloConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/12.
 */

//ApolloInjector门面类
public class ApolloInjector {
    private static final Logger logger = LoggerFactory.getLogger(ApolloInjector.class);

    private static volatile  Injector injector;

    private static  final Object lock = new Object();

    //获取Injector实现类
    private static Injector getInjector() {
        if(injector ==null) {
            synchronized (lock) {
                try {
                    injector = ServiceBootstrap.loadFirst(Injector.class);
                }catch (Throwable ex) {
                    ApolloConfigException exception = new ApolloConfigException("Unable to initialize Apollo Injector!", ex);
                    logger.error(ex.toString());
                    throw ex;
                }
            }
        }
        return injector;
    }

    public static <T> T getInstance(Class<T> clazz) {
        try {
            return getInjector().getInstance(clazz);
        }catch (Throwable ex) {

        }
    }

}
