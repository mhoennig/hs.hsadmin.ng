// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

/**
 * Reads a JSON node value.
 *
 * @param <T>
 */
@FunctionalInterface
public interface JSonFieldReader<T extends AccessMappings> {

    /**
     * Reads a JSON node value.
     *
     * @param target your target entity or DTO type
     *
     */
    void readInto(T target);
}
