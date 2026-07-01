package net.hostsharing.hsadminng.rbac.test.cust;

import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.context.Context;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SynchronizationType;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestCustomerController.class)
@Import({ StrictMapper.class,
          MessageTranslator.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class TestCustomerControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @MockitoBean
    TestCustomerRepository testCustomerRepository;

    @MockitoBean
    EntityManagerWrapper em;

    @MockitoBean
    EntityManager entityManager;

    @MockitoBean
    EntityManagerFactory emf;

    @BeforeEach
    void beforeEach() {
        when(emf.createEntityManager()).thenReturn(entityManager);
        when(emf.createEntityManager(any(Map.class))).thenReturn(entityManager);
        when(emf.createEntityManager(any(SynchronizationType.class))).thenReturn(entityManager);
        when(emf.createEntityManager(any(SynchronizationType.class), any(Map.class))).thenReturn(entityManager);
    }

    @Test
    void listCustomersReturnsCustomersFromRepository() throws Exception {
        // given
        val customerUuid = UUID.randomUUID();
        given(testCustomerRepository.findCustomerByOptionalPrefixLike("xxx")).willReturn(List.of(
                new TestCustomerEntity(customerUuid, 0, "xxx", 10001, "admin@example.org")));

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/test/customers?prefix=xxx")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .header("Hostsharing-Assumed-Roles", "rbac.global#global:ADMIN")
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).assumeRoles("rbac.global#global:ADMIN");
        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].uuid", is(customerUuid.toString())))
            .andExpect(jsonPath("$[0].prefix", is("xxx")))
            .andExpect(jsonPath("$[0].reference", is(10001)))
            .andExpect(jsonPath("$[0].adminUserName", is("admin@example.org")));

    }

    @Test
    void addCustomerCreatesCustomer() throws Exception {
        // given
        val customerUuid = UUID.randomUUID();
        given(testCustomerRepository.save(any())).willAnswer(invocation -> {
            val entity = invocation.<TestCustomerEntity>getArgument(0);
            entity.setUuid(customerUuid);
            return entity;
        });

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/test/customers")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "prefix": "xxx",
                                    "reference": 10001,
                                    "adminUserName": "admin@example.org"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/api/test/customers/" + customerUuid))
            .andExpect(jsonPath("uuid", is(customerUuid.toString())))
            .andExpect(jsonPath("prefix", is("xxx")))
            .andExpect(jsonPath("reference", is(10001)))
            .andExpect(jsonPath("adminUserName", is("admin@example.org")));
        verify(testCustomerRepository).save(argThat(entity ->
                entity.getPrefix().equals("xxx") &&
                        entity.getReference() == 10001 &&
                        entity.getAdminUserName().equals("admin@example.org")));
    }
}
