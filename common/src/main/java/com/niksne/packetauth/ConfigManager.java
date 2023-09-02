package com.niksne.packetauth;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class ConfigManager {

    private final String configFilePath;
    private final String defaultConfigPath;
    private Map<String, String> configMap;

    public ConfigManager(String path, String name, String parent) {
        this.configFilePath = String.format("%s/%s.yml", path, name);
        this.defaultConfigPath = String.format("config/%s.yml", parent);
        this.configMap = new HashMap<>();
        loadConfig();
    }


    public boolean getBool(String key) {
        return Boolean.parseBoolean(configMap.getOrDefault(key, ""));
    }
    public String getString(String key) {
        loadConfig();
        return configMap.getOrDefault(key, "");
    }

    public String putString(String key, String value) {
        loadConfig();
        if (configMap.containsKey(key)) configMap.replace(key, value);
        else configMap.put(key, value);
        saveConfig();
        return value;
    }

    public void addString(String key, String value) {
        loadConfig();
        if (configMap.containsKey(key)) return;
        else configMap.put(key, value);
        saveConfig();
    }

    public void removeString(String key) {
        loadConfig();
        configMap.remove(key);
        saveConfig();
    }

    public boolean containsKey(String key) {
        loadConfig();
        return configMap.containsKey(key);
    }

    public Map<String, String> asMap() {
        loadConfig();
        return configMap;
    }

    public void clear() {
        configMap.clear();
        saveConfig();
    }

    public void overwrite(Map<String, String> config) {
        configMap = config;
        saveConfig();
    }

    private void loadConfig() {
        configMap.clear();
        File configFile = new File(configFilePath);
        try {
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                copyDefaultConfig();
            }
            Yaml yaml = new Yaml();
            Map<String, String> loadedConfig;
            try (InputStream stream = new FileInputStream(configFile)){
                loadedConfig = yaml.load(stream);
            }
            if (loadedConfig != null) {
                configMap.putAll(loadedConfig);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveConfig() {
        File configFile = new File(configFilePath);
        try {
            DumperOptions options = new DumperOptions();
            options.setIndent(2);
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            TreeMap<String, String> sorted = new TreeMap<>();
            sorted.putAll(configMap);
            try (Writer writer = new FileWriter(configFile)) {
                yaml.dump(sorted, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyDefaultConfig() {
        try {
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(defaultConfigPath)) {
                assert stream != null;
                Files.copy(stream, Path.of(configFilePath));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}