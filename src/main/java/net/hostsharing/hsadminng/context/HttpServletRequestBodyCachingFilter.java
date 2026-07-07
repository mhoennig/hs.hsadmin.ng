package net.hostsharing.hsadminng.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Component
public class HttpServletRequestBodyCachingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain)
            throws ServletException, IOException {

        if (hasFormOrMultipartContent(request)) {
            // the container parses such bodies itself for getParameter(...); buffering would consume
            // the stream and make the form parameters unavailable, e.g. for POST /fake-jwt/token
            filterChain.doFilter(request, response);
        } else {
            filterChain.doFilter(new HttpServletRequestWithCachedBody(request), response);
        }
    }

    private static boolean hasFormOrMultipartContent(final HttpServletRequest request) {
        final var contentType = request.getContentType();
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

        HttpServletRequestWithCachedBody(final HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
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
