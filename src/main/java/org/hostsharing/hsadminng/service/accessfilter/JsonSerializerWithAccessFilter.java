// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.lang3.NotImplementedException;
import org.hostsharing.hsadminng.service.UserRoleAssignmentService;
import org.hostsharing.hsadminng.service.util.ReflectionUtil;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

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

        new JSonSerializationWithAccessFilter(this, ctx, userRoleAssignmentService, jsonGenerator, serializerProvider, dto)
                .serialize();
    }

    protected JSonFieldWriter<T> jsonFieldWriter(final Field field) {

        return (final T dto, final JsonGenerator jsonGenerator) -> {
            final String fieldName = field.getName();
            final Object fieldValue = ReflectionUtil.getValue(dto, field);
            // TODO mhoennig turn this into a dispatch table?
            // TODO mhoennig: or maybe replace by serializerProvider.defaultSerialize...()?
            // But the latter makes it difficult for parallel structure with the deserializer (clumsy API).
            // Alternatively extract the supported types to subclasses of some abstract class and
            // here as well as in the deserializer just access the matching implementation through a map.
            // Or even completely switch from Jackson to GSON?

            if (fieldValue == null) {
                jsonGenerator.writeNullField(fieldName);
            } else if (String.class.isAssignableFrom(field.getType())) {
                jsonGenerator.writeStringField(fieldName, (String) fieldValue);
            } else if (Integer.class.isAssignableFrom(field.getType()) || int.class.isAssignableFrom(field.getType())) {
                jsonGenerator.writeNumberField(fieldName, (int) fieldValue);
            } else if (Long.class.isAssignableFrom(field.getType()) || long.class.isAssignableFrom(field.getType())) {
                jsonGenerator.writeNumberField(fieldName, (long) fieldValue);
            } else if (LocalDate.class.isAssignableFrom(field.getType())) {
                jsonGenerator.writeStringField(fieldName, fieldValue.toString());
            } else if (Enum.class.isAssignableFrom(field.getType())) {
                jsonGenerator.writeStringField(fieldName, ((Enum) fieldValue).name());
            } else if (Boolean.class.isAssignableFrom(field.getType()) || boolean.class.isAssignableFrom(field.getType())) {
                jsonGenerator.writeBooleanField(fieldName, (Boolean) fieldValue);
            } else if (BigDecimal.class.isAssignableFrom(field.getType())) {
                jsonGenerator.writeNumberField(fieldName, (BigDecimal) fieldValue);
            } else {
                throw new NotImplementedException("property type not yet implemented: " + field);
            }
        };
    }

    /**
     * INTERNAL implementation of JSON serialization, where {@link JsonSerializerWithAccessFilter}
     * is a stateless bean, this inner class exists only during the actual serialization and
     * contains a serialization state.
     */
    private class JSonSerializationWithAccessFilter extends JSonAccessFilter<T> {

        private final JsonSerializerWithAccessFilter serializer;
        private final JsonGenerator jsonGenerator;
        private final SerializerProvider serializerProvider;

        public JSonSerializationWithAccessFilter(
                final JsonSerializerWithAccessFilter serializer,
                final ApplicationContext ctx,
                final UserRoleAssignmentService userRoleAssignmentService,
                final JsonGenerator jsonGenerator,
                final SerializerProvider serializerProvider,
                final T dto) {
            super(ctx, userRoleAssignmentService, dto);
            this.serializer = serializer;
            this.jsonGenerator = jsonGenerator;
            this.serializerProvider = serializerProvider;
        }

        // Jackson serializes into the JsonGenerator, thus no return value needed.
        public void serialize() throws IOException {

            jsonGenerator.writeStartObject();
            for (Field field : dto.getClass().getDeclaredFields()) {
                toJSon(dto, jsonGenerator, field);
            }
            jsonGenerator.writeEndObject();
        }

        protected void writeJSonField(final T dto, final Field field, final JsonGenerator jsonGenerator) throws IOException {
            serializer.jsonFieldWriter(field).write(dto, jsonGenerator);
        }

        private void toJSon(final T dto, final JsonGenerator jsonGenerator, final Field field) throws IOException {
            if (isAllowedToRead(getLoginUserRoles(), field)) {
                writeJSonField(dto, field, jsonGenerator);
            }
        }

        private boolean isAllowedToRead(final Set<Role> roles, final Field field) {
            for (Role role : roles) {
                if (role.isAllowedToRead(field)) {
                    return true;
                }
            }
            return ReflectionUtil.newInstance(Role.Anybody.class).isAllowedToRead(field); // TODO
        }
    }

}
