package net.hostsharing.hsadminng.config;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class AuthConfigControllerUnitTest {

    @Test
    void reportsFakeJwtMethodWithTokenUrlWhenTheFakeJwtProfileIsActive() {
        val environment = new MockEnvironment();
        environment.setActiveProfiles("fake-jwt");

        val response = new AuthConfigController(environment, "").getAuthConfig().getBody();

        assertThat(response.getMethod()).isEqualTo("fake-jwt");
        assertThat(response.getTokenUrl()).isEqualTo("/fake-jwt/token");
        assertThat(response.getAuthority()).isNull();
    }

    @Test
    void reportsOidcMethodWithTheConfiguredIssuerAsAuthorityOtherwise() {
        val response = new AuthConfigController(
                new MockEnvironment(), "https://login.example.org/realms/example")
                .getAuthConfig().getBody();

        assertThat(response.getMethod()).isEqualTo("oidc");
        assertThat(response.getAuthority()).isEqualTo("https://login.example.org/realms/example");
        assertThat(response.getTokenUrl()).isNull();
    }
}
