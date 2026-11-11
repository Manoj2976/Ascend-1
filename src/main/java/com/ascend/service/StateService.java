package com.ascend.service;

import com.ascend.domain.*;
import com.ascend.dto.*;
import com.ascend.repo.*;
import com.ascend.web.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.*;

/**
 * Whole-account export / import in the React app's localStorage shape.
 * Import is how you migrate the data you already entered in the browser into the database.
 */
@Service
@RequiredArgsConstructor
public class StateService {
    private static final int MAX_ITEMS = 5000;
    private static final int MAX_TIMETABLE_CHARS = 500_000;
    private static final int MAX_DIET_CHARS = 2_000_000;

    private final TaskService taskService;
    private final GoalService goalService;
    private final JournalService journalService;
    private final ApplicationService applicationService;
    private final FocusService focusService;
    private final SettingsService settingsService;

    private final TaskRepository tasks;
    private final GoalRepository goals;
    private final JournalRepository journal;
    private final ApplicationRepository apps;
    private final FocusRepository focus;
    private final ReminderLogRepository reminderLogs;
    private final UserDataRepository userData;
    private final ObjectMapper json;

    @Transactional(readOnly = true)
    public StateDto export(UUID userId) {
        UserData data = userData.findById(userId).orElse(null);
        return new StateDto(
                taskService.list(userId),
                goalService.list(userId),
                journalService.list(userId),
                applicationService.list(userId),
                focusService.list(userId),
                settingsService.get(userId),
                parse(data == null ? null : data.getTimetableJson()),
                parse(data == null ? null : data.getDietJson()),
                data == null ? 0L : data.getRevision());
    }

