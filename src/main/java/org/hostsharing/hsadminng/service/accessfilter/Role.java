// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import static com.google.common.base.Verify.verify;

import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.User;
import org.hostsharing.hsadminng.domain.UserRoleAssignment;
import org.hostsharing.hsadminng.security.AuthoritiesConstants;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * These enum values are used to specify the minimum role required to grant access to resources,
 * see usages of {@link AccessFor}.
 * also they can be assigned to users via {@link UserRoleAssignment}.
 * Some of the concrete values make only sense in one of these contexts.
 * <p>
 * Further, there are two kinds of roles: independent and dependent.
 * Independent roles like {@link #HOSTMASTER} are absolute roles which means unrelated to any concrete entity.
 * Dependent roles like {@link #CUSTOMER_CONTRACTUAL_CONTACT} are relative to a specific entity,
 * in this case to a specific {@link Customer}.
 * <p>
 */
/*
 * TODO: Maybe splitting it up into UserRole and RequiredRole would make it more clear?
 * And maybe instead of a level, we could then add the comprised roles in the constructor?
 * This could also be a better way to express that the financial contact has no rights to
 * other users resources (see also ACTUAL_CUSTOMER_USER vs. ANY_CUSTOMER_USER).
 */
public enum Role {
    /**
     * Default for access rights requirement. You can read it as: 'Nobody is allowed to ...'.
     * This is usually used for fields which are managed by hsadminNg itself.
     * <p>
     * This role cannot be assigned to a user.
     * </p>
     */
    NOBODY(0),

    /**
     * Hostmasters are initialize/update/read and field which, except where NOBODY is allowed to.
     * <p>
     * This role can be assigned to a user via {@link User#setAuthorities}.
     * </p>
     */
    HOSTMASTER(1, AuthoritiesConstants.HOSTMASTER),

    /**
     * This role is for administrators, e.g. to create memberships and book shared and assets.
     * <p>
     * This role can be assigned to a user via {@link User#setAuthorities}.
     * </p>
     */
    ADMIN(2, AuthoritiesConstants.ADMIN),

    /**
     * This role is for members of the support team.
     * <p>
     * This role can be assigned to a user via {@link User#setAuthorities}.
     * </p>
     */
    SUPPORTER(3, AuthoritiesConstants.SUPPORTER),

    /**
     * This role is for contractual contacts of a customer, like a director of the company.
     * <p>
     * Who has this role, has the broadest access to all resources which belong to this customer.
     * Everything which relates to the contract with the customer, needs this role.
     * <p>
     * This role can be assigned to a user via {@link UserRoleAssignment}.
     * </p>
     */
    CUSTOMER_CONTRACTUAL_CONTACT(20),

    /**
     * This role is for financial contacts of a customer, e.g. for accessing billing data.
     * <p>
     * The financial contact only covers {@link Role#CUSTOMER_FINANCIAL_CONTACT}, {@link Role#ANY_CUSTOMER_CONTACT} and
     * {@link Role#ANYBODY}, but not other <em>normal</em> user roles.
     * </p>
     * <p>
     * This role can be assigned to a user via {@link UserRoleAssignment}.
     * </p>
     */
    CUSTOMER_FINANCIAL_CONTACT(22) {

        @Override
        public boolean covers(final Role role) {
            return role == CUSTOMER_FINANCIAL_CONTACT || role == ANY_CUSTOMER_CONTACT || role == ANYBODY;
        }
    },

    /**
     * This role is for technical contacts of a customer.
     * <p>
     * This role can be assigned to a user via {@link UserRoleAssignment}.
     * </p>
     */
    CUSTOMER_TECHNICAL_CONTACT(22),

    /**
     * This meta-role is to specify that any kind of customer contact can get access to the resource.
     * <p>
     * It's only used to specify the required role and cannot be assigned to a user.
     * </p>
     */
    ANY_CUSTOMER_CONTACT(29),

    /**
     * Some user belonging to a customer without a more precise role.
     */
    // TODO: It's mostly a placeholder for more precise future roles like a "webspace admin".
    // This also shows that it's a bit ugly that we need the roles of all modules in this enum
    // because types for attributes of annotations are quite limited in Java.
    ACTUAL_CUSTOMER_USER(80),

    /**
     * Use this to grant rights to any user, also special function users who have no
     * rights on other users resources.
     * <p>
     * It's only used to specify the required role and cannot be assigned to a user.
     * </p>
     */
    ANY_CUSTOMER_USER(89),

    /**
     * This role is meant to specify that a resources can be accessed by anybody, even without login.
     * <p>
     * It can be used to specify the required role and is the implicit role for un-authenticated users.
     * </p>
     */
    ANYBODY(99, AuthoritiesConstants.ANONYMOUS),

    /**
     * Pseudo-role to mark init/update access as ignored because the field is display-only.
     * <p>
     * This allows REST clients to send the whole response back as a new update request.
     * This role is not covered by any and covers itself no role.
     * <p>
     * It's only used to specify the required role and cannot be assigned to a user.
     * </p>
     */
    IGNORED;

    private final Integer level;
    private final Optional<String> authority;

    Role(final int level, final String authority) {
        this.level = level;
        this.authority = Optional.of(authority);
    }

    Role(final int level) {
        this.level = level;
        this.authority = Optional.empty();
    }

    Role() {
        this.level = null;
        this.authority = Optional.empty();
    }

    /**
     * @param field a field of a DTO with AccessMappings
     * @return true if update access can be ignored because the field is just for display anyway
     */
    public static boolean toBeIgnoredForUpdates(final Field field) {
        final AccessFor accessForAnnot = field.getAnnotation(AccessFor.class);
        if (accessForAnnot == null) {
            return true;
        }
        final Role[] updateAccessFor = field.getAnnotation(AccessFor.class).update();
        return updateAccessFor.length == 1 && updateAccessFor[0].isIgnored();
    }

    /**
     * @return the independent authority related 1:1 to this Role or empty if no independent authority is related 1:1
     * @see AuthoritiesConstants
     */
    public Optional<String> getAuthority() {
        return authority;
    }

    /**
     * @return true if the role is the IGNORED role
     */
    public boolean isIgnored() {
        return this == Role.IGNORED;
    }

    /**
     * @return the role with the broadest access rights
     */
    public static Role broadest(final Role role, final Role... roles) {
        Role broadests = role;
        for (Role r : roles) {
            if (r.covers(broadests)) {
                broadests = r;
            }
        }
        return broadests;
    }

    /**
     * Determines if 'this' actual role covered the given required role.
     * <p>
     * Where 'this' means the Java instance itself as a role of a system user.
     * <p>
     * {@code
     * Role.HOSTMASTER.covers(Role.ANY_CUSTOMER_USER) == true
     * }
     *
     * @param role The required role for a resource.
     * @return whether this role comprises the given role
     */
    public boolean covers(final Role role) {
        if (this.isIgnored() || role.isIgnored()) {
            return false;
        }
        return this == role || this.level < role.level;
    }

    /**
     * Determines if 'this' actual role covers any of the given required roles.
     * <p>
     * Where 'this' means the Java instance itself as a role of a system user.
     * <p>
     * {@code
     * Role.HOSTMASTER.coversAny(Role.CUSTOMER_CONTRACTUAL_CONTACT, Role.CUSTOMER_FINANCIAL_CONTACT) == true
     * }
     *
     * @param roles The alternatively required roles for a resource. Must be at least one.
     * @return whether this role comprises any of the given roles
     */
    public boolean coversAny(final Role... roles) {
        verify(roles != null && roles.length > 0, "roles expected");

        for (Role role : roles) {
            if (this.covers(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this role of a user allows to initialize the given field when creating the resource.
     *
     * @param field a field of the DTO of a resource
     * @return true if allowed
     */
    public boolean isAllowedToInit(final Field field) {

        final AccessFor accessFor = field.getAnnotation(AccessFor.class);
        if (accessFor == null) {
            return false;
        }

        return isRoleCovered(accessFor.init());
    }

    /**
     * Checks if this role of a user allows to update the given field.
     *
     * @param field a field of the DTO of a resource
     * @return true if allowed
     */
    public boolean isAllowedToUpdate(final Field field) {

        final AccessFor accessFor = field.getAnnotation(AccessFor.class);
        if (accessFor == null) {
            return false;
        }

        return isRoleCovered(accessFor.update());
    }

    /**
     * Checks if this role of a user allows to read the given field.
     *
     * @param field a field of the DTO of a resource
     * @return true if allowed
     */
    public boolean isAllowedToRead(final Field field) {

        final AccessFor accessFor = field.getAnnotation(AccessFor.class);
        if (accessFor == null) {
            return false;
        }

        return isRoleCovered(accessFor.read());
    }

    private boolean isRoleCovered(final Role[] requiredRoles) {
        for (Role accessAllowedForRole : requiredRoles) {
            if (this.covers(accessAllowedForRole)) {
                return true;
            }
        }
        return false;
    }
}
