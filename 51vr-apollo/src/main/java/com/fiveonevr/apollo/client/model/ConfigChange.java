package com.fiveonevr.apollo.client.model;


import com.fiveonevr.apollo.client.enums.PropertyChangeType;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/12.
 */


public class ConfigChange {
  private final String namespace;
  private final String propertyName;
  private String oldValue;
  private String newValue;
  private PropertyChangeType changeType;


  public ConfigChange(String namespace, String propertyName, String oldValue, String newValue,
                      PropertyChangeType changeType) {
    this.namespace = namespace;
    this.propertyName = propertyName;
    this.oldValue = oldValue;
    this.newValue = newValue;
    this.changeType = changeType;
  }

  public String getPropertyName() {
    return propertyName;
  }

  public String getOldValue() {
    return oldValue;
  }

  public String getNewValue() {
    return newValue;
  }

  public PropertyChangeType getChangeType() {
    return changeType;
  }

  public void setOldValue(String oldValue) {
    this.oldValue = oldValue;
  }

  public void setNewValue(String newValue) {
    this.newValue = newValue;
  }

  public void setChangeType(PropertyChangeType changeType) {
    this.changeType = changeType;
  }

  public String getNamespace() {
    return namespace;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ConfigChange{");
    sb.append("namespace='").append(namespace).append('\'');
    sb.append(", propertyName='").append(propertyName).append('\'');
    sb.append(", oldValue='").append(oldValue).append('\'');
    sb.append(", newValue='").append(newValue).append('\'');
    sb.append(", changeType=").append(changeType);
    sb.append('}');
    return sb.toString();
  }
}
