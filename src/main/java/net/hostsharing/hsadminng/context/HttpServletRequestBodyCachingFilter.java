package net.hostsharing.hsadminng.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.hostsharing.hsadminng.errors.CustomErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class HttpServletRequestBodyCachingFilter extends OncePerRequestFilter {

    static final int MAX_REQUEST_BODY_SIZE = 256 * 1024;

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain)
            throws ServletException, IOException {

        // cheap early rejection on a trusted Content-Length; also the only bound for the unbuffered form path below
        if (request.getContentLengthLong() > MAX_REQUEST_BODY_SIZE) {
            rejectOversizedBody(request, response);
        } else if (hasFormOrMultipartContent(request)) {
            // the container parses such bodies itself for getParameter(...); buffering would consume
            // the stream and make the form parameters unavailable, e.g. for POST /fake-jwt/token;
            // an undeclared or under-declared content length is left to the container's own form/multipart limits
            filterChain.doFilter(request, response);
        } else {
            // the actual A-10 bound: reads at most MAX+1 regardless of a missing or lying Content-Length
            val cachedBody = request.getInputStream().readNBytes(MAX_REQUEST_BODY_SIZE + 1);
            if (cachedBody.length > MAX_REQUEST_BODY_SIZE) {
                rejectOversizedBody(request, response);
                return;
            }
            filterChain.doFilter(new HttpServletRequestWithCachedBody(request, cachedBody), response);
        }
    }

    // sendError would render the container's default error page, but API clients parse the CustomErrorResponse schema
    private void rejectOversizedBody(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), new CustomErrorResponse(
                request.getContextPath(),
                HttpStatus.CONTENT_TOO_LARGE,
                "the request body exceeds the maximum size of " + MAX_REQUEST_BODY_SIZE + " bytes"));
    }

    private static boolean hasFormOrMultipartContent(final HttpServletRequest request) {
        val contentType = request.getContentType();
        return contentType != null &&
                (contentType.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        || contentType.startsWith("multipart/"));
    }

    /**
     * Makes the request body replayable: each call to getInputStream()/getReader() serves a fresh
     * stream over the cached body, so it can still be read for the audit-log (`Context.toCurl`)
     * after Spring's request-body handling already consumed it.
     */
    private static class HttpServletRequestWithCachedBody extends HttpServletRequestWrapper {

        private final byte[] cachedBody;

        HttpServletRequestWithCachedBody(final HttpServletRequest request, final byte[] cachedBody) {
            super(request);
            this.cachedBody = cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new HttpServletRequestBodyCache(cachedBody);
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), characterEncoding()));
        }

        private Charset characterEncoding() {
            return getCharacterEncoding() != null ? Charset.forName(getCharacterEncoding()) : StandardCharsets.UTF_8;
        }
    }
}
