package com.fiveonevr.apollo.client.spring.config;

import com.fiveonevr.apollo.client.ConfigService;
import com.fiveonevr.apollo.client.internals.Config;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

//BeanFactoryPostProcessor
public class PropertySourcesProcessor implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {
    private static final Multimap<Integer, String> NAMESPACE_NAMES = LinkedHashMultimap.create();
    private static final Set<BeanFactory> AUTO_UPDATE_INITIALIZED_BEAN_FACTORIES = Sets.newConcurrentHashSet();


    private ConfigurableEnvironment environment;

    public static boolean addNamespaces(Collection<String> namespaces, int order) {
        return NAMESPACE_NAMES.putAll(order, namespaces);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        initializePropertySources();
        //initializeAutoUpdatePropertiesFeature(beanFactory);
    }

    private void initializePropertySources() {
        ImmutableSortedSet<Integer> orders = ImmutableSortedSet.copyOf(NAMESPACE_NAMES.keySet());
        Iterator<Integer> iterator = orders.iterator();
        while (iterator.hasNext()) {
            int order = iterator.next();
            for (String namespace : NAMESPACE_NAMES.get(order)) {
                Config config = ConfigService.getConfig(namespace);
            }
        }
    }

//    private void ensureBootstrapPropertyPrecedence(ConfigurableEnvironment environment) {
//        MutablePropertySources propertySources = environment.getPropertySources();
//
//        PropertySource<?> bootstrapPropertySource = propertySources
//                .get(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME);
//
//        // not exists or already in the first place
//        if (bootstrapPropertySource == null || propertySources.precedenceOf(bootstrapPropertySource) == 0) {
//            return;
//        }
//
//        propertySources.remove(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME);
//        propertySources.addFirst(bootstrapPropertySource);
//    }



    @Override
    public void setEnvironment(Environment environment) {
        //it is safe enough to cast as all known environment is derived from ConfigurableEnvironment
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public int getOrder() {
        //make it as early as possible
        return Ordered.HIGHEST_PRECEDENCE;
    }

    // for test only
    static void reset() {
        NAMESPACE_NAMES.clear();
        AUTO_UPDATE_INITIALIZED_BEAN_FACTORIES.clear();
    }
}

