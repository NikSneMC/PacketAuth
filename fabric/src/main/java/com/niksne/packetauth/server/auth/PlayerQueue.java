package com.niksne.packetauth.server.auth;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.niksne.packetauth.server.Utils.*;

public class PlayerQueue {

    private static PlayerQueue instance;

    private final Map<String, QueuePlayer> queue = new HashMap<>();

    private final Consumer<ServerPlayerEntity> onJoin;
    private final Consumer<ServerPlayerEntity> onAccepted;
    private final Consumer<ServerPlayerEntity> onLeave;

    public PlayerQueue(Consumer<ServerPlayerEntity> onJoin, Consumer<ServerPlayerEntity> onAccepted, Consumer<ServerPlayerEntity> onLeave) {
        this.onJoin = onJoin;
        this.onAccepted = onAccepted;
        this.onLeave = onLeave;

        instance = this;
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (this.onJoin != null) {
                this.onJoin.accept(player);
            }
            long delay = (long) eval(getKickDelay().replace("%ping%", String.valueOf(player.pingMilliseconds)));
            ScheduledExecutorService service  = Executors.newScheduledThreadPool(1);
            service.scheduleWithFixedDelay(
                    () -> {player.networkHandler.disconnect(Text.of(getKickMsg().replace("%name%", player.getEntityName())));
                        service.shutdown();}, delay, delay, TimeUnit.MILLISECONDS
            );
            this.queue.put(player.getEntityName(), new QueuePlayer(player, service));
        });

        ServerPlayConnectionEvents.DISCONNECT.register(((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (this.onLeave != null) {
                this.onLeave.accept(player);
            }

            this.queue.remove(player.getEntityName());
        }));
    }

    public void allow(ServerPlayerEntity player) {this.allow(player.getEntityName());}

    public void allow(String name) {
        QueuePlayer player = this.queue.remove(name);
        player.service().shutdown();

        if (this.onAccepted != null) {
            this.onAccepted.accept(player.player());
        }
    }

    public static PlayerQueue getInstance() {
        return instance;
    }
}