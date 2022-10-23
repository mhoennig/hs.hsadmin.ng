package net.hostsharing.test;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hostsharing.hsadminng.mapper.Mapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class MapperUnitTest {

    private Mapper mapper = new Mapper();

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
        final var result = mapper.map(givenSource, TargetBean.class, (s, t) -> {fail("should not have been called");});
        assertThat(result).isNull();
    }

    @Test
    void mapsBean() {
        final SourceBean givenSource = new SourceBean("1234", "Text");
        final var result = mapper.map(givenSource, TargetBean.class);
        assertThat(result).usingRecursiveComparison().isEqualTo(
                new TargetBean("1234", "Text", null)
        );
    }

    @Test
    void mapsBeanWithPostmapper() {
        final SourceBean givenSource = new SourceBean("1234", "Text");
        final var result = mapper.map(givenSource, TargetBean.class, (s, t) -> {t.setC("Extra");});
        assertThat(result).usingRecursiveComparison().isEqualTo(
                new TargetBean("1234", "Text", "Extra")
        );
    }

    @Test
    void mapsList() {
        final var givenSource = List.of(
                new SourceBean("111", "Text A"),
                new SourceBean("222", "Text B"),
                new SourceBean("333", "Text C"));
        final var result = mapper.mapList(givenSource, TargetBean.class);
        assertThat(result).usingRecursiveComparison().isEqualTo(
                List.of(
                        new TargetBean("111", "Text A", null),
                        new TargetBean("222", "Text B", null),
                        new TargetBean("333", "Text C", null)));
    }

    @Test
    void mapsListWithPostMapper() {
        final var givenSource = List.of(
                new SourceBean("111", "Text A"),
                new SourceBean("222", "Text B"),
                new SourceBean("333", "Text C"));
        final var result = mapper.mapList(givenSource, TargetBean.class, (s, t) -> {t.setC("Extra");});
        assertThat(result).usingRecursiveComparison().isEqualTo(
                List.of(
                        new TargetBean("111", "Text A", "Extra"),
                        new TargetBean("222", "Text B", "Extra"),
                        new TargetBean("333", "Text C", "Extra")));
    }

}
