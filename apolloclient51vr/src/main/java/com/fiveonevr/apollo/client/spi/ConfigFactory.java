package com.fiveonevr.apollo.client.spi;


import com.fiveonevr.apollo.client.internals.Config;
import com.fiveonevr.apollo.client.internals.ConfigFile;
import com.fiveonevr.apollo.client.internals.ConfigFileFormat;

/**
 * @author Jason Song(song_s@ctrip.com)
 */

//Config工厂类
public interface ConfigFactory {

  Config create(String namespace);

  ConfigFile createConfigFile(String namespace, ConfigFileFormat configFileFormat);
}
