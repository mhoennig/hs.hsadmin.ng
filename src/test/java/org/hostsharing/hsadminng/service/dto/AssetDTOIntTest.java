package org.hostsharing.hsadminng.service.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomUtils;
import org.hostsharing.hsadminng.domain.Asset;
import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.domain.enumeration.AssetAction;
import org.hostsharing.hsadminng.repository.AssetRepository;
import org.hostsharing.hsadminng.repository.CustomerRepository;
import org.hostsharing.hsadminng.repository.MembershipRepository;
import org.hostsharing.hsadminng.service.AssetService;
import org.hostsharing.hsadminng.service.AssetValidator;
import org.hostsharing.hsadminng.service.MembershipValidator;
import org.hostsharing.hsadminng.service.accessfilter.JSonBuilder;
import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.hostsharing.hsadminng.service.mapper.AssetMapper;
import org.hostsharing.hsadminng.service.mapper.AssetMapperImpl;
import org.hostsharing.hsadminng.service.mapper.CustomerMapperImpl;
import org.hostsharing.hsadminng.service.mapper.MembershipMapperImpl;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
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

import javax.persistence.EntityManager;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hostsharing.hsadminng.service.accessfilter.MockSecurityContext.givenAuthenticatedUser;
import static org.hostsharing.hsadminng.service.accessfilter.MockSecurityContext.givenUserHavingRole;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

@JsonTest
@SpringBootTest(classes = {
    CustomerMapperImpl.class,
    MembershipMapperImpl.class,
    AssetMapperImpl.class,
    AssetDTO.AssetJsonSerializer.class,
    AssetDTO.AssetJsonDeserializer.class
})
@RunWith(SpringRunner.class)
public class AssetDTOIntTest {

    private static final Long SOME_CUSTOMER_ID = RandomUtils.nextLong(100, 199);
    private static final Integer SOME_CUSTOMER_REFERENCE = 10001;
    private static final String SOME_CUSTOMER_PREFIX = "abc";
    private static final String SOME_CUSTOMER_NAME = "Some Customer Name";
    private static final Customer SOME_CUSTOMER = new Customer().id(SOME_CUSTOMER_ID)
        .reference(SOME_CUSTOMER_REFERENCE).prefix(SOME_CUSTOMER_PREFIX).name(SOME_CUSTOMER_NAME);

    private static final Long SOME_MEMBERSHIP_ID = RandomUtils.nextLong(200, 299);
    private static final LocalDate SOME_MEMBER_FROM_DATE = LocalDate.parse("2000-12-06");
    private static final Membership SOME_MEMBERSHIP = new Membership().id(SOME_MEMBERSHIP_ID)
        .customer(SOME_CUSTOMER).memberFromDate(SOME_MEMBER_FROM_DATE);
    private static final String SOME_MEMBERSHIP_DISPLAY_LABEL = "Some Customer Name [10001:abc] 2000-12-06 - ...";

    private static final Long SOME_ASSET_ID = RandomUtils.nextLong(300, 399);
    private static final Asset SOME_ASSET = new Asset().id(SOME_ASSET_ID).membership(SOME_MEMBERSHIP);

    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AssetMapper assetMapper;

    @MockBean
    private AssetRepository assetRepository;

    @MockBean
    private AssetValidator assetValidator;

    @MockBean
    private CustomerRepository customerRepository;

    @MockBean
    private MembershipRepository membershipRepository;

    @MockBean
    private MembershipValidator membershipValidator;

    @MockBean
    private AssetService assetService;

    @MockBean
    private EntityManager em;

    @Before
    public void init() {
        given(customerRepository.findById(SOME_CUSTOMER_ID)).willReturn(Optional.of(SOME_CUSTOMER));
        given(membershipRepository.findById(SOME_MEMBERSHIP_ID)).willReturn(Optional.of(SOME_MEMBERSHIP));
        given(assetRepository.findById(SOME_ASSET_ID)).willReturn((Optional.of(SOME_ASSET)));
    }

