package com.ascend.web;

import com.ascend.dto.StateDto;
import com.ascend.service.StateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Whole account in one call, in the same shape as the React app's localStorage. */
@RestController
@RequestMapping("/api/state")
@RequiredArgsConstructor
public class StateController {
    private final StateService state;

    @GetMapping
    public StateDto export(@AuthenticationPrincipal UUID userId) {
        return state.export(userId);
    }

    /** Replaces everything on the server. Use it once to migrate your browser data (Settings -> Export file). */
    @PutMapping
    public StateDto replace(@AuthenticationPrincipal UUID userId, @RequestBody StateDto body) {
        return state.replace(userId, body);
    }
}
