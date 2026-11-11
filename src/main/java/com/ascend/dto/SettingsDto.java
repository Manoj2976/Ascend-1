package com.ascend.dto;

public record SettingsDto(
        String name,
        String theme,
        int windowMin,
        int intervalMin,
        boolean sound,
        boolean notifications,
        int streakThreshold,
        String timezone,
        boolean telegramLinked) {
}
