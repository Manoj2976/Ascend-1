package com.ascend.web;

import com.ascend.domain.PushSubscription;
import com.ascend.dto.PushDtos.*;
import com.ascend.notify.LinkCodeStore;
import com.ascend.notify.NotificationService;
import com.ascend.notify.ReminderMessage;
import com.ascend.notify.TelegramService;
import com.ascend.notify.TelegramWebhookHandler;
import com.ascend.notify.WebPushSender;
import com.ascend.repo.PushSubscriptionRepository;
import com.ascend.service.SettingsService;
import com.ascend.config.AscendProperties;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    private final WebPushSender webPush;
    private final PushSubscriptionRepository subscriptions;
    private final NotificationService notifications;
    private final TelegramService telegram;
    private final TelegramWebhookHandler webhook;
    private final LinkCodeStore linkCodes;
    private final SettingsService settings;
    private final AscendProperties props;

    // ---- Web Push -------------------------------------------------------------------------------

    /** The browser needs this public key to subscribe (applicationServerKey). */
    @GetMapping("/push/config")
    public Config pushConfig() {
        return new Config(webPush.enabled(), webPush.publicKey());
    }

    /** Body is the browser's PushSubscription.toJSON(): {endpoint, keys:{p256dh, auth}}. */
    @PostMapping("/push/subscribe")
    @Transactional
    public Map<String, Object> subscribe(@AuthenticationPrincipal UUID userId, @Valid @RequestBody Subscribe r) {
        PushSubscription sub = subscriptions.findByEndpoint(r.endpoint()).orElseGet(PushSubscription::new);
        sub.setUserId(userId);
        sub.setEndpoint(r.endpoint());
        sub.setP256dh(r.keys().p256dh());
        sub.setAuth(r.keys().auth());
        subscriptions.save(sub);
        return Map.of("subscribed", true, "devices", subscriptions.findByUserId(userId).size());
    }

    @DeleteMapping("/push/subscribe")
    @Transactional
    public Map<String, Object> unsubscribe(@AuthenticationPrincipal UUID userId, @Valid @RequestBody Unsubscribe r) {
        subscriptions.findByEndpoint(r.endpoint()).filter(s -> s.getUserId().equals(userId)).ifPresent(subscriptions::delete);
        return Map.of("subscribed", false);
    }

    /** Sends a test message through every channel that is set up. */
    @PostMapping("/notifications/test")
    public TestResult test(@AuthenticationPrincipal UUID userId) {
        return notifications.notifyUser(userId, ReminderMessage.test());
    }

    // ---- Telegram -------------------------------------------------------------------------------

    /** Step 1 of linking: returns a t.me deep link. Tapping it opens the bot and sends "/start CODE". */
    @PostMapping("/telegram/link-code")
    public TelegramLink linkCode(@AuthenticationPrincipal UUID userId) {
        if (!telegram.enabled()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Telegram bot is not configured on the server");
        String code = linkCodes.create(userId);
        String bot = telegram.botUsername();
        String link = bot.isEmpty() ? "" : "https://t.me/" + bot + "?start=" + code;
        return new TelegramLink(code, link, bot, (int) LinkCodeStore.TTL.toMinutes());
    }

    @DeleteMapping("/telegram/link")
    public Map<String, Object> unlink(@AuthenticationPrincipal UUID userId) {
        settings.setTelegramChat(userId, null);
        return Map.of("linked", false);
    }

    /** Called by Telegram itself. The secret in the path is the only credential, so keep it private. */
    @PostMapping("/telegram/webhook/{secret}")
    public Map<String, Object> telegramWebhook(@PathVariable String secret, @RequestBody JsonNode update) {
        String expected = props.telegram().webhookSecret();
        if (expected == null || expected.isBlank()
                || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8))) {
            throw ApiException.notFound("Page");
        }
        try {
            webhook.handle(update);
        } catch (Exception e) {
            log.warn("Telegram update failed: {}", e.toString()); // always answer 200 so Telegram doesn't retry forever
        }
        return Map.of("ok", true);
    }
}
