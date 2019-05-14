// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

/**
 * Similar to a BiConsumer<T, JsonGenerator>, but declaring IOException as needed by JsonGenerator.
 *
 * @param <T>
 */
@FunctionalInterface
public interface JSonFieldWriter<T extends AccessMappings> {

    /**
     * Writes a JSON field and value.
     *
     * @param object your entity or DTO type
     * @param jsonGenerator provides low level methods for writing JSON fields
     */
    void write(T object, JsonGenerator jsonGenerator) throws IOException;
}
