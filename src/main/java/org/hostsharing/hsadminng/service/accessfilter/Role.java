package org.hostsharing.hsadminng.service.accessfilter;

import org.hostsharing.hsadminng.security.SecurityUtils;

import java.lang.reflect.Field;

public enum Role {
    NOBODY(0), HOSTMASTER(1), ADMIN(2), SUPPORTER(3),
    ANY_CUSTOMER_CONTACT(20), CONTRACTUAL_CONTACT(21), FINANCIAL_CONTACT(22), TECHNICAL_CONTACT(22),
    ANY_CUSTOMER_USER(80),
    ANYBODY(99);

    private final int level;

    Role(final int level) {
        this.level = level;
    }

    boolean covers(final Role role) {
        return this == role || this.level < role.level;
    }

    public boolean isAllowedToInit(Field field) {

        final AccessFor accessFor = field.getAnnotation(AccessFor.class);
        if (accessFor == null) {
            return false;
        }

        return isRoleCovered(accessFor.init());
    }

    public boolean isAllowedToUpdate(Field field) {

        final Role loginUserRole = SecurityUtils.getCurrentUserLogin().map(u -> Role.valueOf(u.toUpperCase())).orElse(Role.ANYBODY);

        final AccessFor accessFor = field.getAnnotation(AccessFor.class);
        if (accessFor == null) {
            return false;
        }

        return isRoleCovered(accessFor.update());
    }

    public boolean isAllowedToRead(Field field) {

        final Role loginUserRole = SecurityUtils.getCurrentUserLogin().map(u -> Role.valueOf(u.toUpperCase())).orElse(Role.ANYBODY);

        final AccessFor accessFor = field.getAnnotation(AccessFor.class);
        if (accessFor == null) {
            return false;
        }

        return isRoleCovered(accessFor.read());
    }

    private boolean isRoleCovered(Role[] requiredRoles) {
        for (Role accessAllowedForRole : requiredRoles) {
            if (this.covers(accessAllowedForRole)) {
                return true;
            }
        }
        return false;
    }

}
