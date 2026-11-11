package com.ascend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull LocalDate date,
        String time,
        @Size(max = 40) String category,
        String repeat,
        String goalId,
        @Size(max = 2000) String notes) {
}
