package com.ascend.repo;

import com.ascend.domain.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {
    List<Goal> findByUserId(UUID userId);

    Optional<Goal> findByIdAndUserId(UUID id, UUID userId);

    void deleteByUserId(UUID userId);

    @Modifying
    @Query("update Goal g set g.parentId = null where g.userId = :userId and g.parentId = :parentId")
    int clearParent(@Param("userId") UUID userId, @Param("parentId") UUID parentId);
}
