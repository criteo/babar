package com.bhnte.babar.agent.config;

import java.util.Map;

public class Config {

    final Map<String, String> mainConfig;

    Config(Map<String, String> mainConfig) {
        this.mainConfig = mainConfig;
    }

    public String get(String key) {
        return getString(key);
    }

    public String getString(String key) {
        return getString(mainConfig, key);
    }

    private String getString(Map<String, String> config, String key) {
        return config.get(key);
    }

    public String getStringOrDefault(String key, String def) {
        return getStringOrDefault(mainConfig, key, def);
    }

    private String getStringOrDefault(Map<String, String> config, String key, String def) {
        return config.containsKey(key) ? config.get(key) : def;
    }

    public Integer getInt(String key) {
        return getInt(mainConfig, key);
    }

    private Integer getInt(Map<String, String> config, String key) {
        return Integer.valueOf(config.get(key));
    }

    public Integer getIntOrDefault(String key, Integer def) {
        return getIntOrDefault(mainConfig, key, def);
    }

    private Integer getIntOrDefault(Map<String, String> config, String key, Integer def) {
        return config.containsKey(key) ? Integer.valueOf(config.get(key)) : def;
    }

    public Long getLong(String key) {
        return getLong(mainConfig, key);
    }

    private Long getLong(Map<String, String> config, String key) {
        return Long.valueOf(config.get(key));
    }

    public Long getLongOrDefault(String key, Long def) {
        return getLongOrDefault(mainConfig, key, def);
    }

    private Long getLongOrDefault(Map<String, String> config, String key, Long def) {
        return config.containsKey(key) ? Long.valueOf(config.get(key)) : def;
    }

    public Double getDouble(String key) {
        return getDouble(mainConfig, key);
    }

    private Double getDouble(Map<String, String> config, String key) {
        return Double.valueOf(config.get(key));
    }

    public Double getDoubleOrDefault(String key, Double def) {
        return getDoubleOrDefault(mainConfig, key, def);
    }

    private Double getDoubleOrDefault(Map<String, String> config, String key, Double def) {
        return config.containsKey(key) ? Double.valueOf(config.get(key)) : def;
    }

    public Boolean getBoolean(Map<String, String> config, String key) {
        return Boolean.valueOf(config.get(key));
    }

    public Boolean getBooleanOrDefault(Map<String, String> config, String key, Boolean def) {
        return config.containsKey(key) ? Boolean.valueOf(config.get(key)) : def;
    }
}
