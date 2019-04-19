package org.hostsharing.hsadminng.service.accessfilter;

import org.hostsharing.hsadminng.security.SecurityUtils;

import java.lang.reflect.Field;

/**
 * These enum values are on the one hand used to define the minimum role required to grant access to resources,
 * but on the other hand also for the roles users can be assigned to.
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
     * This meta-role is to specify that any kind of customer contact can get access to the resource.
     */
    ANY_CUSTOMER_CONTACT(20),

    /**
     * This role is for contractual contacts of a customer, like a director of the company.
     * Who has this role, has the broadest access to all resources which belong to this customer.
     * Everything which relates to the contract with the customer, needs this role.
     */
    CONTRACTUAL_CONTACT(21),

    /**
     * This role is for financial contacts of a customer, e.g. for accessing billing data.
     */
    FINANCIAL_CONTACT(22),

    /**
     * This role is for technical contacts of a customer.
     */
    TECHNICAL_CONTACT(22),

    /**
     * Any user which belongs to a customer has at least this role.
     */
    ANY_CUSTOMER_USER(80),

    /**
     * This role is meant to specify that a resources can be accessed by anybody, even without login.
     * It's currently only used for technical purposes.
     */
    ANYBODY(99);

    private final int level;

    Role(final int level) {
        this.level = level;
    }

    /**
     * Determines if the given role is covered by this role.
     *
     * Where 'this' means the Java instance itself as a role of a system user.
     *
     * @example
     * Role.HOSTMASTER.covers(Role.ANY_CUSTOMER_USER) == true
     *
     * @param role The required role for a resource.
     *
     * @return whether this role comprises the given role
     */
    boolean covers(final Role role) {
        return this == role || this.level < role.level;
    }

    /**
     * Checks if this role of a user allows to initialize the given field when creating the resource.
     *
     * @param field a field of the DTO of a resource
     *
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
     *
     * @return true if allowed
     */
    public boolean isAllowedToUpdate(final Field field) {

        final Role loginUserRole = SecurityUtils.getCurrentUserLogin().map(u -> Role.valueOf(u.toUpperCase())).orElse(Role.ANYBODY);

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
     *
     * @return true if allowed
     */
    public boolean isAllowedToRead(final Field field) {

        final Role loginUserRole = SecurityUtils.getCurrentUserLogin().map(u -> Role.valueOf(u.toUpperCase())).orElse(Role.ANYBODY);

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
