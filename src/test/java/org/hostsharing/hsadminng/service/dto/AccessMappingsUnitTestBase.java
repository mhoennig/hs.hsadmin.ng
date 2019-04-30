// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import org.hostsharing.hsadminng.service.accessfilter.*;
import org.hostsharing.hsadminng.service.util.ReflectionUtil;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.springframework.boot.jackson.JsonComponent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Usually base classes for unit tests are not a good idea, but because
 * DTOs which implement AccessMapping are more like a DSL,
 * this base class should be used to enforce its required structure.
 */
public abstract class AccessMappingsUnitTestBase<D> {

    private final Class<? extends AccessMappings> dtoClass;
    private final BiFunction<Long, Long, D> createSampleDTO;
    private final BiFunction<Long, Long, D> createRandomDTO;

    public AccessMappingsUnitTestBase(
            Class<? extends AccessMappings> dtoClass,
            final BiFunction<Long, Long, D> createSampleDTO,
            final BiFunction<Long, Long, D> createRandomDTO) {
        this.dtoClass = dtoClass;
        this.createSampleDTO = createSampleDTO;
        this.createRandomDTO = createRandomDTO;
    }

    @Test
    public void shouldConvertToString() {
        final D sampleDto = createSampleDTO.apply(1234L, 77L);
        final String dtoAsString = dtoToString(sampleDto);
        assertThat(sampleDto.toString()).isEqualTo(dtoAsString);
    }

    @Test
    @SuppressWarnings("all")
    public void shouldImplementEqualsJustUsingClassAndId() {
        final D dto = createSampleDTO.apply(1234L, 77L);
        assertThat(dto.equals(dto)).isTrue();

        final D dtoWithSameId = createSampleDTO.apply(1234L, 77L);
        assertThat(dto.equals(dtoWithSameId)).isTrue();

        final D dtoWithAnotherId = createSampleDTO.apply(RandomUtils.nextLong(2000, 9999), 77L);
        assertThat(dtoWithAnotherId.equals(dtoWithSameId)).isFalse();

        final D dtoWithoutId = createSampleDTO.apply(null, RandomUtils.nextLong());
        assertThat(dto.equals(dtoWithoutId)).isFalse();
        assertThat(dtoWithoutId.equals(dto)).isFalse();

        assertThat(dto.equals(null)).isFalse();
        assertThat(dto.equals("")).isFalse();
    }

    @Test
    public void shouldImplementHashCodeJustUsingClassAndId() {
        final long randomId = RandomUtils.nextLong();
        final D dto = createSampleDTO.apply(randomId, RandomUtils.nextLong());
        assertThat(dto.hashCode()).isEqualTo(Objects.hashCode(randomId));

        final D dtoWithoutId = createSampleDTO.apply(null, RandomUtils.nextLong());
        assertThat(dtoWithoutId.hashCode()).isEqualTo(Objects.hashCode(null));
    }

    @Test
    public void shouldImplementAccessMappings() {
        assertThat(dtoClass.getInterfaces()).as("must implement " + AccessMappings.class).contains(AccessMappings.class);
    }

    @Test
    public void shouldImplementSerializer() {
        shouldImplementJsonComponent(JsonSerializerWithAccessFilter.class);
    }

    @Test
    public void shouldImplementDeserializer() {
        shouldImplementJsonComponent(JsonDeserializerWithAccessFilter.class);
    }

    // --- only test fixture below ---

    protected AccessRightsMatcher initAccessFor(final Class<D> dtoClass, final Role role) {
        return new AccessRightsMatcher(dtoClass, role, AccessFor::init);
    }

    protected AccessRightsMatcher updateAccessFor(final Class<D> dtoClass, final Role role) {
        return new AccessRightsMatcher(dtoClass, role, AccessFor::update);
    }

    protected AccessRightsMatcher readAccessFor(final Class<D> dtoClass, final Role role) {
        return new AccessRightsMatcher(dtoClass, role, AccessFor::read);
    }

    // This class should have the same generics as the outer class, but then the
    // method references (AccessFor::*) can't be resolved anymore by the Java compiler.
    protected static class AccessRightsMatcher {

        private final Object dtoClass;
        private final Role role;

        private final String[] namesOfFieldsWithAccessForAnnotation;
        private final String[] namesOfAccessibleFields;

