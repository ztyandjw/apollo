package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.enums.ConfigFileFormat;

public class DefaultConfigFactory implements ConfigFactory{



    //Config最终通过ConfigFactory获取
    @Override
    public Config create(String namespace) {
        //目前仅支持通用模式
        //ConfigFileFormat format = determineFileFormat(namespace);

        return new DefaultConfig(namespace, createLocalConfigRepository(namespace));

    }

    private LocalFileConfigRepository createLocalConfigRepository(String namespace) {
        //localConfigRepository 入参namespace 和 RemoteConfigRepository对象
        return new LocalFileConfigRepository(namespace, createRemoteConfigRepository(namespace));
    }

    //RemoteConfigRepository是LocalFileConfigRepository的入参
    private RemoteConfigRepository createRemoteConfigRepository(String namespace) {
        return new RemoteConfigRepository(namespace);
    }


    //根据namespace判断属于哪种configfileformat
    private ConfigFileFormat determineFileFormat(String namespace) {
        String lowerCase = namespace.toLowerCase();
        for(ConfigFileFormat configFileFormat : ConfigFileFormat.values()) {
            if(lowerCase.endsWith("." + configFileFormat.getValue())) {
                return configFileFormat;
            }
        }
        return ConfigFileFormat.Properties;
    }
}
