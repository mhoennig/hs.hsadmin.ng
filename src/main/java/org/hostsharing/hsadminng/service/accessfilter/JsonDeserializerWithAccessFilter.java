// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import org.hostsharing.hsadminng.service.util.ReflectionUtil;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.springframework.context.ApplicationContext;

public abstract class JsonDeserializerWithAccessFilter<T extends AccessMappings> extends JsonDeserializer<T> {

    private final ApplicationContext ctx;

    public JsonDeserializerWithAccessFilter(final ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public T deserialize(
            final JsonParser jsonParser,
            final DeserializationContext deserializationContext) {

        final Class<T> dtoClass = ReflectionUtil
                .determineGenericClassParameter(this.getClass(), JsonDeserializerWithAccessFilter.class, 0);
        return new JSonDeserializationWithAccessFilter<>(ctx, jsonParser, deserializationContext, dtoClass).deserialize();
    }
}
