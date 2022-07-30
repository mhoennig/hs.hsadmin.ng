package net.hostsharing.hsadminng.hspackage;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hscustomer.CustomerEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PackageController.class)
class PackageControllerTest {

    final CustomerEntity cust = new CustomerEntity(UUID.randomUUID(), "xyz", 10001, "xyz@example.com");
    final PackageEntity pac00 = new PackageEntity(UUID.randomUUID(), "xyz00", cust);
    final PackageEntity pac01 = new PackageEntity(UUID.randomUUID(), "xyz01", cust);
    final PackageEntity pac02 = new PackageEntity(UUID.randomUUID(), "xyz02", cust);
    @Autowired
    MockMvc mockMvc;
    @MockBean
    private Context contextMock;
    @MockBean
    private PackageRepository packageRepositoryMock;

    @Test
    void findAll() throws Exception {

        // given
        final var givenPacs = asList(pac00, pac01, pac02);
        when(packageRepositoryMock.findAll()).thenReturn(givenPacs);

        // when
        final var pacs = mockMvc.perform(MockMvcRequestBuilders
                .get("/api/package")
                .header("current-user", "mike@hostsharing.net")
                .header("assumed-roles", "customer#xyz.admin")
                .accept(MediaType.APPLICATION_JSON))

            // then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].name", is("xyz00")))
            .andExpect(jsonPath("$[1].uuid", is(pac01.getUuid().toString())))
            .andExpect(jsonPath("$[2].customer.prefix", is("xyz")));

        verify(contextMock).setCurrentUser("mike@hostsharing.net");
        verify(contextMock).assumeRoles("customer#xyz.admin");
    }

}
