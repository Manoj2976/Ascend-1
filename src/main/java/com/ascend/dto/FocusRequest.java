package com.ascend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

public record FocusRequest(@Min(1) @Max(600) int minutes, LocalDate date) {
}
