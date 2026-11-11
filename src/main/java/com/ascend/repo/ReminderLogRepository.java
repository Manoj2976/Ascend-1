package com.ascend.repo;

import com.ascend.domain.ReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface ReminderLogRepository extends JpaRepository<ReminderLog, String> {
    void deleteByTaskId(UUID taskId);

    void deleteByDayBefore(LocalDate day);
}
