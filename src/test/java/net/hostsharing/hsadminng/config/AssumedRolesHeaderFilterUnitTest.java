package net.hostsharing.hsadminng.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssumedRolesHeaderFilterUnitTest {

    private final AssumedRolesHeaderFilter filter = new AssumedRolesHeaderFilter();

    @Test
    void wrapsDeprecatedAssumedRolesHeaderAsCurrentHeader() throws ServletException, IOException {
        final var request = new MockHttpServletRequest();
        request.addHeader(AssumedRolesHeader.DEPRECATED_NAME, "some-role");
        request.addHeader(AssumedRolesHeader.DEPRECATED_NAME, "another-role");
        request.addHeader("some-other-header", "some-other-value");

        final var wrappedRequest = filter(request);

        assertThat(wrappedRequest).isInstanceOf(HttpServletRequestWrapper.class);
        assertThat(wrappedRequest).isNotSameAs(request);

        assertThat(wrappedRequest.getHeader(AssumedRolesHeader.NAME)).isEqualTo("some-role");
        assertThat(wrappedRequest.getHeader(AssumedRolesHeader.NAME.toUpperCase())).isEqualTo("some-role");
        assertThat(wrappedRequest.getHeader("some-other-header")).isEqualTo("some-other-value");

        assertThat(headers(wrappedRequest, AssumedRolesHeader.NAME)).containsExactly("some-role", "another-role");
        assertThat(headers(wrappedRequest, "some-other-header")).containsExactly("some-other-value");

        assertThat(headerNames(wrappedRequest)).contains(
                AssumedRolesHeader.DEPRECATED_NAME,
                "some-other-header",
                AssumedRolesHeader.NAME);
    }

    @Test
    void doesNotAddCurrentAssumedRolesHeaderNameIfAlreadyPresentCaseInsensitive() throws ServletException, IOException {
        final var request = mock(HttpServletRequest.class);
        when(request.getHeader(AssumedRolesHeader.NAME)).thenReturn(null);
        when(request.getHeader(AssumedRolesHeader.DEPRECATED_NAME)).thenReturn("some-role");
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of(
                AssumedRolesHeader.NAME.toUpperCase(),
                AssumedRolesHeader.DEPRECATED_NAME)));

        final var wrappedRequest = filter(request);

        assertThat(headerNames(wrappedRequest)).containsExactly(
                AssumedRolesHeader.NAME.toUpperCase(),
                AssumedRolesHeader.DEPRECATED_NAME);
    }

    @Test
    void rejectsConflictingAssumedRolesHeaders() throws ServletException, IOException {
        final var request = new MockHttpServletRequest();
        request.addHeader(AssumedRolesHeader.NAME, "some-role");
        request.addHeader(AssumedRolesHeader.DEPRECATED_NAME, "another-role");
        final var response = new MockHttpServletResponse();

        final var forwardedRequest = new AtomicReference<ServletRequest>();
        filter.doFilterInternal(request, response, (servletRequest, servletResponse) -> forwardedRequest.set(servletRequest));

        assertThat(forwardedRequest.get()).isNull();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains(
                "\"message\":\"ERROR: [400] headers 'Hostsharing-Assumed-Roles' and 'assumed-roles' must either match or only one may be used\"");
    }

    private HttpServletRequest filter(final HttpServletRequest request) throws ServletException, IOException {
        final var forwardedRequest = new AtomicReference<ServletRequest>();

        filter.doFilterInternal(
                request,
                new MockHttpServletResponse(),
                (servletRequest, servletResponse) -> forwardedRequest.set(servletRequest));

        return (HttpServletRequest) forwardedRequest.get();
    }

    private List<String> headers(final HttpServletRequest request, final String name) {
        return Collections.list(request.getHeaders(name));
    }

    private List<String> headerNames(final HttpServletRequest request) {
        return Collections.list(request.getHeaderNames());
    }
}
