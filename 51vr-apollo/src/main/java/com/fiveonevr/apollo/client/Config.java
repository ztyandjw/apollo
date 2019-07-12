package com.fiveonevr.apollo.client;

import com.fiveonevr.apollo.client.enums.ConfigSourceType;

import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/12.
 */

//Config接口
public interface Config {

    String getProperty(String key, String defaultValue);

    Integer getIntProperty(String key, Integer defaultValue);

    Long getLongProperty(String key, Long defaultValue);

    Short getShortProperty(String key, Short defaultValue);


    Float getFloatProperty(String key, Float defaultValue);


    Double getDoubleProperty(String key, Double defaultValue);


    Byte getByteProperty(String key, Byte defaultValue);


    Boolean getBooleanProperty(String key, Boolean defaultValue);


    String[] getArrayProperty(String key, String delimiter, String[] defaultValue);


    Date getDateProperty(String key, Date defaultValue);


    Date getDateProperty(String key, String format, Date defaultValue);


    Date getDateProperty(String key, String format, Locale locale, Date defaultValue);


    <T extends Enum<T>> T getEnumProperty(String key, Class<T> enumType, T defaultValue);


    long getDurationProperty(String key, long defaultValue);


    void addChangeListener(ConfigChangeListener listener);


    void addChangeListener(ConfigChangeListener listener, Set<String> interestedKeys);


    void addChangeListener(ConfigChangeListener listener, Set<String> interestedKeys, Set<String> interestedKeyPrefixes);


    boolean removeChangeListener(ConfigChangeListener listener);


    Set<String> getPropertyNames();


    <T> T getProperty(String key, Function<String, T> function, T defaultValue);

    public ConfigSourceType getSourceType();
}
