// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.accessfilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hostsharing.hsadminng.service.accessfilter.JSonAccessFilterTestFixture.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.hostsharing.hsadminng.service.UserRoleAssignmentService;

import com.fasterxml.jackson.core.JsonGenerator;

import org.apache.commons.lang3.NotImplementedException;
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

public class JSonSerializationWithAccessFilterUnitTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ApplicationContext ctx;

    @Mock
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Mock
    private JsonGenerator jsonGenerator;

    @Mock
    private UserRoleAssignmentService userRoleAssignmentService;

    @Mock
    private GivenCustomerService givenCustomerService;

    private SecurityContextMock securityContext;

    private final GivenDto givenDTO = createSampleDto();

    @Before
    public void init() {
        securityContext = SecurityContextMock.usingMock(userRoleAssignmentService)
                .havingAuthenticatedUser()
                .withRole(GivenCustomerDto.class, 888L, Role.AnyCustomerUser.ROLE);

        given(ctx.getAutowireCapableBeanFactory()).willReturn(autowireCapableBeanFactory);
        given(autowireCapableBeanFactory.createBean(GivenCustomerService.class)).willReturn(givenCustomerService);
        given(givenCustomerService.findOne(888L)).willReturn(
                Optional.of(
                        new GivenCustomerDto()
                                .with(dto -> dto.id = 888L)));
    }

    @Test
    public void shouldSerializeStringField() throws IOException {
        // when
        serialize(givenDTO);

        // then
        verify(jsonGenerator).writeStringField("openStringField", givenDTO.openStringField);
    }

    @Test
    public void shouldSerializeIntegerField() throws IOException {
        // when
        serialize(givenDTO);

        // then
        verify(jsonGenerator).writeNumberField("openIntegerField", givenDTO.openIntegerField);
    }

    @Test
    public void shouldSerializePrimitiveIntField() throws IOException {
        // when
        serialize(givenDTO);

        // then
        verify(jsonGenerator).writeNumberField("openPrimitiveIntField", givenDTO.openPrimitiveIntField);
    }

    @Test
    public void shouldSerializeLongField() throws IOException {
        // when
        final Throwable actual = catchThrowable(() -> serialize(givenDTO));

        // then
        verify(jsonGenerator).writeNumberField("openLongField", givenDTO.openLongField);
    }

    @Test
    public void shouldSerializePrimitiveLongField() throws IOException {
        // when
        serialize(givenDTO);

        // then
        verify(jsonGenerator).writeNumberField("openPrimitiveLongField", givenDTO.openPrimitiveLongField);
    }

    @Test
    public void shouldSerializeBooleanField() throws IOException {
        // when
        serialize(givenDTO);

        // then
        verify(jsonGenerator).writeBooleanField("openBooleanField", givenDTO.openBooleanField);
    }

    @Test
    public void shouldSerializePrimitiveBooleanField() throws IOException {
        // when
        serialize(givenDTO);

        // then
        verify(jsonGenerator).writeBooleanField("openPrimitiveBooleanField", givenDTO.openPrimitiveBooleanField);
    }

    @Test
    public void shouldSerializeBigDecimalField() throws IOException {
        // when
        final Throwable actual = catchThrowable(() -> serialize(givenDTO));

        // then
        verify(jsonGenerator).writeNumberField("openBigDecimalField", givenDTO.openBigDecimalField);
    }

    @Test
    public void shouldSerializeLocalDateField() throws IOException {
        // when
        serialize(givenDTO);

        // then
        verify(jsonGenerator).writeStringField("openLocalDateField", givenDTO.openLocalDateFieldAsString);
    }

    @Test
    public void shouldSerializeEnumField() throws IOException {
        // when
        serialize(givenDTO);

        // then
        verify(jsonGenerator).writeStringField("openEnumField", givenDTO.openEnumFieldAsString);
    }

    @Test
    public void shouldSerializeRestrictedFieldIfRequiredRoleIsCoveredByUser() throws IOException {

        // given
        securityContext.havingAuthenticatedUser()
                .withRole(GivenCustomerDto.class, 888L, Role.of(Role.CustomerFinancialContact.class));

        // when
        serialize(givenDTO);

        // then
        verify(jsonGenerator).writeStringField("restrictedField", givenDTO.restrictedField);
    }

    @Test
    public void shouldNotSerializeRestrictedFieldIfRequiredRoleIsNotCoveredByUser() throws IOException {

        // given
        securityContext.havingAuthenticatedUser().withRole(GivenCustomerDto.class, 888L, Role.AnyCustomerUser.ROLE);

        // when
        serialize(givenDTO);

        // then
        verify(jsonGenerator, never()).writeStringField("restrictedField", givenDTO.restrictedField);
    }

    @Test
    public void shouldThrowExceptionForUnimplementedFieldType() {

        // given
        class Arbitrary {

        }
        class GivenDtoWithUnimplementedFieldType implements AccessMappings {

            @AccessFor(read = Role.Anybody.class)
            Arbitrary fieldWithUnimplementedType = new Arbitrary();

            @Override
            public Long getId() {
                return null;
            }
        }
        final GivenDtoWithUnimplementedFieldType givenDtoWithUnimplementedFieldType = new GivenDtoWithUnimplementedFieldType();
        SecurityContextFake.havingAuthenticatedUser();

        // when
        final Throwable actual = catchThrowable(() -> serialize(givenDtoWithUnimplementedFieldType));

        // then
        assertThat(actual).isInstanceOf(NotImplementedException.class);
    }

    // --- fixture code below ---

    private <T extends AccessMappings> void serialize(final T dto) throws IOException {
        // @formatter:off
        new JsonSerializerWithAccessFilter<T>(ctx, userRoleAssignmentService) {}
            .serialize(dto, jsonGenerator, null);
        // @formatter:on
    }
}
