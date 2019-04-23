package org.hostsharing.hsadminng.service.accessfilter;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.lang3.NotImplementedException;

import java.io.IOException;
import java.lang.reflect.Field;

public class JSonSerializerWithAccessFilter <T> extends JSonAccessFilter<T> {
    private final JsonGenerator jsonGenerator;
    private final SerializerProvider serializerProvider;

    public JSonSerializerWithAccessFilter(final JsonGenerator jsonGenerator,
                                          final SerializerProvider serializerProvider,
                                          final T dto) {
        super(dto);
        this.jsonGenerator = jsonGenerator;
        this.serializerProvider = serializerProvider;
    }

    // Jackson serializes into the JsonGenerator, thus no return value needed.
    public void serialize() throws IOException {

        jsonGenerator.writeStartObject();
        for (Field prop : dto.getClass().getDeclaredFields()) {
            toJSon(dto, jsonGenerator, prop);
        }
        jsonGenerator.writeEndObject();
    }

    private void toJSon(final Object dto, final JsonGenerator jsonGenerator, final Field prop) throws IOException {
        if (getLoginUserRole().isAllowedToRead(prop)) {
            final String fieldName = prop.getName();
            // TODO: maybe replace by serializerProvider.defaultSerialize...()?
            //  But that makes it difficult for parallel structure with the deserializer (clumsy API).
            //  Alternatively extract the supported types to subclasses of some abstract class and
            //  here as well as in the deserializer just access the matching implementation through a map.
            //  Or even completely switch from Jackson to GSON?
            if (Integer.class.isAssignableFrom(prop.getType()) || int.class.isAssignableFrom(prop.getType())) {
                jsonGenerator.writeNumberField(fieldName, (int) get(dto, prop));
            } else if (Long.class.isAssignableFrom(prop.getType()) || long.class.isAssignableFrom(prop.getType())) {
                jsonGenerator.writeNumberField(fieldName, (long) get(dto, prop));
            } else if (String.class.isAssignableFrom(prop.getType())) {
                jsonGenerator.writeStringField(fieldName, (String) get(dto, prop));
            } else {
                throw new NotImplementedException("property type not yet implemented: " + prop);
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
