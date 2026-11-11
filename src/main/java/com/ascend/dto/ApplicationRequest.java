package com.ascend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ApplicationRequest(
        @NotBlank @Size(max = 120) String company,
        @Size(max = 120) String role,
        String status,
        LocalDate date,
        @Size(max = 1000) String link,
        @Size(max = 2000) String notes) {
}
