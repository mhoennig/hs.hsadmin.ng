package org.hostsharing.hsadminng.service.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hostsharing.hsadminng.service.CustomerService;
import org.hostsharing.hsadminng.service.MembershipService;
import org.hostsharing.hsadminng.service.accessfilter.JSonDeserializerWithAccessFilter;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hostsharing.hsadminng.service.accessfilter.JSonBuilder.asJSon;
import static org.hostsharing.hsadminng.service.accessfilter.MockSecurityContext.givenAuthenticatedUser;
import static org.hostsharing.hsadminng.service.accessfilter.MockSecurityContext.givenUserHavingRole;
import static org.mockito.BDDMockito.given;

public class MembershipDTOUnitTest {

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
    private MembershipService membershipService;

    @Mock
    private CustomerService customerService;

    @Before
    public void init() {
        given(jsonParser.getCodec()).willReturn(codec);

        given (ctx.getAutowireCapableBeanFactory()).willReturn(autowireCapableBeanFactory);
        given(autowireCapableBeanFactory.createBean(MembershipService.class)).willReturn(membershipService);
        given(autowireCapableBeanFactory.createBean(CustomerService.class)).willReturn(customerService);
        given(customerService.findOne(1234L)).willReturn(Optional.of(new CustomerDTO()
            .with(dto -> dto.setId(1234L))
        ));
    }

    @Test
    public void adminShouldHaveRightToCreate() throws IOException {
        givenAuthenticatedUser();
        givenUserHavingRole(null, null, Role.ADMIN);
        givenJSonTree(asJSon(ImmutablePair.of("customerId", 1234L)));

        // when
        final MembershipDTO actualDto = new JSonDeserializerWithAccessFilter<>(ctx, jsonParser, null, MembershipDTO.class).deserialize();

        // then
        assertThat(actualDto.getCustomerId()).isEqualTo(1234L);
    }

    @Test
    public void contractualContactShouldNotHaveRightToCreate() throws IOException {
        givenAuthenticatedUser();
        givenUserHavingRole(CustomerDTO.class, 1234L, Role.CONTRACTUAL_CONTACT);
        givenJSonTree(asJSon(ImmutablePair.of("customerId", 1234L)));

        // when
        Throwable exception = catchThrowable(() -> new JSonDeserializerWithAccessFilter<>(ctx, jsonParser, null, MembershipDTO.class).deserialize());

        // then
        assertThat(exception).isInstanceOfSatisfying(BadRequestAlertException.class, badRequestAlertException -> {
            assertThat(badRequestAlertException.getParam()).isEqualTo("MembershipDTO.customerId");
            assertThat(badRequestAlertException.getErrorKey()).isEqualTo("referencingProhibited");
        });
    }

    // --- only fixture code below ---

    private void givenJSonTree(String givenJSon) throws IOException {
        given(codec.readTree(jsonParser)).willReturn(new ObjectMapper().readTree(givenJSon));
    }

}
