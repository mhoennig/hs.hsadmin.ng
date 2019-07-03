// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.hostsharing.hsadminng.service.accessfilter.JSonAccessFilterTestFixture.*;
import static org.hostsharing.hsadminng.service.accessfilter.JSonBuilder.asJSon;
import static org.mockito.BDDMockito.given;

import org.hostsharing.hsadminng.security.AuthoritiesConstants;
import org.hostsharing.hsadminng.service.UserRoleAssignmentService;
import org.hostsharing.hsadminng.service.accessfilter.Role.Admin;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

@SuppressWarnings("ALL")
public class JSonDeserializationWithAccessFilterUnitTest {

    public static final String SOME_BIG_DECIMAL_AS_STRING = "5432191234888.1";
    public static final BigDecimal SOME_BIG_DECIMAL = new BigDecimal(SOME_BIG_DECIMAL_AS_STRING)
            .setScale(2, BigDecimal.ROUND_HALF_UP);
    public static final BigDecimal SOME_BIG_DECIMAL_WITH_ANOTHER_SCALE = new BigDecimal(SOME_BIG_DECIMAL_AS_STRING)
            .setScale(5, BigDecimal.ROUND_HALF_UP);
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
    private UserRoleAssignmentService userRoleAssignmentService;

    @Mock
    private GivenService givenService;

    @Mock
    private GivenChildService givenChildService;

    @Mock
    private GivenParentService givenParentService;

    @Mock
    private GivenCustomerService givenCustomerService;
    private SecurityContextMock securityContext;

    @Before
    public void init() {
        securityContext = SecurityContextMock.usingMock(userRoleAssignmentService)
                .havingAuthenticatedUser()
                .withRole(GivenDto.class, 1234L, Role.AnyCustomerUser.ROLE);

        given(ctx.getAutowireCapableBeanFactory()).willReturn(autowireCapableBeanFactory);
        given(autowireCapableBeanFactory.createBean(GivenService.class)).willReturn(givenService);
        given(givenService.findOne(1234L)).willReturn(
                Optional.of(
                        new GivenDto()
                                .with(dto -> {
                                    dto.id = 1234L;
                                    dto.customerId = 888L;
                                    dto.openIntegerField = 11111;
                                    dto.openPrimitiveIntField = 2222;
                                    dto.openLongField = 33333333333333L;
                                    dto.openPrimitiveLongField = 44444444L;
                                    dto.openBooleanField = true;
                                    dto.openPrimitiveBooleanField = false;
                                    dto.openBigDecimalField = SOME_BIG_DECIMAL;
                                    dto.openStringField = "3333";
                                    dto.restrictedField = "initial value of restricted field";
                                    dto.restrictedBigDecimalField = SOME_BIG_DECIMAL;
                                })));
        given(autowireCapableBeanFactory.createBean(GivenCustomerService.class)).willReturn(givenCustomerService);
        given(givenCustomerService.findOne(888L)).willReturn(
                Optional.of(
                        new GivenCustomerDto()
                                .with(dto -> dto.id = 888L)));

        given(autowireCapableBeanFactory.createBean(GivenChildService.class)).willReturn(givenChildService);
        given(autowireCapableBeanFactory.createBean(GivenParentService.class)).willReturn(givenParentService);

        given(jsonParser.getCodec()).willReturn(codec);
    }

    @Test
    public void shouldDeserializeNullField() throws IOException {
        // given
        givenJSonTree(
                asJSon(
                        ImmutablePair.of("id", 1234L),
                        ImmutablePair.of("customerId", 888L),
                        ImmutablePair.of("openStringField", null)));

        // when
        final GivenDto actualDto = deserializerFor(GivenDto.class).deserialize(jsonParser, null);

        // then
        assertThat(actualDto.openStringField).isNull();
    }

    @Test
    public void shouldDeserializeStringField() throws IOException {
        // given
        givenJSonTree(
                asJSon(
                        ImmutablePair.of("id", 1234L),
                        ImmutablePair.of("customerId", 888L),
                        ImmutablePair.of("openStringField", "String Value")));

        // when
        final GivenDto actualDto = deserializerFor(GivenDto.class).deserialize(jsonParser, null);

        // then
        assertThat(actualDto.openStringField).isEqualTo("String Value");
    }

