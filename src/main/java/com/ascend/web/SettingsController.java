package com.ascend.web;

import com.ascend.dto.SettingsDto;
import com.ascend.dto.SettingsRequest;
import com.ascend.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {
    private final SettingsService settings;

    @GetMapping
    public SettingsDto get(@AuthenticationPrincipal UUID userId) {
        return settings.get(userId);
    }

    @PutMapping
    public SettingsDto update(@AuthenticationPrincipal UUID userId, @Valid @RequestBody SettingsRequest r) {
        return settings.update(userId, r);
    }
}
