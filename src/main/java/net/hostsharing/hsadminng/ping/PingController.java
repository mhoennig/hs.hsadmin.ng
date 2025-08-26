package net.hostsharing.hsadminng.ping;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.generated.api.v1.api.TestApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "casTicket")
public class PingController implements TestApi {

    @Autowired
    private MessageTranslator messageTranslator;

    @PreAuthorize("permitAll()")
    @Timed("app.api.ping")
    public ResponseEntity<String> ping() {
        final var userName = SecurityContextHolder.getContext().getAuthentication().getName();
        // HOWTO translate text with placeholders - also see in resource files i18n/messages_*.properties.
        final var translatedMessage = messageTranslator.translate("pong {0} - in English", userName);
        return ResponseEntity.ok(translatedMessage + "\n");
    }

    @PreAuthorize("isAuthenticated()")
    @Timed("app.api.pong")
    public ResponseEntity<String> pong() {
        final var userName = SecurityContextHolder.getContext().getAuthentication().getName();
        // HOWTO translate text with placeholders - also see in resource files i18n/messages_*.properties.
        final var translatedMessage = messageTranslator.translate("ping {0} - in English", userName);
        return ResponseEntity.ok(translatedMessage + "\n");
    }
}
