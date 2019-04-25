package org.hostsharing.hsadminng.service.accessfilter;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;

public class JSonSerializerWithAccessFilter<T> extends JSonAccessFilter<T> {
    private final JsonGenerator jsonGenerator;
    private final SerializerProvider serializerProvider;

    public JSonSerializerWithAccessFilter(final ApplicationContext ctx,
                                          final JsonGenerator jsonGenerator,
                                          final SerializerProvider serializerProvider,
                                          final T dto) {
        super(ctx, dto);
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
        if (getLoginUserRole().isAllowedToRead(field)) {
            final String fieldName = field.getName();
            // TODO: maybe replace by serializerProvider.defaultSerialize...()?
            //  But that makes it difficult for parallel structure with the deserializer (clumsy API).
            //  Alternatively extract the supported types to subclasses of some abstract class and
            //  here as well as in the deserializer just access the matching implementation through a map.
            //  Or even completely switch from Jackson to GSON?
            final Object fieldValue = get(dto, field);
            if (fieldValue == null) {
                jsonGenerator.writeNullField(fieldName);
            } else if (Integer.class.isAssignableFrom(field.getType()) || int.class.isAssignableFrom(field.getType())) {
                jsonGenerator.writeNumberField(fieldName, (int) fieldValue);
            } else if (Long.class.isAssignableFrom(field.getType()) || long.class.isAssignableFrom(field.getType())) {
                jsonGenerator.writeNumberField(fieldName, (long) fieldValue);
            } else if (LocalDate.class.isAssignableFrom(field.getType())) {
                jsonGenerator.writeStringField(fieldName, fieldValue.toString()); // TODO proper format
            } else if (Enum.class.isAssignableFrom(field.getType())) {
                jsonGenerator.writeStringField(fieldName, fieldValue.toString()); // TODO proper representation
            } else if (String.class.isAssignableFrom(field.getType())) {
                jsonGenerator.writeStringField(fieldName, (String) fieldValue);
            } else {
                throw new NotImplementedException("property type not yet implemented: " + field);
            }
        }
    }

    private Object get(final Object dto, final Field field) {
        try {
            field.setAccessible(true);
            return field.get(dto);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("getting field " + field + " failed", e);
        }
    }

}
