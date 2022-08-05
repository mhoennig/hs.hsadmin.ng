package net.hostsharing.hsadminng.rbac.rbacuser;

import net.hostsharing.hsadminng.context.Context;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static net.hostsharing.hsadminng.rbac.rbacrole.TestRbacRole.*;
import static net.hostsharing.hsadminng.rbac.rbacuser.TestRbacUser.userAaa;
import static net.hostsharing.hsadminng.rbac.rbacuser.TestRbacUser.userBbb;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RbacUserController.class)
class RbacUserControllerRestTest {

    @Autowired
    MockMvc mockMvc;
    @MockBean
    Context contextMock;
    @MockBean
    RbacUserRepository rbacUserRepository;

    @Test
    void willListAllUsers() throws Exception {

        // given
        when(rbacUserRepository.findByOptionalNameLike(null)).thenReturn(
            List.of(userAaa, userBbb));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                .get("/api/rbacusers")
                .header("current-user", "mike@hostsharing.net")
                .accept(MediaType.APPLICATION_JSON))

            // then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].name", is(userAaa.getName())))
            .andExpect(jsonPath("$[1].name", is(userBbb.getName())));
    }

    @Test
    void willListUsersByName() throws Exception {

        // given
        when(rbacUserRepository.findByOptionalNameLike("admin@aaa")).thenReturn(
            List.of(userAaa));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                .get("/api/rbacusers")
                .param("name", "admin@aaa")
                .header("current-user", "mike@hostsharing.net")
                .accept(MediaType.APPLICATION_JSON))

            // then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name", is(userAaa.getName())));
    }
}