        AccessRightsMatcher(final Class dtoClass, final Role role, final Function<AccessFor, Role[]> access) {
            this.dtoClass = dtoClass;
            this.role = role;

            final Set<Field> fieldsWithAccessForAnnotation = determineFieldsWithAccessForAnnotation(dtoClass);
            this.namesOfFieldsWithAccessForAnnotation = fieldsWithAccessForAnnotation.stream()
                    .map(Field::getName)
                    .collect(Collectors.toList())
                    .toArray(new String[] {});
            this.namesOfAccessibleFields = fieldsWithAccessForAnnotation.stream()
                    .filter(f -> allows(f, access, role))
                    .map(Field::getName)
                    .collect(Collectors.toList())
                    .toArray(new String[] {});
        }

        public void shouldBeExactlyFor(final String... expectedFields) {
            assertThat(namesOfAccessibleFields).containsExactlyInAnyOrder(expectedFields);
        }

        public void shouldBeForNothing() {
            assertThat(namesOfAccessibleFields).isEmpty();
        }

        public void shouldBeForAllFields() {
            assertThat(namesOfAccessibleFields).containsExactlyInAnyOrder(namesOfFieldsWithAccessForAnnotation);
        }

        private static Set<Field> determineFieldsWithAccessForAnnotation(final Class<?> dtoClass) {

            final Set<Field> fieldsWithAccessForAnnotation = new HashSet<>();

            for (Field field : dtoClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(AccessFor.class)) {
                    final AccessFor accessFor = field.getAnnotation(AccessFor.class);
                    fieldsWithAccessForAnnotation.add(field);
                }
            }

            return fieldsWithAccessForAnnotation;
        }

        private static boolean allows(final Field field, final Function<AccessFor, Role[]> access, final Role role) {
            if (field.isAnnotationPresent(AccessFor.class)) {
                final AccessFor accessFor = field.getAnnotation(AccessFor.class);
                return role.coversAny(access.apply(accessFor));
            }
            return false;
        }
    }

    private String dtoToString(final D dto) {
        final StringBuilder fieldValues = new StringBuilder();
        boolean firstField = true;
        for (Field field : dto.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(AccessFor.class)) {
                firstField = appendCommaOptionally(fieldValues, firstField);
                appendFieldName(fieldValues, field);
                appendFieldValue(dto, fieldValues, field);
            }
        }
        return dto.getClass().getSimpleName() + "{" + fieldValues + "}";
    }

    private void appendFieldValue(final D dto, final StringBuilder fieldValues, final Field field) {
        final Object value = ReflectionUtil.getValue(dto, field);
        final boolean inQuotes = isJHipsterToStringUsingQuotes(field);
        if (inQuotes) {
            fieldValues.append("'");
        }
        fieldValues.append(value);
        if (inQuotes) {
            fieldValues.append("'");
        }
    }

    private void appendFieldName(final StringBuilder fieldValues, final Field field) {
        fieldValues.append(removeEnd(field.getName(), "Id"));
        fieldValues.append("=");
    }

    private boolean appendCommaOptionally(final StringBuilder fieldValues, boolean firstField) {
        if (firstField) {
            firstField = false;
        } else {
            fieldValues.append(", ");
        }
        return firstField;
    }

    private boolean isJHipsterToStringUsingQuotes(final Field field) {
        return !Number.class.isAssignableFrom(field.getType()) && !Boolean.class.isAssignableFrom(field.getType());
    }

    private void shouldImplementJsonComponent(final Class<?> expectedSuperclass) {
        for (Class<?> declaredClass : dtoClass.getDeclaredClasses()) {
            if (expectedSuperclass.isAssignableFrom(declaredClass)) {
                assertThat(declaredClass.isAnnotationPresent(JsonComponent.class))
                        .as(declaredClass + " requires @" + JsonComponent.class.getSimpleName())
                        .isTrue();
                assertThat(ReflectionUtil.determineGenericClassParameter(declaredClass, expectedSuperclass, 0))
                        .as(declaredClass + " must resolve generic parameter of " + expectedSuperclass + " to type of DTO")
                        .isEqualTo(dtoClass);
                assertThat(Modifier.isPublic(declaredClass.getModifiers())).as(declaredClass + " must be public").isTrue();
                assertThat(Modifier.isStatic(declaredClass.getModifiers())).as(declaredClass + " must be static").isTrue();
                assertThat(Modifier.isFinal(declaredClass.getModifiers())).as(declaredClass + " must not be final").isFalse();
                assertThat(Modifier.isAbstract(declaredClass.getModifiers())).as(declaredClass + " must not be abstract")
                        .isFalse();
                return;
            }
        }
        fail("no " + expectedSuperclass + " defined for " + dtoClass);
    }
}
