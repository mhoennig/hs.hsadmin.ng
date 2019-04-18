package org.hostsharing.hsadminng.service.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hostsharing.hsadminng.security.SecurityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@JsonTest
@RunWith(SpringRunner.class)
public class CustomerDTOUnitTest {


    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testSerializationAsContractualCustomerContact() throws JsonProcessingException {

        // given
        CustomerDTO given = createSomeCustomerDTO();
        givenLoginUserWithRole("ANY_CUSTOMER_USER");

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
        CustomerDTO given = createSomeCustomerDTO();
        givenLoginUserWithRole("SUPPORTER");

        // when
        String actual = objectMapper.writeValueAsString(given);

        // then
        assertThat(actual).isEqualTo(createExpectedJSon(given));
    }

    @Test
    public void testDeserializeAsContractualCustomerContact() throws IOException {
        // given
        String json = "{\"id\":1234,\"reference\":10001,\"prefix\":\"abc\",\"name\":\"Mein Name\",\"contractualAddress\":\"Eine Adresse\",\"contractualSalutation\":\"Hallo\",\"billingAddress\":\"Noch eine Adresse\",\"billingSalutation\":\"Moin\",\"remark\":\"Eine Bemerkung\"}";
        givenLoginUserWithRole("CONTRACTUAL_CONTACT");

        // when
        CustomerDTO actual = objectMapper.readValue(json, CustomerDTO.class);

        // then
        CustomerDTO expected = new CustomerDTO();
        expected.setId(1234L);
        expected.setReference(10001);
        expected.setPrefix("abc");
        expected.setName("Mein Name");
        expected.setContractualAddress(null); // not allowed
        expected.setContractualSalutation("Hallo");
        expected.setBillingAddress("Noch eine Adresse");
        expected.setBillingSalutation("Moin");
        expected.setRemark("Eine Bemerkung");
        assertEquals(actual, expected);
    }

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

    private void givenLoginUserWithRole(String userName) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(userName, userName));
        SecurityContextHolder.setContext(securityContext);
        Optional<String> login = SecurityUtils.getCurrentUserLogin();
        assertThat(login).describedAs("precondition failed").contains(userName);
    }
}
