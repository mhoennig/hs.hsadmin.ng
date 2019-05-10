// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import static org.hostsharing.hsadminng.service.util.ReflectionUtil.unchecked;

import org.hostsharing.hsadminng.service.UserRoleAssignmentService;
import org.hostsharing.hsadminng.service.util.ReflectionUtil;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.node.*;
import com.google.common.base.Joiner;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Actual implementation of JSON deserialization, where {link JSonDeserializerWithAccessFilter}
 * is a stateless bean, {@link JSonDeserializationWithAccessFilter} exists only during the actual
 * deserialization and contains a deserialization state.
 *
 * @param <T> DTO class to serialize
 */
public class JSonDeserializationWithAccessFilter<T> extends JSonAccessFilter<T> {

    private final TreeNode treeNode;
    private final Set<Field> updatingFields = new HashSet<>();

    public JSonDeserializationWithAccessFilter(
            final ApplicationContext ctx,
            final UserRoleAssignmentService userRoleAssignmentService,
            final JsonParser jsonParser,
            final DeserializationContext deserializationContext,
            Class<T> dtoClass) {
        super(ctx, userRoleAssignmentService, unchecked(dtoClass::newInstance));
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
                final Object newValue = readValueFromJSon(treeNode, field);
                writeValueToDto(dto, field, newValue);
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

    private void overwriteUnmodifiedFieldsWithCurrentValues(final T currentDto) {
        if (currentDto == null) {
            return;
        }
        for (Field field : currentDto.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(AccessFor.class)) {
                boolean updatingField = updatingFields.contains(field);
                if (updatingField && !isActuallyUpdated(field, dto, currentDto)) {
                    updatingFields.remove(field);
                    updatingField = false;
                }
                if (!updatingField) {
                    final Object value = ReflectionUtil.getValue(currentDto, field);
                    ReflectionUtil.setValue(dto, field, value);
                }
            }

        }
    }

    private Object readValueFromJSon(final TreeNode treeNode, final Field field) {
        return readValueFromJSon(treeNode, field.getName(), field.getType());
    }

    private Object readValueFromJSon(final TreeNode treeNode, final String fieldName, final Class<?> fieldClass) {
        final TreeNode fieldNode = treeNode.get(fieldName);
        if (fieldNode instanceof NullNode) {
            return null;
        }
        if (fieldNode instanceof TextNode) {
            return ((TextNode) fieldNode).asText();
        }
        if (fieldNode instanceof IntNode) {
            return ((IntNode) fieldNode).asInt();
        }
        if (fieldNode instanceof LongNode) {
            return ((LongNode) fieldNode).asLong();
        }
        if (fieldNode instanceof DoubleNode) {
            // TODO: we need to figure out, why DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS does not work
            return ((DoubleNode) fieldNode).asDouble();
        }
        if (fieldNode instanceof ArrayNode && LocalDate.class.isAssignableFrom(fieldClass)) {
            return LocalDate.of(
                    ((ArrayNode) fieldNode).get(0).asInt(),
                    ((ArrayNode) fieldNode).get(1).asInt(),
                    ((ArrayNode) fieldNode).get(2).asInt());
        }
        throw new NotImplementedException(
                "JSon node type not implemented: " + fieldNode.getClass() + " -> " + fieldName + ": " + fieldClass);
    }

    private void writeValueToDto(final T dto, final Field field, final Object value) {
        if (value == null) {
            ReflectionUtil.setValue(dto, field, null);
        } else if (field.getType().isAssignableFrom(value.getClass())) {
            ReflectionUtil.setValue(dto, field, value);
        } else if (int.class.isAssignableFrom(field.getType())) {
            ReflectionUtil.setValue(dto, field, ((Number) value).intValue());
        } else if (Long.class.isAssignableFrom(field.getType()) || long.class.isAssignableFrom(field.getType())) {
            ReflectionUtil.setValue(dto, field, ((Number) value).longValue());
        } else if (BigDecimal.class.isAssignableFrom(field.getType())) {
            ReflectionUtil.setValue(dto, field, new BigDecimal(value.toString()));
        } else if (Boolean.class.isAssignableFrom(field.getType()) || boolean.class.isAssignableFrom(field.getType())) {
            ReflectionUtil.setValue(dto, field, Boolean.valueOf(value.toString()));
        } else if (field.getType().isEnum()) {
            ReflectionUtil.setValue(dto, field, ReflectionUtil.asEnumValue(field.getType(), value));
        } else if (LocalDate.class.isAssignableFrom(field.getType())) {
            ReflectionUtil.setValue(dto, field, LocalDate.parse(value.toString()));
        } else {
            throw new NotImplementedException("property type not yet implemented: " + field);
        }
        updatingFields.add(field);
    }

    private void checkAccessToWrittenFields(final T currentDto) {
        updatingFields.forEach(
                field -> {
                    // TODO this ugly code needs cleanup
                    if (!field.equals(selfIdField)) {
                        final Set<Role> roles = getLoginUserRoles();
                        if (isInitAccess()) {
                            if (!isAllowedToInit(roles, field)) {
                                if (!field.equals(parentIdField)) {
                                    throw new BadRequestAlertException(
                                            "Initialization of field " + toDisplay(field)
                                                    + " prohibited for current user roles "
                                                    + Joiner.on("+").join(roles),
                                            toDisplay(field),
                                            "initializationProhibited");
                                } else {
                                    throw new BadRequestAlertException(
                                            "Referencing field " + toDisplay(field) + " prohibited for current user roles "
                                                    + Joiner.on("+").join(roles),
                                            toDisplay(field),
                                            "referencingProhibited");
                                }
                            }
                        } else if (!Role.toBeIgnoredForUpdates(field) && !isAllowedToUpdate(getLoginUserRoles(), field)) {
                            throw new BadRequestAlertException(
                                    "Update of field " + toDisplay(field) + " prohibited for current user roles "
                                            + Joiner.on("+").join(roles),
                                    toDisplay(field),
                                    "updateProhibited");
                        }
                    }
                });
    }

    private boolean isAllowedToInit(final Set<Role> roles, final Field field) {
        for (Role role : roles) {
            if (role.isAllowedToInit(field)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllowedToUpdate(final Set<Role> roles, final Field field) {
        for (Role role : roles) {
            if (role.isAllowedToUpdate(field)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInitAccess() {
        return getId() == null;
    }

    private boolean isActuallyUpdated(final Field field, final T dto, T currentDto) {
        return 0 != ObjectUtils.compare(ReflectionUtil.getValue(dto, field), ReflectionUtil.getValue(currentDto, field));
    }
}
