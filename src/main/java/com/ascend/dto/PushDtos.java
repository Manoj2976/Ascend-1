package com.ascend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PushDtos {
    private PushDtos() {}

    public record Config(boolean enabled, String publicKey) {
    }

    public record Keys(@NotBlank @Size(max = 256) String p256dh, @NotBlank @Size(max = 256) String auth) {
    }

    /** Matches PushSubscription.toJSON() from the browser. */
    public record Subscribe(@NotBlank @Size(max = 2048) String endpoint, @NotNull @Valid Keys keys) {
    }

    public record Unsubscribe(@NotBlank String endpoint) {
    }

    public record TestResult(int pushSent, boolean telegramSent) {
    }

    public record TelegramLink(String code, String deepLink, String botUsername, int expiresInMinutes) {
    }
}
