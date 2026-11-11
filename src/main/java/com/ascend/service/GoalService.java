package com.ascend.service;

import com.ascend.domain.Goal;
import com.ascend.domain.GoalType;
import com.ascend.dto.GoalDto;
import com.ascend.dto.GoalRequest;
import com.ascend.dto.LogRequest;
import com.ascend.repo.GoalRepository;
import com.ascend.repo.TaskRepository;
import com.ascend.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoalService {
    private final GoalRepository goals;
    private final TaskRepository tasks;
    private final SettingsService settings;

    @Transactional(readOnly = true)
    public List<GoalDto> list(UUID userId) {
        return goals.findByUserId(userId).stream().map(GoalService::toDto).toList();
    }

    @Transactional
    public GoalDto create(UUID userId, GoalRequest r) {
        Goal g = new Goal();
        g.setUserId(userId);
        g.setType(GoalType.valueOf(r.type().toUpperCase()));
        fill(g, userId, r);
        return toDto(goals.save(g));
    }

    @Transactional
    public GoalDto update(UUID userId, UUID id, GoalRequest r) {
        Goal g = find(userId, id);
        if (g.getType() != GoalType.valueOf(r.type().toUpperCase())) {
            throw ApiException.badRequest("A target's type cannot be changed");
        }
        fill(g, userId, r);
        return toDto(goals.save(g));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Goal g = find(userId, id);
        tasks.clearGoal(userId, id);   // keep the tasks, just unlink them
        goals.clearParent(userId, id); // keep child targets, just unlink them
        goals.delete(g);
    }

    /** Add to (or subtract from) a daily target's count for one day. */
    @Transactional
    public GoalDto log(UUID userId, UUID id, LogRequest r) {
        Goal g = find(userId, id);
        if (g.getType() != GoalType.DAILY) throw ApiException.badRequest("Only daily targets keep a daily log");
        LocalDate day = r.date() != null ? r.date() : settings.today(userId);
        int next = Math.max(0, g.getLogs().getOrDefault(day, 0) + r.delta());
        if (next == 0) g.getLogs().remove(day);
        else g.getLogs().put(day, next);
        return toDto(goals.save(g));
    }

    private Goal find(UUID userId, UUID id) {
        return goals.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Target"));
    }

    private void fill(Goal g, UUID userId, GoalRequest r) {
        g.setTitle(r.title().trim());
        g.setUnit(r.unit() == null ? "" : r.unit().trim());
        g.setTarget(r.target());
        if (g.getType() != GoalType.DAILY) g.setProgress(r.progress() == null ? g.getProgress() : r.progress());

        if (g.getType() == GoalType.MONTHLY) {
            if (r.month() == null || r.month().isBlank()) throw ApiException.badRequest("A monthly target needs a month");
            g.setMonth(r.month());
            g.setParentId(resolveParent(userId, r.parentId()));
        }
        if (g.getType() == GoalType.FINAL) g.setDeadline(r.deadline());
    }

    private UUID resolveParent(UUID userId, String parentId) {
        if (parentId == null || parentId.isBlank()) return null;
        UUID pid;
        try {
            pid = UUID.fromString(parentId);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Invalid parentId");
        }
        Goal p = goals.findByIdAndUserId(pid, userId).orElseThrow(() -> ApiException.badRequest("Unknown parentId"));
        if (p.getType() != GoalType.FINAL) throw ApiException.badRequest("A monthly target can only belong to a final target");
        return p.getId();
    }

    public static GoalDto toDto(Goal g) {
        Map<String, Integer> logs = new TreeMap<>();
        g.getLogs().forEach((d, n) -> logs.put(d.toString(), n));
        return new GoalDto(
                g.getId().toString(),
                g.getType().name(),
                g.getTitle(),
                g.getUnit() == null ? "" : g.getUnit(),
                g.getTarget(),
                g.getProgress(),
                g.getMonth() == null ? "" : g.getMonth(),
                g.getDeadline() == null ? "" : g.getDeadline().toString(),
                g.getParentId() == null ? "" : g.getParentId().toString(),
                logs);
    }
}