    @Test
    public void shouldDeserializeIntegerField() throws IOException {
        // given
        givenJSonTree(
                asJSon(
                        ImmutablePair.of("id", 1234L),
                        ImmutablePair.of("customerId", 888L),
                        ImmutablePair.of("openIntegerField", 1234)));

        // when
        // @formatter:off
        final GivenDto actualDto = deserializerFor(GivenDto.class).deserialize(jsonParser, null);;
        // @formatter:on

        // then
        assertThat(actualDto.openIntegerField).isEqualTo(1234);
    }

    @Test
    public void shouldDeserializeRestrictedBigDecimalFieldIfUnchangedByCompareTo() throws IOException {
        // given
        assumeThat(SOME_BIG_DECIMAL_WITH_ANOTHER_SCALE).isNotEqualTo(SOME_BIG_DECIMAL);
        givenJSonTree(
                asJSon(
                        ImmutablePair.of("id", 1234L),
                        ImmutablePair.of("customerId", 888L),
                        ImmutablePair.of("restrictedBigDecimalField", SOME_BIG_DECIMAL_WITH_ANOTHER_SCALE)));

        // when
        final GivenDto actualDto = deserializerFor(GivenDto.class).deserialize(jsonParser, null);
        ;

        // then
        assertThat(actualDto.restrictedBigDecimalField).isEqualByComparingTo(SOME_BIG_DECIMAL);
        assertThat(actualDto.restrictedBigDecimalField).isEqualByComparingTo(SOME_BIG_DECIMAL_WITH_ANOTHER_SCALE);
    }

    @Test
    // TODO: split in separate tests for each type, you see all errors at once (if any) and it's easier to debug when there are
    // problems
    public void shouldDeserializeAcessibleFieldOfAnyType() throws IOException {
        // given
        givenJSonTree(
                asJSon(
                        ImmutablePair.of("id", 1234L),
                        ImmutablePair.of("customerId", 888L),
                        ImmutablePair.of("openIntegerField", 11),
                        ImmutablePair.of("openPrimitiveIntField", 22),
                        ImmutablePair.of("openLongField", 333333333333333333L),
                        ImmutablePair.of("openPrimitiveLongField", 44444L),
                        ImmutablePair.of("openBooleanField", true),
                        ImmutablePair.of("openPrimitiveBooleanField", false),
                        // TODO: ImmutablePair.of("openBigDecimalField", new BigDecimal("99999999999999999999.1")),
                        // check why DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS is not working!
                        ImmutablePair.of("openBigDecimalField", new BigDecimal("99999999999999.1")),
                        ImmutablePair.of("openLocalDateField", LocalDate.parse("2019-04-25")),
                        ImmutablePair.of("openLocalDateField2", Arrays.asList(2019, 4, 24)),
                        ImmutablePair.of("openEnumField", TestEnum.GREEN)));

        // when
        final GivenDto actualDto = deserializerFor(GivenDto.class).deserialize(jsonParser, null);
        ;

        // then
        assertThat(actualDto.openIntegerField).isEqualTo(11);
        assertThat(actualDto.openPrimitiveIntField).isEqualTo(22);
        assertThat(actualDto.openLongField).isEqualTo(333333333333333333L);
        assertThat(actualDto.openPrimitiveLongField).isEqualTo(44444L);
        assertThat(actualDto.openBooleanField).isEqualTo(true);
        assertThat(actualDto.openPrimitiveBooleanField).isEqualTo(false);
        assertThat(actualDto.openBigDecimalField).isEqualTo(new BigDecimal("99999999999999.1"));
        assertThat(actualDto.openLocalDateField).isEqualTo(LocalDate.parse("2019-04-25"));
        assertThat(actualDto.openLocalDateField2).isEqualTo(LocalDate.parse("2019-04-24"));
        assertThat(actualDto.openEnumField).isEqualTo(TestEnum.GREEN);
    }

    @Test
    public void shouldNotDeserializeFieldWithUnknownJSonNodeType() throws IOException {
        // given
        givenJSonTree(
                asJSon(
                        ImmutablePair.of("id", 1234L),
                        ImmutablePair.of("customerId", 888L),
                        ImmutablePair.of("openArrayField", Arrays.asList(11, 22, 33))));

        // when
        Throwable exception = catchThrowable(() -> deserializerFor(GivenDto.class).deserialize(jsonParser, null));

        // then
        assertThat(exception).isInstanceOf(NotImplementedException.class);
    }

