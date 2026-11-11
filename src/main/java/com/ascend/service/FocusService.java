package com.ascend.service;

import com.ascend.domain.FocusLog;
import com.ascend.dto.FocusRequest;
import com.ascend.repo.FocusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FocusService {
    private final FocusRepository focus;
    private final SettingsService settings;

    @Transactional(readOnly = true)
    public Map<String, Integer> list(UUID userId) {
        Map<String, Integer> out = new TreeMap<>();
        focus.findByUserId(userId).forEach(f -> out.put(f.getDate().toString(), f.getMinutes()));
        return out;
    }

    /** Adds minutes to the day's total and returns the new total. */
    @Transactional
    public int add(UUID userId, FocusRequest r) {
        LocalDate day = r.date() != null ? r.date() : settings.today(userId);
        FocusLog log = focus.findByUserIdAndDate(userId, day).orElseGet(() -> {
            FocusLog n = new FocusLog();
            n.setUserId(userId);
            n.setDate(day);
            return n;
        });
        log.setMinutes(log.getMinutes() + r.minutes());
        return focus.save(log).getMinutes();
    }
}
