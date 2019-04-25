package org.hostsharing.hsadminng.service.accessfilter;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.hostsharing.hsadminng.service.IdToDtoResolver;
import org.hostsharing.hsadminng.service.dto.FluentBuilder;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class JSonSerializerWithAccessFilterUnitTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ApplicationContext ctx;

    @Mock
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Mock
    private JsonGenerator jsonGenerator;

    @Mock
    private GivenCustomerService givenCustomerService;

    private final GivenDto givenDTO = createSampleDto();

    @Before
    public void init() {
        MockSecurityContext.givenAuthenticatedUser();
        MockSecurityContext.givenUserHavingRole(GivenCustomerDto.class, 888L, Role.ANY_CUSTOMER_USER);

        given(ctx.getAutowireCapableBeanFactory()).willReturn(autowireCapableBeanFactory);
        given(autowireCapableBeanFactory.createBean(GivenCustomerService.class)).willReturn(givenCustomerService);
        given(givenCustomerService.findOne(888L)).willReturn(Optional.of(new GivenCustomerDto()
            .with(dto -> dto.id = 888L)
        ));
    }

    @Test
    public void shouldSerializeStringField() throws IOException {
        // when
        new JSonSerializerWithAccessFilter<>(ctx, jsonGenerator, null, givenDTO).serialize();

        // then
        verify(jsonGenerator).writeStringField("openStringField", givenDTO.openStringField);
    }

    @Test
    public void shouldSerializeRestrictedFieldIfRequiredRoleIsCoveredByUser() throws IOException {

        // given
        MockSecurityContext.givenAuthenticatedUser();
        MockSecurityContext.givenUserHavingRole(GivenCustomerDto.class, 888L, Role.FINANCIAL_CONTACT);

        // when
        new JSonSerializerWithAccessFilter<>(ctx, jsonGenerator, null, givenDTO).serialize();

        // then
        verify(jsonGenerator).writeStringField("restrictedField", givenDTO.restrictedField);
    }

    @Test
    public void shouldNotSerializeRestrictedFieldIfRequiredRoleIsNotCoveredByUser() throws IOException {

        // given
        MockSecurityContext.givenAuthenticatedUser();
        MockSecurityContext.givenUserHavingRole(GivenCustomerDto.class, 888L, Role.ANY_CUSTOMER_USER);

        // when
        new JSonSerializerWithAccessFilter<>(ctx, jsonGenerator, null, givenDTO).serialize();

        // then
        verify(jsonGenerator, never()).writeStringField("restrictedField", givenDTO.restrictedField);
    }

    @Test
    public void shouldThrowExceptionForUnimplementedFieldType() {

        // given
        class Arbitrary {
        }
        class GivenDtoWithUnimplementedFieldType {
            @AccessFor(read = Role.ANYBODY)
            Arbitrary fieldWithUnimplementedType = new Arbitrary();
        }
        final GivenDtoWithUnimplementedFieldType givenDtoWithUnimplementedFieldType = new GivenDtoWithUnimplementedFieldType();

        // when
        final Throwable actual = catchThrowable(() -> new JSonSerializerWithAccessFilter<>(ctx, jsonGenerator, null, givenDtoWithUnimplementedFieldType).serialize());

        // then
        assertThat(actual).isInstanceOf(NotImplementedException.class);
    }

    // --- fixture code below ---

    private GivenDto createSampleDto() {
        final GivenDto dto = new GivenDto();
        dto.customerId = 888L;
        dto.restrictedField = RandomStringUtils.randomAlphabetic(10);
        dto.openStringField = RandomStringUtils.randomAlphabetic(10);
        dto.openIntegerField = RandomUtils.nextInt();
        dto.openLongField = RandomUtils.nextLong();
        return dto;
    }

    private static class GivenCustomerDto extends FluentBuilder<GivenCustomerDto> {
        @SelfId(resolver = GivenService.class)
        @AccessFor(read = Role.ANYBODY)
        Long id;
    }

    private abstract class GivenCustomerService implements IdToDtoResolver<GivenCustomerDto> {
    }

    private static class GivenDto {

        @SelfId(resolver = GivenService.class)
        @AccessFor(read = Role.ANYBODY)
        Long id;

        @ParentId(resolver = GivenCustomerService.class)
        @AccessFor(read = {Role.TECHNICAL_CONTACT, Role.FINANCIAL_CONTACT})
        Long customerId;

        @AccessFor(read = {Role.TECHNICAL_CONTACT, Role.FINANCIAL_CONTACT})
        String restrictedField;

        @AccessFor(read = Role.ANYBODY)
        String openStringField;

        @AccessFor(read = Role.ANYBODY)
        Integer openIntegerField;

        @AccessFor(read = Role.ANYBODY)
        Long openLongField;
    }

    private abstract class GivenService implements IdToDtoResolver<GivenService> {
    }


}
