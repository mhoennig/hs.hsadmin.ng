package org.hostsharing.hsadminng.service.accessfilter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.NotImplementedException;
import org.hostsharing.hsadminng.service.util.ReflectionUtil;

import java.lang.reflect.Field;

import static org.hostsharing.hsadminng.service.util.ReflectionUtil.unchecked;

public class JSonDeserializerWithAccessFilter<T> {

    private final T dto;
    private final TreeNode treeNode;

    public JSonDeserializerWithAccessFilter(final JsonParser jsonParser, final DeserializationContext deserializationContext, Class<T> dtoClass) {
        this.treeNode = unchecked(() -> jsonParser.getCodec().readTree(jsonParser));
        this.dto = unchecked(() -> dtoClass.newInstance());
    }

    public T deserialize() {
        treeNode.fieldNames().forEachRemaining(fieldName -> {
            try {
                final Field field = dto.getClass().getDeclaredField(fieldName);
                final Object value = readValue(treeNode, field);
                writeValue(dto, field, value);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("setting field " + fieldName + " failed", e);
            }
        });
        return dto;
    }

    private Object readValue(final TreeNode treeNode, final Field field) {
        final TreeNode fieldNode = treeNode.get(field.getName());
        if (fieldNode instanceof TextNode) {
            return ((TextNode)fieldNode).asText();
        } else if (fieldNode instanceof IntNode) {
            return ((IntNode)fieldNode).asInt();
        } else if (fieldNode instanceof LongNode) {
            return ((LongNode)fieldNode).asLong();
        } else {
            throw new NotImplementedException("property type not yet implemented: " + field);
        }
    }

    private void writeValue(final T dto, final Field field, final Object value) {
        if ( field.getType().isAssignableFrom(value.getClass()) ) {
            ReflectionUtil.setValue(dto, field, value);
        } else  if (Integer.class.isAssignableFrom(field.getType()) || int.class.isAssignableFrom(field.getType())) {
            ReflectionUtil.setValue(dto, field, ((Number)value).intValue());
        } else if (Long.class.isAssignableFrom(field.getType()) || long.class.isAssignableFrom(field.getType())) {
            ReflectionUtil.setValue(dto, field, ((Number)value).longValue());
        } else  {
            throw new NotImplementedException("property type not yet implemented: " + field);
        }
    }
}
