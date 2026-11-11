package com.ascend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

public record LogRequest(LocalDate date, @Min(-1000) @Max(1000) int delta) {
}
