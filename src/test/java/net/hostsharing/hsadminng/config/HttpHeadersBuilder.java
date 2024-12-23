package net.hostsharing.hsadminng.config;

import org.springframework.http.HttpHeaders;

public class HttpHeadersBuilder {

    public static HttpHeaders headers(final String key, final String value) {
        final var headers = new HttpHeaders();
        headers.set(key, value);
        return headers;
    }
}
