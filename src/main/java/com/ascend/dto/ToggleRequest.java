package com.ascend.dto;

import java.time.LocalDate;

/** {@code date} is the occurrence being ticked. Defaults to today in the user's time zone. */
public record ToggleRequest(LocalDate date) {
}
