package com.ascend.dto;

import java.util.List;

/** Wire shape used by the React app: empty string means "none", repeat is lower-case. */
public record TaskDto(
        String id,
        String title,
        String date,
        String time,
        String category,
        String repeat,
        String goalId,
        String notes,
        List<String> doneDates) {
}
