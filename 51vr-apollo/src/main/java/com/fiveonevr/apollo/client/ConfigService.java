package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.build.ApolloInjector;
import com.fiveonevr.apollo.client.constants.ConfigConsts;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/12.
 */

//对外暴露的ConfigService
public class ConfigService {
    //主体
    private static final ConfigService configService = new ConfigService();
    //单例模式，需要用volatle 或者 final修饰
    private volatile ConfigManager configManager;
    private volatile ConfigRegistry configRegistry;

    //单例获取configManager
    private ConfigManager getManager() {
        if(configManager == null) {
            synchronized (this) {
                if(configManager == null) {
                    //通过guava类加载器获取
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

    //获取ConfigManager，调用ConfigManager.getConfig方法
    public static Config getConfig(String namespace) {
        return configService.getManager().getConfig(namespace);
    }
}