    /** Replaces everything the user has with the given state. */
    @Transactional
    public StateDto replace(UUID userId, StateDto in) {
        if (size(in.tasks()) > MAX_ITEMS || size(in.goals()) > MAX_ITEMS || size(in.applications()) > MAX_ITEMS) {
            throw ApiException.badRequest("Too many items to import");
        }

        UserData data = userData.findById(userId).orElseGet(() -> new UserData(userId));
        if (in.revision() != null && in.revision() != data.getRevision()) {
            throw ApiException.conflict("Your data changed on another device. Reload to get the latest version.");
        }
        String timetableJson = serialize(in.timetable(), MAX_TIMETABLE_CHARS, "time table");
        String dietJson = serialize(in.diet(), MAX_DIET_CHARS, "diet log");

        // Tasks keep their id when the client sent a UUID, so reminder history survives a sync.
        Set<UUID> incomingTaskIds = new HashSet<>();
        for (TaskDto d : in.tasks() == null ? List.<TaskDto>of() : in.tasks()) {
            UUID id = uuid(d.id());
            if (id != null) incomingTaskIds.add(id);
        }
        tasks.findByUserId(userId).stream().map(Task::getId)
                .filter(id -> !incomingTaskIds.contains(id))
                .forEach(reminderLogs::deleteByTaskId);
        tasks.deleteByUserId(userId);
        goals.deleteByUserId(userId);
        journal.deleteByUserId(userId);
        apps.deleteByUserId(userId);
        focus.deleteByUserId(userId);
        tasks.flush();

        // Goals first, so tasks and monthly targets can be re-pointed at their new ids.
        Map<String, UUID> goalIds = new HashMap<>();
        List<GoalDto> goalDtos = in.goals() == null ? List.of() : in.goals();
        for (GoalDto g : goalDtos) goalIds.put(str(g.id()), claim(g.id(), goals::existsById));

        for (GoalDto d : goalDtos) {
            GoalType type = parseGoalType(d.type());
            Goal g = new Goal();
            g.setId(goalIds.get(str(d.id())));
            g.setUserId(userId);
            g.setType(type);
            g.setTitle(clip(d.title(), 200, "Untitled"));
            g.setUnit(clip(d.unit(), 40, ""));
            g.setTarget(Math.max(1, d.target()));
            g.setProgress(Math.max(0, d.progress()));
            if (type == GoalType.MONTHLY) {
                g.setMonth(d.month() != null && d.month().matches("\\d{4}-(0[1-9]|1[0-2])") ? d.month() : null);
                g.setParentId(goalIds.get(str(d.parentId())));
            }
            if (type == GoalType.FINAL) g.setDeadline(date(d.deadline()));
            if (type == GoalType.DAILY && d.logs() != null) {
                d.logs().forEach((k, v) -> {
                    LocalDate day = date(k);
                    if (day != null && v != null && v > 0) g.getLogs().put(day, v);
                });
            }
            goals.save(g);
        }

        for (TaskDto d : in.tasks() == null ? List.<TaskDto>of() : in.tasks()) {
            LocalDate start = date(d.date());
            if (start == null || d.title() == null || d.title().isBlank()) continue;
            Task t = new Task();
            t.setId(claim(d.id(), tasks::existsById));
            t.setUserId(userId);
            t.setTitle(clip(d.title(), 200, "Untitled"));
            t.setDate(start);
            t.setTime(TaskService.parseTime(d.time()));
            t.setCategory(clip(d.category(), 40, "Personal"));
            t.setRepeat(Repeat.parse(d.repeat()));
            t.setNotes(clip(d.notes(), 2000, ""));
            t.setGoalId(goalIds.get(str(d.goalId())));
            if (d.doneDates() != null) d.doneDates().stream().map(StateService::date).filter(Objects::nonNull).forEach(t.getDoneDates()::add);
            tasks.save(t);
        }

        if (in.journal() != null) {
            in.journal().forEach((k, v) -> {
                LocalDate day = date(k);
                if (day == null || v == null) return;
                JournalEntry e = new JournalEntry();
                e.setUserId(userId);
                e.setDate(day);
                e.setMood(v.mood() != null && v.mood() >= 0 && v.mood() <= 4 ? v.mood() : null);
                e.setText(clip(v.text(), 10000, ""));
                e.setLearned(clip(v.learned(), 5000, ""));
                e.setPlan(clip(v.plan(), 5000, ""));
                journal.save(e);
            });
        }

        for (ApplicationDto d : in.applications() == null ? List.<ApplicationDto>of() : in.applications()) {
            if (d.company() == null || d.company().isBlank()) continue;
            Application a = new Application();
            a.setId(claim(d.id(), apps::existsById));
            a.setUserId(userId);
            a.setCompany(clip(d.company(), 120, ""));
            a.setRole(clip(d.role(), 120, ""));
            a.setStatus(ApplicationService.STATUSES.contains(d.status()) ? d.status() : "Wishlist");
            a.setDate(date(d.date()));
            a.setLink(clip(d.link(), 1000, ""));
            a.setNotes(clip(d.notes(), 2000, ""));
            apps.save(a);
        }

        if (in.focus() != null) {
            in.focus().forEach((k, v) -> {
                LocalDate day = date(k);
                if (day == null || v == null || v <= 0) return;
                FocusLog f = new FocusLog();
                f.setUserId(userId);
                f.setDate(day);
                f.setMinutes(v);
                focus.save(f);
            });
        }

        if (in.settings() != null) {
            SettingsDto s = in.settings();
            SettingsRequest req = new SettingsRequest(
                    s.name(),
                    s.theme() != null && s.theme().matches("light|dark|auto") ? s.theme() : null,
                    inRange(s.windowMin(), 5, 180), inRange(s.intervalMin(), 1, 60),
                    s.sound(), s.notifications(), inRange(s.streakThreshold(), 10, 100), null);
            settingsService.update(userId, req);
        }

        data.setTimetableJson(timetableJson);
        data.setDietJson(dietJson);
        data.setRevision(data.getRevision() + 1);
        userData.save(data);
        return export(userId);
    }

    /**
     * Uses the client's id when it is a UUID nobody else owns (the caller has already deleted this user's rows),
     * otherwise makes a new one. The check stops one account from claiming another account's row.
     */
    private static UUID claim(String clientId, java.util.function.Predicate<UUID> existsForAnyone) {
        UUID id = uuid(clientId);
        return id != null && !existsForAnyone.test(id) ? id : UUID.randomUUID();
    }

    private static UUID uuid(String s) {
        try {
            return s == null ? null : UUID.fromString(s.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String serialize(JsonNode node, int maxChars, String what) {
        if (node == null || node.isNull()) return null;
        String text = node.toString();
        if (text.length() > maxChars) throw ApiException.badRequest("Your " + what + " is too large to save");
        return text;
    }

    private JsonNode parse(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return json.readTree(text);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static Integer inRange(int v, int lo, int hi) {
        return v >= lo && v <= hi ? v : null;
    }

    private static GoalType parseGoalType(String s) {
        try {
            return GoalType.valueOf(s == null ? "" : s.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Unknown target type: " + s);
        }
    }

    private static LocalDate date(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeException e) {
            return null;
        }
    }

    private static String str(String s) {
        return s == null ? "" : s;
    }

    private static int size(Collection<?> c) {
        return c == null ? 0 : c.size();
    }

    private static String clip(String s, int max, String fallback) {
        if (s == null || s.isBlank()) return fallback;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
