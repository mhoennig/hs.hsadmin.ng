package org.hostsharing.hsadminng.service.accessfilter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.NotImplementedException;
import org.hostsharing.hsadminng.service.util.ReflectionUtil;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.hostsharing.hsadminng.service.util.ReflectionUtil.unchecked;

public class JSonDeserializerWithAccessFilter<T> extends JSonAccessFilter<T> {

    private final TreeNode treeNode;
    private final Set<Field> modifiedFields = new HashSet<>();

    public JSonDeserializerWithAccessFilter(final ApplicationContext ctx, final JsonParser jsonParser, final DeserializationContext deserializationContext, Class<T> dtoClass) {
        super(ctx, unchecked(dtoClass::newInstance));
        this.treeNode = unchecked(() -> jsonParser.getCodec().readTree(jsonParser));
    }

    // Jackson deserializes from the JsonParser, thus no input parameter needed.
    public T deserialize() {
        deserializeValues();
        checkAccessToModifiedFields();
        return dto;
    }

    private void deserializeValues() {
        treeNode.fieldNames().forEachRemaining(fieldName -> {
            try {
                final Field field = dto.getClass().getDeclaredField(fieldName);
                final Object value = readValue(treeNode, field);
                writeValue(dto, field, value);
                markAsModified(field);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("setting field " + fieldName + " failed", e);
            }
        });
    }

    private Object readValue(final TreeNode treeNode, final Field field) {
        final TreeNode fieldNode = treeNode.get(field.getName());
        if (fieldNode instanceof TextNode) {
            return ((TextNode) fieldNode).asText();
        } else if (fieldNode instanceof IntNode) {
            return ((IntNode) fieldNode).asInt();
        } else if (fieldNode instanceof LongNode) {
            return ((LongNode) fieldNode).asLong();
        } else {
            throw new NotImplementedException("property type not yet implemented: " + field);
        }
    }

    private void writeValue(final T dto, final Field field, final Object value) {
        if (field.getType().isAssignableFrom(value.getClass())) {
            ReflectionUtil.setValue(dto, field, value);
        } else if (Integer.class.isAssignableFrom(field.getType()) || int.class.isAssignableFrom(field.getType())) {
            ReflectionUtil.setValue(dto, field, ((Number) value).intValue());
        } else if (Long.class.isAssignableFrom(field.getType()) || long.class.isAssignableFrom(field.getType())) {
            ReflectionUtil.setValue(dto, field, ((Number) value).longValue());
        } else {
            throw new NotImplementedException("property type not yet implemented: " + field);
        }
    }

    private void markAsModified(final Field field) {
        modifiedFields.add(field);
    }

    private void checkAccessToModifiedFields() {
        modifiedFields.forEach(field -> {
            if ( !field.equals(selfIdField) ) {
                if (getId() == null) {
                    if (!getLoginUserRole().isAllowedToInit(field)) {
                        if ( !field.equals(parentIdField)) {
                            throw new BadRequestAlertException("Initialization of field prohibited for current user", toDisplay(field), "initializationProhibited");
                        } else {
                            throw new BadRequestAlertException("Referencing field prohibited for current user", toDisplay(field), "referencingProhibited");
                        }
                    }
                } else if (!getLoginUserRole().isAllowedToUpdate(field)) {
                        throw new BadRequestAlertException("Update of field prohibited for current user", toDisplay(field), "updateProhibited");
                }
            }
        });
    }
}
