package ru.niksne.packetauth.login;

import org.jetbrains.annotations.NotNull;

public sealed interface LoginCheckerAction {
    record Kick(
            @NotNull
            String reason
    ) implements LoginCheckerAction {}

    record SendToken(
            @NotNull
            String token
    ) implements LoginCheckerAction {}

    record Pass() implements LoginCheckerAction {}
}