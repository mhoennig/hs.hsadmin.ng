package net.hostsharing.hsadminng.ping;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.NoSecurityRequirement;
import net.hostsharing.hsadminng.generated.api.v1.api.TestApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("isAuthenticated()")
@NoSecurityRequirement
public class PingController implements TestApi {

    @Autowired
    private MessageTranslator messageTranslator;

    @PreAuthorize("permitAll()")
    @Timed("app.api.ping")
    public ResponseEntity<String> ping() {
        // HOWTO translate text with placeholders - also see in resource files i18n/messages_*.properties.
        final var translatedMessage = messageTranslator.translate("test.pinged--in-your-language");
        return ResponseEntity.ok(translatedMessage + "\n");
    }

    @PreAuthorize("isAuthenticated()")
    @Timed("app.api.pong")
    public ResponseEntity<String> pong() {
        final var userName = SecurityContextHolder.getContext().getAuthentication().getName();
        // HOWTO translate text with placeholders - also see in resource files i18n/messages_*.properties.
        final var translatedMessage = messageTranslator.translate("test.ponged-{0}--in-your-language", userName);
        return ResponseEntity.ok(translatedMessage + "\n");
    }
}
