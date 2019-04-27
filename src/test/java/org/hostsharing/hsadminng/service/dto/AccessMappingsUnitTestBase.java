package org.hostsharing.hsadminng.service.dto;

import org.hostsharing.hsadminng.service.accessfilter.AccessFor;
import org.hostsharing.hsadminng.service.accessfilter.Role;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Usually base classes for unit tests are not a good idea, but because
 * DTOs which implement AccessMapping are more like a DSL,
 * this base class should be used to enforce its required structure.
 */
public abstract class AccessMappingsUnitTestBase {

    protected AccessRightsMatcher initAccesFor(final Class<AssetDTO> dtoClass, final Role role) {
        return new AccessRightsMatcher(dtoClass, role, AccessFor::init);
    }

    protected AccessRightsMatcher updateAccesFor(final Class<AssetDTO> dtoClass, final Role role) {
        return new AccessRightsMatcher(dtoClass, role, AccessFor::update);
    }

    protected AccessRightsMatcher readAccesFor(final Class<AssetDTO> dtoClass, final Role role) {
        return new AccessRightsMatcher(dtoClass, role, AccessFor::read);
    }

    protected static class AccessRightsMatcher {
        private final Class<AssetDTO> dtoClass;
        private final Role role;

        private final String[] namesOfFieldsWithAccessForAnnotation;
        private final String[] namesOfAccessibleFields;

        AccessRightsMatcher(final Class<AssetDTO> dtoClass, final Role role, final Function<AccessFor, Role[]> access) {
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


        private static Set<Field> determineFieldsWithAccessForAnnotation(final Class<AssetDTO> dtoClass) {

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
}
