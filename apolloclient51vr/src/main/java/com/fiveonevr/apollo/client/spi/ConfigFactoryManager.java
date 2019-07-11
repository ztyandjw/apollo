package com.fiveonevr.apollo.client.spi;

import com.fiveonevr.apollo.client.spi.ConfigFactory;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface ConfigFactoryManager {
  /**
   * Get the config factory for the namespace.
   *
   * @param namespace the namespace
   * @return the config factory for this namespace
   */
  public ConfigFactory getFactory(String namespace);
}
