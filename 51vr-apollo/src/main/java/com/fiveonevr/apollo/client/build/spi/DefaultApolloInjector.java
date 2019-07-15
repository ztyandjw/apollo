package com.fiveonevr.apollo.client.build.spi;

import com.fiveonevr.apollo.client.build.Injector;
import com.fiveonevr.apollo.client.exceptions.ApolloConfigException;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//apolloInjector实现类，通过spi初始化
public class DefaultApolloInjector implements Injector {
    private static final Logger logger = LoggerFactory.getLogger(DefaultApolloInjector.class);

    private com.google.inject.Injector googleInjector;

    public DefaultApolloInjector (){
        try {
            this.googleInjector = Guice.createInjector(new ApolloModule());
        }catch (Throwable ex) {
            ApolloConfigException exception = new ApolloConfigException("Unable to initialize Apollo Injector!", ex);
            logger.error(ex.toString());
            throw exception;
        }
    }

    @Override
    public <T> T getInstance(Class<T> clazz) {
        try {
            return googleInjector.getInstance(clazz);
        } catch (Throwable ex) {
            logger.error(ex.toString());
            throw new ApolloConfigException(String.format("Unable to load instance for %s!", clazz.getName()), ex);
        }
    }

    private static class ApolloModule extends AbstractModule {

        @Override
        protected void configure() {
            //bind some clazz
        }
    }

}
