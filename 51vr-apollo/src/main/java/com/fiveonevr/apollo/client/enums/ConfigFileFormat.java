package com.fiveonevr.apollo.client.enums;

import com.fiveonevr.apollo.client.utils.StringUtils;

public enum ConfigFileFormat {

    Properties("properties"),
    XML("xml"),
    JSON("json"),
    YML("yml"),
    YAML("yaml"),
    TXT("txt");

    private String value;

    ConfigFileFormat(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ConfigFileFormat fromString(String value) {
        if(StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("value can't be empty");
        }
        switch (value.toLowerCase()) {
            case "properties":
                return Properties;
            case "xml":
                return XML;
            case "json":
                return JSON;
            case "yml":
                return YML;
            case "yaml":
                return YAML;
            case "txt":
                return TXT;
        }
        throw new IllegalArgumentException(value + " can't mapping enum value");
    }

    public static boolean isValidFormat(String value) {
        try {
            fromString(value);
            return true;
        }catch (IllegalArgumentException e) {
            return false;
        }
    }


    //如果是yaml的返回true
    public static boolean isPropertiesCompatible(ConfigFileFormat format) {
        return format == YAML || format == YML;
    }
}
