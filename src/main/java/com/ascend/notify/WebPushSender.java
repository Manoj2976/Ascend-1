package com.ascend.notify;

import com.ascend.config.AscendProperties;
import com.ascend.domain.PushSubscription;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Security;

/** Sends VAPID-signed Web Push messages to browsers. Disabled (and harmless) when no VAPID keys are configured. */
@Component
@Slf4j
public class WebPushSender {
    public enum Outcome { OK, GONE, FAILED }

    private final AscendProperties.Vapid vapid;
    private PushService service;

    public WebPushSender(AscendProperties props) {
        this.vapid = props.vapid();
    }

    @PostConstruct
    void init() {
        if (!vapid.configured()) {
            log.info("Web Push disabled: set VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY to enable it.");
            return;
        }
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            service = new PushService(vapid.publicKey(), vapid.privateKey(), vapid.subject());
            log.info("Web Push enabled.");
        } catch (Exception e) {
            log.error("Web Push disabled: VAPID keys are invalid ({}).", e.getMessage());
        }
    }

    public boolean enabled() {
        return service != null;
    }

    public String publicKey() {
        return enabled() ? vapid.publicKey() : "";
    }

    public Outcome send(PushSubscription sub, String jsonPayload) {
        if (service == null) return Outcome.FAILED;
        try {
            Notification n = new Notification(sub.getEndpoint(), sub.getP256dh(), sub.getAuth(),
                    jsonPayload.getBytes(StandardCharsets.UTF_8));
            int status = service.send(n).getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) return Outcome.OK;
            if (status == 404 || status == 410) return Outcome.GONE; // browser unsubscribed
            log.warn("Push service answered HTTP {}", status);
            return Outcome.FAILED;
        } catch (Exception e) {
            log.warn("Push send failed: {}", e.toString());
            return Outcome.FAILED;
        }
    }
}
