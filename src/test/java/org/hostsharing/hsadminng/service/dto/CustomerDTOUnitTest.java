package org.hostsharing.hsadminng.service.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hostsharing.hsadminng.service.accessfilter.MockSecurityContext.givenAuthenticatedUser;
import static org.hostsharing.hsadminng.service.accessfilter.MockSecurityContext.givenUserHavingRole;
import static org.junit.Assert.assertEquals;

@JsonTest
@RunWith(SpringRunner.class)
public class CustomerDTOUnitTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testSerializationAsContractualCustomerContact() throws JsonProcessingException {

        // given
        givenAuthenticatedUser();
        givenUserHavingRole(CustomerDTO.class, null, Role.ANY_CUSTOMER_USER);
        CustomerDTO given = createSomeCustomerDTO();

        // when
        String actual = objectMapper.writeValueAsString(given);

        // then
        given.setContractualAddress(null);
        given.setContractualSalutation(null);
        given.setBillingAddress(null);
        given.setBillingSalutation(null);
        given.setRemark(null);
        assertEquals(createExpectedJSon(given), actual);
    }

    @Test
    public void testSerializationAsSupporter() throws JsonProcessingException {

        // given
        givenAuthenticatedUser();
        givenUserHavingRole(CustomerDTO.class, null, Role.SUPPORTER);
        CustomerDTO given = createSomeCustomerDTO();

        // when
        String actual = objectMapper.writeValueAsString(given);

        // then
        assertThat(actual).isEqualTo(createExpectedJSon(given));
    }

    @Test
    public void testDeserializeAsContractualCustomerContact() throws IOException {
        // given
        givenAuthenticatedUser();
        givenUserHavingRole(CustomerDTO.class, null, Role.CONTRACTUAL_CONTACT);
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
                toJSonFieldDefinitionIfPresent("contractualSalutation", dto.getContractualSalutation()) +
                toJSonFieldDefinitionIfPresent("contractualAddress", dto.getContractualAddress()) +
                toJSonFieldDefinitionIfPresent("billingSalutation", dto.getBillingSalutation()) +
                toJSonFieldDefinitionIfPresent("billingAddress", dto.getBillingAddress()) +
                toJSonFieldDefinitionIfPresent("remark", dto.getRemark()) ;
        return "{" + json.substring(0, json.length() - 1) + "}";
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

    private CustomerDTO createSomeCustomerDTO() {
        CustomerDTO given = new CustomerDTO();
        given.setId(1234L);
        given.setReference(10001);
        given.setPrefix("abc");
        given.setName("Mein Name");
        given.setContractualAddress("Eine Adresse");
        given.setContractualSalutation("Hallo");
        given.setBillingAddress("Noch eine Adresse");
        given.setBillingSalutation("Moin");
        given.setRemark("Eine Bemerkung");
        return given;
    }
}
