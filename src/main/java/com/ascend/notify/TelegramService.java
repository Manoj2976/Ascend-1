package com.ascend.notify;

import com.ascend.config.AscendProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Thin client for the Telegram Bot API. The bot token is never logged. */
@Service
@Slf4j
public class TelegramService {
    private final AscendProperties.Telegram cfg;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public TelegramService(AscendProperties props, ObjectMapper mapper) {
        this.cfg = props.telegram();
        this.mapper = mapper;
    }

    public boolean enabled() {
        return cfg.configured();
    }

    public String botUsername() {
        return cfg.botUsername() == null ? "" : cfg.botUsername().replace("@", "");
    }

    public boolean sendText(String chatId, String text) {
        return call("sendMessage", body("chat_id", chatId, "text", text)) != null;
    }

    /** Reminder with a one-tap "Done" button that ticks the task and stops further reminders. */
    public boolean sendReminder(String chatId, String text, UUID taskId, LocalDate day) {
        Map<String, Object> b = body("chat_id", chatId, "text", text);
        if (taskId != null && day != null) {
            b.put("reply_markup", Map.of("inline_keyboard", List.of(List.of(
                    Map.of("text", "✅ Done", "callback_data", "done:" + taskId + ":" + day)))));
        }
        return call("sendMessage", b) != null;
    }

    public void answerCallback(String callbackId, String text) {
        call("answerCallbackQuery", body("callback_query_id", callbackId, "text", text));
    }

    public void editMessageText(String chatId, long messageId, String text) {
        call("editMessageText", body("chat_id", chatId, "message_id", messageId, "text", text));
    }

    /** Registers {PUBLIC_URL}/api/telegram/webhook/{secret} so Telegram pushes button taps and /start to us. */
    @EventListener(ApplicationReadyEvent.class)
    public void registerWebhook() {
        if (!cfg.webhookReady()) {
            if (cfg.configured()) log.info("Telegram: set PUBLIC_URL and TELEGRAM_WEBHOOK_SECRET to enable linking and the Done button.");
            return;
        }
        String base = cfg.publicUrl().replaceAll("/+$", "");
        JsonNode res = call("setWebhook", body("url", base + "/api/telegram/webhook/" + cfg.webhookSecret(),
                "allowed_updates", List.of("message", "callback_query")));
        log.info(res != null ? "Telegram webhook registered." : "Telegram webhook registration failed.");
    }

    private JsonNode call(String method, Map<String, Object> payload) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.telegram.org/bot" + cfg.botToken() + "/" + method))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                log.warn("Telegram {} failed with HTTP {}", method, res.statusCode());
                return null;
            }
            return mapper.readTree(res.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.warn("Telegram {} failed: {}", method, e.getClass().getSimpleName()); // message may contain the token URL
            return null;
        }
    }

    private static Map<String, Object> body(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }
}
