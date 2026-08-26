package net.hostsharing.hsadminng.config;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthConfigControllerUnitTest {

    @Test
    void reportsOidcMethodWithTheConfiguredIssuerAsAuthority() {
        val response = new AuthConfigController("https://login.example.org/realms/example")
                .getAuthConfig().getBody();

        assertThat(response.getMethod()).isEqualTo("oidc");
        assertThat(response.getAuthority()).isEqualTo("https://login.example.org/realms/example");
    }

    @Test
    void reportsTheFakeOidcProviderAsAuthorityInTheFakeJwtProfile() {
        // the fake-jwt profile serves a complete OIDC provider, so it is reported like any other issuer
        val response = new AuthConfigController("http://localhost:8080/fake-jwt")
                .getAuthConfig().getBody();

        assertThat(response.getMethod()).isEqualTo("oidc");
        assertThat(response.getAuthority()).isEqualTo("http://localhost:8080/fake-jwt");
    }
}
