package net.hostsharing.hsadminng.hs.validation;

import net.hostsharing.hsadminng.mapper.PatchableMapWrapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnumerationPropertyUnitTest {

    @Test
    void enumerationPropertyCreatesInstance() {
        final var prop = EnumerationProperty.enumerationProperty("myProp");

        assertThat(prop).isNotNull();
        assertThat(prop.propertyName()).isEqualTo("myProp");
        assertThat(prop.simpleTypeName()).isEqualTo("enumeration");
    }

    @Test
    void valuesReturnsSelfAndIsUsedInValidation() {
        final var prop = EnumerationProperty.enumerationProperty("myProp").values("A", "B");

        // validate accepts allowed value
        assertThat(validate(prop, "A")).isEmpty();

        // validate rejects disallowed value — kills negate-conditional and lambda true/false
        final var errors = validate(prop, "C");
        assertThat(errors).anyMatch(s -> s.contains("expected to be one of"));

        assertThat(prop.values("X", "Y")).isSameAs(prop); // kills replaced return null
        assertThat(validate(prop, "X")).isEmpty();
    }

    @Test
    void deferredInitWithoutDeferredDoesNothing() {
        final var prop = EnumerationProperty.enumerationProperty("p").values("A");

        // hasDeferredInit() is false → negated conditional survived, must not change values
        prop.deferredInit(new ValidatableProperty[0]);

        assertThat(validate(prop, "A")).isEmpty();
        assertThat(validate(prop, "B")).isNotEmpty();
    }

    @Test
    void deferredInitWithAlreadySetValuesThrows() {
        final var prop = EnumerationProperty.enumerationProperty("p").values("A");
        prop.valuesFromProperties("prefix.");

        assertThatThrownBy(() -> prop.deferredInit(new ValidatableProperty[0]))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has values");
    }

    @Test
    void valuesFromPropertiesDeferredInitFiltersPrefix() {
        final var prop = EnumerationProperty.enumerationProperty("p").valuesFromProperties("hs.");
        assertThat(prop.hasDeferredInit()).isTrue();
        assertThat(prop.valuesFromProperties("x.")).isSameAs(prop); // kills return null

        // prepare allProperties with mixed names — kills filter true/false, map, toArray, setDeferredInit
        final var p1 = EnumerationProperty.enumerationProperty("hs.alpha");
        final var p2 = EnumerationProperty.enumerationProperty("hs.beta");
        final var p3 = EnumerationProperty.enumerationProperty("other.gamma");
        final var all = new ValidatableProperty[]{p1, p2, p3};

        final var deferredProp = EnumerationProperty.enumerationProperty("target").valuesFromProperties("hs.");
        deferredProp.deferredInit(all);

        // after deferredInit, values are ["alpha","beta"] (prefix stripped)
        assertThat(validate(deferredProp, "alpha")).isEmpty();
        assertThat(validate(deferredProp, "beta")).isEmpty();
        assertThat(validate(deferredProp, "gamma")).isNotEmpty();
        assertThat(validate(deferredProp, "other.gamma")).isNotEmpty();
    }

    @Test
    void simpleTypeName() {
        assertThat(EnumerationProperty.enumerationProperty("p").simpleTypeName()).isEqualTo("enumeration");
    }

    @Test
    void validateViaProviderUsesEnumeration() {
        // integration via ValidatableProperty.validate(PropertiesProvider) — also kills base-class branches
        final var prop = EnumerationProperty.enumerationProperty("myProp").values("A", "B").required();

        final var emptyProvider = providerWith();
        assertThat(prop.validate(emptyProvider)).anyMatch(s -> s.contains("is required"));

        final var okProvider = providerWith("myProp", "A");
        assertThat(prop.validate(okProvider)).isEmpty();

        final var badProvider = providerWith("myProp", "C");
        assertThat(badProvider.directProps().get("myProp")).isEqualTo("C");
        assertThat(prop.validate(badProvider)).anyMatch(s -> s.contains("expected to be one of"));
    }

    private static List<String> validate(final EnumerationProperty prop, final String value) {
        final var result = new ArrayList<String>();
        prop.validate(result, value, mock(PropertiesProvider.class));
        return result;
    }

    private static PropertiesProvider providerWith(final Object... keyValues) {
        final var map = new HashMap<String, Object>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        final var wrapper = PatchableMapWrapper.of(map);
        final var provider = mock(PropertiesProvider.class);
        when(provider.directProps()).thenReturn(wrapper);
        when(provider.isLoaded()).thenReturn(false);
        when(provider.getContextValue(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
        return provider;
    }
}