    @Test
    public void shouldDeserializeStringFieldIfRequiredRoleIsCoveredByUser() throws IOException {
        // given
        securityContext.havingAuthenticatedUser()
                .withRole(GivenCustomerDto.class, 888L, Role.CustomerFinancialContact.ROLE);
        givenJSonTree(
                asJSon(
                        ImmutablePair.of("id", 1234L),
                        ImmutablePair.of("customerId", 888L),
                        ImmutablePair.of("restrictedField", "update value of restricted field")));

        // when
        final GivenDto actualDto = deserializerFor(GivenDto.class).deserialize(jsonParser, null);

        // then
        assertThat(actualDto.restrictedField).isEqualTo("update value of restricted field");
    }

    @Test
    public void shouldDeserializeUnchangedStringFieldIfRequiredRoleIsNotCoveredByUser() throws IOException {
        // given
        securityContext.havingAuthenticatedUser()
                .withRole(GivenCustomerDto.class, 888L, Role.CustomerFinancialContact.ROLE);
        givenJSonTree(
                asJSon(
                        ImmutablePair.of("id", 1234L),
                        ImmutablePair.of("customerId", 888L),
                        ImmutablePair.of("restrictedField", "initial value of restricted field")));

        // when
        final GivenDto actualDto = deserializerFor(GivenDto.class).deserialize(jsonParser, null);

        // then
        assertThat(actualDto.restrictedField).isEqualTo("initial value of restricted field");
    }

    @Test
    public void shouldNotDeserializeUpatedStringFieldIfRequiredRoleIsNotCoveredByUser() throws IOException {
        // given
        securityContext.havingAuthenticatedUser()
                .withRole(GivenCustomerDto.class, 888L, Role.ActualCustomerUser.ROLE);
        givenJSonTree(
                asJSon(
                        ImmutablePair.of("customerId", 888L),
                        ImmutablePair.of("restrictedField", "updated value of restricted field")));

        // when
        final Throwable exception = catchThrowable(() -> deserializerFor(GivenDto.class).deserialize(jsonParser, null));

        // then
        assertThat(exception).isInstanceOfSatisfying(BadRequestAlertException.class, badRequestAlertException -> {
            assertThat(badRequestAlertException.getParam()).isEqualTo("GivenDto.restrictedField");
            assertThat(badRequestAlertException.getErrorKey()).isEqualTo("initializationProhibited");
        });
    }

    @Test
    public void shouldNotInitializeFieldIfRequiredRoleIsNotCoveredByUser() throws IOException {
        // given
        securityContext.havingAuthenticatedUser()
                .withRole(GivenCustomerDto.class, 888L, Role.ActualCustomerUser.ROLE);
        givenJSonTree(
                asJSon(
                        ImmutablePair.of("customerId", 888L),
                        ImmutablePair.of("restrictedField", "another value of restricted field")));

        // when
        final Throwable exception = catchThrowable(() -> deserializerFor(GivenDto.class).deserialize(jsonParser, null));

        // then
        assertThat(exception).isInstanceOfSatisfying(BadRequestAlertException.class, badRequestAlertException -> {
            assertThat(badRequestAlertException.getParam()).isEqualTo("GivenDto.restrictedField");
            assertThat(badRequestAlertException.getErrorKey()).isEqualTo("initializationProhibited");
        });
    }

    @Test
    public void shouldNotCreateIfRoleRequiredByParentEntityIsNotCoveredByUser() throws IOException {
        // given
        securityContext.havingAuthenticatedUser()
                .withRole(GivenCustomerDto.class, 9999L, Role.CustomerContractualContact.ROLE);
        givenJSonTree(
                asJSon(
                        ImmutablePair.of("parentId", 1234L)));

        // when
        Throwable exception = catchThrowable(
                () -> deserializerFor(GivenChildDto.class).deserialize(jsonParser, null));

        // then
        assertThat(exception).isInstanceOfSatisfying(BadRequestAlertException.class, badRequestAlertException -> {
            assertThat(badRequestAlertException.getParam()).isEqualTo("GivenChildDto.parentId");
            assertThat(badRequestAlertException.getErrorKey()).isEqualTo("referencingProhibited");
        });
    }

