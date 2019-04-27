package org.hostsharing.hsadminng.service.accessfilter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.ImmutablePair;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hostsharing.hsadminng.service.accessfilter.JSonAccessFilterTestFixture.*;
import static org.hostsharing.hsadminng.service.accessfilter.JSonBuilder.asJSon;
import static org.hostsharing.hsadminng.service.accessfilter.MockSecurityContext.givenAuthenticatedUser;
import static org.hostsharing.hsadminng.service.accessfilter.MockSecurityContext.givenUserHavingRole;
import static org.mockito.BDDMockito.given;

@SuppressWarnings("ALL")
public class JSonDeserializationWithAccessFilterUnitTest {

    public static final String SOME_BIG_DECIMAL_AS_STRING = "5432191234888.1";
    public static final BigDecimal SOME_BIG_DECIMAL = new BigDecimal(SOME_BIG_DECIMAL_AS_STRING).setScale(2, BigDecimal.ROUND_HALF_UP);
    public static final BigDecimal SOME_BIG_DECIMAL_WITH_ANOTHER_SCALE = new BigDecimal(SOME_BIG_DECIMAL_AS_STRING).setScale(5, BigDecimal.ROUND_HALF_UP);
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

    @Mock
    private GivenCustomerService givenCustomerService;

    @Before
    public void init() {
        givenAuthenticatedUser();
        givenUserHavingRole(GivenDto.class, 1234L, Role.ACTUAL_CUSTOMER_USER);

        given(ctx.getAutowireCapableBeanFactory()).willReturn(autowireCapableBeanFactory);
        given(autowireCapableBeanFactory.createBean(GivenService.class)).willReturn(givenService);
        given(givenService.findOne(1234L)).willReturn(Optional.of(new GivenDto()
            .with(dto -> dto.id = 1234L)
            .with(dto -> dto.customerId = 888L)
            .with(dto -> dto.openIntegerField = 11111)
            .with(dto -> dto.openPrimitiveIntField = 2222)
            .with(dto -> dto.openLongField = 33333333333333L)
            .with(dto -> dto.openPrimitiveLongField = 44444444L)
            .with(dto -> dto.openBooleanField = true)
            .with(dto -> dto.openPrimitiveBooleanField = false)
            .with(dto -> dto.openBigDecimalField = SOME_BIG_DECIMAL)
            .with(dto -> dto.openStringField = "3333")
            .with(dto -> dto.restrictedField = "initial value of restricted field")
            .with(dto -> dto.restrictedBigDecimalField = SOME_BIG_DECIMAL)
        ));
        given(autowireCapableBeanFactory.createBean(GivenCustomerService.class)).willReturn(givenCustomerService);
        given(givenCustomerService.findOne(888L)).willReturn(Optional.of(new GivenCustomerDto()
            .with(dto -> dto.id = 888L)
        ));

        given(jsonParser.getCodec()).willReturn(codec);
    }

    @Test
    public void shouldDeserializeNullField() throws IOException {
        // given
        givenJSonTree(asJSon(
            ImmutablePair.of("id", 1234L),
            ImmutablePair.of("customerId", 888L),
            ImmutablePair.of("openStringField", null)));

        // when
        GivenDto actualDto = new JSonDeserializationWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize();

        // then
        assertThat(actualDto.openStringField).isNull();
    }

    @Test
    public void shouldDeserializeStringField() throws IOException {
        // given
        givenJSonTree(asJSon(
            ImmutablePair.of("id", 1234L),
            ImmutablePair.of("customerId", 888L),
            ImmutablePair.of("openStringField", "String Value")));

        // when
        GivenDto actualDto = new JSonDeserializationWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize();

        // then
        assertThat(actualDto.openStringField).isEqualTo("String Value");
    }

