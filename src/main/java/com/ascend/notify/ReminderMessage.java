package com.ascend.notify;

import java.time.LocalDate;
import java.util.UUID;

/** taskId and day are set for real reminders (they enable the Telegram "Done" button); null for test messages. */
public record ReminderMessage(String title, String body, String tag, UUID taskId, LocalDate day) {
    public static ReminderMessage test() {
        return new ReminderMessage("⏰ Test reminder", "If you can see this, reminders will reach you.", "ascend-test", null, null);
    }
}
