package net.hostsharing.hsadminng.rbac.rbacrole;

import net.hostsharing.hsadminng.context.Context;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static java.util.Arrays.asList;
import static net.hostsharing.hsadminng.rbac.rbacrole.TestRbacRole.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RbacRoleController.class)
class RbacRoleControllerRestTest {

    @Autowired
    MockMvc mockMvc;
    @MockBean
    Context contextMock;
    @MockBean
    RbacRoleRepository rbacRoleRepository;

    @Test
    void apiCustomersWillReturnCustomersFromRepository() throws Exception {

        // given
        when(rbacRoleRepository.findAll()).thenReturn(
            asList(hostmasterRole, customerXxxOwner, customerXxxAdmin));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                .get("/api/rbac-roles")
                .header("current-user", "mike@hostsharing.net")
                .accept(MediaType.APPLICATION_JSON))

            // then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].roleName", is("global#hostsharing.admin")))
            .andExpect(jsonPath("$[1].roleName", is("customer#xxx.owner")))
            .andExpect(jsonPath("$[2].roleName", is("customer#xxx.admin")))
            .andExpect(jsonPath("$[2].uuid", is(customerXxxAdmin.getUuid().toString())))
            .andExpect(jsonPath("$[2].objectUuid", is(customerXxxAdmin.getObjectUuid().toString())))
            .andExpect(jsonPath("$[2].objectTable", is(customerXxxAdmin.getObjectTable().toString())))
            .andExpect(jsonPath("$[2].objectIdName", is(customerXxxAdmin.getObjectIdName().toString())));
    }
}
