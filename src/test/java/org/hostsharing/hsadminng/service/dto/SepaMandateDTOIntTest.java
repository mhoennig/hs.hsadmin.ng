// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hostsharing.hsadminng.service.dto.SepaMandateDTOUnitTest.createSampleDTO;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.SepaMandate;
import org.hostsharing.hsadminng.repository.CustomerRepository;
import org.hostsharing.hsadminng.repository.MembershipRepository;
import org.hostsharing.hsadminng.repository.SepaMandateRepository;
import org.hostsharing.hsadminng.security.AuthoritiesConstants;
import org.hostsharing.hsadminng.service.MembershipValidator;
import org.hostsharing.hsadminng.service.SepaMandateService;
import org.hostsharing.hsadminng.service.UserRoleAssignmentService;
import org.hostsharing.hsadminng.service.accessfilter.JSonBuilder;
import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.hostsharing.hsadminng.service.accessfilter.SecurityContextMock;
import org.hostsharing.hsadminng.service.mapper.CustomerMapperImpl;
import org.hostsharing.hsadminng.service.mapper.MembershipMapperImpl;
import org.hostsharing.hsadminng.service.mapper.SepaMandateMapper;
import org.hostsharing.hsadminng.service.mapper.SepaMandateMapperImpl;
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
import java.util.Objects;
import java.util.Optional;

import javax.persistence.EntityManager;

@JsonTest
@SpringBootTest(
        classes = {
                CustomerMapperImpl.class,
                MembershipMapperImpl.class,
                SepaMandateMapperImpl.class,
                SepaMandateDTO.JsonSerializer.class,
                SepaMandateDTO.JsonDeserializer.class
        })
@RunWith(SpringRunner.class)
public class SepaMandateDTOIntTest {

    private static final Long SOME_CUSTOMER_ID = RandomUtils.nextLong(100, 199);
    private static final Integer SOME_CUSTOMER_REFERENCE = 10001;
    private static final String SOME_CUSTOMER_PREFIX = "abc";
    private static final String SOME_CUSTOMER_NAME = "Some Customer Name";
    private static final String SOME_CUSTOMER_DISPLAY_LABEL = "Some Customer Name [10001:abc]";
    private static final Customer SOME_CUSTOMER = new Customer().id(SOME_CUSTOMER_ID)
            .reference(SOME_CUSTOMER_REFERENCE)
            .prefix(SOME_CUSTOMER_PREFIX)
            .name(SOME_CUSTOMER_NAME);

    private static final Long SOME_SEPA_MANDATE_ID = RandomUtils.nextLong(300, 399);
    private static final SepaMandate SOME_SEPA_MANDATE = new SepaMandate().id(SOME_SEPA_MANDATE_ID).customer(SOME_CUSTOMER);

    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SepaMandateMapper sepaMandateMapper;

    @MockBean
    private SepaMandateRepository sepaMandateRepository;

    @MockBean
    private CustomerRepository customerRepository;

    @MockBean
    private MembershipRepository membershipRepository;

    @MockBean
    private MembershipValidator membershipValidator;

    @MockBean
    private SepaMandateService sepaMandateService;

    @MockBean
    private EntityManager em;

    @MockBean
    public UserRoleAssignmentService userRoleAssignmentService;

    private SecurityContextMock securityContext;

    @Before
    public void init() {
        given(customerRepository.findById(SOME_CUSTOMER_ID)).willReturn(Optional.of(SOME_CUSTOMER));
        given(sepaMandateRepository.findById(SOME_SEPA_MANDATE_ID)).willReturn((Optional.of(SOME_SEPA_MANDATE)));

        securityContext = SecurityContextMock.usingMock(userRoleAssignmentService);
    }

