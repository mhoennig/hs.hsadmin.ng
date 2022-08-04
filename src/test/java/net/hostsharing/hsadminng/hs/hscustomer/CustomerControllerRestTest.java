package net.hostsharing.hsadminng.hs.hscustomer;

import net.hostsharing.hsadminng.context.Context;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerRestTest {

    @Autowired
    MockMvc mockMvc;
    @MockBean
    Context contextMock;
    @MockBean
    CustomerRepository customerRepositoryMock;

    @Test
    void apiCustomersWillReturnAllCustomersFromRepositoryIfNoCriteriaGiven() throws Exception {

        // given
        when(customerRepositoryMock.findCustomerByOptionalPrefixLike(null)).thenReturn(asList(TestCustomer.xxx, TestCustomer.yyy));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                .get("/api/customers")
                .header("current-user", "mike@hostsharing.net")
                .accept(MediaType.APPLICATION_JSON))

            // then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].prefix", is(TestCustomer.xxx.getPrefix())))
            .andExpect(jsonPath("$[1].reference", is(TestCustomer.yyy.getReference())));
    }

    @Test
    void apiCustomersWillReturnMatchingCustomersFromRepositoryIfCriteriaGiven() throws Exception {

        // given
        when(customerRepositoryMock.findCustomerByOptionalPrefixLike("x")).thenReturn(asList(TestCustomer.xxx));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                .get("/api/customers")
                .header("current-user", "mike@hostsharing.net")
                .param("prefix", "x")
                .accept(MediaType.APPLICATION_JSON))

            // then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].prefix", is(TestCustomer.xxx.getPrefix())));
    }
}
