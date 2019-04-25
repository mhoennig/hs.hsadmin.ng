package org.hostsharing.hsadminng.service.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.enumeration.CustomerKind;
import org.hostsharing.hsadminng.domain.enumeration.VatRegion;
import org.hostsharing.hsadminng.repository.CustomerRepository;
import org.hostsharing.hsadminng.service.CustomerService;
import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.hostsharing.hsadminng.service.mapper.CustomerMapper;
import org.hostsharing.hsadminng.service.mapper.CustomerMapperImpl;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hostsharing.hsadminng.service.accessfilter.MockSecurityContext.givenAuthenticatedUser;
import static org.hostsharing.hsadminng.service.accessfilter.MockSecurityContext.givenUserHavingRole;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

@JsonTest
@SpringBootTest(classes = {CustomerMapperImpl.class, CustomerRepository.class, CustomerService.class, CustomerDTO.CustomerJsonSerializer.class, CustomerDTO.CustomerJsonDeserializer.class})
@RunWith(SpringRunner.class)
public class CustomerDTOUnitTest {

    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @MockBean
    private CustomerRepository customerRepository;

    @MockBean
    private CustomerService customerService;

    @Test
    public void testSerializationAsContractualCustomerContact() throws JsonProcessingException {

        // given
        givenAuthenticatedUser();
        givenUserHavingRole(CustomerDTO.class, 1234L, Role.CONTRACTUAL_CONTACT);
        CustomerDTO given = createSomeCustomerDTO(1234L);

        // when
        String actual = objectMapper.writeValueAsString(given);

        // then
        given.setRemark(null);
        assertEquals(createExpectedJSon(given), actual);
    }

    @Test
    public void testSerializationAsTechnicalCustomerUser() throws JsonProcessingException {

        // given
        givenAuthenticatedUser();
        givenUserHavingRole(CustomerDTO.class, 1234L, Role.TECHNICAL_CONTACT);
        CustomerDTO given = createSomeCustomerDTO(1234L);

        // when
        String actual = objectMapper.writeValueAsString(given);

        // then
        final String expectedJSon = "{" +
            toJSonFieldDefinition("id", given.getId()) + "," +
            toJSonFieldDefinition("reference", given.getReference()) + "," +
            toJSonFieldDefinition("prefix", given.getPrefix()) + "," +
            toJSonFieldDefinition("name", given.getName()) +
            "}";
        assertEquals(expectedJSon, actual);
    }

    @Test
    public void testSerializationAsSupporter() throws JsonProcessingException {

        // given
        givenAuthenticatedUser();
        givenUserHavingRole(CustomerDTO.class, null, Role.SUPPORTER);
        CustomerDTO given = createSomeCustomerDTO(1234L);

        // when
        String actual = objectMapper.writeValueAsString(given);

        // then
        assertThat(actual).isEqualTo(createExpectedJSon(given));
    }

    @Test
    public void testDeserializeAsContractualCustomerContact() throws IOException {
        // given
        givenAuthenticatedUser();
        givenUserHavingRole(CustomerDTO.class, 1234L, Role.CONTRACTUAL_CONTACT);
        given(customerRepository.findById(1234L)).willReturn(Optional.of(new Customer().id(1234L)));
        String json = "{\"id\":1234,\"contractualSalutation\":\"Hallo Updated\",\"billingSalutation\":\"Moin Updated\"}";

        // when
        CustomerDTO actual = objectMapper.readValue(json, CustomerDTO.class);

        // then
        CustomerDTO expected = new CustomerDTO();
        expected.setId(1234L);
        expected.setContractualSalutation("Hallo Updated");
        expected.setBillingSalutation("Moin Updated");
        assertThat(actual).isEqualToComparingFieldByField(expected);
    }

    // --- only test fixture below ---

    private String createExpectedJSon(CustomerDTO dto) {
        String json = // the fields in alphanumeric order:
            toJSonFieldDefinitionIfPresent("id", dto.getId()) +
                toJSonFieldDefinitionIfPresent("reference", dto.getReference()) +
                toJSonFieldDefinitionIfPresent("prefix", dto.getPrefix()) +
                toJSonFieldDefinitionIfPresent("name", dto.getName()) +
                toJSonFieldDefinitionIfPresent("kind", "LEGAL") +
                toJSonNullFieldDefinition("birthDate") +
                toJSonNullFieldDefinition("birthPlace") +
                toJSonFieldDefinitionIfPresent("registrationCourt", "Registergericht") +
                toJSonFieldDefinitionIfPresent("registrationNumber", "Registernummer") +
                toJSonFieldDefinitionIfPresent("vatRegion", "DOMESTIC") +
                toJSonFieldDefinitionIfPresent("vatNumber", "DE1234") +
                toJSonFieldDefinitionIfPresent("contractualSalutation", dto.getContractualSalutation()) +
                toJSonFieldDefinitionIfPresent("contractualAddress", dto.getContractualAddress()) +
                toJSonFieldDefinitionIfPresent("billingSalutation", dto.getBillingSalutation()) +
                toJSonFieldDefinitionIfPresent("billingAddress", dto.getBillingAddress()) +
                toJSonFieldDefinitionIfPresent("remark", dto.getRemark());
        return "{" + json.substring(0, json.length() - 1) + "}";
    }



    private String toJSonFieldDefinition(String name, String value) {
        return inQuotes(name) + ":" + (value != null ? inQuotes(value) : "null");
    }

    private String toJSonFieldDefinition(String name, Number value) {
        return inQuotes(name) + ":" + (value != null ? value : "null");
    }

    private String toJSonNullFieldDefinition(String name) {
        return inQuotes(name) + ":null,";
    }

    private String toJSonFieldDefinitionIfPresent(String name, String value) {
        return value != null ? inQuotes(name) + ":" + inQuotes(value) + "," : "";
    }

    private String toJSonFieldDefinitionIfPresent(String name, Number value) {
        return value != null ? inQuotes(name) + ":" + value + "," : "";
    }

    private String inQuotes(Object value) {
        return "\"" + value.toString() + "\"";
    }

    private CustomerDTO createSomeCustomerDTO(final long id) {
        final CustomerDTO given = new CustomerDTO();
        given.setId(id);
        given.setReference(10001);
        given.setPrefix("abc");
        given.setName("Mein Name");
        given.setKind(CustomerKind.LEGAL);
        given.setRegistrationCourt("Registergericht");
        given.setRegistrationNumber("Registernummer");
        given.setVatRegion(VatRegion.DOMESTIC);
        given.setVatNumber("DE1234");
        given.setContractualAddress("Eine Adresse");
        given.setContractualSalutation("Hallo");
        given.setBillingAddress("Noch eine Adresse");
        given.setBillingSalutation("Moin");
        given.setRemark("Eine Bemerkung");
        return given;
    }
}
