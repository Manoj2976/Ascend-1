package com.ascend.service;

import com.ascend.domain.UserSettings;
import com.ascend.dto.SettingsDto;
import com.ascend.dto.SettingsRequest;
import com.ascend.repo.UserSettingsRepository;
import com.ascend.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettingsService {
    private final UserSettingsRepository repo;

    @Transactional
    public UserSettings getOrCreate(UUID userId) {
        return repo.findById(userId).orElseGet(() -> repo.save(new UserSettings(userId)));
    }

    @Transactional
    public SettingsDto get(UUID userId) {
        return toDto(getOrCreate(userId));
    }

    @Transactional
    public SettingsDto update(UUID userId, SettingsRequest r) {
        UserSettings s = getOrCreate(userId);
        apply(s, r);
        return toDto(repo.save(s));
    }

    public void apply(UserSettings s, SettingsRequest r) {
        if (r.name() != null) s.setName(r.name().trim());
        if (r.theme() != null) s.setTheme(r.theme());
        if (r.windowMin() != null) s.setWindowMin(r.windowMin());
        if (r.intervalMin() != null) s.setIntervalMin(r.intervalMin());
        if (r.sound() != null) s.setSound(r.sound());
        if (r.notifications() != null) s.setNotifications(r.notifications());
        if (r.streakThreshold() != null) s.setStreakThreshold(r.streakThreshold());
        if (r.timezone() != null && !r.timezone().isBlank()) {
            try {
                s.setTimezone(ZoneId.of(r.timezone().trim()).getId());
            } catch (DateTimeException e) {
                throw ApiException.badRequest("Unknown time zone: " + r.timezone());
            }
        }
    }

    @Transactional
    public void setTelegramChat(UUID userId, String chatId) {
        if (chatId != null) {
            // one chat belongs to one account
            repo.findByTelegramChatId(chatId).filter(o -> !o.getUserId().equals(userId)).ifPresent(o -> {
                o.setTelegramChatId(null);
                repo.save(o);
            });
        }
        UserSettings s = getOrCreate(userId);
        s.setTelegramChatId(chatId);
        repo.save(s);
    }

    /** The user's time zone, falling back to IST if the stored value is somehow invalid. */
    public ZoneId zone(UUID userId) {
        return zoneOf(getOrCreate(userId));
    }

    public static ZoneId zoneOf(UserSettings s) {
        try {
            return ZoneId.of(s.getTimezone());
        } catch (DateTimeException e) {
            return ZoneId.of("Asia/Kolkata");
        }
    }

    public LocalDate today(UUID userId) {
        return LocalDate.now(zone(userId));
    }

    public static SettingsDto toDto(UserSettings s) {
        return new SettingsDto(s.getName(), s.getTheme(), s.getWindowMin(), s.getIntervalMin(), s.isSound(),
                s.isNotifications(), s.getStreakThreshold(), s.getTimezone(),
                s.getTelegramChatId() != null && !s.getTelegramChatId().isBlank());
    }
}
