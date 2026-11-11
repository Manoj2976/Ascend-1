package com.ascend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Remembers how many reminders were sent for one occurrence of a task (task + day),
 * so the scheduler can repeat every N minutes and survive restarts.
 */
@Entity
@Table(name = "reminder_logs", indexes = {
        @Index(name = "idx_rlog_task", columnList = "task_id"),
        @Index(name = "idx_rlog_day", columnList = "log_day")})
@Getter @Setter @NoArgsConstructor
public class ReminderLog {
    /** "{taskId}:{yyyy-MM-dd}" */
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "log_day", nullable = false)
    private LocalDate day;

    @Column(name = "last_notified_at")
    private Instant lastNotifiedAt;

    @Column(name = "send_count", nullable = false)
    private int count;

    public static String key(UUID taskId, LocalDate day) {
        return taskId + ":" + day;
    }
}
