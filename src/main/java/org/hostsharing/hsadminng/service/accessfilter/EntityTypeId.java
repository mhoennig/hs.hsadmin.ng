// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import java.lang.annotation.*;

/**
 * Specifies the entityTypeId to be used in UserRoleAssignment.
 */
@Documented
@Target({ ElementType.FIELD, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface EntityTypeId {

    /**
     * The ID of the entity type, max length: 32.
     * 
     * Pattern: "module.Entity", e.g. "customer.Membership"
     */
    String value();
}
