package com.ascend.reminder;

import com.ascend.domain.Repeat;
import com.ascend.domain.Task;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Same rules as the React app's occursOn(): which days does a (possibly repeating) task appear on? */
public final class RecurrenceRules {
    private RecurrenceRules() {}

    public static boolean occursOn(Repeat repeat, LocalDate start, LocalDate day) {
        return switch (repeat) {
            case DAILY -> !day.isBefore(start);
            case WEEKDAYS -> {
                DayOfWeek d = day.getDayOfWeek();
                yield !day.isBefore(start) && d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY;
            }
            case WEEKLY -> !day.isBefore(start) && day.getDayOfWeek() == start.getDayOfWeek();
            case NONE -> day.equals(start);
        };
    }

    public static boolean occursOn(Task t, LocalDate day) {
        return occursOn(t.getRepeat(), t.getDate(), day);
    }
}
