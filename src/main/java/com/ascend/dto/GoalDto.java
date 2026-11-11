package com.ascend.dto;

import java.util.Map;

public record GoalDto(
        String id,
        String type,
        String title,
        String unit,
        int target,
        int progress,
        String month,
        String deadline,
        String parentId,
        Map<String, Integer> logs) {
}
