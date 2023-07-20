package com.niksne.packetauth.velocity;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final String configFilePath;
    private final String defaultConfigPath;
    private final Map<String, String> configMap;

    public ConfigManager(String name) {
        this.configFilePath = "plugins/PacketAuth/" + name;
        this.defaultConfigPath = "config.yml";
        this.configMap = new HashMap<>();
        loadConfig();
    }

    public String getString(String key) {
        configMap.clear();
        loadConfig();
        return configMap.getOrDefault(key, null);
    }

    private void loadConfig() {
        File configFile = new File(configFilePath);
        try {
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                copyDefaultConfig();
            }
            InputStream inputStream = Files.newInputStream(configFile.toPath());
            Yaml yaml = new Yaml();
            Map<String, String> loadedConfig = yaml.load(inputStream);
            if (loadedConfig != null) {
                configMap.putAll(loadedConfig);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyDefaultConfig() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(defaultConfigPath);
            assert inputStream != null;
            Files.copy(inputStream, Path.of(configFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}