package org.hostsharing.hsadminng.service.accessfilter;

import java.lang.annotation.*;

/**
 * Used to mark a field within a DTO as containing the own id,
 * it's needed to identify an existing entity for update functions.
 * Initialization and update rights have no meaning for such fields,
 * its initialized automatically and never updated.
 *
 * @see AccessFor
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SelfId {
}
