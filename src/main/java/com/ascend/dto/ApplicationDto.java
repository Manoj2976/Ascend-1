package com.ascend.dto;

public record ApplicationDto(
        String id,
        String company,
        String role,
        String status,
        String date,
        String link,
        String notes) {
}
