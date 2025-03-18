package net.hostsharing.hsadminng.context;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;
import static org.springframework.transaction.annotation.Propagation.MANDATORY;

@Service
@AllArgsConstructor
public class Context {

    private static final Set<String> HEADERS_TO_IGNORE = Set.of(
            "accept-encoding",
            "connection",
            "content-length",
            "host",
            "user-agent");

    @PersistenceContext
    private EntityManager em;

    @Autowired(required = false)
    private HttpServletRequest request;

    @Transactional(propagation = MANDATORY)
    public void define(final String currentSubject) {
        define(currentSubject, null);
    }

    @Transactional(propagation = MANDATORY)
    public void define(final String currentSubject, final String assumedRoles) {
        define(toTask(request), toCurl(request), currentSubject, assumedRoles);
    }

    @Transactional(propagation = MANDATORY)
    public void assumeRoles(final String assumedRoles) {
        final var currentSubject = SecurityContextHolder.getContext().getAuthentication().getName();
        define(toTask(request), toCurl(request), currentSubject, assumedRoles);
    }

    @Transactional(propagation = MANDATORY)
    public void define(
            final String currentTask,
            final String currentRequest,
            final String currentSubject,
            final String assumedRoles) {
        final var query = em.createNativeQuery("""
                call base.defineContext(
                    cast(:currentTask as varchar(127)),
                    cast(:currentRequest as text),
                    cast(:currentSubject as varchar(63)),
                    cast(:assumedRoles as text));
                """);
        query.setParameter("currentTask", shortenToMaxLength(currentTask, 127));
        query.setParameter("currentRequest", currentRequest);
        query.setParameter("currentSubject", currentSubject);
        query.setParameter("assumedRoles", assumedRoles != null ? assumedRoles : "");
        query.executeUpdate();
    }

    public String fetchCurrentTask() {
        return (String) em.createNativeQuery("select current_setting('hsadminng.currentTask');").getSingleResult();
    }

    public String fetchCurrentSubject() {
        return String.valueOf(em.createNativeQuery("select base.currentSubject()").getSingleResult());
    }

    public UUID fetchCurrentSubjectUuid() {
        return (UUID) em.createNativeQuery("select rbac.currentSubjectUuid()", UUID.class).getSingleResult();
    }

    public String[] fetchAssumedRoles() {
        return (String[]) em.createNativeQuery("select base.assumedRoles() as roles", String[].class).getSingleResult();
    }

    public UUID[] fetchCurrentSubjectOrAssumedRolesUuids() {
        return (UUID[]) em.createNativeQuery("select rbac.currentSubjectOrAssumedRolesUuids() as uuids", UUID[].class).getSingleResult();
    }

    public static String getCallerMethodNameFromStackFrame(final int skipFrames) {
        final Optional<StackWalker.StackFrame> caller =
                StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                        .walk(frames -> frames
                                .skip(skipFrames)
                                .filter(c -> c.getDeclaringClass() != Context.class)
                                .filter(c -> c.getDeclaringClass()
                                        .getPackageName()
                                        .startsWith("net.hostsharing.hsadminng"))
                                .filter(c -> !c.getDeclaringClass().getName().contains("$$SpringCGLIB$$"))
                                .findFirst());
        return caller.map(
                        c -> c.getDeclaringClass().getSimpleName() + "." + c.getMethodName())
                .orElse("unknown");
    }

    private String toTask(final HttpServletRequest request) {
        if (isRequestScopeAvailable()) {
            return request.getMethod() + " " + request.getRequestURI();
        } else {
            return getCallerMethodNameFromStackFrame(2);
        }
    }

    @SneakyThrows
    private String toCurl(final HttpServletRequest request) {
        if (!isRequestScopeAvailable()) {
            return null;
        }

        var curlCommand = "curl -0 -v";

        // append method
        curlCommand += " -X " + request.getMethod();

        // append request url
        curlCommand += " " + request.getRequestURI();

        // append headers
        final var headers = Collections.list(request.getHeaderNames()).stream()
                .filter(not(HEADERS_TO_IGNORE::contains))
                .collect(Collectors.toSet());
        for (String headerName : headers) {
            final var headerValue = request.getHeader(headerName);
            curlCommand += " \\" + System.lineSeparator() + String.format("-H '%s:%s'", headerName, headerValue);
        }

        // body
        final String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        if (!StringUtils.isEmpty(body)) {
            curlCommand += " \\" + System.lineSeparator() + "--data-binary @- ";
            curlCommand +=
                    "<< EOF" + System.lineSeparator() + System.lineSeparator() + body + System.lineSeparator() + "EOF";
        }

        return curlCommand;
    }

    private boolean isRequestScopeAvailable() {
        return RequestContextHolder.getRequestAttributes() != null;
    }

    private static String shortenToMaxLength(final String raw, final int maxLength) {
        if (raw == null) {
            return "";
        }
        if (raw.length() <= maxLength) {
            return raw;
        }
        return raw.substring(0, maxLength - 3) + "...";
    }
}
