package org.hostsharing.hsadminng.service.dto;

import org.apache.commons.lang3.RandomUtils;
import org.hostsharing.hsadminng.service.accessfilter.AccessFor;
import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.hostsharing.hsadminng.service.util.ReflectionUtil;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Usually base classes for unit tests are not a good idea, but because
 * DTOs which implement AccessMapping are more like a DSL,
 * this base class should be used to enforce its required structure.
 */
public abstract class AccessMappingsUnitTestBase<D> {

    @Test
    public void shouldConvertToString() {
        final D sampleDto = createSampleDto(1234L);
        final String dtoAsString = dtoToString(sampleDto);
        assertThat(sampleDto.toString()).isEqualTo(dtoAsString);
    }

    @Test
    @SuppressWarnings("all")
    public void shouldImplementEqualsJustUsingClassAndId() {
        final D dto = createSampleDto(1234L);
        assertThat(dto.equals(dto)).isTrue();

        final D dtoWithSameId = createRandomDto(1234L);
        assertThat(dto.equals(dtoWithSameId)).isTrue();

        final D dtoWithAnotherId = createRandomDto(RandomUtils.nextLong(2000, 9999));
        assertThat(dtoWithAnotherId.equals(dtoWithSameId)).isFalse();

        final D dtoWithoutId = createRandomDto(null);
        assertThat(dto.equals(dtoWithoutId)).isFalse();
        assertThat(dtoWithoutId.equals(dto)).isFalse();

        assertThat(dto.equals(null)).isFalse();
        assertThat(dto.equals("")).isFalse();
    }

    @Test
    public void shouldImplementHashCodeJustUsingClassAndId() {
        final long randomId = RandomUtils.nextLong();
        final D dto = createSampleDto(randomId);
        assertThat(dto.hashCode()).isEqualTo(Objects.hashCode(randomId));

        final D dtoWithoutId = createRandomDto(null);
        assertThat(dtoWithoutId.hashCode()).isEqualTo(Objects.hashCode(null));
    }

    protected abstract D createSampleDto(final Long id);

    protected abstract D createRandomDto(final Long id);

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
                .map(Field::getName).collect(Collectors.toList()).toArray(new String[]{});
            this.namesOfAccessibleFields = fieldsWithAccessForAnnotation.stream()
                .filter(f -> allows(f, access, role)).map(Field::getName).collect(Collectors.toList()).toArray(new String[]{});
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
}
