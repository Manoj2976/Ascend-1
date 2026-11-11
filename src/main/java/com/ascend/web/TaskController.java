package com.ascend.web;

import com.ascend.dto.TaskDto;
import com.ascend.dto.TaskRequest;
import com.ascend.dto.ToggleRequest;
import com.ascend.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService tasks;

    @GetMapping
    public List<TaskDto> list(@AuthenticationPrincipal UUID userId) {
        return tasks.list(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDto create(@AuthenticationPrincipal UUID userId, @Valid @RequestBody TaskRequest r) {
        return tasks.create(userId, r);
    }

    @PutMapping("/{id}")
    public TaskDto update(@AuthenticationPrincipal UUID userId, @PathVariable UUID id, @Valid @RequestBody TaskRequest r) {
        return tasks.update(userId, id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        tasks.delete(userId, id);
    }

    /** Tick / untick one occurrence. Body is optional: {"date":"2026-11-10"}. */
    @PostMapping("/{id}/toggle")
    public TaskDto toggle(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                          @RequestBody(required = false) ToggleRequest r) {
        return tasks.toggle(userId, id, r == null ? null : r.date());
    }
}
