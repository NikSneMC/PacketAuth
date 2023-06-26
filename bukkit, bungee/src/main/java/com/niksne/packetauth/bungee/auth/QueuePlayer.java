package com.niksne.packetauth.bungee.auth;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public record QueuePlayer(ProxiedPlayer player, ScheduledTask task) {

}