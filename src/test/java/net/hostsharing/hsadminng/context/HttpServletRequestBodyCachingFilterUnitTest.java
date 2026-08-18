package net.hostsharing.hsadminng.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.UncheckedIOException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

class HttpServletRequestBodyCachingFilterUnitTest {

    private static final int MAX_EXPECTED_REQUEST_BODY_SIZE = HttpServletRequestBodyCachingFilter.MAX_REQUEST_BODY_SIZE;

    // the JavaTimeModule mirrors Spring Boot's auto-configured ObjectMapper for the CustomErrorResponse timestamp
    private final HttpServletRequestBodyCachingFilter filter = new HttpServletRequestBodyCachingFilter(
            new ObjectMapper().registerModule(new JavaTimeModule()));

    // verifies Fix for Security-Finding A-10
    @Test
    void rejectsOversizedRequestBodyBeforePassingItDownTheFilterChain() throws Exception {
        val request = new MockHttpServletRequest("POST", "/api/some-endpoint");
        request.setContentType("application/json");
        request.setContent(new byte[MAX_EXPECTED_REQUEST_BODY_SIZE + 1]);
        val response = new MockHttpServletResponse();
        val filterChainInvoked = new AtomicBoolean();

        filter.doFilterInternal(request, response, (req, res) -> filterChainInvoked.set(true));

        assertRejectedWithCustomErrorResponseJson(response);
        assertThat(filterChainInvoked).isFalse();
    }

    // verifies Fix for Security-Finding A-10
    @Test
    void rejectsOversizedRequestBodyWithoutDeclaredContentLength() throws Exception {
        val request = new MockHttpServletRequest("POST", "/api/some-endpoint") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContentType("application/json");
        request.setContent(new byte[MAX_EXPECTED_REQUEST_BODY_SIZE + 1]);
        val response = new MockHttpServletResponse();
        val filterChainInvoked = new AtomicBoolean();

        filter.doFilterInternal(request, response, (req, res) -> filterChainInvoked.set(true));

        assertRejectedWithCustomErrorResponseJson(response);
        assertThat(filterChainInvoked).isFalse();
    }

    // verifies Fix for Security-Finding A-10
    @Test
    void acceptsRequestBodyAtTheSizeLimit() throws Exception {
        val request = new MockHttpServletRequest("POST", "/api/some-endpoint");
        request.setContentType("application/json");
        request.setContent(new byte[MAX_EXPECTED_REQUEST_BODY_SIZE]);
        val response = new MockHttpServletResponse();
        val filterChainInvoked = new AtomicBoolean();

        filter.doFilterInternal(request, response, (req, res) -> filterChainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChainInvoked).isTrue();
    }

    @Test
    void wrappedRequestBodyIsReplayableEvenAfterItGotConsumed() throws Exception {
        // given
        val givenBody = "{\"name\": \"some-value\"}";
        val request = new MockHttpServletRequest("POST", "/api/some-endpoint");
        request.setContent(givenBody.getBytes(StandardCharsets.UTF_8));
        val wrappedRequest = new AtomicReference<HttpServletRequest>();

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

    // verifies Fix for Security-Finding A-10: the size bound also applies to form/multipart bodies
    @Test
    void rejectsOversizedFormEncodedRequestBody() throws Exception {
        val request = new MockHttpServletRequest("POST", "/fake-jwt/token");
        request.setContentType("application/x-www-form-urlencoded");
        request.setContent(new byte[MAX_EXPECTED_REQUEST_BODY_SIZE + 1]);
        val response = new MockHttpServletResponse();
        val filterChainInvoked = new AtomicBoolean();

        filter.doFilterInternal(request, response, (req, res) -> filterChainInvoked.set(true));

        assertRejectedWithCustomErrorResponseJson(response);
        assertThat(filterChainInvoked).isFalse();
    }

    @Test
    void formEncodedRequestIsPassedThroughUnwrapped() throws Exception {
        // given a form-encoded request, whose body the container itself parses for getParameter(...)
        val request = new MockHttpServletRequest("POST", "/fake-jwt/token");
        request.setContentType("application/x-www-form-urlencoded");
        request.setContent("username=xyz-form_user&password=ignored".getBytes(StandardCharsets.UTF_8));
        val passedRequest = new AtomicReference<HttpServletRequest>();

        // when
        filter.doFilterInternal(request, new MockHttpServletResponse(),
                (req, res) -> passedRequest.set((HttpServletRequest) req));

        // then the request is not wrapped, so the container can still parse the form parameters
        assertThat(passedRequest.get()).isSameAs(request);
    }

    // API clients parse the documented CustomErrorResponse schema, thus even the
    // pre-controller 413 rejection must render it instead of the container's error page
    private static void assertRejectedWithCustomErrorResponseJson(final MockHttpServletResponse response)
            throws Exception {
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString())
                .contains("\"statusCode\":413")
                .contains("\"statusPhrase\":\"Payload Too Large\"")
                .contains("\"message\":\"ERROR: [413] the request body exceeds the maximum size of 262144 bytes\"");
    }

    @Test
    void wrappedRequestReaderUsesDeclaredCharacterEncoding() throws Exception {
        // given
        val givenBody = "{\"name\": \"säme-välue\"}";
        val request = new MockHttpServletRequest("POST", "/api/some-endpoint");
        request.setCharacterEncoding("ISO-8859-1");
        request.setContent(givenBody.getBytes(StandardCharsets.ISO_8859_1));
        val wrappedRequest = new AtomicReference<HttpServletRequest>();

        // when
        filter.doFilterInternal(request, new MockHttpServletResponse(),
                (req, res) -> wrappedRequest.set((HttpServletRequest) req));

        // then
        assertThat(wrappedRequest.get().getReader().lines().collect(joining())).isEqualTo(givenBody);
    }
}
