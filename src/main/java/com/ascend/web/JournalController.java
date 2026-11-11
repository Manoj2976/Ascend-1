package com.ascend.web;

import com.ascend.dto.JournalDto;
import com.ascend.dto.JournalRequest;
import com.ascend.service.JournalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/journal")
@RequiredArgsConstructor
public class JournalController {
    private final JournalService journal;

    @GetMapping
    public Map<String, JournalDto> list(@AuthenticationPrincipal UUID userId) {
        return journal.list(userId);
    }

    @GetMapping("/{date}")
    public JournalDto get(@AuthenticationPrincipal UUID userId,
                          @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return journal.get(userId, date);
    }

    @PutMapping("/{date}")
    public JournalDto save(@AuthenticationPrincipal UUID userId,
                           @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @Valid @RequestBody JournalRequest r) {
        return journal.save(userId, date, r);
    }

    @DeleteMapping("/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId,
                       @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        journal.delete(userId, date);
    }
}
