package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.build.ApolloInjector;
import com.fiveonevr.apollo.client.enums.ConfigSourceType;
import com.fiveonevr.apollo.client.utils.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

public class LocalFileConfigRepository extends AbstractConfigRepository{

    private static final Logger logger = LoggerFactory.getLogger(LocalFileConfigRepository.class);
    private volatile ConfigSourceType sourceType = ConfigSourceType.LOCAL;
    private final static String CONFIG_DIR = "/config-cache";
    private final String namespace;
    private File baseDir;
    private volatile Properties properties;
    private volatile ConfigRepository upstreamRepository;
    private ConfigUtil configUtil;

    public LocalFileConfigRepository(String namespace) {
        this(namespace, null);


    }

    public LocalFileConfigRepository(String namespace, ConfigRepository upstreamRepository) {
        this.namespace = namespace;
        this.configUtil = ApolloInjector.getInstance(ConfigUtil.class);


    }




    @Override
    protected void sync() {



    }


    private boolean trySyncFromUpstream() {

    }


    @Override
    public Properties getProperty() {
        return null;
    }

    @Override
    public void setUpstreamRepository(ConfigRegistry upstreamRepository) {

    }

    @Override
    public ConfigSourceType getSourceType() {
        return null;
    }
}
