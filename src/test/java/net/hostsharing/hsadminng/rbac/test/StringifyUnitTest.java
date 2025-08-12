package net.hostsharing.hsadminng.rbac.test;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.hostsharing.hsadminng.repr.Stringify;
import net.hostsharing.hsadminng.repr.Stringifyable;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static net.hostsharing.hsadminng.repr.Symbol.symbol;
import static net.hostsharing.hsadminng.repr.Stringify.stringify;
import static org.assertj.core.api.Assertions.assertThat;

class StringifyUnitTest {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldNameConstants
    public static class TestBean implements Stringifyable {

        private static Stringify<TestBean> toString = stringify(TestBean.class, "bean")
                .withProp(TestBean.Fields.caption, TestBean::getCaption)
                .withProp(TestBean.Fields.contentA, TestBean::getContentA)
                .withProp(TestBean.Fields.contentB, TestBean::getContentB)
                .withProp(TestBean.Fields.value, TestBean::getValue)
                .withProp(TestBean.Fields.active, TestBean::isActive);

        private UUID uuid;

        private String caption;

        private SubBeanWithUnquotedValues contentA;

        private SubBeanWithQuotedValues contentB;

        private int value;
        private boolean active;

        @Override
        public String toString() {
            return toString.apply(this);
        }

        @Override
        public String toShortString() {
            return caption;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubBeanWithUnquotedValues implements Stringifyable {

        private static Stringify<SubBeanWithUnquotedValues> toString = stringify(SubBeanWithUnquotedValues.class)
                .withProp(SubBeanWithUnquotedValues::getKey)
                .withProp(SubBeanWithUnquotedValues::getValue)
                .withProp(SubBeanWithUnquotedValues::isActive, v -> v ? symbol("active") : symbol("inactive"))
                .quotedValues(false);

        private String key;
        private String value;
        private boolean active;

        @Override
        public String toString() {
            return toString.apply(this);
        }

        @Override
        public String toShortString() {
            return key + ":" + value;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubBeanWithQuotedValues implements Stringifyable {

        private static Stringify<SubBeanWithQuotedValues> toString = stringify(SubBeanWithQuotedValues.class)
                .withProp(SubBeanWithQuotedValues::getKey)
                .withProp(SubBeanWithQuotedValues::getValue)
                .withProp(SubBeanWithQuotedValues::isActive, v -> v ? "active" : "inactive")
                .withSeparator(": ")
                .quotedValues(true);

        private String key;
        private Integer value;
        private boolean active;

        @Override
        public String toString() {
            return toString.apply(this);
        }

        @Override
        public String toShortString() {
            return key + ":" + value;
        }
    }

    @Test
    void stringifyWhenAllPropsHaveValues() {
        final var given = new TestBean(UUID.randomUUID(), "some caption",
                new SubBeanWithUnquotedValues("some key", "some value", true),
                new SubBeanWithQuotedValues("some key", 1234, false),
                42,
                false);
        final var result = given.toString();
        assertThat(result).isEqualTo(
                "bean(caption='some caption', contentA='some key:some value', contentB='some key:1234', value=42, active=false)");
    }

    @Test
    void stringifyWhenAllNullablePropsHaveNulValues() {
        final var given = new TestBean();
        final var result = given.toString();
        assertThat(result).isEqualTo("bean(value=0, active=false)");
    }

    @Test
    void stringifyWithoutExplicitNameUsesSimpleClassName() {
        final var given = new SubBeanWithUnquotedValues("some key", "some value", false);
        final var result = given.toString();
        assertThat(result).isEqualTo("SubBeanWithUnquotedValues(some key, some value, inactive)");
    }

    @Test
    void stringifyWithQuotedValueTrueQuotesEvenIntegers() {
        final var given = new SubBeanWithQuotedValues("some key", 1234, true);
        final var result = given.toString();
        assertThat(result).isEqualTo("SubBeanWithQuotedValues('some key': '1234': 'active')");
    }
}