    @Test
    public void shouldDeserializeIntegerField() throws IOException {
        // given
        givenJSonTree(asJSon(
            ImmutablePair.of("id", 1234L),
            ImmutablePair.of("customerId", 888L),
            ImmutablePair.of("openIntegerField", 1234)));

        // when
        GivenDto actualDto = new JSonDeserializationWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize();

        // then
        assertThat(actualDto.openIntegerField).isEqualTo(1234);
    }


    @Test
    public void shouldDeserializeRestrictedBigDecimalFieldIfUnchangedByCompareTo() throws IOException {
        // given
        assertThat(SOME_BIG_DECIMAL_WITH_ANOTHER_SCALE).as("precondition failed").isNotEqualTo(SOME_BIG_DECIMAL);
        givenJSonTree(asJSon(
            ImmutablePair.of("id", 1234L),
            ImmutablePair.of("customerId", 888L),
            ImmutablePair.of("restrictedBigDecimalField", SOME_BIG_DECIMAL_WITH_ANOTHER_SCALE)));

        // when
        GivenDto actualDto = new JSonDeserializationWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize();

        // then
        assertThat(actualDto.restrictedBigDecimalField).isEqualByComparingTo(SOME_BIG_DECIMAL);
        assertThat(actualDto.restrictedBigDecimalField).isEqualByComparingTo(SOME_BIG_DECIMAL_WITH_ANOTHER_SCALE);
    }

    @Test
    // TODO: split in separate tests for each type, you see all errors at once (if any) and it's easier to debug when there are problems
    public void shouldDeserializeAcessibleFieldOfAnyType() throws IOException {
        // given
        givenJSonTree(asJSon(
            ImmutablePair.of("id", 1234L),
            ImmutablePair.of("customerId", 888L),
            ImmutablePair.of("openIntegerField", 11),
            ImmutablePair.of("openPrimitiveIntField", 22),
            ImmutablePair.of("openLongField", 333333333333333333L),
            ImmutablePair.of("openPrimitiveLongField", 44444L),
            ImmutablePair.of("openBooleanField", true),
            ImmutablePair.of("openPrimitiveBooleanField", false),
            // TODO: ImmutablePair.of("openBigDecimalField", new BigDecimal("99999999999999999999.1")),
            //  check why DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS is not working!
            ImmutablePair.of("openBigDecimalField", new BigDecimal("99999999999999.1")),
            ImmutablePair.of("openLocalDateField", LocalDate.parse("2019-04-25")),
            ImmutablePair.of("openLocalDateField2", Arrays.asList(2019, 4, 24)),
            ImmutablePair.of("openEnumField", TestEnum.GREEN)
            )
        );

        // when
        GivenDto actualDto = new JSonDeserializationWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize();

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
        givenJSonTree(asJSon(
            ImmutablePair.of("id", 1234L),
            ImmutablePair.of("customerId", 888L),
            ImmutablePair.of("openArrayField", Arrays.asList(11, 22, 33))
            )
        );

        // when
        Throwable exception = catchThrowable(() -> new JSonDeserializationWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize());

        // then
        assertThat(exception).isInstanceOf(NotImplementedException.class);
    }

    @Test
    public void shouldDeserializeStringFieldIfRequiredRoleIsCoveredByUser() throws IOException {
        // given
        givenAuthenticatedUser();
        givenUserHavingRole(GivenCustomerDto.class, 888L, Role.FINANCIAL_CONTACT);
        givenJSonTree(asJSon(
            ImmutablePair.of("id", 1234L),
            ImmutablePair.of("customerId", 888L),
            ImmutablePair.of("restrictedField", "update value of restricted field")));

        // when
        GivenDto actualDto = new JSonDeserializationWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize();

        // then
        assertThat(actualDto.restrictedField).isEqualTo("update value of restricted field");
    }

