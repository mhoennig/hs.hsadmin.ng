// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import org.hostsharing.hsadminng.service.UserRoleAssignmentService;
import org.hostsharing.hsadminng.service.util.ReflectionUtil;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * Actual implementation of JSON serialization, where {link JsonSerializerWithAccessFilter}
 * is a stateless bean, {@link JSonSerializationWithAccessFilter} exists only during the actual
 * serialization and contains a serialization state.
 *
 * @param <T> DTO class to serialize
 */
public class JSonSerializationWithAccessFilter<T> extends JSonAccessFilter<T> {

    private final JsonGenerator jsonGenerator;
    private final SerializerProvider serializerProvider;

    public JSonSerializationWithAccessFilter(
            final ApplicationContext ctx,
            final UserRoleAssignmentService userRoleAssignmentService,
            final JsonGenerator jsonGenerator,
            final SerializerProvider serializerProvider,
            final T dto) {
        super(ctx, userRoleAssignmentService, dto);
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

    private void toJSon(final Object dto, final JsonGenerator jsonGenerator, final Field field) throws IOException {
        if (isAllowedToRead(getLoginUserRoles(), field)) {
            final String fieldName = field.getName();
            // TODO: maybe replace by serializerProvider.defaultSerialize...()?
            // But that makes it difficult for parallel structure with the deserializer (clumsy API).
            // Alternatively extract the supported types to subclasses of some abstract class and
            // here as well as in the deserializer just access the matching implementation through a map.
            // Or even completely switch from Jackson to GSON?
            final Object fieldValue = ReflectionUtil.getValue(dto, field);
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
        }
    }

    private boolean isAllowedToRead(final Set<Role> roles, final Field field) {
        for (Role role : roles) {
            if (role.isAllowedToRead(field)) {
                return true;
            }
        }
        return Role.ANYBODY.isAllowedToRead(field);
    }

}
