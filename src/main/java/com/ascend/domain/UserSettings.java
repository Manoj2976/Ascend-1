package com.ascend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "user_settings")
@Getter @Setter @NoArgsConstructor
public class UserSettings {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(length = 80)
    private String name = "";

    @Column(length = 10)
    private String theme = "light";

    @Column(name = "window_min", nullable = false)
    private int windowMin = 30;

    @Column(name = "interval_min", nullable = false)
    private int intervalMin = 5;

    private boolean sound = true;
    private boolean notifications = true;

    @Column(name = "streak_threshold", nullable = false)
    private int streakThreshold = 80;

    @Column(length = 64, nullable = false)
    private String timezone = "Asia/Kolkata";

    @Column(name = "telegram_chat_id", length = 40)
    private String telegramChatId;

    public UserSettings(UUID userId) {
        this.userId = userId;
    }
}
