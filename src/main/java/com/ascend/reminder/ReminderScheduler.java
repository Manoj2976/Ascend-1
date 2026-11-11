package com.ascend.reminder;

import com.ascend.domain.UserSettings;
import com.ascend.repo.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Wakes up every few seconds (REMINDER_TICK_MS, default 15s) so a task due at 9:00 PM
 * is announced within seconds of 9:00 PM. Runs on one thread, so ticks never overlap.
 * Run a single instance of the backend, or reminders will be sent once per instance.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {
    private final UserSettingsRepository settings;
    private final ReminderProcessor processor;

    @Scheduled(fixedDelayString = "${ascend.reminders.tick-ms:15000}", initialDelayString = "${ascend.reminders.tick-ms:15000}")
    public void tick() {
        Instant now = Instant.now();
        for (UserSettings s : settings.findByNotificationsTrue()) {
            try {
                processor.processUser(s, now);
            } catch (Exception e) {
                log.error("Reminder run failed for user {}", s.getUserId(), e);
            }
        }
    }

    @Scheduled(cron = "0 30 3 * * *")
    public void cleanup() {
        processor.cleanup(LocalDate.now().minusDays(3));
    }
}
