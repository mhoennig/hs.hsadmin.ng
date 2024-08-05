package net.hostsharing.hsadminng.rbac.test;

import net.hostsharing.hsadminng.rbac.rbacobject.BaseEntity;
import net.hostsharing.hsadminng.mapper.EntityPatcher;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assumptions.assumeThat;

public abstract class PatchUnitTestBase<R, E> {

    @Test
    void willPatchNoProperty() {
        // given
        final var givenEntity = newInitialEntity();
        final var patchResource = newPatchResource();

        // when
        createPatcher(givenEntity).apply(patchResource);

        // then
        final var expectedEntity = newInitialEntity();
        assertThat(givenEntity).usingRecursiveComparison().isEqualTo(expectedEntity);
    }

    @Test
    @SuppressWarnings("unchecked")
    void willPatchAllProperties() {
        // given
        final var givenEntity = newInitialEntity();
        final var patchResource = newPatchResource();
        propertyTestDescriptors().forEach(testCase ->
                testCase.patchResource(patchResource)
        );

        // when
        createPatcher(givenEntity).apply(patchResource);

        // then
        final var expectedEntity = newInitialEntity();
        propertyTestDescriptors().forEach(testCase ->
                testCase.updateEntity(expectedEntity)
        );
        assertThat(givenEntity).usingRecursiveComparison().isEqualTo(expectedEntity);
    }

    @ParameterizedTest
    @MethodSource("propertyTestCases")
    void willPatchOnlyGivenProperty(final Property<R, Object, E, Object> testCase) {

        // given
        final var givenEntity = newInitialEntity();
        final var patchResource = newPatchResource();
        testCase.patchResource(patchResource);

        // when
        createPatcher(givenEntity).apply(patchResource);

        // then
        final var expectedEntity = newInitialEntity();
        testCase.updateEntity(expectedEntity);
        assertThat(givenEntity).usingRecursiveComparison().isEqualTo(expectedEntity);
    }

    @ParameterizedTest
    @MethodSource("propertyTestCases")
    void willThrowExceptionIfNotNullableValueIsNull(final Property<R, Object, E, Object> testCase) {
        assumeThat(testCase instanceof JsonNullableProperty).isTrue();
        assumeThat(testCase.nullable).isFalse();

        // given
        final var givenEntity = newInitialEntity();
        final var patchResource = newPatchResource();
        testCase.patchResourceToNullValue(patchResource);

        // when
        final var actualException = catchThrowableOfType(
                () -> createPatcher(givenEntity).apply(patchResource),
                IllegalArgumentException.class);

        // then
        assertThat(actualException).hasMessage("property '" + testCase.name + "' must not be null");
        assertThat(givenEntity).usingRecursiveComparison().isEqualTo(newInitialEntity());
    }

    @ParameterizedTest
    @MethodSource("propertyTestCases")
    void willPatchOnlyGivenPropertyToNull(final Property<R, Object, E, Object> testCase) {
        assumeThat(testCase.nullable).isTrue();

        // given
        final var givenEntity = newInitialEntity();
        final var patchResource = newPatchResource();
        testCase.patchResourceToNullValue(patchResource);

        // when
        createPatcher(givenEntity).apply(patchResource);

        // then
        final var expectedEntity = newInitialEntity();
        testCase.entitySetter.accept(expectedEntity, null);
        assertThat(givenEntity).usingRecursiveComparison().isEqualTo(expectedEntity);
    }

    @ParameterizedTest
    @MethodSource("propertyTestCases")
    void willNotPatchIfGivenPropertyNotGiven(final Property<R, Object, E, Object> testCase) {

        // given
        final var givenEntity = newInitialEntity();
        final var patchResource = newPatchResource();
        testCase.patchResourceToNotGiven(patchResource);

        // when
        createPatcher(givenEntity).apply(patchResource);

        // then
        final var expectedEntity = newInitialEntity();
        assertThat(givenEntity).usingRecursiveComparison().isEqualTo(expectedEntity);
    }

    protected abstract E newInitialEntity();

    protected abstract R newPatchResource();

    protected abstract EntityPatcher<R> createPatcher(final E entity);

    @SuppressWarnings("types")
    protected abstract Stream<Property> propertyTestDescriptors();

    private Stream<Arguments> propertyTestCases() {
        return propertyTestDescriptors()
                .map(tc -> Arguments.of(Named.of(tc.name, tc)));
    }

    protected static abstract class Property<R, RV, E, EV> {

