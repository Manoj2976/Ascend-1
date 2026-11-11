package com.ascend.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Whole account in the same shape the React app keeps in localStorage. Used for sync, backup and migration.
 * <p>
 * {@code revision}: on export it is the server's current revision. On import it is the revision the client's
 * copy was based on; if it is not the server's current one the import is refused with 409 so that a stale
 * device cannot overwrite newer data. Leave it out to overwrite unconditionally.
 */
public record StateDto(
        List<TaskDto> tasks,
        List<GoalDto> goals,
        Map<String, JournalDto> journal,
        List<ApplicationDto> applications,
        Map<String, Integer> focus,
        SettingsDto settings,
        JsonNode timetable,
        JsonNode diet,
        Long revision) {
}
