package net.hostsharing.hsadminng.config;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.generated.api.v1.api.AuthConfigApi;
import net.hostsharing.hsadminng.generated.api.v1.model.AuthConfigResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Reports the authentication configuration derived from this backend's own configuration,
 * so that frontends can configure their login at runtime instead of redundantly per environment. */
@RestController
@NoSecurityRequirement
public class AuthConfigController implements AuthConfigApi {

    private final AuthConfigResource authConfig;

    public AuthConfigController(
            final Environment environment,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") final String issuerUri) {
        this.authConfig = new AuthConfigResource();
        if (environment.acceptsProfiles(Profiles.of("fake-jwt"))) {
            authConfig.setMethod("fake-jwt");
            authConfig.setTokenUrl(FakeJwtController.TOKEN_PATH);
        } else {
            authConfig.setMethod("oidc");
            authConfig.setAuthority(issuerUri);
        }
    }

    @Timed("app.api.authConfig")
    @Override
    public ResponseEntity<AuthConfigResource> getAuthConfig() {
        return ResponseEntity.ok(authConfig);
    }
}
