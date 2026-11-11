package com.ascend.repo;

import com.ascend.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalRepository extends JpaRepository<JournalEntry, UUID> {
    List<JournalEntry> findByUserIdOrderByDateDesc(UUID userId);

    Optional<JournalEntry> findByUserIdAndDate(UUID userId, LocalDate date);

    void deleteByUserId(UUID userId);

    void deleteByUserIdAndDate(UUID userId, LocalDate date);
}