    @Test
    public void shouldCreateIfRoleRequiredByReferencedEntityIsCoveredByUser() throws IOException {
        // given
        securityContext.havingAuthenticatedUser()
                .withRole(GivenCustomerDto.class, 888L, Role.CustomerContractualContact.ROLE);
        givenJSonTree(
                asJSon(
                        ImmutablePair.of("parentId", 1234L)));

        // when
        final GivenChildDto actualDto = deserializerFor(GivenChildDto.class).deserialize(jsonParser, null);
        ;

        // then
        assertThat(actualDto.parentId).isEqualTo(1234L);
    }

    @Test
    public void shouldResolveParentIdFromIdOfSerializedSubEntity() throws IOException {
        // given
        securityContext.havingAuthenticatedUser()
                .withRole(GivenParent.class, 1234L, Role.CustomerContractualContact.ROLE);
        givenJSonTree(
                asJSon(
                        ImmutablePair.of(
                                "parent",
                                asJSon(
                                        ImmutablePair.of("id", 1234L)))));
        given(givenParentService.findOne(1234L)).willReturn(Optional.of(new GivenParent().id(1234)));

        // when
        final GivenChild actualDto = deserializerFor(GivenChild.class).deserialize(jsonParser, null);

        // then
        assertThat(actualDto.parent.id).isEqualTo(1234L);
    }

    @Test
    public void shouldNotUpdateFieldIfRequiredRoleIsNotCoveredByUser() throws IOException {
        // given
        securityContext.havingAuthenticatedUser()
                .withRole(GivenCustomerDto.class, 888L, Role.ActualCustomerUser.ROLE);
        givenJSonTree(
                asJSon(
                        ImmutablePair.of("id", 1234L),
                        ImmutablePair.of("customerId", 888L),
                        ImmutablePair.of("restrictedField", "Restricted String Value")));

        // when
        final Throwable exception = catchThrowable(
                () -> deserializerFor(GivenDto.class).deserialize(jsonParser, null));

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
        final Throwable exception = catchThrowable(
                () -> deserializerFor(GivenDtoWithMultipleSelfId.class).deserialize(jsonParser, null));

        // then
        assertThat(exception).isInstanceOf(AssertionError.class)
                .hasMessage("multiple @SelfId detected in GivenDtoWithMultipleSelfId");
    }

    @Test
    public void shouldDetectUnknownFieldType() throws IOException {
        // given
        securityContext.havingAuthenticatedUser().withAuthority(AuthoritiesConstants.ADMIN);
        givenJSonTree(asJSon(ImmutablePair.of("unknown", new Arbitrary())));

        // when
        final Throwable exception = catchThrowable(
                () -> deserializerFor(GivenDtoWithUnknownFieldType.class).deserialize(jsonParser, null));

        // then
        assertThat(exception).isInstanceOf(NotImplementedException.class)
                .hasMessageStartingWith("property type not yet implemented: ")
                .hasMessageContaining("Arbitrary")
                .hasMessageContaining("GivenDtoWithUnknownFieldType.unknown");
    }

    @Test
    public void shouldDetectUnknownPropertyName() throws IOException {
        // given
        securityContext.havingAuthenticatedUser().withAuthority(AuthoritiesConstants.ADMIN);
        givenJSonTree(asJSon(ImmutablePair.of("somePropWhichDoesNotExist", "Some Value")));

        // when
        final Throwable exception = catchThrowable(
                () -> deserializerFor(GivenDto.class).deserialize(jsonParser, null));

        // then
        assertThat(exception).isInstanceOfSatisfying(BadRequestAlertException.class, (exc) -> {
            assertThat(exc).hasMessageStartingWith("Unknown property");
            assertThat(exc.getParam()).isEqualTo("somePropWhichDoesNotExist");
            assumeThat(exc.getErrorKey()).isEqualTo("unknownProperty");
        });
    }

    @Test
    public void shouldNotDeserializeArrayValue() throws IOException {
        // given
        securityContext.havingAuthenticatedUser().withAuthority(AuthoritiesConstants.ADMIN);
        givenJSonTree(asJSon(ImmutablePair.of("openStringField", Arrays.asList(1, 2))));

        // when
        final Throwable exception = catchThrowable(
                () -> deserializerFor(GivenDto.class).deserialize(jsonParser, null));

        // then
        assertThat(exception).isInstanceOf(NotImplementedException.class);
    }

