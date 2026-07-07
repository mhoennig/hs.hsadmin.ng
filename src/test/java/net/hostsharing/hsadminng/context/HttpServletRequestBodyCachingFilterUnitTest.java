package net.hostsharing.hsadminng.context;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.UncheckedIOException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

class HttpServletRequestBodyCachingFilterUnitTest {

    private final HttpServletRequestBodyCachingFilter filter = new HttpServletRequestBodyCachingFilter();

    @Test
    void wrappedRequestBodyIsReplayableEvenAfterItGotConsumed() throws Exception {
        // given
        final var givenBody = "{\"name\": \"some-value\"}";
        final var request = new MockHttpServletRequest("POST", "/api/some-endpoint");
        request.setContent(givenBody.getBytes(StandardCharsets.UTF_8));
        final var wrappedRequest = new AtomicReference<HttpServletRequest>();

        // when
        filter.doFilterInternal(request, new MockHttpServletResponse(), (req, res) -> {
            wrappedRequest.set((HttpServletRequest) req);
            // consume the body like Spring's request-body handling does
            try {
                req.getInputStream().readAllBytes();
            } catch (final IOException exc) {
                throw new UncheckedIOException(exc);
            }
        });

        // then the body can still be read, even multiple times
        assertThat(wrappedRequest.get().getReader().lines().collect(joining())).isEqualTo(givenBody);
        assertThat(wrappedRequest.get().getReader().lines().collect(joining())).isEqualTo(givenBody);
    }

    @Test
    void formEncodedRequestIsPassedThroughUnwrapped() throws Exception {
        // given a form-encoded request, whose body the container itself parses for getParameter(...)
        final var request = new MockHttpServletRequest("POST", "/fake-jwt/token");
        request.setContentType("application/x-www-form-urlencoded");
        request.setContent("username=xyz-form_user&password=ignored".getBytes(StandardCharsets.UTF_8));
        final var passedRequest = new AtomicReference<HttpServletRequest>();

        // when
        filter.doFilterInternal(request, new MockHttpServletResponse(),
                (req, res) -> passedRequest.set((HttpServletRequest) req));

        // then the request is not wrapped, so the container can still parse the form parameters
        assertThat(passedRequest.get()).isSameAs(request);
    }

    @Test
    void wrappedRequestReaderUsesDeclaredCharacterEncoding() throws Exception {
        // given
        final var givenBody = "{\"name\": \"säme-välue\"}";
        final var request = new MockHttpServletRequest("POST", "/api/some-endpoint");
        request.setCharacterEncoding("ISO-8859-1");
        request.setContent(givenBody.getBytes(StandardCharsets.ISO_8859_1));
        final var wrappedRequest = new AtomicReference<HttpServletRequest>();

        // when
        filter.doFilterInternal(request, new MockHttpServletResponse(),
                (req, res) -> wrappedRequest.set((HttpServletRequest) req));

        // then
        assertThat(wrappedRequest.get().getReader().lines().collect(joining())).isEqualTo(givenBody);
    }
}
