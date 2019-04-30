// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import static com.google.common.base.Verify.verify;

import java.lang.reflect.Field;

/**
 * These enum values are on the one hand used to define the minimum role required to grant access to resources,
 * but on the other hand also for the roles users can be assigned to.
 * <p>
 * TODO: Maybe splitting it up into UserRole and RequiredRole would make it more clear?
 * And maybe instead of a level, we could then add the comprised roles in the constructor?
 * This could also be a better way to express that the financial contact has no rights to
 * other users resources (see also ACTUAL_CUSTOMER_USEr vs. ANY_CUSTOMER_USER).
 */
public enum Role {
    /**
     * Default for access rights requirement. You can read it as: 'Nobody is allowed to ...'.
     * This is usually used for fields which are managed by hsadminNg itself.
     */
    NOBODY(0),

    /**
     * Hostmasters are initialize/update/read and field which, except where NOBODY is allowed to.
     */
    HOSTMASTER(1),

    /**
     * This role is for administrators, e.g. to create memberships and book shared and assets.
     */
    ADMIN(2),

    /**
     * This role is for members of the support team.
     */
    SUPPORTER(3),

    /**
     * This role is for contractual contacts of a customer, like a director of the company.
     * Who has this role, has the broadest access to all resources which belong to this customer.
     * Everything which relates to the contract with the customer, needs this role.
     */
    CONTRACTUAL_CONTACT(20),

    /**
     * This role is for financial contacts of a customer, e.g. for accessing billing data.
     */
    FINANCIAL_CONTACT(22) {

        @Override
        public boolean covers(final Role role) {
            if (role == ACTUAL_CUSTOMER_USER) {
                return false;
            }
            return super.covers(role);
        }
    },

    /**
     * This role is for technical contacts of a customer.
     */
    TECHNICAL_CONTACT(22),

    /**
     * This meta-role is to specify that any kind of customer contact can get access to the resource.
     */
    ANY_CUSTOMER_CONTACT(29),

    /**
     * Any user which belongs to a customer has at least this role.
     */
    ACTUAL_CUSTOMER_USER(80),

    /**
     * Use this to grant rights to any user, also special function users who have no
     * rights on other users resources.
     */
    ANY_CUSTOMER_USER(89),

    /**
     * This role is meant to specify that a resources can be accessed by anybody, even without login.
     * It's currently only used for technical purposes.
     */
    ANYBODY(99),

    /**
     * Pseudo-role to mark init/update access as ignored because the field is display-only.
     * This allows REST clients to send the whole response back as a new update request.
     * This role is not covered by any and covers itself no role.
     */
    IGNORED;

    private final Integer level;

    Role() {
        this.level = null;
    }

    Role(final int level) {
        this.level = level;
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
     * @return true if the role is the IGNORED role
     */
    public boolean isIgnored() {
        return this == Role.IGNORED;
    }

    /**
     * @return true if this role is independent of a target object, false otherwise.
     */
    public boolean isIndependent() {
        return covers(Role.SUPPORTER);
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
     * Role.HOSTMASTER.coversAny(Role.CONTRACTUAL_CONTACT, Role.FINANCIAL_CONTACT) == true
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
