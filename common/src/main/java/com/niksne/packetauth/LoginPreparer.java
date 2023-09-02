package com.niksne.packetauth;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class LoginPreparer {
    private final Set<String> outdated;
    private final MySQLManager db;
    private final boolean autogenEnabled;
    private final boolean tokenDisablingEnabled;
    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
    private final long delay;

    public LoginPreparer(ConfigManager config, MySQLManager db, Set<String> outdated, String name, long ping) {
        this.outdated = outdated;
        this.outdated.add(name);
        if (db != null) db.close();
        this.db = Utils.checkStorageType(config);
        this.autogenEnabled = Utils.checkAutogen(config);
        this.tokenDisablingEnabled = Utils.checkTokenDisabling(config, this.db);
        long p = ping;
        if (p <= 0) p = 1;
        this.delay = (long) Utils.eval(config.getString("kick.delay").replace("%ping%", String.valueOf(p)));
    }

    public Set<String> getOutdated() {
        return outdated;
    }
    public MySQLManager getDb() {
        return db;
    }

    public boolean isAutogenEnabled() {
        return autogenEnabled;
    }

    public boolean isTokenDisablingEnabled() {
        return tokenDisablingEnabled;
    }

    public ScheduledExecutorService getService() {
        return service;
    }

    public long getDelay() {
        return delay;
    }
}
