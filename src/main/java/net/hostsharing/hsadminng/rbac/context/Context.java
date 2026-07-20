package net.hostsharing.hsadminng.rbac.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import net.hostsharing.hsadminng.errors.ForbiddenException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    // headers containing credentials which must not leak into the audit-log (base.tx_context.currentRequest)
    private static final Set<String> HEADERS_TO_MASK = Set.of(
            "authorization",
            "cookie",
            "hostsharing-api-key",
            "x-api-key");

    // patterns of property names whose values are masked in the audit-logged request body,
    // e.g. the write-only password/totpKey properties of hosting-asset configs:
    // any name ending with "password", starting or ending with "secret", or ending with "totpKey"
    private static final Pattern BODY_PROPERTIES_TO_MASK = Pattern.compile(
            "password$|^secret|secret$|totpKey$",
            Pattern.CASE_INSENSITIVE);
    private static final String MASKED_PROPERTY_VALUE = "<masked>";
    private static final ObjectMapper BODY_MASKING_JSON_MAPPER = new ObjectMapper();

    @PersistenceContext
    private EntityManager em;

    @Autowired(required = false)
    private HttpServletRequest request;

    @Transactional(propagation = MANDATORY)
    public void define(final String currentSubject) {
        define(currentSubject, null);
    }

    @Transactional(propagation = MANDATORY)
    public void define() {
        val auth = SecurityContextHolder.getContext().getAuthentication();
        // TODO.impl [for Story#458]: this code works for simplified JWT in tests as well as the real Keycloak, but there should be only one way
        // if "preferred_username" is set, use it, otherwise use "sub"
        val username = Optional.of(auth)
                .filter(JwtAuthenticationToken.class::isInstance)
                .map(JwtAuthenticationToken.class::cast)
                .map(JwtAuthenticationToken::getToken)
                .map(token -> token.getClaimAsString("preferred_username"))
                .filter(claim -> !claim.isBlank()) // force to getName ("sub") if blank
                .orElseGet(auth::getName);
        define(toTask(request), toCurl(request), username, null, currentSubjectGroupNamesFromJWT());
    }

    @Transactional(propagation = MANDATORY)
    public void define(final String currentSubject, final String assumedRoles) {
        define(toTask(request), toCurl(request), currentSubject, assumedRoles, null);
    }

    @Transactional(propagation = MANDATORY)
    public void assumeRoles(final String assumedRoles) {
        final var currentSubject = SecurityContextHolder.getContext().getAuthentication().getName();
        define(toTask(request), toCurl(request), currentSubject, assumedRoles, currentSubjectGroupNamesFromJWT());
    }

    @Transactional(propagation = MANDATORY)
    public void define(
            final String currentTask,
            final String currentRequest,
            final String currentSubject,
            final String assumedRoles) {
        define(currentTask, currentRequest, currentSubject, assumedRoles, null);
    }

    // TODO.refa: this method has to many String parameters, maybe reafactor to a builder-pattern?
    @Transactional(propagation = MANDATORY)
    public void define(
            final String currentTask,
            final String currentRequest,
            final String currentSubject,
            final String assumedRoles,
            final String currentSubjectGroups) {
        final var query = em.createNativeQuery("""
                call base.defineContext(
                    cast(:currentTask as varchar(127)),
                    cast(:currentRequest as text),
                    cast(:currentSubject as varchar(63)),
                    cast(:assumedRoles as text),
                    cast(:currentSubjectGroups as text));
                """);
        query.setParameter("currentTask", shortenToMaxLength(currentTask, 127));
        query.setParameter("currentRequest", currentRequest);
        query.setParameter("currentSubject", subjectName(currentSubject));
        query.setParameter("assumedRoles", assumedRoles != null ? assumedRoles : "");
        query.setParameter("currentSubjectGroups", currentSubjectGroups != null ? currentSubjectGroups : "");
        query.executeUpdate();
    }

    @Transactional(propagation = MANDATORY)
    public void requireGlobalAdmin(final String message) {
        define();
        if (!isGlobalAdmin()) {
            throw new ForbiddenException(message);
        }
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

    public String[] fetchAssumedRolesNames() {
        return (String[]) em.createNativeQuery("select base.assumedRoles() as roles", String[].class).getSingleResult();
    }

    public UUID[] fetchCurrentSubjectOrAssumedRolesUuids() {
        return (UUID[]) em.createNativeQuery("select rbac.currentSubjectOrAssumedRolesUuids() as uuids", UUID[].class).getSingleResult();
    }

    public boolean isGlobalAdmin() {
        return (boolean) em.createNativeQuery("select rbac.isGlobalAdmin()", boolean.class).getSingleResult();
    }

    public boolean hasAssumedRole() {
        val assumedRoles = fetchAssumedRolesNames();
        return assumedRoles != null && Stream.of(assumedRoles).anyMatch(StringUtils::isNotBlank);
    }

    public boolean hasGlobalAdminRole() {
        return (boolean) em.createNativeQuery("select rbac.hasGlobalAdminRole()", boolean.class).getSingleResult();
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

    private String subjectName(final String nameOrUuid) {
        if (nameOrUuid == null) {
            return null;
        }
        // a subject UUID (e.g. JWT "sub" claim) is the default, a plain subject name the fallback
        return toUuid(nameOrUuid)
                .map(authenticatedUuid -> findSubjectNameByUuid(authenticatedUuid)
                        .orElseThrow(() -> new NoSuchElementException("cannot find Subject by uuid: " + authenticatedUuid)))
                .orElse(nameOrUuid);
    }

    private static Optional<UUID> toUuid(final String maybeUuid) {
        try {
            return Optional.of(UUID.fromString(maybeUuid));
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Optional<String> findSubjectNameByUuid(final UUID authenticatedUuid) {
        final Stream<?> subjectNames = em.createNativeQuery("SELECT name FROM rbac.subject s WHERE s.uuid=:uuid")
                .setParameter("uuid", authenticatedUuid)
                .getResultStream();
        return subjectNames.findFirst().map(Object::toString);
    }

    public List<String> fetchClaimedSubjectGroupNames() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(JwtAuthenticationToken.class::isInstance)
                .map(JwtAuthenticationToken.class::cast)
                .map(JwtAuthenticationToken::getToken)
                .map(token -> token.getClaim("groups"))
                .map(Context::groupNamesFromClaim)
                .orElse(List.of());
    }

    private String currentSubjectGroupNamesFromJWT() {
        return String.join(";", fetchClaimedSubjectGroupNames());
    }

    private static List<String> groupNamesFromClaim(final Object semicolonSeparatedGroupsClaim) {
        final Stream<?> groupNames = semicolonSeparatedGroupsClaim instanceof Collection<?> groups
                ? groups.stream()
                : Stream.of(semicolonSeparatedGroupsClaim);

        return groupNames
                .map(String::valueOf)
                // split like the DB layer splits currentSubjectGroups, see rbac.determineCurrentSubjectGroupUuids
                .flatMap(name -> Stream.of(name.split(";")))
                .map(String::trim)
                .filter(not(String::isBlank))
                .distinct()
                .toList();
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
                .filter(headerName -> !HEADERS_TO_IGNORE.contains(headerName.toLowerCase()))
                .collect(Collectors.toSet());
        for (String headerName : headers) {
            final var headerValue = HEADERS_TO_MASK.contains(headerName.toLowerCase())
                    ? MASKED_PROPERTY_VALUE
                    : request.getHeader(headerName);
            curlCommand += " \\" + System.lineSeparator() + String.format("-H '%s:%s'", headerName, headerValue);
        }

        // body
        final String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        if (!StringUtils.isEmpty(body)) {
            curlCommand += " \\" + System.lineSeparator() + "--data-binary @- ";
            curlCommand +=
                    "<< EOF" + System.lineSeparator() + System.lineSeparator() + withMaskedProperties(body)
                            + System.lineSeparator() + "EOF";
        }

        return curlCommand;
    }

    // masks the values of sensitive JSON body properties, e.g. hosting-asset passwords,
    // so they don't leak into the audit-log (base.tx_context.currentRequest)
    private static String withMaskedProperties(final String body) {
        try {
            final var root = BODY_MASKING_JSON_MAPPER.readTree(body);
            return maskProperties(root) ? root.toString() : body;
        } catch (final JsonProcessingException exc) {
            return body; // not JSON, nothing to mask
        }
    }

    private static boolean maskProperties(final JsonNode node) {
        var masked = false;
        if (node.isObject()) {
            final var objectNode = (ObjectNode) node;
            final var fieldNames = new ArrayList<String>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);
            for (final var fieldName : fieldNames) {
                if (BODY_PROPERTIES_TO_MASK.matcher(fieldName).find()) {
                    objectNode.put(fieldName, MASKED_PROPERTY_VALUE);
                    masked = true;
                } else {
                    masked |= maskProperties(objectNode.get(fieldName));
                }
            }
        } else if (node.isArray()) {
            for (final var element : node) {
                masked |= maskProperties(element);
            }
        }
        return masked;
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
