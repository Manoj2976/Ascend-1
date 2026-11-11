package com.ascend;

import com.ascend.domain.Repeat;
import com.ascend.domain.Task;
import com.ascend.domain.UserSettings;
import com.ascend.notify.NotificationService;
import com.ascend.reminder.ReminderProcessor;
import com.ascend.repo.ReminderLogRepository;
import com.ascend.repo.TaskRepository;
import com.ascend.repo.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The "Bath at 9:00 PM" rule: first reminder at 9:00, then every 5 minutes,
 * until the task is ticked or 30 minutes have passed.
 */
@SpringBootTest
@ActiveProfiles("test")
class ReminderProcessorTest {
    @Autowired ReminderProcessor processor;
    @Autowired TaskRepository tasks;
    @Autowired ReminderLogRepository logs;
    @Autowired UserSettingsRepository settingsRepo;
    @MockBean NotificationService notifications; // we only count how many reminders the rule decides to send

    private final LocalDate day = LocalDate.of(2026, 11, 10);
    private UserSettings settings;
    private Task bath;

    @BeforeEach
    void setUp() {
        logs.deleteAll();
        tasks.deleteAll();
        settingsRepo.deleteAll();

        settings = new UserSettings(UUID.randomUUID());
        settings.setTimezone("UTC");
        settings.setWindowMin(30);
        settings.setIntervalMin(5);
        settingsRepo.save(settings);

        bath = new Task();
        bath.setUserId(settings.getUserId());
        bath.setTitle("Bath");
        bath.setDate(day);
        bath.setTime(LocalTime.of(21, 0));
        bath.setRepeat(Repeat.NONE);
        tasks.save(bath);
    }

    private int at(String hhmmss) {
        return processor.processUser(settings, Instant.parse("2026-11-10T" + hhmmss + "Z"));
    }

    @Test
    void staysQuietBeforeTheDueTime() {
        assertEquals(0, at("20:59:50"));
    }

    @Test
    void firesAtDueTimeThenEveryFiveMinutes() {
        assertEquals(1, at("21:00:05"));
        assertEquals(0, at("21:02:00"));
        assertEquals(0, at("21:04:30"));
        assertEquals(1, at("21:05:05"));
        assertEquals(0, at("21:05:30"));
        assertEquals(1, at("21:10:05"));
    }

    @Test
    void stopsOnceTheTaskIsTicked() {
        assertEquals(1, at("21:00:05"));
        Task t = tasks.findById(bath.getId()).orElseThrow();
        t.getDoneDates().add(day);
        tasks.save(t);
        assertEquals(0, at("21:05:05"));
        assertEquals(0, at("21:10:05"));
    }

    @Test
    void stopsAfterTheThirtyMinuteWindow() {
        int total = 0;
        for (String t : new String[]{"21:00:05", "21:05:05", "21:10:05", "21:15:05", "21:20:05", "21:25:05"}) total += at(t);
        assertEquals(6, total);
        assertEquals(0, at("21:30:05"));
        assertEquals(0, at("21:40:00"));
    }

    @Test
    void repeatingTasksRingOnLaterDays() {
        Task t = tasks.findById(bath.getId()).orElseThrow();
        t.setRepeat(Repeat.DAILY);
        tasks.save(t);
        Instant nextDay = Instant.parse("2026-11-12T21:00:10Z");
        assertEquals(1, processor.processUser(settings, nextDay));
    }

    @Test
    void tasksWithoutATimeNeverRing() {
        Task t = tasks.findById(bath.getId()).orElseThrow();
        t.setTime(null);
        tasks.save(t);
        assertEquals(0, at("21:00:05"));
    }
}