    @Test
    public void shouldSerializePartiallyForFinancialCustomerContact() throws JsonProcessingException {

        // given
        givenAuthenticatedUser();
        givenUserHavingRole(CustomerDTO.class, SOME_CUSTOMER_ID, Role.FINANCIAL_CONTACT);
        final AssetDTO given = createSomeAssetDTO(SOME_ASSET_ID);

        // when
        final String actual = objectMapper.writeValueAsString(given);

        // then
        given.setRemark(null);
        assertEquals(createExpectedJSon(given), actual);
    }

    @Test
    public void shouldSerializeCompletelyForSupporter() throws JsonProcessingException {

        // given
        givenAuthenticatedUser();
        givenUserHavingRole(Role.SUPPORTER);
        final AssetDTO given = createSomeAssetDTO(SOME_ASSET_ID);

        // when
        final String actual = objectMapper.writeValueAsString(given);

        // then
        assertEquals(createExpectedJSon(given), actual);
    }

    @Test
    public void shouldNotDeserializeForContractualCustomerContact() {
        // given
        givenAuthenticatedUser();
        givenUserHavingRole(CustomerDTO.class, SOME_CUSTOMER_ID, Role.CONTRACTUAL_CONTACT);
        final String json = new JSonBuilder()
            .withFieldValue("id", SOME_ASSET_ID)
            .withFieldValue("remark", "Updated Remark")
            .toString();

        // when
        final Throwable actual = catchThrowable(() -> objectMapper.readValue(json, AssetDTO.class));

        // then
        assertThat(actual).isInstanceOfSatisfying(BadRequestAlertException.class, bre ->
            assertThat(bre.getMessage()).isEqualTo("Update of field AssetDTO.remark prohibited for current user role CONTRACTUAL_CONTACT")
        );
    }

    @Test
    public void shouldDeserializeForAdminIfRemarkIsChanged() throws IOException {
        // given
        givenAuthenticatedUser();
        givenUserHavingRole(Role.ADMIN);
        final String json = new JSonBuilder()
            .withFieldValue("id", SOME_ASSET_ID)
            .withFieldValue("remark", "Updated Remark")
            .toString();

        // when
        final AssetDTO actual = objectMapper.readValue(json, AssetDTO.class);

        // then
        final AssetDTO expected = new AssetDTO();
        expected.setId(SOME_ASSET_ID);
        expected.setMembershipId(SOME_MEMBERSHIP_ID);
        expected.setRemark("Updated Remark");
        expected.setMembershipDisplayLabel(SOME_MEMBERSHIP_DISPLAY_LABEL);
        assertThat(actual).isEqualToIgnoringGivenFields(expected, "displayLabel");
    }

    // --- only test fixture below ---

    private String createExpectedJSon(AssetDTO dto) {
        return new JSonBuilder()
            .withFieldValueIfPresent("id", dto.getId())
            .withFieldValueIfPresent("documentDate", dto.getDocumentDate().toString())
            .withFieldValueIfPresent("valueDate", dto.getValueDate().toString())
            .withFieldValueIfPresent("action", dto.getAction().name())
            .withFieldValueIfPresent("amount", dto.getAmount().doubleValue())
            .withFieldValueIfPresent("remark", dto.getRemark())
            .withFieldValueIfPresent("membershipId", dto.getMembershipId())
            .withFieldValue("membershipDisplayLabel", dto.getMembershipDisplayLabel())
            .toString();
    }


    private AssetDTO createSomeAssetDTO(final long id) {
        final AssetDTO given = new AssetDTO();
        given.setId(id);
        given.setAction(AssetAction.PAYMENT);
        given.setAmount(new BigDecimal("512.01"));
        given.setDocumentDate(LocalDate.parse("2019-04-27"));
        given.setValueDate(LocalDate.parse("2019-04-28"));
        given.setMembershipId(SOME_MEMBERSHIP_ID);
        given.setRemark("Some Remark");
        given.setMembershipDisplayLabel("Display Label for Membership #" + SOME_MEMBERSHIP_ID);
        return given;
    }
}