    @Test
    public void shouldSerializePartiallyForFinancialCustomerContact() throws JsonProcessingException {

        // given
        securityContext.havingAuthenticatedUser().withRole(CustomerDTO.class, SOME_CUSTOMER_ID, Role.FINANCIAL_CONTACT);
        final SepaMandateDTO given = createSampleDTO(SOME_SEPA_MANDATE_ID, SOME_CUSTOMER_ID);

        // when
        final String actual = objectMapper.writeValueAsString(given);

        // then
        given.setRemark(null);
        assertEquals(createExpectedJSon(given), actual);
    }

    @Test
    public void shouldSerializeCompletelyForSupporter() throws JsonProcessingException {

        // given
        securityContext.havingAuthenticatedUser().withAuthority(AuthoritiesConstants.SUPPORTER);
        final SepaMandateDTO given = createSampleDTO(SOME_SEPA_MANDATE_ID, SOME_CUSTOMER_ID);

        // when
        final String actual = objectMapper.writeValueAsString(given);

        // then
        assertEquals(createExpectedJSon(given), actual);
    }

    @Test
    public void shouldNotDeserializeForContractualCustomerContact() {
        // given
        securityContext.havingAuthenticatedUser().withRole(CustomerDTO.class, SOME_CUSTOMER_ID, Role.CONTRACTUAL_CONTACT);
        final String json = new JSonBuilder()
                .withFieldValue("id", SOME_SEPA_MANDATE_ID)
                .withFieldValue("remark", "Updated Remark")
                .toString();

        // when
        final Throwable actual = catchThrowable(() -> objectMapper.readValue(json, SepaMandateDTO.class));

        // then
        assertThat(actual).isInstanceOfSatisfying(
                BadRequestAlertException.class,
                bre -> assertThat(bre.getMessage()).isEqualTo(
                        "Update of field SepaMandateDTO.remark prohibited for current user role(s): CONTRACTUAL_CONTACT"));
    }

    @Test
    public void shouldDeserializeForAdminIfRemarkIsChanged() throws IOException {
        // given
        securityContext.havingAuthenticatedUser().withAuthority(AuthoritiesConstants.ADMIN);
        final String json = new JSonBuilder()
                .withFieldValue("id", SOME_SEPA_MANDATE_ID)
                .withFieldValue("remark", "Updated Remark")
                .toString();

        // when
        final SepaMandateDTO actual = objectMapper.readValue(json, SepaMandateDTO.class);

        // then
        final SepaMandateDTO expected = new SepaMandateDTO();
        expected.setId(SOME_SEPA_MANDATE_ID);
        expected.setCustomerId(SOME_CUSTOMER_ID);
        expected.setRemark("Updated Remark");
        expected.setCustomerDisplayLabel(SOME_CUSTOMER_DISPLAY_LABEL);
        assertThat(actual).isEqualToIgnoringGivenFields(expected, "displayLabel");
    }

    // --- only test fixture below ---

    private String createExpectedJSon(SepaMandateDTO dto) {
        return new JSonBuilder()
                .withFieldValueIfPresent("id", dto.getId())
                .withFieldValueIfPresent("reference", dto.getReference())
                .withFieldValueIfPresent("iban", dto.getIban())
                .withFieldValueIfPresent("bic", dto.getBic())
                .withFieldValueIfPresent("grantingDocumentDate", Objects.toString(dto.getGrantingDocumentDate()))
                .withFieldValueIfPresent("revokationDocumentDate", Objects.toString(dto.getRevokationDocumentDate()))
                .withFieldValueIfPresent("validFromDate", Objects.toString(dto.getValidFromDate()))
                .withFieldValueIfPresent("validUntilDate", Objects.toString(dto.getValidUntilDate()))
                .withFieldValueIfPresent("lastUsedDate", Objects.toString(dto.getLastUsedDate()))
                .withFieldValueIfPresent("remark", dto.getRemark())
                .withFieldValueIfPresent("customerId", dto.getCustomerId())
                .withFieldValue("customerDisplayLabel", dto.getCustomerDisplayLabel())
                .toString();
    }
}
