package com.ascend;

import com.ascend.domain.Repeat;
import com.ascend.reminder.RecurrenceRules;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecurrenceRulesTest {
    private static final LocalDate MON = LocalDate.of(2026, 11, 2); // a Monday

    @Test
    void noneOnlyOccursOnItsDate() {
        assertTrue(RecurrenceRules.occursOn(Repeat.NONE, MON, MON));
        assertFalse(RecurrenceRules.occursOn(Repeat.NONE, MON, MON.plusDays(1)));
    }

    @Test
    void dailyStartsOnItsDate() {
        assertFalse(RecurrenceRules.occursOn(Repeat.DAILY, MON, MON.minusDays(1)));
        assertTrue(RecurrenceRules.occursOn(Repeat.DAILY, MON, MON));
        assertTrue(RecurrenceRules.occursOn(Repeat.DAILY, MON, MON.plusDays(40)));
    }

    @Test
    void weekdaysSkipWeekends() {
        assertTrue(RecurrenceRules.occursOn(Repeat.WEEKDAYS, MON, MON.plusDays(4)));  // Friday
        assertFalse(RecurrenceRules.occursOn(Repeat.WEEKDAYS, MON, MON.plusDays(5))); // Saturday
        assertFalse(RecurrenceRules.occursOn(Repeat.WEEKDAYS, MON, MON.plusDays(6))); // Sunday
        assertTrue(RecurrenceRules.occursOn(Repeat.WEEKDAYS, MON, MON.plusDays(7)));  // next Monday
    }

    @Test
    void weeklyKeepsTheSameWeekday() {
        assertTrue(RecurrenceRules.occursOn(Repeat.WEEKLY, MON, MON.plusWeeks(3)));
        assertFalse(RecurrenceRules.occursOn(Repeat.WEEKLY, MON, MON.plusWeeks(3).plusDays(1)));
        assertFalse(RecurrenceRules.occursOn(Repeat.WEEKLY, MON, MON.minusWeeks(1)));
    }

    @Test
    void repeatParsingIsForgiving() {
        assertEquals(Repeat.DAILY, Repeat.parse("daily"));
        assertEquals(Repeat.WEEKDAYS, Repeat.parse(" WEEKDAYS "));
        assertEquals(Repeat.NONE, Repeat.parse(""));
        assertEquals(Repeat.NONE, Repeat.parse("nonsense"));
        assertEquals("weekly", Repeat.WEEKLY.wire());
    }
}
