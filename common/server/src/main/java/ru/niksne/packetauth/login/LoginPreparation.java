package ru.niksne.packetauth.login;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledExecutorService;

public record LoginPreparation(
    @NotNull
    ScheduledExecutorService service,
    @NotNull
    Long delay
) {}
