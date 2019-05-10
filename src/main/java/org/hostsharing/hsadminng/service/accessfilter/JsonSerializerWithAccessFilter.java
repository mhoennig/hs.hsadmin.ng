// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import org.hostsharing.hsadminng.service.UserRoleAssignmentService;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.springframework.context.ApplicationContext;

import java.io.IOException;

/**
 * A base class for a Spring bean for JSON serialization with field-based access filters.
 * Where {@link JSonSerializationWithAccessFilter} is the actual stateful implementation and
 * it's instances only exist during the process of serialization, this class is a stateless just
 * used for service and context injection.
 *
 * @param <T> DTO class to serialize
 */
public abstract class JsonSerializerWithAccessFilter<T extends AccessMappings> extends JsonSerializer<T> {

    protected final ApplicationContext ctx;
    protected final UserRoleAssignmentService userRoleAssignmentService;

    public JsonSerializerWithAccessFilter(
            final ApplicationContext ctx,
            final UserRoleAssignmentService userRoleAssignmentService) {
        this.ctx = ctx;
        this.userRoleAssignmentService = userRoleAssignmentService;
    }

    @Override
    public void serialize(
            final T dto,
            final JsonGenerator jsonGenerator,
            final SerializerProvider serializerProvider) throws IOException {

        new JSonSerializationWithAccessFilter<T>(ctx, userRoleAssignmentService, jsonGenerator, serializerProvider, dto)
                .serialize();
    }
}
