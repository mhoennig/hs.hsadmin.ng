package net.hostsharing.hsadminng.config;

import org.springframework.http.HttpHeaders;

import java.util.Map;

public class HttpHeadersBuilder {

    @SafeVarargs
    public static HttpHeaders headers(final Map.Entry<String, String>... headers) {
        final var httpHeaders = new HttpHeaders();
        for (Map.Entry<String, String> entry : headers) {
            httpHeaders.set(entry.getKey(), entry.getValue());
        }
        return httpHeaders;
    }
}
