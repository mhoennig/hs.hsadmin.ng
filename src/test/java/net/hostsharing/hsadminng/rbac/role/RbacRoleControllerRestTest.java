package net.hostsharing.hsadminng.rbac.role;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SynchronizationType;
import java.util.Map;

import static java.util.Arrays.asList;
import static net.hostsharing.hsadminng.rbac.role.TestRbacRole.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RbacRoleController.class)
@Import({StrictMapper.class, DisableSecurityConfig.class})
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
class RbacRoleControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @MockitoBean
    RbacRoleRepository rbacRoleRepository;

    @MockitoBean
    EntityManagerWrapper em;

    @MockitoBean
    EntityManagerFactory emf;

    @BeforeEach
    void init() {
        when(emf.createEntityManager()).thenReturn(em);
        when(emf.createEntityManager(any(Map.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class), any(Map.class))).thenReturn(em);
    }

    @Test
    void apiCustomersWillReturnCustomersFromRepository() throws Exception {

        // given
        when(rbacRoleRepository.findAll()).thenReturn(
                asList(hostmasterRole, customerXxxOwner, customerXxxAdmin));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/rbac/roles")
                        .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].roleName", is("rbac.global#global:ADMIN")))
                .andExpect(jsonPath("$[1].roleName", is("rbactest.customer#xxx:OWNER")))
                .andExpect(jsonPath("$[2].roleName", is("rbactest.customer#xxx:ADMIN")))
                .andExpect(jsonPath("$[2].uuid", is(customerXxxAdmin.getUuid().toString())))
                .andExpect(jsonPath("$[2].['object.uuid']", is(customerXxxAdmin.getObjectUuid().toString())))
                .andExpect(jsonPath("$[2].objectTable", is(customerXxxAdmin.getObjectTable())))
                .andExpect(jsonPath("$[2].objectIdName", is(customerXxxAdmin.getObjectIdName())));
    }
}
