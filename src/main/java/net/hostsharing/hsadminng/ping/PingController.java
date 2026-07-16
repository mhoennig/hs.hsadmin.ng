package net.hostsharing.hsadminng.ping;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.NoSecurityRequirement;
import net.hostsharing.hsadminng.generated.api.v1.api.TestApi;
import net.hostsharing.hsadminng.generated.api.v1.model.ApiVersionGetResponse200Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@NoSecurityRequirement
public class PingController implements TestApi {

    @Autowired
    private MessageTranslator messageTranslator;

    @Autowired
    private VersionProvider versionProvider;

    @Timed("app.api.ping")
    @Override
    public ResponseEntity<String> ping() {
        // HOWTO translate text with placeholders - also see in resource files i18n/messages_*.properties.
        final var translatedMessage = messageTranslator.translate("test.pinged--in-your-language");
        return ResponseEntity.ok(translatedMessage + "\n");
    }

    @Timed("app.api.pong")
    @Override
    public ResponseEntity<String> pong() {
        return createPongResponse();
    }

    @Timed("app.api.pong")
    @Override
    public ResponseEntity<String> pongPost() {
        return createPongResponse();
    }

    @Timed("app.api.version")
    @Override
    public ResponseEntity<ApiVersionGetResponse200Resource> version() {
        return versionProvider.getVersion();
    }

    private ResponseEntity<String> createPongResponse() {
        final var userName = SecurityContextHolder.getContext().getAuthentication().getName();
        // HOWTO translate text with placeholders - also see in resource files i18n/messages_*.properties.
        final var translatedMessage = messageTranslator.translate("test.ponged-{0}--in-your-language", userName);
        return ResponseEntity.ok(translatedMessage + "\n");
    }
}
