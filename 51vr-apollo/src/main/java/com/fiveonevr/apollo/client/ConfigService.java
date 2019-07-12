package com.fiveonevr.apollo.client;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/12.
 */

public class ConfigService {

    private volatile  ConfigManager configManager;
    private volatile  ConfigRegistry configRegistry;

    private ConfigManager getManager() {
        if(configManager == null) {
            synchronized (this) {
                if(configManager == null) {

                }
            }
        }
    }

}
