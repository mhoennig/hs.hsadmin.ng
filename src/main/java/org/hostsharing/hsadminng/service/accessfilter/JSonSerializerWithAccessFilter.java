package org.hostsharing.hsadminng.service.accessfilter;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.lang3.NotImplementedException;
import org.hostsharing.hsadminng.security.SecurityUtils;
import org.hostsharing.hsadminng.service.dto.CustomerDTO;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@JsonComponent
public class JSonSerializerWithAccessFilter extends JsonSerializer<Object> {

    @Override
    public void serialize(Object dto, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {

        jsonGenerator.writeStartObject();
        for (Field prop : CustomerDTO.class.getDeclaredFields()) {
            toJSon(dto, jsonGenerator, prop);
        }

        jsonGenerator.writeEndObject();
    }

    private void toJSon(Object dto, JsonGenerator jsonGenerator, Field prop) throws IOException {
        if (getLoginUserRole().isAllowedToRead(prop)) {
            final String fieldName = prop.getName();
            if (Integer.class.isAssignableFrom(prop.getType()) || int.class.isAssignableFrom(prop.getType())) {
                jsonGenerator.writeNumberField(fieldName, (int) get(dto, prop));
            } else if (Long.class.isAssignableFrom(prop.getType()) || long.class.isAssignableFrom(prop.getType())) {
                jsonGenerator.writeNumberField(fieldName, (long) get(dto, prop));
            } else if (String.class.isAssignableFrom(prop.getType())) {
                jsonGenerator.writeStringField(fieldName, (String) get(dto, prop));
            } else {
                throw new NotImplementedException("property type not yet implemented" + prop);
            }
        }
    }

    private Object get(Object dto, Field field) {
        try {
            field.setAccessible(true);
            return field.get(dto);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Role getLoginUserRole() {
        return SecurityUtils.getCurrentUserLogin().map(u -> Role.valueOf(u.toUpperCase())).orElse(Role.ANYBODY);
    }

    private Object invoke(Object dto, Method method) {
        try {
            return method.invoke(dto);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
