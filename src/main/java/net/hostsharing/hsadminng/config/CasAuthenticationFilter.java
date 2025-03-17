package net.hostsharing.hsadminng.config;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Do NOT use @Component (or similar) here, this would register the filter directly.
// But we need to register it in the SecurityFilterChain created by WebSecurityConfig.
// The bean gets created in net.hostsharing.hsadminng.config.WebSecurityConfig.authenticationFilter.
@AllArgsConstructor
public class CasAuthenticationFilter extends OncePerRequestFilter {

    private CasAuthenticator authenticator;

    @Override
    @SneakyThrows
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {

        if (request.getHeader("authorization") != null) {
            final var authenticatedRequest = new AuthenticatedHttpServletRequestWrapper(request);
            final var currentSubject = authenticator.authenticate(request);
            authenticatedRequest.addHeader("current-subject", currentSubject);
            filterChain.doFilter(authenticatedRequest, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
