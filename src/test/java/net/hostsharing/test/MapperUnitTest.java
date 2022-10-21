package net.hostsharing.test;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.mapper.Mapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class MapperUnitTest {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceBean {
        private String a;
        private String b;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetBean {
        private String a;
        private String b;
        private String c;
    }

    @Test
    void mapsNullBeanToNull() {
        final SourceBean givenSource = null;
        final var result = Mapper.map(givenSource, TargetBean.class, (s, t) -> { fail("should not have been called"); });
        assertThat(result).isNull();
    }

    @Test
    void mapsBean() {
        final SourceBean givenSource = new SourceBean("1234", "Text");
        final var result = Mapper.map(givenSource, TargetBean.class, null);
        assertThat(result).usingRecursiveComparison().isEqualTo(
                new TargetBean("1234", "Text", null)
        );
    }

    @Test
    void mapsBeanWithPostmapper() {
        final SourceBean givenSource = new SourceBean("1234", "Text");
        final var result = Mapper.map(givenSource, TargetBean.class, (s, t) -> { t.setC("Extra"); });
        assertThat(result).usingRecursiveComparison().isEqualTo(
                new TargetBean("1234", "Text", "Extra")
        );
    }
}
