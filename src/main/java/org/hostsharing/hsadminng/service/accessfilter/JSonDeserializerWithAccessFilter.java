package org.hostsharing.hsadminng.service.accessfilter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.NotImplementedException;
import org.hostsharing.hsadminng.security.SecurityUtils;
import org.hostsharing.hsadminng.service.util.ReflectionUtil;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.hostsharing.hsadminng.service.util.ReflectionUtil.unchecked;

public class JSonDeserializerWithAccessFilter<T> {

    private final T dto;
    private final TreeNode treeNode;
    private final Set<Field> modifiedFields = new HashSet<>();
    private Field selfIdField = null;

    public JSonDeserializerWithAccessFilter(final JsonParser jsonParser, final DeserializationContext deserializationContext, Class<T> dtoClass) {
        this.treeNode = unchecked(() -> jsonParser.getCodec().readTree(jsonParser));
        this.dto = unchecked(dtoClass::newInstance);
    }

    // Jackson deserializes from the JsonParser, thus no input parameter needed.
    public T deserialize() {
        determineSelfIdField();
        deserializeValues();
        checkAccessToModifiedFields();
        return dto;
    }

     private void determineSelfIdField() {
        for (Field field : dto.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(SelfId.class)) {
                if (selfIdField != null) {
                    throw new AssertionError("multiple @" + SelfId.class.getSimpleName() + " detected in " + field.getDeclaringClass().getSimpleName());
                }
                selfIdField = field;
            }
        }
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

    private Object getId() {
        if (selfIdField == null) {
            return null;
        }
        return ReflectionUtil.getValue(dto, selfIdField);
    }

    private void checkAccessToModifiedFields() {
        modifiedFields.forEach(field -> {
            if ( !field.equals(selfIdField) ) {
                if (getId() == null) {
                    if (!getLoginUserRole().isAllowedToInit(field)) {
                        throw new BadRequestAlertException("Initialization of field prohibited for current user", toDisplay(field), "initializationProhibited");
                    }
                } else if (getId() != null) {
                    if (!getLoginUserRole().isAllowedToUpdate(field)) {
                        throw new BadRequestAlertException("Update of field prohibited for current user", toDisplay(field), "updateProhibited");
                    }
                }
            }
        });
    }

    private String toDisplay(final Field field) {
        return field.getDeclaringClass().getSimpleName() + "." + field.getName();
    }

    private Role getLoginUserRole() {
        return SecurityUtils.getCurrentUserLogin().map(u -> Role.valueOf(u.toUpperCase())).orElse(Role.ANYBODY);
    }
}
