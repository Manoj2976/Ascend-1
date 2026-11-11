package com.ascend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "ascend")
public record AscendProperties(
        Jwt jwt,
        String corsOrigins,
        boolean allowRegistration,
        Vapid vapid,
        Telegram telegram,
        Reminders reminders) {

    public record Jwt(String secret, int expiryDays) {}

    public record Vapid(String publicKey, String privateKey, String subject) {
        public boolean configured() {
            return publicKey != null && !publicKey.isBlank() && privateKey != null && !privateKey.isBlank();
        }
    }

    public record Telegram(String botToken, String botUsername, String webhookSecret, String publicUrl) {
        public boolean configured() {
            return botToken != null && !botToken.isBlank();
        }

        public boolean webhookReady() {
            return configured() && notBlank(webhookSecret) && notBlank(publicUrl);
        }

        private static boolean notBlank(String s) {
            return s != null && !s.isBlank();
        }
    }

    public record Reminders(long tickMs) {}

    public List<String> corsOriginList() {
        if (corsOrigins == null) return List.of();
        return Arrays.stream(corsOrigins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
