// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import org.hostsharing.hsadminng.service.IdToDtoResolver;

import java.lang.annotation.*;

/**
 * Used to mark a field within a DTO as containing the id of a referenced entity,
 * it's needed to determine access rights for entity creation.
 *
 * @see AccessFor
 */
@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ParentId {

    /// The service which can load the referenced DTO.
    Class<? extends IdToDtoResolver<?>> resolver();
}
