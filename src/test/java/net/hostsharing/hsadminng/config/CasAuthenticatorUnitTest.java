package net.hostsharing.hsadminng.config;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class CasAuthenticatorUnitTest {

    final RealCasAuthenticator casAuthenticator = new RealCasAuthenticator();

    @Test
    void bypassesAuthenticationIfNoCasServerIsConfigured() {

        // given
        final var request = mock(HttpServletRequest.class);
        // bypassing the CAS-server HTTP-request fakes the user from the authorization header's fake CAS-ticket
        given(request.getHeader("authorization")).willReturn("Bearer given-user");

        // when
        final var userName = casAuthenticator.authenticate(request);

        // then
        assertThat(userName).isEqualTo("given-user");
    }
}