    @Test
    public void shouldNotDeserializeObjectValue() throws IOException {
        // given
        securityContext.havingAuthenticatedUser().withAuthority(AuthoritiesConstants.ADMIN);
        givenJSonTree("{ \"openStringField\": {\"a\": 1, \"b\": 2 } }");

        // when
        final Throwable exception = catchThrowable(
                () -> deserializerFor(GivenDto.class).deserialize(jsonParser, null));

        // then
        assertThat(exception).isInstanceOf(NotImplementedException.class);
    }

    @Test
    public void shouldIgnorePropertyToIgnoreForInit() throws IOException {
        // given
        securityContext.havingAuthenticatedUser().withAuthority(Admin.ROLE.authority());
        givenJSonTree(
                asJSon(
                        ImmutablePair.of("displayLabel", "Some Value")));

        // when
        deserializerFor(GivenDto.class).deserialize(jsonParser, null);
        final Throwable exception = catchThrowable(() -> deserializerFor(GivenDto.class).deserialize(jsonParser, null));

        // then
        assertThat(exception).isNull();
    }

    @Test
    public void shouldIgnorePropertyToIgnoreForUpdate() throws IOException {
        // given
        securityContext.havingAuthenticatedUser().withAuthority(AuthoritiesConstants.ADMIN);
        givenJSonTree(
                asJSon(
                        ImmutablePair.of("id", 1234L),
                        ImmutablePair.of("displayLabel", "Some Value")));

        // when
        final Throwable exception = catchThrowable(() -> deserializerFor(GivenDto.class).deserialize(jsonParser, null));

        // then
        assertThat(exception).isNull();
    }

    // --- only fixture code below ---

    private void givenJSonTree(String givenJSon) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        given(codec.readTree(jsonParser)).willReturn(new ObjectMapper().readTree(givenJSon));
    }

    // We need specialied factories for the deserializer subclasses
    // so that the generic type can be accessed via reflection.
    // And it's down here to keep the ugly formatting out of the test cases.
    // The trick with the unused ...-parameterr might look strange but the reason is
    // that I wanted the concrete class to be navigable in the tests and
    // multiple deserializer(Class<...Dto>) methods would have the same erasure
    // and adding the type the method name, is redundant for the reader.

    public JsonDeserializerWithAccessFilter<GivenDto> deserializerFor(
            final Class<GivenDto> clazz,
            final GivenDto... qualifier) {
        return new JsonDeserializerWithAccessFilter<GivenDto>(ctx, userRoleAssignmentService) {
            // no need to overload any method here
        };
    }

    public JsonDeserializerWithAccessFilter<GivenChildDto> deserializerFor(
            final Class<GivenChildDto> clazz,
            final GivenChildDto... qualifier) {
        return new JsonDeserializerWithAccessFilter<GivenChildDto>(ctx, userRoleAssignmentService) {
            // no need to overload any method here
        };
    }

    private JsonDeserializer<GivenDtoWithMultipleSelfId> deserializerFor(
            final Class<GivenDtoWithMultipleSelfId> clazz,
            final GivenDtoWithMultipleSelfId... qualifier) {
        return new JsonDeserializerWithAccessFilter<GivenDtoWithMultipleSelfId>(ctx, userRoleAssignmentService) {
            // no need to overload any method here
        };
    }

    private JsonDeserializer<GivenDtoWithUnknownFieldType> deserializerFor(
            final Class<GivenDtoWithUnknownFieldType> clazz,
            final GivenDtoWithUnknownFieldType... qualifier) {
        return new JsonDeserializerWithAccessFilter<GivenDtoWithUnknownFieldType>(ctx, userRoleAssignmentService) {
            // no need to overload any method here
        };
    }

    public JsonDeserializerWithAccessFilter<GivenChild> deserializerFor(
            final Class<GivenChild> clazz,
            final GivenChild... qualifier) {
        return new JsonDeserializerWithAccessFilter<GivenChild>(ctx, userRoleAssignmentService) {

            @Override
            protected JSonFieldReader<GivenChild> jsonFieldReader(final TreeNode treeNode, final Field field) {
                if ("parent".equals(field.getName())) {
                    return (final GivenChild target) -> {
                        final long id = getSubNode(treeNode, "id").asLong();
                        target.parent = givenParentService.findOne(id)
                                .orElseThrow(
                                        () -> new BadRequestAlertException(
                                                GivenParent.class.getSimpleName() + "#" + id + " not found",
                                                String.valueOf(id),
                                                "idNotFound"));
                    };
                }
                return super.jsonFieldReader(treeNode, field);
            }
        };
    }

}
