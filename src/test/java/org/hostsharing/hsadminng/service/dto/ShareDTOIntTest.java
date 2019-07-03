// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.domain.Share;
import org.hostsharing.hsadminng.domain.enumeration.ShareAction;
import org.hostsharing.hsadminng.repository.CustomerRepository;
import org.hostsharing.hsadminng.repository.MembershipRepository;
import org.hostsharing.hsadminng.repository.ShareRepository;
import org.hostsharing.hsadminng.service.MembershipValidator;
import org.hostsharing.hsadminng.service.ShareService;
import org.hostsharing.hsadminng.service.ShareValidator;
import org.hostsharing.hsadminng.service.UserRoleAssignmentService;
import org.hostsharing.hsadminng.service.accessfilter.JSonBuilder;
import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.hostsharing.hsadminng.service.accessfilter.Role.CustomerContractualContact;
import org.hostsharing.hsadminng.service.accessfilter.Role.CustomerFinancialContact;
import org.hostsharing.hsadminng.service.accessfilter.SecurityContextMock;
import org.hostsharing.hsadminng.service.mapper.CustomerMapperImpl;
import org.hostsharing.hsadminng.service.mapper.MembershipMapperImpl;
import org.hostsharing.hsadminng.service.mapper.ShareMapper;
import org.hostsharing.hsadminng.service.mapper.ShareMapperImpl;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import javax.persistence.EntityManager;

@JsonTest
@SpringBootTest(
        classes = {
                CustomerMapperImpl.class,
                MembershipMapperImpl.class,
                ShareMapperImpl.class,
                ShareDTO.JsonSerializer.class,
                ShareDTO.JsonDeserializer.class
        })
@RunWith(SpringRunner.class)
public class ShareDTOIntTest {

    private static final Long SOME_CUSTOMER_ID = RandomUtils.nextLong(100, 199);
    private static final Integer SOME_CUSTOMER_REFERENCE = 10001;
    private static final String SOME_CUSTOMER_PREFIX = "abc";
    private static final String SOME_CUSTOMER_NAME = "Some Customer Name";
    private static final Customer SOME_CUSTOMER = new Customer().id(SOME_CUSTOMER_ID)
            .reference(SOME_CUSTOMER_REFERENCE)
            .prefix(SOME_CUSTOMER_PREFIX)
            .name(SOME_CUSTOMER_NAME);

    private static final Long SOME_MEMBERSHIP_ID = RandomUtils.nextLong(200, 299);
    private static final LocalDate SOME_MEMBER_FROM_DATE = LocalDate.parse("2000-12-06");
    private static final Membership SOME_MEMBERSHIP = new Membership().id(SOME_MEMBERSHIP_ID)
            .customer(SOME_CUSTOMER)
            .memberFromDate(SOME_MEMBER_FROM_DATE);
    private static final String SOME_MEMBERSHIP_DISPLAY_LABEL = "Some Customer Name [10001:abc] 2000-12-06 - ...";

    private static final Long SOME_SHARE_ID = RandomUtils.nextLong(300, 399);
    private static final Share SOME_SHARE = new Share().id(SOME_SHARE_ID).membership(SOME_MEMBERSHIP);

    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShareMapper shareMapper;

    @MockBean
    private ShareRepository shareRepository;

    @MockBean
    private ShareValidator shareValidator;

    @MockBean
    private CustomerRepository customerRepository;

    @MockBean
    private MembershipRepository membershipRepository;

    @MockBean
    private MembershipValidator membershipValidator;

    @MockBean
    private ShareService shareService;

    @MockBean
    private EntityManager em;

    @MockBean
    private UserRoleAssignmentService userRoleAssignmentService;

    private SecurityContextMock securityContext;

    @Before
    public void init() {
        given(customerRepository.findById(SOME_CUSTOMER_ID)).willReturn(Optional.of(SOME_CUSTOMER));
        given(membershipRepository.findById(SOME_MEMBERSHIP_ID)).willReturn(Optional.of(SOME_MEMBERSHIP));
        given(shareRepository.findById(SOME_SHARE_ID)).willReturn((Optional.of(SOME_SHARE)));

        securityContext = SecurityContextMock.usingMock(userRoleAssignmentService);
    }

