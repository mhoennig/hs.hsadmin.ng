package net.hostsharing.hsadminng.context;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import com.vladmihalcea.hibernate.type.array.UUIDArrayType;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
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
    public void define(final String currentUser) {
        define(currentUser, null);
    }

    @Transactional(propagation = MANDATORY)
    public void define(final String currentUser, final String assumedRoles) {
        define(toTask(request), toCurl(request), currentUser, assumedRoles);
    }

    @Transactional(propagation = MANDATORY)
    public void define(
            final String currentTask,
            final String currentRequest,
            final String currentUser,
            final String assumedRoles) {
        final var query = em.createNativeQuery(
                """
                        call defineContext(
                            cast(:currentTask as varchar), 
                            cast(:currentRequest as varchar), 
                            cast(:currentUser as varchar), 
                            cast(:assumedRoles as varchar));
                        """);
        query.setParameter("currentTask", shortenToMaxLength(currentTask, 96));
        query.setParameter("currentRequest", shortenToMaxLength(currentRequest, 512)); // TODO.spec: length?
        query.setParameter("currentUser", currentUser);
        query.setParameter("assumedRoles", assumedRoles != null ? assumedRoles : "");
        query.executeUpdate();
    }

    public String getCurrentTask() {
        return (String) em.createNativeQuery("select current_setting('hsadminng.currentTask');").getSingleResult();
    }

    public String getCurrentUser() {
        return String.valueOf(em.createNativeQuery("select currentUser()").getSingleResult());
    }

    public UUID getCurrentUserUUid() {
        return (UUID) em.createNativeQuery("select currentUserUUid()").getSingleResult();
    }

    public String[] getAssumedRoles() {
        return (String[]) em.createNativeQuery("select assumedRoles() as roles")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .addScalar("roles", StringArrayType.INSTANCE)
                .getSingleResult();
    }

    public UUID[] currentSubjectsUuids() {
        return (UUID[]) em.createNativeQuery("select currentSubjectsUuids() as uuids")
                .unwrap(org.hibernate.query.NativeQuery.class)
                .addScalar("uuids", UUIDArrayType.INSTANCE) // TODO.blog
                .getSingleResult();
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
                                .filter(c -> !c.getDeclaringClass().getName().contains("BySpringCGLIB$$"))
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
