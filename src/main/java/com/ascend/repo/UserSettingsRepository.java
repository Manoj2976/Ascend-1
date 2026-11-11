package com.ascend.repo;

import com.ascend.domain.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID> {
    List<UserSettings> findByNotificationsTrue();

    Optional<UserSettings> findByTelegramChatId(String chatId);
}
