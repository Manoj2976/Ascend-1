package com.ascend.notify;

import com.ascend.domain.PushSubscription;
import com.ascend.domain.UserSettings;
import com.ascend.dto.PushDtos.TestResult;
import com.ascend.repo.PushSubscriptionRepository;
import com.ascend.repo.UserSettingsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/** Fans one reminder out to every channel the user has set up: browser push and Telegram. */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final WebPushSender webPush;
    private final TelegramService telegram;
    private final PushSubscriptionRepository subscriptions;
    private final UserSettingsRepository settings;
    private final ObjectMapper mapper;

    public TestResult notifyUser(UUID userId, ReminderMessage m) {
        int pushed = 0;
        if (webPush.enabled()) {
            String payload = payload(m);
            for (PushSubscription sub : subscriptions.findByUserId(userId)) {
                switch (webPush.send(sub, payload)) {
                    case OK -> pushed++;
                    case GONE -> subscriptions.delete(sub);
                    case FAILED -> { }
                }
            }
        }

        boolean tg = false;
        if (telegram.enabled()) {
            String chat = settings.findById(userId).map(UserSettings::getTelegramChatId).orElse(null);
            if (chat != null && !chat.isBlank()) {
                String text = m.title() + "\n" + m.body();
                tg = m.taskId() != null
                        ? telegram.sendReminder(chat, text, m.taskId(), m.day())
                        : telegram.sendText(chat, text);
            }
        }
        return new TestResult(pushed, tg);
    }

    private String payload(ReminderMessage m) {
        try {
            return mapper.writeValueAsString(Map.of("title", m.title(), "body", m.body(), "tag", m.tag(), "url", "/"));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
