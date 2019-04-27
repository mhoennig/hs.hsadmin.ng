package org.hostsharing.hsadminng.service.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hostsharing.hsadminng.domain.enumeration.ShareAction;
import org.hostsharing.hsadminng.service.CustomerService;
import org.hostsharing.hsadminng.service.MembershipService;
import org.hostsharing.hsadminng.service.accessfilter.JSonDeserializationWithAccessFilter;
import org.hostsharing.hsadminng.service.accessfilter.JSonSerializationWithAccessFilter;
import org.hostsharing.hsadminng.service.accessfilter.Role;
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
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hostsharing.hsadminng.service.accessfilter.JSonBuilder.asJSon;
import static org.hostsharing.hsadminng.service.accessfilter.MockSecurityContext.givenAuthenticatedUser;
import static org.hostsharing.hsadminng.service.accessfilter.MockSecurityContext.givenUserHavingRole;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ShareDTOUnitTest {

    private static final long SOME_MEMBERSHIP_ID = 12345L;
    private static final long SOME_CUSTOMER_ID = 1234L;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ApplicationContext ctx;

    @Mock
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Mock
    private JsonParser jsonParser;

    @Mock
    private JsonGenerator jsonGenerator;

    @Mock
    private ObjectCodec codec;

    @Mock
    private TreeNode treeNode;

    @Mock
    private CustomerService customerService;

    @Mock
    private MembershipService membershipService;

    @Before
    public void init() {
        given(jsonParser.getCodec()).willReturn(codec);

        given(ctx.getAutowireCapableBeanFactory()).willReturn(autowireCapableBeanFactory);
        given(ctx.getAutowireCapableBeanFactory()).willReturn(autowireCapableBeanFactory);
        given(autowireCapableBeanFactory.createBean(CustomerService.class)).willReturn(customerService);
        given(autowireCapableBeanFactory.createBean(MembershipService.class)).willReturn(membershipService);

        given(customerService.findOne(SOME_CUSTOMER_ID)).willReturn(Optional.of(new CustomerDTO()));
        given(membershipService.findOne(SOME_MEMBERSHIP_ID)).willReturn(Optional.of(new MembershipDTO().with(dto -> dto.setCustomerId(SOME_CUSTOMER_ID))));
    }

    @Test
    public void adminShouldHaveRightToCreate() throws IOException {
        givenAuthenticatedUser();
        givenUserHavingRole(null, null, Role.ADMIN);
        givenJSonTree(asJSon(ImmutablePair.of("membershipId", SOME_MEMBERSHIP_ID)));

        // when
        final ShareDTO actualDto = new JSonDeserializationWithAccessFilter<>(ctx, jsonParser, null, ShareDTO.class).deserialize();

        // then
        assertThat(actualDto.getMembershipId()).isEqualTo(SOME_MEMBERSHIP_ID);
    }

    @Test
    public void contractualContactShouldNotHaveRightToCreate() throws IOException {
        givenAuthenticatedUser();
        givenUserHavingRole(CustomerDTO.class, SOME_CUSTOMER_ID, Role.CONTRACTUAL_CONTACT);
        givenJSonTree(asJSon(ImmutablePair.of("membershipId", ShareDTOUnitTest.SOME_MEMBERSHIP_ID)));

        // when
        Throwable exception = catchThrowable(() -> new JSonDeserializationWithAccessFilter<>(ctx, jsonParser, null, ShareDTO.class).deserialize());

        // then
        assertThat(exception).isInstanceOfSatisfying(BadRequestAlertException.class, badRequestAlertException -> {
            assertThat(badRequestAlertException.getParam()).isEqualTo("ShareDTO.membershipId");
            assertThat(badRequestAlertException.getErrorKey()).isEqualTo("referencingProhibited");
        });
    }

    @Test
    public void financialContactShouldHaveRightToReadAllButRemark() throws IOException {
        givenAuthenticatedUser();
        givenUserHavingRole(CustomerDTO.class, SOME_CUSTOMER_ID, Role.FINANCIAL_CONTACT);
        final ShareDTO givenDTO = createShareDto();

        // when
        new JSonSerializationWithAccessFilter<>(ctx, jsonGenerator, null, givenDTO).serialize();

        // then
        verify(jsonGenerator).writeNumberField("id", givenDTO.getId());
        verify(jsonGenerator).writeNumberField("membershipId", givenDTO.getMembershipId());
        verify(jsonGenerator, never()).writeStringField(eq("remark"), anyString());
    }

    @Test
    public void supporterShouldHaveRightToRead() throws IOException {
        givenAuthenticatedUser();
        givenUserHavingRole(null, null, Role.SUPPORTER);
        final ShareDTO givenDTO = createShareDto();

        // when
        new JSonSerializationWithAccessFilter<>(ctx, jsonGenerator, null, givenDTO).serialize();

        // then
        verify(jsonGenerator).writeNumberField("id", givenDTO.getId());
        verify(jsonGenerator).writeNumberField("membershipId", givenDTO.getMembershipId());
        verify(jsonGenerator).writeStringField("remark", givenDTO.getRemark());
    }

    // --- only fixture code below ---

    private void givenJSonTree(String givenJSon) throws IOException {
        given(codec.readTree(jsonParser)).willReturn(new ObjectMapper().readTree(givenJSon));
    }

    private ShareDTO createShareDto() {
        final ShareDTO givenDTO = new ShareDTO();
        givenDTO.setId(1234567L);
        givenDTO.setMembershipId(SOME_MEMBERSHIP_ID);
        givenDTO.setAction(ShareAction.SUBSCRIPTION);
        givenDTO.setQuantity(3);
        givenDTO.setDocumentDate(LocalDate.parse("2019-04-22"));
        givenDTO.setMembershipDisplayReference("2019-04-21"); // TODO: why is this not a LocalDate?
        givenDTO.setValueDate(LocalDate.parse("2019-04-30"));
        givenDTO.setRemark("Some Remark");
        return givenDTO;
    }

}
