package com.niksne.packetauth;

import java.util.HashMap;
import java.util.Map;

public class MigrateConfig {

    public MigrateConfig(ConfigManager config, ConfigManager tokens) {
        if (!config.containsKey("config.version")) {
            Map<String, String> old = new HashMap<>();
            old.put("kick.message", config.getString("kick.message"));
            config.removeString("kick.message");
            old.put("kick.delay", config.getString("kick.delay"));
            config.removeString("kick.delay");
            if (tokens != null) tokens.overwrite(config.asMap());
            config.clear();
            config.putString("config.version", "1.6.2");
            config.putString("kick.outdated", "&cYou need &aPacket Auth %version% or never &cto play on this server!");
            config.putString("kick.message", old.getOrDefault("kick.message", "&cAuthorization error!"));
            config.putString("kick.delay", old.getOrDefault("kick.delay", "%ping% + 200"));
            config.putString("tokendisabling.enabled", "false");
            config.putString("tokengen.enabled", "true");
            config.putString("tokengen.length", "4096");
            config.putString("tokengen.symbols", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
        } else if (config.getString("config.version").compareTo("1.6.2") < 0) {
            config.putString("config.version", "1.6.2");
            config.addString("tokenDisabling.enabled", "false");
        }
    }
}
