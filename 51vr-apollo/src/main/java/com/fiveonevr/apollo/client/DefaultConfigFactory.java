package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.enums.ConfigFileFormat;
import com.fiveonevr.apollo.client.utils.DefaultConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultConfigFactory implements ConfigFactory{

    private static final Logger logger = LoggerFactory.getLogger(DefaultConfigFactory.class);



    @Override
    public Config create(String namespace) {
        ConfigFileFormat format = determineFileFormat(namespace);
        return new DefaultConfig(namespace, createLocalConfigRepository(namespace));

    }

    LocalFileConfigRepository createLocalConfigRepository(String namespace) {
        return new LocalFileConfigRepository(namespace, createRemoteConfigRepository(namespace));
    }

    RemoteConfigRepository createRemoteConfigRepository(String namespace) {
        return new RemoteConfigRepository(namespace);
    }


    //根据namespace判断属于哪种configfileformat
    ConfigFileFormat determineFileFormat(String namespace) {
        String lowerCase = namespace.toLowerCase();
        for(ConfigFileFormat configFileFormat : ConfigFileFormat.values()) {
            if(lowerCase.endsWith("." + configFileFormat.getValue())) {
                return configFileFormat;
            }
        }
        return ConfigFileFormat.Properties;
    }
}
