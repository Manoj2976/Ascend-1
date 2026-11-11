package com.ascend.repo;

import com.ascend.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByUserId(UUID userId);

    Optional<Task> findByIdAndUserId(UUID id, UUID userId);

    void deleteByUserId(UUID userId);

    /** Tasks that have a time and could occur on {@code today}; exact recurrence is checked in Java. */
    @Query("select t from Task t where t.userId = :userId and t.time is not null and t.date <= :today "
            + "and (t.repeat <> com.ascend.domain.Repeat.NONE or t.date = :today)")
    List<Task> findReminderCandidates(@Param("userId") UUID userId, @Param("today") LocalDate today);

    @Modifying
    @Query("update Task t set t.goalId = null where t.userId = :userId and t.goalId = :goalId")
    int clearGoal(@Param("userId") UUID userId, @Param("goalId") UUID goalId);
}
