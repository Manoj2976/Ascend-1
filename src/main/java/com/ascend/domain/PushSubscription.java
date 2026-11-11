package com.ascend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "push_subscriptions", indexes = @Index(name = "idx_push_user", columnList = "user_id"))
@Getter @Setter @NoArgsConstructor
public class PushSubscription {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 2048)
    private String endpoint;

    @Column(nullable = false, length = 256)
    private String p256dh;

    @Column(nullable = false, length = 256)
    private String auth;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
