package org.hostsharing.hsadminng.service.accessfilter;

import java.lang.annotation.*;

/**
 * Used to mark a field within a DTO as containing the id of a referenced entity,
 * it's needed to determine access rights for entity creation.
 *
 * @see AccessFor
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ParentId {
    /// The DTO class of the referenced entity.
    Class<?> value();
}
