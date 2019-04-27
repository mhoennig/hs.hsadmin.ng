package org.hostsharing.hsadminng.security;

import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Utility class for Spring Security.
 */
public final class SecurityUtils {

    private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);

    private static List<UserRoleAssignment> userRoleAssignments = new ArrayList<>();

    private SecurityUtils() {
    }

    /**
     * Get the login of the current user.
     *
     * @return the login of the current user
     */
    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(securityContext.getAuthentication())
            .map(authentication -> {
                if (authentication.getPrincipal() instanceof UserDetails) {
                    UserDetails springSecurityUser = (UserDetails) authentication.getPrincipal();
                    return springSecurityUser.getUsername();
                } else if (authentication.getPrincipal() instanceof String) {
                    return (String) authentication.getPrincipal();
                }
                return null;
            });
    }

    /**
     * Get the JWT of the current user.
     *
     * @return the JWT of the current user
     */
    public static Optional<String> getCurrentUserJWT() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(securityContext.getAuthentication())
            .filter(authentication -> authentication.getCredentials() instanceof String)
            .map(authentication -> (String) authentication.getCredentials());
    }

    /**
     * Check if a user is authenticated.
     *
     * @return true if the user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(securityContext.getAuthentication())
            .map(authentication -> authentication.getAuthorities().stream()
                .noneMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(AuthoritiesConstants.ANONYMOUS)))
            .orElse(false);
    }

    /**
     * If the current user has a specific authority (security role).
     * <p>
     * The name of this method comes from the isUserInRole() method in the Servlet API
     *
     * @param authority the authority to check
     * @return true if the current user has the authority, false otherwise
     */
    public static boolean isCurrentUserInRole(String authority) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(securityContext.getAuthentication())
            .map(authentication -> authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority)))
            .orElse(false);
    }

    public static Role getLoginUserRoleFor(final Class<?> onDtoClass, final Long onId) {
        final Role highestRole = userRoleAssignments.stream().
            map(ura ->
                matches(onDtoClass, onId, ura)
                    ? ura.role
                    : Role.ANYBODY).
            reduce(Role.ANYBODY, (r1, r2) -> r1.covers(r2) ? r1 : r2);
        log.info("getLoginUserRoleFor({}, {}) returned {}", onDtoClass, onId, highestRole);
        return highestRole;
    }

    private static boolean matches(Class<?> onDtoClass, Long onId, UserRoleAssignment ura) {
        final boolean matches = (ura.onClass == null || onDtoClass == ura.onClass) && (ura.onId == null || ura.onId.equals(onId));
        return matches;
    }

    // TODO: depends on https://plan.hostsharing.net/project/hsadmin/us/67?milestone=34
    public static void addUserRole(final Class<?> onClass, final Long onId, final Role role) {
        log.info("addUserRole({}, {}, {})", onClass, onId, role);
        userRoleAssignments.add(new UserRoleAssignment(onClass, onId, role));

    }

    public static void clearUserRoles() {
        userRoleAssignments.clear();
    }

    private static class UserRoleAssignment {
        final Class<?> onClass;
        final Long onId;
        final Role role;

        UserRoleAssignment(Class<?> onClass, Long onId, Role role) {
            this.onClass = onClass;
            this.onId = onId;
            this.role = role;
        }
    }
}
