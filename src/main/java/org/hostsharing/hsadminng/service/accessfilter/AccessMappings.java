// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import java.io.Serializable;

/**
 * A marker interface for DTO classes which can be used by {@link JsonSerializerWithAccessFilter} and
 * {@link JsonDeserializerWithAccessFilter}.
 */
public interface AccessMappings extends Serializable {

    Long getId();
}
