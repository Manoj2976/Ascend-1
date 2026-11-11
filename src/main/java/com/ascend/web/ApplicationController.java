package com.ascend.web;

import com.ascend.dto.ApplicationDto;
import com.ascend.dto.ApplicationRequest;
import com.ascend.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService apps;

    @GetMapping
    public List<ApplicationDto> list(@AuthenticationPrincipal UUID userId) {
        return apps.list(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationDto create(@AuthenticationPrincipal UUID userId, @Valid @RequestBody ApplicationRequest r) {
        return apps.create(userId, r);
    }

    @PutMapping("/{id}")
    public ApplicationDto update(@AuthenticationPrincipal UUID userId, @PathVariable UUID id, @Valid @RequestBody ApplicationRequest r) {
        return apps.update(userId, id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        apps.delete(userId, id);
    }
}
