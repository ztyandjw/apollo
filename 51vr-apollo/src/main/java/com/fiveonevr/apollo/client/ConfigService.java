package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.build.ApolloInjector;
import com.fiveonevr.apollo.client.constants.ConfigConsts;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/12.
 */

public class ConfigService {
    private static final ConfigService configService = new ConfigService();
    private volatile  ConfigManager configManager;
    private volatile  ConfigRegistry configRegistry;

    //单例模式获取ConfigManager，Config的管理类
    private ConfigManager getManager() {
        if(configManager == null) {
            synchronized (this) {
                if(configManager == null) {
                    configManager = ApolloInjector.getInstance(ConfigManager.class);
                }
            }
        }
        return configManager;
    }

    //单例模式获取ConfigRegistry
    private ConfigRegistry getConfigRegistry() {
        if(configRegistry == null) {
            synchronized (this) {
                if(configRegistry ==  null) {
                    configRegistry = ApolloInjector.getInstance(ConfigRegistry.class);
                }
            }
        }
        return configRegistry;
    }

    //通过namespace application获取config
    public static Config getAppConfig() {
        return getConfig(ConfigConsts.NAMESPACE_APPLICATION);
    }

    public static Config getConfig(String namespace) {
        return configService.getManager().getConfig(namespace);
    }
}
