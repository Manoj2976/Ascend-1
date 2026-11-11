package com.ascend.reminder;

import com.ascend.domain.ReminderLog;
import com.ascend.domain.Task;
import com.ascend.domain.UserSettings;
import com.ascend.notify.NotificationService;
import com.ascend.notify.ReminderMessage;
import com.ascend.repo.ReminderLogRepository;
import com.ascend.repo.TaskRepository;
import com.ascend.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * The reminder rule, for one user at one instant:
 * a task is "ringing" from its due time until due + windowMin. While ringing and not ticked,
 * send a reminder immediately, then again every intervalMin. Ticking the task stops it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderProcessor {
    /** Scheduler ticks are not perfectly regular, so allow a little slack before the next repeat is "due". */
    static final long SLACK_MS = 5_000;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("h:mm a");

    private final TaskRepository tasks;
    private final ReminderLogRepository logs;
    private final NotificationService notifications;

    /** @return how many reminders were sent. */
    public int processUser(UserSettings s, Instant nowInstant) {
        ZoneId zone = SettingsService.zoneOf(s);
        ZonedDateTime now = nowInstant.atZone(zone);
        LocalDate today = now.toLocalDate();
        long intervalMs = Math.max(1, s.getIntervalMin()) * 60_000L;
        int sent = 0;

        List<Task> candidates = tasks.findReminderCandidates(s.getUserId(), today);
        for (Task t : candidates) {
            if (!RecurrenceRules.occursOn(t, today) || t.getDoneDates().contains(today)) continue;

            ZonedDateTime due = today.atTime(t.getTime()).atZone(zone);
            ZonedDateTime end = due.plusMinutes(s.getWindowMin());
            if (now.isBefore(due) || now.isAfter(end)) continue; // not ringing

            String key = ReminderLog.key(t.getId(), today);
            ReminderLog log = logs.findById(key).orElseGet(() -> {
                ReminderLog n = new ReminderLog();
                n.setId(key);
                n.setTaskId(t.getId());
                n.setDay(today);
                return n;
            });
            if (log.getLastNotifiedAt() != null
                    && Duration.between(log.getLastNotifiedAt(), nowInstant).toMillis() < intervalMs - SLACK_MS) {
                continue; // reminded recently
            }

            int attempt = log.getCount() + 1;
            long minsLeft = Math.max(0, (long) Math.ceil(Duration.between(now, end).toSeconds() / 60.0));
            ReminderMessage msg = attempt == 1
                    ? new ReminderMessage("⏰ " + t.getTitle(), "It's time (" + due.format(CLOCK) + "). Tick it off when you're done.",
                    "task-" + t.getId(), t.getId(), today)
                    : new ReminderMessage("⏰ Still pending: " + t.getTitle(),
                    "Reminder " + attempt + " · " + minsLeft + " min left in the window",
                    "task-" + t.getId(), t.getId(), today);
            notifications.notifyUser(s.getUserId(), msg);

            log.setCount(attempt);
            log.setLastNotifiedAt(nowInstant);
            logs.save(log);
            sent++;
        }
        return sent;
    }

    @Transactional
    public void cleanup(LocalDate before) {
        logs.deleteByDayBefore(before);
    }
}
