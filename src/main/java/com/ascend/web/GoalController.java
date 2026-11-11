package com.ascend.web;

import com.ascend.dto.GoalDto;
import com.ascend.dto.GoalRequest;
import com.ascend.dto.LogRequest;
import com.ascend.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {
    private final GoalService goals;

    @GetMapping
    public List<GoalDto> list(@AuthenticationPrincipal UUID userId) {
        return goals.list(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalDto create(@AuthenticationPrincipal UUID userId, @Valid @RequestBody GoalRequest r) {
        return goals.create(userId, r);
    }

    @PutMapping("/{id}")
    public GoalDto update(@AuthenticationPrincipal UUID userId, @PathVariable UUID id, @Valid @RequestBody GoalRequest r) {
        return goals.update(userId, id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        goals.delete(userId, id);
    }

    /** Daily targets: {"delta": 1} adds one for today, {"date":"2026-11-09","delta":-1} removes one from that day. */
    @PostMapping("/{id}/log")
    public GoalDto log(@AuthenticationPrincipal UUID userId, @PathVariable UUID id, @Valid @RequestBody LogRequest r) {
        return goals.log(userId, id, r);
    }
}
