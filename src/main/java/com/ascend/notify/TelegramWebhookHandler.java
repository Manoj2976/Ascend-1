package com.ascend.notify;

import com.ascend.domain.UserSettings;
import com.ascend.repo.UserSettingsRepository;
import com.ascend.service.SettingsService;
import com.ascend.service.TaskService;
import com.ascend.web.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Handles updates Telegram posts to our webhook: "/start CODE" to link, "/stop" to unlink, "Done" button taps. */
@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramWebhookHandler {
    private final TelegramService telegram;
    private final LinkCodeStore codes;
    private final SettingsService settingsService;
    private final UserSettingsRepository settingsRepo;
    private final TaskService taskService;

    public void handle(JsonNode update) {
        if (update.hasNonNull("message")) onMessage(update.get("message"));
        else if (update.hasNonNull("callback_query")) onCallback(update.get("callback_query"));
    }

    private void onMessage(JsonNode msg) {
        String chatId = msg.path("chat").path("id").asText("");
        String text = msg.path("text").asText("").trim();
        if (chatId.isEmpty() || text.isEmpty()) return;

        if (text.startsWith("/start")) {
            String code = text.length() > 6 ? text.substring(6).trim() : "";
            Optional<UUID> user = codes.consume(code);
            if (user.isPresent()) {
                settingsService.setTelegramChat(user.get(), chatId);
                telegram.sendText(chatId, "✅ Linked! I'll message you here when a task is due, and keep reminding until you tap Done.");
            } else {
                telegram.sendText(chatId, "Open Ascend → Settings → Link Telegram, then tap the link it gives you. (Codes expire after 10 minutes.)");
            }
        } else if (text.startsWith("/stop")) {
            settingsRepo.findByTelegramChatId(chatId).ifPresent(s -> settingsService.setTelegramChat(s.getUserId(), null));
            telegram.sendText(chatId, "Unlinked. You won't get reminders here any more.");
        }
    }

    private void onCallback(JsonNode cb) {
        String id = cb.path("id").asText("");
        String data = cb.path("data").asText("");
        String chatId = cb.path("message").path("chat").path("id").asText("");
        long messageId = cb.path("message").path("message_id").asLong(0);

        Optional<UserSettings> owner = chatId.isEmpty() ? Optional.empty() : settingsRepo.findByTelegramChatId(chatId);
        if (owner.isEmpty() || !data.startsWith("done:")) {
            telegram.answerCallback(id, "Not linked");
            return;
        }
        try {
            String[] parts = data.split(":", 3); // done:<taskId>:<yyyy-MM-dd>
            var task = taskService.setDone(owner.get().getUserId(), UUID.fromString(parts[1]), LocalDate.parse(parts[2]), true);
            telegram.answerCallback(id, "Marked done ✅");
            if (messageId != 0) telegram.editMessageText(chatId, messageId, "✅ " + task.title() + " — done");
        } catch (ApiException e) {
            telegram.answerCallback(id, e.getMessage());
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException | DateTimeException e) {
            telegram.answerCallback(id, "That button is out of date");
        }
    }
}
