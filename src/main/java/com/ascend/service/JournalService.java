package com.ascend.service;

import com.ascend.domain.JournalEntry;
import com.ascend.dto.JournalDto;
import com.ascend.dto.JournalRequest;
import com.ascend.repo.JournalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JournalService {
    private final JournalRepository journal;

    @Transactional(readOnly = true)
    public Map<String, JournalDto> list(UUID userId) {
        Map<String, JournalDto> out = new LinkedHashMap<>();
        journal.findByUserIdOrderByDateDesc(userId).forEach(e -> out.put(e.getDate().toString(), toDto(e)));
        return out;
    }

    @Transactional(readOnly = true)
    public JournalDto get(UUID userId, LocalDate date) {
        return journal.findByUserIdAndDate(userId, date).map(JournalService::toDto).orElse(new JournalDto(null, "", "", ""));
    }

    /** Upsert. Null fields are left unchanged so the UI can autosave one box at a time. */
    @Transactional
    public JournalDto save(UUID userId, LocalDate date, JournalRequest r) {
        JournalEntry e = journal.findByUserIdAndDate(userId, date).orElseGet(() -> {
            JournalEntry n = new JournalEntry();
            n.setUserId(userId);
            n.setDate(date);
            return n;
        });
        if (r.clearMood()) e.setMood(null);
        else if (r.mood() != null) e.setMood(r.mood());
        if (r.text() != null) e.setText(r.text());
        if (r.learned() != null) e.setLearned(r.learned());
        if (r.plan() != null) e.setPlan(r.plan());
        return toDto(journal.save(e));
    }

    @Transactional
    public void delete(UUID userId, LocalDate date) {
        journal.deleteByUserIdAndDate(userId, date);
    }

    public static JournalDto toDto(JournalEntry e) {
        return new JournalDto(e.getMood(), nz(e.getText()), nz(e.getLearned()), nz(e.getPlan()));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
