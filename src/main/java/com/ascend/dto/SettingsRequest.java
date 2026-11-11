package com.ascend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Partial update: null fields are left unchanged. */
public record SettingsRequest(
        @Size(max = 80) String name,
        @Pattern(regexp = "light|dark|auto") String theme,
        @Min(5) @Max(180) Integer windowMin,
        @Min(1) @Max(60) Integer intervalMin,
        Boolean sound,
        Boolean notifications,
        @Min(10) @Max(100) Integer streakThreshold,
        @Size(max = 64) String timezone) {
}
