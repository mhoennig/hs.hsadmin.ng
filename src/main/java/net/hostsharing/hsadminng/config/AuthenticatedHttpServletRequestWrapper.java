package net.hostsharing.hsadminng.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.*;

public class AuthenticatedHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private final Map<String, String> customHeaders = new HashMap<>();

    public AuthenticatedHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    public void addHeader(final String name, final String value) {
        customHeaders.put(name, value);
    }

    @Override
    public String getHeader(final String name) {
        // Check custom headers first
        final var customHeaderValue = customHeaders.get(name);
        if (customHeaderValue != null) {
            return customHeaderValue;
        }
        // Fall back to the original headers
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        // Combine original headers and custom headers
        final var headerNames = new HashSet<>(customHeaders.keySet());
        final var originalHeaderNames = super.getHeaderNames();
        while (originalHeaderNames.hasMoreElements()) {
            headerNames.add(originalHeaderNames.nextElement());
        }
        return Collections.enumeration(headerNames);
    }

    @Override
    public Enumeration<String> getHeaders(final String name) {
        // Combine original headers and custom header
        final var values = new HashSet<String>();
        if (customHeaders.containsKey(name)) {
            values.add(customHeaders.get(name));
        }
        final var originalValues = super.getHeaders(name);
        while (originalValues.hasMoreElements()) {
            values.add(originalValues.nextElement());
        }
        return Collections.enumeration(values);
    }
}
