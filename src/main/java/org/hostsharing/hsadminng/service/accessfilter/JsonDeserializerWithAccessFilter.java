// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import org.hostsharing.hsadminng.service.UserRoleAssignmentService;
import org.hostsharing.hsadminng.service.util.ReflectionUtil;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.springframework.context.ApplicationContext;

public abstract class JsonDeserializerWithAccessFilter<T extends AccessMappings> extends JsonDeserializer<T> {

    private final ApplicationContext ctx;
    private final UserRoleAssignmentService userRoleAssignmentService;

    public JsonDeserializerWithAccessFilter(
            final ApplicationContext ctx,
            final UserRoleAssignmentService userRoleAssignmentService) {
        this.ctx = ctx;
        this.userRoleAssignmentService = userRoleAssignmentService;
    }

    @Override
    public T deserialize(
            final JsonParser jsonParser,
            final DeserializationContext deserializationContext) {

        final Class<T> dtoClass = ReflectionUtil
                .determineGenericClassParameter(this.getClass(), JsonDeserializerWithAccessFilter.class, 0);
        return new JSonDeserializationWithAccessFilter<T>(
                ctx,
                userRoleAssignmentService,
                jsonParser,
                deserializationContext,
                dtoClass).deserialize();
    }
}
