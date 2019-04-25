package org.hostsharing.hsadminng.service.accessfilter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hostsharing.hsadminng.service.IdToDtoResolver;
import org.hostsharing.hsadminng.service.dto.FluentBuilder;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hostsharing.hsadminng.service.accessfilter.JSonBuilder.asJSon;
import static org.hostsharing.hsadminng.service.accessfilter.MockSecurityContext.givenAuthenticatedUser;
import static org.hostsharing.hsadminng.service.accessfilter.MockSecurityContext.givenUserHavingRole;
import static org.mockito.BDDMockito.given;

@SuppressWarnings("ALL")
public class JSonDeserializerWithAccessFilterUnitTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ApplicationContext ctx;

    @Mock
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Mock
    private JsonParser jsonParser;

    @Mock
    private ObjectCodec codec;

    @Mock
    private TreeNode treeNode;

    @Mock
    private GivenService givenService;

    @Mock
    private GivenChildService givenChildService;

    @Before
    public void init() {
        givenAuthenticatedUser();
        givenUserHavingRole(GivenDto.class, 1234L, Role.ACTUAL_CUSTOMER_USER);

        given (ctx.getAutowireCapableBeanFactory()).willReturn(autowireCapableBeanFactory);
        given(autowireCapableBeanFactory.createBean(GivenService.class)).willReturn(givenService);
        given(givenService.findOne(1234L)).willReturn(Optional.of(new GivenDto()
            .with(dto -> dto.id = 1234L)
            .with(dto -> dto.openIntegerField = 1)
            .with(dto -> dto.openLongField = 2L)
            .with(dto -> dto.openStringField = "3")
            .with(dto -> dto.restrictedField = "initial value of restricted field")
        ));

        given(jsonParser.getCodec()).willReturn(codec);
    }

    @Test
    public void shouldDeserializeStringField() throws IOException {
        // given
        givenJSonTree(asJSon(
            ImmutablePair.of("id", 1234L),
            ImmutablePair.of("openStringField", "String Value")));

        // when
        GivenDto actualDto = new JSonDeserializerWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize();

        // then
        assertThat(actualDto.openStringField).isEqualTo("String Value");
    }

    @Test
    public void shouldDeserializeIntegerField() throws IOException {
        // given
        givenJSonTree(asJSon(
            ImmutablePair.of("id", 1234L),
            ImmutablePair.of("openIntegerField", 1234)));

        // when
        GivenDto actualDto = new JSonDeserializerWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize();

        // then
        assertThat(actualDto.openIntegerField).isEqualTo(1234);
    }

    @Test
    public void shouldDeserializeLongField() throws IOException {
        // given
        givenJSonTree(asJSon(
            ImmutablePair.of("id", 1234L),
            ImmutablePair.of("openLongField", 1234L)));

        // when
        GivenDto actualDto = new JSonDeserializerWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize();

        // then
        assertThat(actualDto.openLongField).isEqualTo(1234L);
    }

    @Test
    public void shouldDeserializeStringFieldIfRequiredRoleIsCoveredByUser() throws IOException {
        // given
        givenAuthenticatedUser();
        givenUserHavingRole(GivenDto.class, 1234L, Role.FINANCIAL_CONTACT);
        givenJSonTree(asJSon(
            ImmutablePair.of("id", 1234L),
            ImmutablePair.of("restrictedField", "update value of restricted field")));

        // when
        GivenDto actualDto = new JSonDeserializerWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize();

        // then
        assertThat(actualDto.restrictedField).isEqualTo("update value of restricted field");
    }

    @Test
    public void shouldDeserializeUnchangedStringFieldIfRequiredRoleIsNotCoveredByUser() throws IOException {
        // given
        givenAuthenticatedUser();
        givenUserHavingRole(GivenDto.class, 1234L, Role.ANY_CUSTOMER_USER);
        givenJSonTree(asJSon(
            ImmutablePair.of("id", 1234L),
            ImmutablePair.of("restrictedField", "initial value of restricted field")));

        // when
        GivenDto actualDto = new JSonDeserializerWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize();

        // then
        assertThat(actualDto.restrictedField).isEqualTo("initial value of restricted field");
    }

    @Test
    public void shouldNotDeserializeUpatedStringFieldIfRequiredRoleIsNotCoveredByUser() throws IOException {
        // given
        givenAuthenticatedUser();
        givenUserHavingRole(GivenDto.class, 1L, Role.ANY_CUSTOMER_USER);
        givenJSonTree(asJSon(ImmutablePair.of("restrictedField", "updated value of restricted field")));

        // when
        Throwable exception = catchThrowable(() -> new JSonDeserializerWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize());

        // then
        assertThat(exception).isInstanceOfSatisfying(BadRequestAlertException.class, badRequestAlertException -> {
            assertThat(badRequestAlertException.getParam()).isEqualTo("GivenDto.restrictedField");
            assertThat(badRequestAlertException.getErrorKey()).isEqualTo("initializationProhibited");
        });
    }

    @Test
    public void shouldInitializeFieldIfRequiredRoleIsNotCoveredByUser() throws IOException {
        // given
        givenAuthenticatedUser();
        givenUserHavingRole(GivenDto.class, 1L, Role.ANY_CUSTOMER_USER);
        givenJSonTree(asJSon(ImmutablePair.of("restrictedField", "another value of restricted field")));

        // when
        Throwable exception = catchThrowable(() -> new JSonDeserializerWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize());

        // then
        assertThat(exception).isInstanceOfSatisfying(BadRequestAlertException.class, badRequestAlertException -> {
            assertThat(badRequestAlertException.getParam()).isEqualTo("GivenDto.restrictedField");
            assertThat(badRequestAlertException.getErrorKey()).isEqualTo("initializationProhibited");
        });
    }

    @Test
    public void shouldNotCreateIfRoleRequiredByParentEntityIsNotCoveredByUser() throws IOException {
        // given
        givenAuthenticatedUser();
        givenUserHavingRole(GivenDto.class, 9999L, Role.CONTRACTUAL_CONTACT);
        givenJSonTree(asJSon(ImmutablePair.of("parentId", 1234L)));

        // when
        Throwable exception = catchThrowable(() -> new JSonDeserializerWithAccessFilter<>(ctx, jsonParser, null, GivenChildDto.class).deserialize());

        // then
        assertThat(exception).isInstanceOfSatisfying(BadRequestAlertException.class, badRequestAlertException -> {
            assertThat(badRequestAlertException.getParam()).isEqualTo("GivenChildDto.parentId");
            assertThat(badRequestAlertException.getErrorKey()).isEqualTo("referencingProhibited");
        });
    }

    @Test
    public void shouldCreateIfRoleRequiredByReferencedEntityIsCoveredByUser() throws IOException {
        // given
        givenAuthenticatedUser();
        givenUserHavingRole(GivenDto.class, 1234L, Role.CONTRACTUAL_CONTACT);
        givenJSonTree(asJSon(ImmutablePair.of("parentId", 1234L)));

        // when
        final GivenChildDto actualDto = new JSonDeserializerWithAccessFilter<>(ctx, jsonParser, null, GivenChildDto.class).deserialize();

        // then
        assertThat(actualDto.parentId).isEqualTo(1234L);
    }

    @Test
    public void shouldNotUpdateFieldIfRequiredRoleIsNotCoveredByUser() throws IOException {
        // given
        givenAuthenticatedUser();
        givenUserHavingRole(GivenDto.class, 1234L, Role.ANY_CUSTOMER_USER);
        givenJSonTree(asJSon(
            ImmutablePair.of("id", 1234L),
            ImmutablePair.of("restrictedField", "Restricted String Value")));

        // when
        Throwable exception = catchThrowable(() -> new JSonDeserializerWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize());

        // then
        assertThat(exception).isInstanceOfSatisfying(BadRequestAlertException.class, badRequestAlertException -> {
            assertThat(badRequestAlertException.getParam()).isEqualTo("GivenDto.restrictedField");
            assertThat(badRequestAlertException.getErrorKey()).isEqualTo("updateProhibited");
        });
    }

    @Test
    public void shouldDetectMultipleSelfIdFields() throws IOException {
        // given
        givenJSonTree(asJSon(ImmutablePair.of("id", 1111L)));

        // when
        Throwable exception = catchThrowable(() -> new JSonDeserializerWithAccessFilter<>(ctx, jsonParser, null, GivenDtoWithMultipleSelfId.class).deserialize());

        // then
        assertThat(exception).isInstanceOf(AssertionError.class).hasMessage("multiple @SelfId detected in GivenDtoWithMultipleSelfId");
    }

    // --- only fixture code below ---

    private void givenJSonTree(String givenJSon) throws IOException {
        given(codec.readTree(jsonParser)).willReturn(new ObjectMapper().readTree(givenJSon));
    }

    abstract class GivenService implements IdToDtoResolver<GivenDto> {
    }

    public static class GivenDto extends FluentBuilder<GivenDto> {

        @SelfId(resolver = GivenService.class)
        @AccessFor(read = Role.ANY_CUSTOMER_USER)
        Long id;

        @AccessFor(init = {Role.TECHNICAL_CONTACT, Role.FINANCIAL_CONTACT}, update = {Role.TECHNICAL_CONTACT, Role.FINANCIAL_CONTACT})
        String restrictedField;

        @AccessFor(init = Role.ANYBODY, update = Role.ANYBODY)
        String openStringField;

        @AccessFor(init = Role.ANYBODY, update = Role.ANYBODY)
        Integer openIntegerField;

        @AccessFor(init = Role.ANYBODY, update = Role.ANYBODY)
        Long openLongField;
    }

    abstract class GivenChildService implements IdToDtoResolver<GivenChildDto> {
    }

    public static class GivenChildDto extends FluentBuilder<GivenChildDto> {

        @SelfId(resolver = GivenChildService.class)
        @AccessFor(read = Role.ANY_CUSTOMER_USER)
        Long id;

        @AccessFor(init = Role.CONTRACTUAL_CONTACT, update = Role.CONTRACTUAL_CONTACT, read = Role.ACTUAL_CUSTOMER_USER)
        @ParentId(resolver = GivenService.class)
        Long parentId;

        @AccessFor(init = {Role.TECHNICAL_CONTACT, Role.FINANCIAL_CONTACT}, update = {Role.TECHNICAL_CONTACT, Role.FINANCIAL_CONTACT})
        String restrictedField;
    }

    public static class GivenDtoWithMultipleSelfId {

        @SelfId(resolver = GivenChildService.class)
        @AccessFor(read = Role.ANY_CUSTOMER_USER)
        Long id;

        @SelfId(resolver = GivenChildService.class)
        @AccessFor(read = Role.ANY_CUSTOMER_USER)
        Long id2;

    }
}
