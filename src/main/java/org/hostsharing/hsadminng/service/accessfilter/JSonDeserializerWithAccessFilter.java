package org.hostsharing.hsadminng.service.accessfilter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.ObjectUtils;
import org.hostsharing.hsadminng.service.util.ReflectionUtil;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.hostsharing.hsadminng.service.util.ReflectionUtil.unchecked;

public class JSonDeserializerWithAccessFilter<T> extends JSonAccessFilter<T> {

    private final TreeNode treeNode;
    private final Set<Field> writtenFields = new HashSet<>();

    public JSonDeserializerWithAccessFilter(final ApplicationContext ctx, final JsonParser jsonParser, final DeserializationContext deserializationContext, Class<T> dtoClass) {
        super(ctx, unchecked(dtoClass::newInstance));
        this.treeNode = unchecked(() -> jsonParser.getCodec().readTree(jsonParser));
    }

    // Jackson deserializes from the JsonParser, thus no input parameter needed.
    public T deserialize() {
        deserializeValues();
        final T currentDto = loadCurrentDto(getId());
        overwriteUnmodifiedFieldsWithCurrentValues(currentDto);
        checkAccessToWrittenFields(currentDto);
        return dto;
    }

    private void deserializeValues() {
        treeNode.fieldNames().forEachRemaining(fieldName -> {
            try {
                final Field field = dto.getClass().getDeclaredField(fieldName);
                final Object newValue = readValue(treeNode, field);
                writeValue(dto, field, newValue);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("setting field " + fieldName + " failed", e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private T loadCurrentDto(final Long id) {
        if (id != null) {
            return (T) loadDto(selfIdField.getAnnotation(SelfId.class).resolver(), id);
        }
        return null;
    }

    private void overwriteUnmodifiedFieldsWithCurrentValues(final Object currentDto) {
        if ( currentDto == null ) {
            return;
        }
        for (Field field : currentDto.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(AccessFor.class) && !writtenFields.contains(field)) {
                final Object value = ReflectionUtil.getValue(currentDto, field);
                ReflectionUtil.setValue(dto, field, value);
            }

        }
    }

    private Object readValue(final TreeNode treeNode, final Field field) {
        return readValue(treeNode, field.getName(), field.getType());

    }

    private Object readValue(final TreeNode treeNode, final String fieldName, final Class<?> fieldClass) {
        final TreeNode fieldNode = treeNode.get(fieldName);
        if (fieldNode instanceof TextNode) {
            return ((TextNode) fieldNode).asText();
        } else if (fieldNode instanceof IntNode) {
            return ((IntNode) fieldNode).asInt();
        } else if (fieldNode instanceof LongNode) {
            return ((LongNode) fieldNode).asLong();
        } else if (fieldNode instanceof ArrayNode && LocalDate.class.isAssignableFrom(fieldClass)) {
            return LocalDate.of(((ArrayNode) fieldNode).get(0).asInt(), ((ArrayNode) fieldNode).get(1).asInt(), ((ArrayNode) fieldNode).get(2).asInt());
        } else {
            throw new NotImplementedException("property type not yet implemented: " + fieldNode + " -> " + fieldName + ": " + fieldClass);
        }
    }

    private void writeValue(final T dto, final Field field, final Object value) {
        if (field.getType().isAssignableFrom(value.getClass())) {
            ReflectionUtil.setValue(dto, field, value);
        } else if (Integer.class.isAssignableFrom(field.getType()) || int.class.isAssignableFrom(field.getType())) {
            ReflectionUtil.setValue(dto, field, ((Number) value).intValue());
        } else if (Long.class.isAssignableFrom(field.getType()) || long.class.isAssignableFrom(field.getType())) {
            ReflectionUtil.setValue(dto, field, ((Number) value).longValue());
        } else if (field.getType().isEnum()) {
            ReflectionUtil.setValue(dto, field, Enum.valueOf((Class<Enum>) field.getType(), value.toString()));
        } else if (LocalDate.class.isAssignableFrom(field.getType())) {
            ReflectionUtil.setValue(dto, field, LocalDate.parse(value.toString()));
        } else {
            throw new NotImplementedException("property type not yet implemented: " + field);
        }
        writtenFields.add(field);
    }

    private void checkAccessToWrittenFields(final T currentDto) {
        writtenFields.forEach(field -> {
            if (!field.equals(selfIdField)) {
                final Role role = getLoginUserRole();
                if (getId() == null) {
                    if (!role.isAllowedToInit(field)) {
                        if (!field.equals(parentIdField)) {
                            throw new BadRequestAlertException("Initialization of field " + toDisplay(field) + " prohibited for current user role " + role, toDisplay(field), "initializationProhibited");
                        } else {
                            throw new BadRequestAlertException("Referencing field " + toDisplay(field) + " prohibited for current user role " + role, toDisplay(field), "referencingProhibited");
                        }
                    }
                } else if (isUpdate(field, dto, currentDto) && !getLoginUserRole().isAllowedToUpdate(field)){
                    throw new BadRequestAlertException("Update of field " + toDisplay(field) + " prohibited for current user role " + role, toDisplay(field), "updateProhibited");
                }
            }
        });
    }

    private boolean isUpdate(final Field field, final T dto, T currentDto) {
        return ObjectUtils.notEqual(ReflectionUtil.getValue(dto, field), ReflectionUtil.getValue(currentDto, field));
    }
}
