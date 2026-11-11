package com.ascend.repo;

import com.ascend.domain.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    List<Application> findByUserId(UUID userId);

    Optional<Application> findByIdAndUserId(UUID id, UUID userId);

    void deleteByUserId(UUID userId);
}
