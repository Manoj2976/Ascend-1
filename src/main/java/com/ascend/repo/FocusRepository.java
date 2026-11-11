package com.ascend.repo;

import com.ascend.domain.FocusLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FocusRepository extends JpaRepository<FocusLog, UUID> {
    List<FocusLog> findByUserId(UUID userId);

    Optional<FocusLog> findByUserIdAndDate(UUID userId, LocalDate date);

    void deleteByUserId(UUID userId);
}
