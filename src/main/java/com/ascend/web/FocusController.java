package com.ascend.web;

import com.ascend.dto.FocusRequest;
import com.ascend.service.FocusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/focus")
@RequiredArgsConstructor
public class FocusController {
    private final FocusService focus;

    /** Minutes focused per day, keyed by date. */
    @GetMapping
    public Map<String, Integer> list(@AuthenticationPrincipal UUID userId) {
        return focus.list(userId);
    }

    /** Add a finished focus session. Returns the day's new total. */
    @PostMapping
    public Map<String, Integer> add(@AuthenticationPrincipal UUID userId, @Valid @RequestBody FocusRequest r) {
        return Map.of("minutes", focus.add(userId, r));
    }
}
