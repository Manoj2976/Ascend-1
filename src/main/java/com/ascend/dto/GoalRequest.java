package com.ascend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record GoalRequest(
        @NotBlank @Pattern(regexp = "(?i)FINAL|MONTHLY|DAILY") String type,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 40) String unit,
        @NotNull @Min(1) @Max(100000) Integer target,
        @Min(0) @Max(100000) Integer progress,
        @Pattern(regexp = "^$|^\\d{4}-(0[1-9]|1[0-2])$", message = "month must look like 2026-11") String month,
        LocalDate deadline,
        String parentId) {
}