    @Test
    public void shouldDeserializeUnchangedStringFieldIfRequiredRoleIsNotCoveredByUser() throws IOException {
        // given
        givenAuthenticatedUser();
        givenUserHavingRole(GivenCustomerDto.class, 888L, Role.ACTUAL_CUSTOMER_USER);
        givenJSonTree(asJSon(
            ImmutablePair.of("id", 1234L),
            ImmutablePair.of("customerId", 888L),
            ImmutablePair.of("restrictedField", "initial value of restricted field")));

        // when
        GivenDto actualDto = new JSonDeserializationWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize();

        // then
        assertThat(actualDto.restrictedField).isEqualTo("initial value of restricted field");
    }

    @Test
    public void shouldNotDeserializeUpatedStringFieldIfRequiredRoleIsNotCoveredByUser() throws IOException {
        // given
        givenAuthenticatedUser();
        givenUserHavingRole(GivenCustomerDto.class, 888L, Role.ACTUAL_CUSTOMER_USER);
        givenJSonTree(asJSon(
            ImmutablePair.of("customerId", 888L),
            ImmutablePair.of("restrictedField", "updated value of restricted field"))
        );

        // when
        Throwable exception = catchThrowable(() -> new JSonDeserializationWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize());

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
        givenUserHavingRole(GivenCustomerDto.class, 888L, Role.ACTUAL_CUSTOMER_USER);
        givenJSonTree(asJSon(
            ImmutablePair.of("customerId", 888L),
            ImmutablePair.of("restrictedField", "another value of restricted field"))
        );

        // when
        Throwable exception = catchThrowable(() -> new JSonDeserializationWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize());

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
        givenJSonTree(asJSon(
            ImmutablePair.of("parentId", 1234L))
        );

        // when
        Throwable exception = catchThrowable(() -> new JSonDeserializationWithAccessFilter<>(ctx, jsonParser, null, GivenChildDto.class).deserialize());

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
        givenJSonTree(asJSon(
            ImmutablePair.of("parentId", 1234L))
        );

        // when
        final GivenChildDto actualDto = new JSonDeserializationWithAccessFilter<>(ctx, jsonParser, null, GivenChildDto.class).deserialize();

        // then
        assertThat(actualDto.parentId).isEqualTo(1234L);
    }

    @Test
    public void shouldNotUpdateFieldIfRequiredRoleIsNotCoveredByUser() throws IOException {
        // given
        givenAuthenticatedUser();
        givenUserHavingRole(GivenCustomerDto.class, 888L, Role.ACTUAL_CUSTOMER_USER);
        givenJSonTree(asJSon(
            ImmutablePair.of("id", 1234L),
            ImmutablePair.of("customerId", 888L),
            ImmutablePair.of("restrictedField", "Restricted String Value")));

        // when
        Throwable exception = catchThrowable(() -> new JSonDeserializationWithAccessFilter<>(ctx, jsonParser, null, GivenDto.class).deserialize());

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
        Throwable exception = catchThrowable(() -> new JSonDeserializationWithAccessFilter<>(ctx, jsonParser, null, GivenDtoWithMultipleSelfId.class).deserialize());

        // then
        assertThat(exception).isInstanceOf(AssertionError.class).hasMessage("multiple @SelfId detected in GivenDtoWithMultipleSelfId");
    }

    @Test
    public void shouldDetectUnknownFieldType() throws IOException {
        // given
        givenAuthenticatedUser();
        givenUserHavingRole(Role.ADMIN);
        givenJSonTree(asJSon(ImmutablePair.of("unknown", new Arbitrary())));

        // when
        Throwable exception = catchThrowable(() -> new JSonDeserializationWithAccessFilter<>(ctx, jsonParser, null, GivenDtoWithUnknownFieldType.class).deserialize());

        // then
        assertThat(exception).isInstanceOf(NotImplementedException.class)
            .hasMessageStartingWith("property type not yet implemented: ")
            .hasMessageContaining("Arbitrary")
            .hasMessageContaining("GivenDtoWithUnknownFieldType.unknown");
    }

    // --- only fixture code below ---

    private void givenJSonTree(String givenJSon) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        given(codec.readTree(jsonParser)).willReturn(new ObjectMapper().readTree(givenJSon));
    }

}
