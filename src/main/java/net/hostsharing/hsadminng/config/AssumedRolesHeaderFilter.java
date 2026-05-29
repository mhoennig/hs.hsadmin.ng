package net.hostsharing.hsadminng.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

// TODO.impl: remove this filter - the whole file and its usages,
//  once all clients have migrated from the deprecated "assumed-roles" header to "Hostsharing-Assumed-Roles".
class AssumedRolesHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull final HttpServletRequest request,
            @NonNull final HttpServletResponse response,
            @NonNull final FilterChain filterChain) throws ServletException, IOException {

        final var assumedRoles = request.getHeader(AssumedRolesHeader.NAME);
        final var assumedRolesDeprecated = request.getHeader(AssumedRolesHeader.DEPRECATED_NAME);

        if (AssumedRolesHeader.isConflict(assumedRoles, assumedRolesDeprecated)) {
            rejectConflictingAssumedRolesHeaders(request, response);
            return;
        }

        if (assumedRoles == null && assumedRolesDeprecated != null) {
            filterChain.doFilter(new AssumedRolesHeaderRequestWrapper(request), response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static final class AssumedRolesHeaderRequestWrapper extends HttpServletRequestWrapper {

        private AssumedRolesHeaderRequestWrapper(final HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(final String name) {
            if (isAssumedRolesHeader(name)) {
                return super.getHeader(AssumedRolesHeader.DEPRECATED_NAME);
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(final String name) {
            if (isAssumedRolesHeader(name)) {
                return super.getHeaders(AssumedRolesHeader.DEPRECATED_NAME);
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            final List<String> headerNames = Collections.list(super.getHeaderNames());
            if (headerNames.stream().noneMatch(AssumedRolesHeaderFilter::isAssumedRolesHeader)) {
                headerNames.add(AssumedRolesHeader.NAME);
            }
            return Collections.enumeration(new ArrayList<>(headerNames));
        }
    }

    private static boolean isAssumedRolesHeader(final String name) {
        return AssumedRolesHeader.NAME.equalsIgnoreCase(name);
    }

    private static void rejectConflictingAssumedRolesHeaders(
            final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        response.setStatus(HTTP_BAD_REQUEST);
        response.setContentType("application/json");
        response.getWriter().write("""
                {"path":"%s","statusCode":400,"statusPhrase":"Bad Request","message":"ERROR: [400] %s"}"""
                .formatted(request.getRequestURI(), AssumedRolesHeader.conflictMessage()));
    }
}


final class AssumedRolesHeader {

    public static final String NAME = "Hostsharing-Assumed-Roles";
    public static final String DEPRECATED_NAME = "assumed-roles";

    private static final String CONFLICT_MESSAGE =
            "headers '" + NAME + "' and '" + DEPRECATED_NAME + "' must either match or only one may be used";

    private AssumedRolesHeader() {
    }

    public static boolean isConflict(final String assumedRoles,
            final String assumedRolesDeprecated) {
        return assumedRoles != null && assumedRolesDeprecated != null && !assumedRoles.equals(assumedRolesDeprecated);
    }

    public static String conflictMessage() {
        return CONFLICT_MESSAGE;
    }
}