        protected final String name;
        protected final BiConsumer<E, EV> entitySetter;
        protected final EV expectedPatchValue;
        protected boolean nullable = true;

        protected Property(
                final String name,
                final BiConsumer<E, EV> entitySetter,
                final EV givenPatchValue) {
            this.name = name;

            this.entitySetter = entitySetter;
            this.expectedPatchValue = givenPatchValue;
        }

        protected abstract void patchResource(R patchResource);

        protected abstract void patchResourceToNullValue(R patchResource);

        protected abstract void patchResourceToNotGiven(R patchResource);

        protected abstract void patchResourceWithExplicitValue(final R patchResource, final RV explicitPatchValue);

        void updateEntity(final E expectedEntity) {
            entitySetter.accept(expectedEntity, expectedPatchValue);
        }

        public Property<R, RV, E, EV> notNullable() {
            nullable = false;
            return this;
        }

        @SuppressWarnings("unchecked")
        protected static <EV, RV> EV sameAs(final RV givenResourceValue) {
            return (EV) givenResourceValue;
        }
    }

    protected static class SimpleProperty<R, RV, E, EV> extends Property<R, RV, E, EV> {

        public final RV givenPatchValue;
        private final BiConsumer<R, RV> resourceSetter;

        public SimpleProperty(
                final String name,
                final BiConsumer<R, RV> resourceSetter,
                final RV givenPatchValue,
                final BiConsumer<E, EV> entitySetter
        ) {
            super(name, entitySetter, sameAs(givenPatchValue));
            this.resourceSetter = resourceSetter;
            this.givenPatchValue = givenPatchValue;
        }

        public SimpleProperty(
                final String name,
                final BiConsumer<R, RV> resourceSetter,
                final RV givenPatchValue,
                final BiConsumer<E, EV> entitySetter,
                final EV expectedPatchValue
        ) {
            super(name, entitySetter, expectedPatchValue);
            this.resourceSetter = resourceSetter;
            this.givenPatchValue = givenPatchValue;
        }

        @Override
        protected void patchResource(final R patchResource) {
            resourceSetter.accept(patchResource, givenPatchValue);
        }

        @Override
        protected void patchResourceToNullValue(final R patchResource) {
            assertThat(nullable).isTrue(); // null can mean "not given" or "null value", not both
            resourceSetter.accept(patchResource, null);
        }

        @Override
        protected void patchResourceToNotGiven(final R patchResource) {
            assertThat(nullable).isFalse(); // null can mean "not given" or "null value", not both
            resourceSetter.accept(patchResource, null);
        }

        @Override
        protected void patchResourceWithExplicitValue(final R patchResource, final RV explicitPatchValue) {
            resourceSetter.accept(patchResource, explicitPatchValue);
        }
    }

    protected static class JsonNullableProperty<R, RV, E extends BaseEntity, EV> extends Property<R, RV, E, EV> {

        private final BiConsumer<R, JsonNullable<RV>> resourceSetter;
        public final RV givenPatchValue;

        public JsonNullableProperty(
                final String name,
                final BiConsumer<R, JsonNullable<RV>> resourceSetter,
                final RV givenPatchValue,
                final BiConsumer<E, EV> entitySetter
        ) {
            super(name, entitySetter, sameAs(givenPatchValue));
            this.resourceSetter = resourceSetter;
            this.givenPatchValue = givenPatchValue;
        }

        public JsonNullableProperty(
                final String name,
                final BiConsumer<R, JsonNullable<RV>> resourceSetter,
                final RV givenPatchValue,
                final BiConsumer<E, EV> entitySetter,
                final EV expectedPatchValue
        ) {
            super(name, entitySetter, expectedPatchValue);
            this.resourceSetter = resourceSetter;
            this.givenPatchValue = givenPatchValue;
        }

        @Override
        protected void patchResource(final R patchResource) {
            resourceSetter.accept(patchResource, JsonNullable.of(givenPatchValue));
        }

        @Override
        protected void patchResourceToNullValue(final R patchResource) {
            resourceSetter.accept(patchResource, JsonNullable.of(null));
        }

        @Override
        protected void patchResourceToNotGiven(final R patchResource) {
            resourceSetter.accept(patchResource, null);
        }

        @Override
        protected void patchResourceWithExplicitValue(final R patchResource, final RV explicitPatchValue) {
            resourceSetter.accept(patchResource, JsonNullable.of(explicitPatchValue));

        }
    }
}
