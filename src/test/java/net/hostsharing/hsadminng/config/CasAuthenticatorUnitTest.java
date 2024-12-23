package net.hostsharing.hsadminng.config;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class CasAuthenticatorUnitTest {

    final CasAuthenticator casAuthenticator = new CasAuthenticator();

    @Test
    void bypassesAuthenticationIfNoCasServerIsConfigured() {

        // given
        final var request = mock(HttpServletRequest.class);
        given(request.getHeader("current-subject")).willReturn("given-user");

        // when
        final var userName = casAuthenticator.authenticate(request);

        // then
        assertThat(userName).isEqualTo("given-user");
    }
}
