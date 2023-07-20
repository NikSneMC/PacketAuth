package com.niksne.packetauth.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ConfigManager {
    private final File configFile;
    private Configuration config;

    public ConfigManager(Plugin plugin) {
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        try {
            if (!configFile.exists()) {
                Files.copy(plugin.getResourceAsStream("config.yml"), configFile.toPath());
            }

            reload();
        } catch (IOException e) {
            //
        }
    }

    public String getString(String key) {
        return config.getString(key);
    }

    public void reload() {
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            //
        }
    }
}