    @Test
    public void shouldSerializePartiallyForFinancialCustomerContact() throws JsonProcessingException {

        // given
        securityContext.havingAuthenticatedUser()
                .withRole(CustomerDTO.class, SOME_CUSTOMER_ID, CustomerFinancialContact.ROLE);
        final ShareDTO given = createSomeShareDTO(SOME_SHARE_ID);

        // when
        final String actual = objectMapper.writeValueAsString(given);

        // then
        given.setRemark(null);
        assertEquals(createExpectedJSon(given), actual);
    }

    @Test
    public void shouldSerializeCompletelyForSupporter() throws JsonProcessingException {

        // given
        securityContext.havingAuthenticatedUser().withAuthority(Role.Supporter.ROLE.authority());
        final ShareDTO given = createSomeShareDTO(SOME_SHARE_ID);

        // when
        final String actual = objectMapper.writeValueAsString(given);

        // then
        assertEquals(createExpectedJSon(given), actual);
    }

    @Test
    public void shouldNotDeserializeForContractualCustomerContact() {
        // given
        securityContext.havingAuthenticatedUser()
                .withRole(CustomerDTO.class, SOME_CUSTOMER_ID, CustomerContractualContact.ROLE);
        final String json = new JSonBuilder()
                .withFieldValue("id", SOME_SHARE_ID)
                .withFieldValue("remark", "Updated Remark")
                .toString();

        // when
        final Throwable actual = catchThrowable(() -> objectMapper.readValue(json, ShareDTO.class));

        // then
        assertThat(actual).isInstanceOfSatisfying(
                BadRequestAlertException.class,
                bre -> assertThat(bre.getMessage())
                        .isEqualTo(
                                "Update of field ShareDTO.remark prohibited for current user role(s): CustomerContractualContact"));
    }

    @Test
    public void shouldDeserializeForAdminIfRemarkIsChanged() throws IOException {
        // given
        securityContext.havingAuthenticatedUser().withAuthority(Role.Admin.ROLE.authority());
        final String json = new JSonBuilder()
                .withFieldValue("id", SOME_SHARE_ID)
                .withFieldValue("remark", "Updated Remark")
                .toString();

        // when
        final ShareDTO actual = objectMapper.readValue(json, ShareDTO.class);

        // then
        final ShareDTO expected = new ShareDTO();
        expected.setId(SOME_SHARE_ID);
        expected.setMembershipId(SOME_MEMBERSHIP_ID);
        expected.setRemark("Updated Remark");
        expected.setMembershipDisplayLabel(SOME_MEMBERSHIP_DISPLAY_LABEL);
        assertThat(actual).isEqualToIgnoringGivenFields(expected, "displayLabel");
    }

    // --- only test fixture below ---

    private String createExpectedJSon(ShareDTO dto) {
        return new JSonBuilder()
                .withFieldValueIfPresent("id", dto.getId())
                .withFieldValueIfPresent("documentDate", dto.getDocumentDate().toString())
                .withFieldValueIfPresent("valueDate", dto.getValueDate().toString())
                .withFieldValueIfPresent("action", dto.getAction().name())
                .withFieldValueIfPresent("quantity", dto.getQuantity())
                .withFieldValueIfPresent("remark", dto.getRemark())
                .withFieldValueIfPresent("membershipId", dto.getMembershipId())
                .withFieldValue("membershipDisplayLabel", dto.getMembershipDisplayLabel())
                .toString();
    }

    private ShareDTO createSomeShareDTO(final long id) {
        final ShareDTO given = new ShareDTO();
        given.setId(id);
        given.setAction(ShareAction.SUBSCRIPTION);
        given.setQuantity(16);
        given.setDocumentDate(LocalDate.parse("2019-04-27"));
        given.setValueDate(LocalDate.parse("2019-04-28"));
        given.setMembershipId(SOME_MEMBERSHIP_ID);
        given.setRemark("Some Remark");
        given.setMembershipDisplayLabel("Display Label for Membership #" + SOME_MEMBERSHIP_ID);
        return given;
    }
}
