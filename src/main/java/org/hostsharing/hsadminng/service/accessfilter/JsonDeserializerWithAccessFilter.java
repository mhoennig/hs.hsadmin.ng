// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import com.google.common.base.Joiner;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.ObjectUtils;
import org.hostsharing.hsadminng.service.UserRoleAssignmentService;
import org.hostsharing.hsadminng.service.util.ReflectionUtil;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Verify.verify;
import static org.hostsharing.hsadminng.service.util.ReflectionUtil.unchecked;

public abstract class JsonDeserializerWithAccessFilter<T extends AccessMappings> extends JsonDeserializer<T> {

    private final ApplicationContext ctx;
    private final UserRoleAssignmentService userRoleAssignmentService;

    public JsonDeserializerWithAccessFilter(
            final ApplicationContext ctx,
            final UserRoleAssignmentService userRoleAssignmentService) {
        this.ctx = ctx;
        this.userRoleAssignmentService = userRoleAssignmentService;
    }

    @Override
    public T deserialize(
            final JsonParser jsonParser,
            final DeserializationContext deserializationContext) {

        final Class<T> dtoClass = ReflectionUtil
                .determineGenericClassParameter(this.getClass(), JsonDeserializerWithAccessFilter.class, 0);
        // @formatter:off
        return new JSonDeserializationWithAccessFilter(
                this, ctx, userRoleAssignmentService, jsonParser, deserializationContext, dtoClass)
            .deserialize();
        // @formatter:on
    }

    protected JSonFieldReader<T> jsonFieldReader(final TreeNode treeNode, final Field field) {
        return (final T object) -> {
            final Object newValue = readValueFromJSon(treeNode, field);
            writeValueToDto(object, field, newValue);
        };
    }

    /**
     * Returns the named subnode of the given node.
     * <p>
     * If entities are used instead of DTOs, JHipster will generate code which sends
     * complete entity trees to the REST endpoint. In most cases, we only need the "id",
     * though.
     * </p>
     *
     * @param node the JSON node of which a subnode is to be returned
     * @param name the name of the subnode within 'node'
     * @return the subnode of 'node' with the given 'name'
     */
    protected final JsonNode getSubNode(final TreeNode node, final String name) {
        verify(node.isObject(), node + " is not a JSON object");
        final ObjectNode objectNode = (ObjectNode) node;
        final JsonNode subNode = objectNode.get(name);
        verify(subNode.isNumber(), node + "." + name + " is not a number");
        return subNode;
    }

    private Object readValueFromJSon(final TreeNode treeNode, final Field field) {
        return readValueFromJSon(treeNode, field.getName(), field.getType());
    }

    private Object readValueFromJSon(final TreeNode treeNode, final String fieldName, final Class<?> fieldClass) {
        // FIXME can be removed? final TreeNode fieldNode = treeNode.get(fieldName);
        if (treeNode instanceof NullNode) {
            return null;
        }
        if (treeNode instanceof TextNode) {
            return ((TextNode) treeNode).asText();
        }
        if (treeNode instanceof IntNode) {
            return ((IntNode) treeNode).asInt();
        }
        if (treeNode instanceof LongNode) {
            return ((LongNode) treeNode).asLong();
        }
        if (treeNode instanceof DoubleNode) {
            // TODO: we need to figure out, why DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS does not work
            return ((DoubleNode) treeNode).asDouble();
        }
        if (treeNode instanceof ArrayNode && LocalDate.class.isAssignableFrom(fieldClass)) {
            return LocalDate.of(
                    ((ArrayNode) treeNode).get(0).asInt(),
                    ((ArrayNode) treeNode).get(1).asInt(),
                    ((ArrayNode) treeNode).get(2).asInt());
        }
        throw new NotImplementedException(
                "JSon node type not implemented: " + treeNode.getClass() + " -> " + fieldName + ": " + fieldClass);
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
    }

    /**
     * Internal implementation of JSON deserialization, where {@link JsonDeserializerWithAccessFilter}
     * is a stateless bean, this inner class exists only during the actual deserialization and contains
     * the deserialization state.
     */
    private class JSonDeserializationWithAccessFilter extends JSonAccessFilter<T> {

        private final TreeNode treeNode;
        private final Set<Field> updatingFields = new HashSet<>();

        public JSonDeserializationWithAccessFilter(
                final JsonDeserializerWithAccessFilter deserializer,
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
                    final TreeNode node = treeNode.get(fieldName);
                    jsonFieldReader(node, field).readInto(dto);
                    updatingFields.add(field);
                } catch (NoSuchFieldException e) {
                    throw new BadRequestAlertException("Unknown property", fieldName, "unknownProperty");
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

        private void checkAccessToWrittenFields(final T currentDto) {
            updatingFields.forEach(
                    field -> {
                        final Set<Role> roles = getLoginUserRoles();
                        if (isInitAccess()) {
                            validateInitAccess(field, roles);
                        } else {
                            validateUpdateAccess(field, roles);
                        }
                    });
        }

        private void validateInitAccess(Field field, Set<Role> roles) {
            if (!Role.toBeIgnoredForUpdates(field) && !isAllowedToInit(roles, field)) {
                if (!field.equals(parentIdField)) {
                    throw new BadRequestAlertException(
                            "Initialization of field " + toDisplay(field)
                                    + " prohibited for current user role(s): "
                                    + asString(roles),
                            toDisplay(field),
                            "initializationProhibited");
                } else {
                    throw new BadRequestAlertException(
                            "Referencing field " + toDisplay(field)
                                    + " prohibited for current user role(s): "
                                    + asString(roles),
                            toDisplay(field),
                            "referencingProhibited");
                }
            }
        }

        private String asString(Set<Role> roles) {
            return Joiner.on("+").join(roles.stream().map(Role::name).toArray());
        }

        private void validateUpdateAccess(Field field, Set<Role> roles) {
            if (!Role.toBeIgnoredForUpdates(field) && !isAllowedToUpdate(getLoginUserRoles(), field)) {
                throw new BadRequestAlertException(
                        "Update of field " + toDisplay(field) + " prohibited for current user role(s): "
                                + asString(roles),
                        toDisplay(field),
                        "updateProhibited");
            }
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

        private <F> boolean isActuallyUpdated(final Field field, final T dto, T currentDto) {
            final Object o1 = ReflectionUtil.getValue(dto, field);
            final Object o2 = ReflectionUtil.getValue(currentDto, field);

            if (o1 instanceof Comparable && o2 instanceof Comparable) {
                verify(
                        o2 instanceof Comparable,
                        "either neither or both objects must implement Comparable"); // $COVERAGE-IGNORE$
                return 0 != ((Comparable) o1).compareTo(o2);
            }
            return ObjectUtils.notEqual(o1, o2);
        }
    }
}
