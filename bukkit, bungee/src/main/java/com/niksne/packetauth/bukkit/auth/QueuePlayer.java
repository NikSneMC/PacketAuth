package com.niksne.packetauth.bukkit.auth;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public record QueuePlayer(Player player, BukkitTask task) {

}