package com.fiveonevr.apollo.client;


import com.fiveonevr.apollo.client.model.ConfigChangeEvent;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/12.
 */


public interface ConfigChangeListener {
  /**
   * Invoked when there is any config change for the namespace.
   * @param changeEvent the event for this change
   */
  public void onChange(ConfigChangeEvent changeEvent);
}
