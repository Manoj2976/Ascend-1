package com.ascend.domain;

public enum Repeat {
    NONE, DAILY, WEEKDAYS, WEEKLY;

    public static Repeat parse(String s) {
        if (s == null || s.isBlank()) return NONE;
        try {
            return valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }

    public String wire() {
        return name().toLowerCase();
    }
}
