package net.hostsharing.hsadminng;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

import static net.hostsharing.hsadminng.Stringify.stringify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class StringifyUnitTest {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldNameConstants
    public static class TestBean implements Stringifyable {

        private static Stringify<TestBean> toString = stringify(TestBean.class, "bean")
                .withProp(TestBean.Fields.label, TestBean::getLabel)
                .withProp(TestBean.Fields.content, TestBean::getContent)
                .withProp(TestBean.Fields.active, TestBean::isActive);

        private UUID uuid;

        private String label;

        private SubBean content;

        private boolean active;

        @Override
        public String toString() {
            return toString.apply(this);
        }

        @Override
        public String toShortString() {
            return label;
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldNameConstants
    public static class SubBean implements Stringifyable {

        private static Stringify<SubBean> toString = stringify(SubBean.class)
                .withProp(SubBean.Fields.key, SubBean::getKey)
                .withProp(Fields.value, SubBean::getValue);

        private String key;
        private Integer value;

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
        final var given = new TestBean(UUID.randomUUID(), "some label",
                new SubBean("some content", 1234), false);
        final var result = given.toString();
        assertThat(result).isEqualTo("bean(label='some label', content='some content:1234', active=false)");
    }

    @Test
    void stringifyWhenAllNullablePropsHaveNulValues() {
        final var given = new TestBean();
        final var result = given.toString();
        assertThat(result).isEqualTo("bean(active=false)");
    }

    @Test
    void stringifyWithoutExplicitNameUsesSimpleClassName() {
        final var given = new SubBean("some key", 1234);
        final var result = given.toString();
        assertThat(result).isEqualTo("SubBean(key='some key', value=1234)");
    }
}
