package net.hostsharing.hsadminng.hs.office.partner;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.NoSuchElementException;
import java.util.UUID;
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
    void willPatchAllProperties() {
        // given
        final var givenEntity = newInitialEntity();
        final var patchResource = newPatchResource();
        testCases().forEach(testCase ->
                testCase.resourceSetter.accept(patchResource, JsonNullable.of(testCase.givenPatchedValue))
        );

        // when
        createPatcher(givenEntity).apply(patchResource);

        // then
        final var expectedEntity = newInitialEntity();
        testCases().forEach(testCase ->
                testCase.entitySetter.accept(expectedEntity, testCase.expectedPatchValue)
        );
        assertThat(givenEntity).usingRecursiveComparison().isEqualTo(expectedEntity);
    }

    @ParameterizedTest
    @MethodSource("testCaseArguments")
    void willPatchOnlyGivenProperty(final TestCase testCase) {

        // given
        final var givenEntity = newInitialEntity();
        final var patchResource = newPatchResource();
        testCase.resourceSetter.accept(patchResource, JsonNullable.of(testCase.givenPatchedValue));

        // when
        createPatcher(givenEntity).apply(patchResource);

        // then
        final var expectedEntity = newInitialEntity();
        testCase.entitySetter.accept(expectedEntity, testCase.expectedPatchValue);
        assertThat(givenEntity).usingRecursiveComparison().isEqualTo(expectedEntity);
    }

    @ParameterizedTest
    @MethodSource("testCaseArguments")
    void willThrowIfUUidCannotBeResolved(final TestCase testCase) {
        assumeThat(testCase.resolvesUuid).isTrue();

        // given
        final var givenEntity = newInitialEntity();
        final var patchResource = newPatchResource();
        final var givenPatchValue = UUID.fromString("11111111-1111-1111-1111-111111111111");
        testCase.resourceSetter.accept(patchResource, JsonNullable.of(givenPatchValue));

        // when
        final var exception = catchThrowableOfType(() -> {
            createPatcher(givenEntity).apply(patchResource);
        }, NoSuchElementException.class);

        // then
        assertThat(exception).isInstanceOf(NoSuchElementException.class)
                .hasMessage("cannot find '" + testCase.name + "' uuid " + givenPatchValue);
    }

    @ParameterizedTest
    @MethodSource("testCaseArguments")
    void willPatchOnlyGivenPropertyToNull(final TestCase testCase) {
        assumeThat(testCase.nullable).isTrue();

        // given
        final var givenEntity = newInitialEntity();
        final var patchResource = newPatchResource();
        testCase.resourceSetter.accept(patchResource, JsonNullable.of(null));

        // when
        createPatcher(givenEntity).apply(patchResource);

        // then
        final var expectedEntity = newInitialEntity();
        testCase.entitySetter.accept(expectedEntity, null);
        assertThat(givenEntity).usingRecursiveComparison().isEqualTo(expectedEntity);
    }

    @ParameterizedTest
    @MethodSource("testCaseArguments")
    void willThrowExceptionIfResourcePropertyIsNull(final TestCase testCase) {
        assumeThat(testCase.nullable).isFalse();

        // given
        final var givenEntity = newInitialEntity();
        final var patchResource = newPatchResource();
        testCase.resourceSetter.accept(patchResource, JsonNullable.of(null));

        // when
        final var actualException = catchThrowableOfType(
                () -> createPatcher(givenEntity).apply(patchResource),
                IllegalArgumentException.class);

        // then
        assertThat(actualException).hasMessage("property '" + testCase.name + "' must not be null");
        assertThat(givenEntity).usingRecursiveComparison().isEqualTo(newInitialEntity());
    }

    @ParameterizedTest
    @MethodSource("testCaseArguments")
    void willNotPatchIfGivenPropertyNotGiven(final TestCase testCase) {

        // given
        final var givenEntity = newInitialEntity();
        final var patchResource = newPatchResource();
        testCase.resourceSetter.accept(patchResource, null);

        // when
        createPatcher(givenEntity).apply(patchResource);

        // then
        final var expectedEntity = newInitialEntity();
        assertThat(givenEntity).usingRecursiveComparison().isEqualTo(expectedEntity);
    }

    abstract E newInitialEntity();

    abstract R newPatchResource();

    abstract EntityPatch<R> createPatcher(final E entity);

    abstract Stream<TestCase> testCases();

    Stream<Arguments> testCaseArguments() {
        return testCases().map(tc -> Arguments.of(Named.of(tc.name, tc)));
    }

    class TestCase {

        private final String name;
        public final Object givenPatchedValue;
        private final BiConsumer<Object, JsonNullable<?>> resourceSetter;
        private final BiConsumer<Object, Object> entitySetter;
        private final Object expectedPatchValue;

        private boolean nullable = true;
        private boolean resolvesUuid = false;

        <R, V, E> TestCase(
                final String name,
                final BiConsumer<R, JsonNullable<V>> resourceSetter,
                final V givenPatchValue,
                final BiConsumer<E, V> entitySetter
        ) {
            this.name = name;
            this.resourceSetter = (BiConsumer<Object, JsonNullable<?>>) (BiConsumer) resourceSetter;
            this.givenPatchedValue = givenPatchValue;
            this.entitySetter = (BiConsumer<Object, Object>) entitySetter;
            this.expectedPatchValue = givenPatchValue;
        }

        <R, V, E, S> TestCase(
                final String name,
                final BiConsumer<R, JsonNullable<V>> resourceSetter,
                final V givenPatchValue,
                final BiConsumer<E, S> entitySetter,
                final S expectedPatchValue
        ) {
            this.name = name;
            this.resourceSetter = (BiConsumer<Object, JsonNullable<?>>) (BiConsumer) resourceSetter;
            this.givenPatchedValue = givenPatchValue;
            this.entitySetter = (BiConsumer<Object, Object>) entitySetter;
            this.expectedPatchValue = expectedPatchValue;
        }

        TestCase notNullable() {
            nullable = false;
            return this;
        }

        TestCase resolvesUuid() {
            resolvesUuid = true;
            return this;
        }
    }
}
