package com.ascend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Any field left null is kept as is, so the UI can autosave one textarea at a time. */
public record JournalRequest(
        @Min(0) @Max(4) Integer mood,
        @Size(max = 10000) String text,
        @Size(max = 5000) String learned,
        @Size(max = 5000) String plan,
        boolean clearMood) {
}
