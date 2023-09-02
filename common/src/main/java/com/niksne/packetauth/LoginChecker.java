package com.niksne.packetauth;

import java.util.Set;

public class LoginChecker {
    private final String action;
    private String reason = "";
    private String token = "";

    public LoginChecker(LoginPreparer preparer, String name, ConfigManager config, MySQLManager db, ConfigManager disabled, ConfigManager tokens, Set<String> verified) {
        if (preparer.getOutdated().contains(name)) {
            action = "kick";
            reason = config.getString("kick.outdated").replace("%version%", "1.6");
        } else if (preparer.isAutogenEnabled() && db == null && !tokens.containsKey(name)) {
            action = "send_token";
            token = tokens.putString(name, Utils.generateRandomToken(config));
        } else if (preparer.isAutogenEnabled() && db != null && !db.hasRecord(config.getString("storage.mysql.tableName"), name)) {
            action = "send_token";
            token = db.saveToken(name, Utils.generateRandomToken(config));
        } else if (!verified.contains(name)) {
            action = "kick";
            reason = config.getString("kick.message");
        } else if (preparer.isTokenDisablingEnabled() && db == null && disabled.containsKey(name)) {
            action = "kick";
            reason =  Utils.parseMessage(config, disabled.getString(name));
        } else if (preparer.isTokenDisablingEnabled() && db != null && db.hasRecord(config.getString("tokenDisabling.tableName"), name)) {
            action = "kick";
            reason =  Utils.parseMessage(config, db.getReason(config.getString("tokenDisabling.tableName"), name));
        } else action = "pass";
        reason = reason.replace("%name%", name);
    }

    public String getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }

    public  String getToken() {
        return token;
    }
}
