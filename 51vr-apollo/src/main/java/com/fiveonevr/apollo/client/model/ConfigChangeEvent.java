package com.fiveonevr.apollo.client.model;

import java.util.Map;
import java.util.Set;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/12.
 */


//Config改变事件
public class ConfigChangeEvent {
  private final String namespace;
  private final Map<String, ConfigChange> changes;


  public ConfigChangeEvent(String namespace,
                           Map<String, ConfigChange> changes) {
    this.namespace = namespace;
    this.changes = changes;
  }


  public Set<String> changedKeys() {
    return changes.keySet();
  }


  public ConfigChange getChange(String key) {
    return changes.get(key);
  }


  public boolean isChanged(String key) {
    return changes.containsKey(key);
  }


  public String getNamespace() {
    return namespace;
  }
}
