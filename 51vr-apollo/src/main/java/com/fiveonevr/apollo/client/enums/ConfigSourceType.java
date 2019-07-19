package com.fiveonevr.apollo.client.enums;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/12.
 */


public enum ConfigSourceType {
  REMOTE("Loaded from remote config service"),
  LOCAL("Loaded from local cache"),
  NONE("Load failed");

  private final String description;

  ConfigSourceType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
