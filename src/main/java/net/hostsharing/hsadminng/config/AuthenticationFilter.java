package net.hostsharing.hsadminng.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFilter implements Filter {

    @Autowired
    private Authenticator authenticator;

    @Override
    @SneakyThrows
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) {
        final var httpRequest = (HttpServletRequest) request;
        final var httpResponse = (HttpServletResponse) response;

        try {
            final var currentSubject = authenticator.authenticate(httpRequest);

            final var authenticatedRequest = new AuthenticatedHttpServletRequestWrapper(httpRequest);
            authenticatedRequest.addHeader("current-subject", currentSubject);

            chain.doFilter(authenticatedRequest, response);
        } catch (final BadCredentialsException exc) {
            // TODO.impl: should not be necessary if ResponseStatusException worked
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
