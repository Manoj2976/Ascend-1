package com.ascend.service;

import com.ascend.domain.Goal;
import com.ascend.domain.Repeat;
import com.ascend.domain.Task;
import com.ascend.domain.UserData;
import com.ascend.dto.TaskDto;
import com.ascend.dto.TaskRequest;
import com.ascend.reminder.RecurrenceRules;
import com.ascend.repo.GoalRepository;
import com.ascend.repo.ReminderLogRepository;
import com.ascend.repo.TaskRepository;
import com.ascend.repo.UserDataRepository;
import com.ascend.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository tasks;
    private final GoalRepository goals;
    private final ReminderLogRepository reminderLogs;
    private final SettingsService settings;
    private final UserDataRepository userData;

    @Transactional(readOnly = true)
    public List<TaskDto> list(UUID userId) {
        return tasks.findByUserId(userId).stream().map(TaskService::toDto).toList();
    }

    @Transactional
    public TaskDto create(UUID userId, TaskRequest r) {
        Task t = new Task();
        t.setUserId(userId);
        fill(t, userId, r);
        bump(userId);
        return toDto(tasks.save(t));
    }

    @Transactional
    public TaskDto update(UUID userId, UUID id, TaskRequest r) {
        Task t = find(userId, id);
        fill(t, userId, r);
        reminderLogs.deleteByTaskId(id); // time or date may have moved: start the reminder cycle fresh
        bump(userId);
        return toDto(tasks.save(t));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Task t = find(userId, id);
        reminderLogs.deleteByTaskId(id);
        bump(userId);
        tasks.delete(t);
    }

    /** Flip done / not done for one occurrence. A linked monthly target moves by +1 / -1, like the UI does. */
    @Transactional
    public TaskDto toggle(UUID userId, UUID id, LocalDate date) {
        Task t = find(userId, id);
        LocalDate day = date != null ? date : settings.today(userId);
        return apply(t, userId, day, !t.getDoneDates().contains(day));
    }

    /** Idempotent version used by the Telegram "Done" button. */
    @Transactional
    public TaskDto setDone(UUID userId, UUID id, LocalDate date, boolean done) {
        Task t = find(userId, id);
        LocalDate day = date != null ? date : settings.today(userId);
        if (t.getDoneDates().contains(day) == done) return toDto(t);
        return apply(t, userId, day, done);
    }

    private TaskDto apply(Task t, UUID userId, LocalDate day, boolean done) {
        if (!RecurrenceRules.occursOn(t, day)) throw ApiException.badRequest("Task does not occur on " + day);
        bump(userId);
        if (done) t.getDoneDates().add(day);
        else t.getDoneDates().remove(day);
        if (t.getGoalId() != null) {
            goals.findByIdAndUserId(t.getGoalId(), userId).ifPresent(g -> {
                g.setProgress(Math.max(0, g.getProgress() + (done ? 1 : -1)));
                goals.save(g);
            });
        }
        return toDto(tasks.save(t));
    }

    /**
     * Any change made outside the app's full-state sync (Telegram "Done", REST calls) bumps the revision,
     * so the app notices on its next check and an older copy can't overwrite the change.
     */
    private void bump(UUID userId) {
        UserData d = userData.findById(userId).orElseGet(() -> new UserData(userId));
        d.setRevision(d.getRevision() + 1);
        userData.save(d);
    }

    private Task find(UUID userId, UUID id) {
        return tasks.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Task"));
    }

    private void fill(Task t, UUID userId, TaskRequest r) {
        t.setTitle(r.title().trim());
        t.setDate(r.date());
        t.setTime(parseTime(r.time()));
        t.setCategory(r.category() == null || r.category().isBlank() ? "Personal" : r.category().trim());
        t.setRepeat(Repeat.parse(r.repeat()));
        t.setNotes(r.notes() == null ? "" : r.notes());
        t.setGoalId(resolveGoal(userId, r.goalId()));
    }

    private UUID resolveGoal(UUID userId, String goalId) {
        if (goalId == null || goalId.isBlank()) return null;
        UUID gid;
        try {
            gid = UUID.fromString(goalId);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Invalid goalId");
        }
        Goal g = goals.findByIdAndUserId(gid, userId).orElseThrow(() -> ApiException.badRequest("Unknown goalId"));
        return g.getId();
    }

    public static LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalTime.parse(s.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("time must look like 21:00");
        }
    }

    public static TaskDto toDto(Task t) {
        return new TaskDto(
                t.getId().toString(),
                t.getTitle(),
                t.getDate().toString(),
                t.getTime() == null ? "" : t.getTime().toString(),
                t.getCategory(),
                t.getRepeat().wire(),
                t.getGoalId() == null ? "" : t.getGoalId().toString(),
                t.getNotes() == null ? "" : t.getNotes(),
                t.getDoneDates().stream().sorted().map(LocalDate::toString).toList());
    }
}
