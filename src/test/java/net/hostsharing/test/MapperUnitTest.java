package net.hostsharing.test;

import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.mapper.Mapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ManyToOne;
import jakarta.validation.ValidationException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MapperUnitTest {

    @Mock
    EntityManager em;

    @InjectMocks
    Mapper mapper;

    final UUID GIVEN_UUID = UUID.randomUUID();

    @Test
    void mapsNullBeanToNull() {
        final SourceBean givenSource = null;
        final var result = mapper.map(givenSource, TargetBean.class, (s, t) -> {fail("should not have been called");});
        assertThat(result).isNull();
    }

    @Test
    void mapsBean() {
        final SourceBean givenSource = SourceBean.builder().a("1234").b("Text").build();
        final var result = mapper.map(givenSource, TargetBean.class);
        assertThat(result).usingRecursiveComparison().isEqualTo(
                TargetBean.builder().a("1234").b("Text").build()
        );
    }

    @Test
    void mapsBeanWithExistingSubEntity() {
        final SourceBean givenSource = SourceBean.builder().a("1234").b("Text").s1(new SubSourceBean1(GIVEN_UUID)).build();
        when(em.find(SubTargetBean1.class, GIVEN_UUID)).thenReturn(new SubTargetBean1(GIVEN_UUID, "xxx"));

        final var result = mapper.map(givenSource, TargetBean.class);
        assertThat(result).usingRecursiveComparison().isEqualTo(
                TargetBean.builder().a("1234").b("Text").s1(new SubTargetBean1(GIVEN_UUID, "xxx")).build()
        );
    }

    @Test
    void mapsBeanWithSubEntityWithNullUuid() {
        final SourceBean givenSource = SourceBean.builder().a("1234").b("Text").s1(new SubSourceBean1(null)).build();

        final var result = mapper.map(givenSource, TargetBean.class);
        assertThat(result).usingRecursiveComparison().isEqualTo(
                TargetBean.builder().a("1234").b("Text").s1(new SubTargetBean1(null, null)).build()
        );
    }

    @Test
    void mapsBeanWithSubEntityWithoutUuidField() {
        final SourceBean givenSource = SourceBean.builder().a("1234").b("Text").s3(new SubSourceBean3("xxx")).build();

        final var result = mapper.map(givenSource, TargetBean.class);
        assertThat(result).usingRecursiveComparison().isEqualTo(
                TargetBean.builder().a("1234").b("Text").s3(new SubTargetBean3("xxx")).build()
        );
    }

    @Test
    void mapsBeanWithSubEntityNotFound() {
        final SourceBean givenSource = SourceBean.builder().a("1234").b("Text").s1(new SubSourceBean1(GIVEN_UUID)).build();
        when(em.find(SubTargetBean1.class, GIVEN_UUID)).thenReturn(null);

        final var exception = catchThrowable(() ->
                mapper.map(givenSource, TargetBean.class)
        );

        assertThat(exception).isInstanceOf(ValidationException.class)
                .hasMessage("Unable to find SubTargetBean1 with uuid " + GIVEN_UUID);
    }

    @Test
    void mapsBeanWithSubEntityNotFoundAndDisplayName() {
        final SourceBean givenSource = SourceBean.builder().a("1234").b("Text").s2(new SubSourceBean2(GIVEN_UUID)).build();
        when(em.find(SubTargetBean2.class, GIVEN_UUID)).thenReturn(null);

        final var exception = catchThrowable(() ->
                mapper.map(givenSource, TargetBean.class)
        );

        assertThat(exception).isInstanceOf(ValidationException.class)
                .hasMessage("Unable to find SomeDisplayName with uuid " + GIVEN_UUID);
    }

    @Test
    void mapsBeanWithPostmapper() {
        final SourceBean givenSource = SourceBean.builder().a("1234").b("Text").build();
        final var result = mapper.map(givenSource, TargetBean.class, (s, t) -> {t.setC("Extra");});
        assertThat(result).usingRecursiveComparison().isEqualTo(
                TargetBean.builder().a("1234").b("Text").c("Extra").build()
        );
    }

    @Test
    void mapsList() {
        final var givenSource = List.of(
                SourceBean.builder().a("111").b("Text A").build(),
                SourceBean.builder().a("222").b("Text B").build(),
                SourceBean.builder().a("333").b("Text C").build());
        final var result = mapper.mapList(givenSource, TargetBean.class);
        assertThat(result).usingRecursiveComparison().isEqualTo(
                List.of(
                        TargetBean.builder().a("111").b("Text A").build(),
                        TargetBean.builder().a("222").b("Text B").build(),
                        TargetBean.builder().a("333").b("Text C").build()));
    }

    @Test
    void mapsListWithPostMapper() {
        final var givenSource = List.of(
                SourceBean.builder().a("111").b("Text A").build(),
                SourceBean.builder().a("222").b("Text B").build(),
                SourceBean.builder().a("333").b("Text C").build());
        final var result = mapper.mapList(givenSource, TargetBean.class, (s, t) -> {t.setC("Extra");});
        assertThat(result).usingRecursiveComparison().isEqualTo(
                List.of(
                        TargetBean.builder().a("111").b("Text A").c("Extra").build(),
                        TargetBean.builder().a("222").b("Text B").c("Extra").build(),
                        TargetBean.builder().a("333").b("Text C").c("Extra").build()));
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceBean {

        private String a;
        private String b;
        private SubSourceBean1 s1;
        private SubSourceBean2 s2;
        private SubSourceBean3 s3;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubSourceBean1 {

        private UUID uuid;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubSourceBean2 {

        private UUID uuid;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubSourceBean3 {

        private String x;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetBean {

        private String a;
        private String b;
        private String c;

        @ManyToOne
        private SubTargetBean1 s1;

        @ManyToOne
        private SubTargetBean2 s2;

        @ManyToOne
        private SubTargetBean3 s3;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubTargetBean1 {

        private UUID uuid;
        private String x;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @DisplayName("SomeDisplayName")
    public static class SubTargetBean2 {

        private UUID uuid;
        private String x;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubTargetBean3 {

        private String x;
    }

}
