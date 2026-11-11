package com.ascend.notify;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Short-lived one-time codes that tie a Telegram chat to a logged-in user. Kept in memory on purpose. */
@Component
public class LinkCodeStore {
    public static final Duration TTL = Duration.ofMinutes(10);
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private record Entry(UUID userId, Instant expiresAt) {}

    private final Map<String, Entry> codes = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String create(UUID userId) {
        purge();
        codes.values().removeIf(e -> e.userId().equals(userId)); // one live code per user
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        String code = sb.toString();
        codes.put(code, new Entry(userId, Instant.now().plus(TTL)));
        return code;
    }

    /** Returns the user the code belongs to and burns it. */
    public Optional<UUID> consume(String code) {
        if (code == null) return Optional.empty();
        Entry e = codes.remove(code.trim().toUpperCase());
        if (e == null || e.expiresAt().isBefore(Instant.now())) return Optional.empty();
        return Optional.of(e.userId());
    }

    private void purge() {
        Instant now = Instant.now();
        codes.values().removeIf(e -> e.expiresAt().isBefore(now));
    }
}
