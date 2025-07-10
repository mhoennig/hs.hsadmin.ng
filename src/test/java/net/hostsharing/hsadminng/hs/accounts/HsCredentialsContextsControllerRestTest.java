package net.hostsharing.hsadminng.hs.accounts;

import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SynchronizationType;

@WebMvcTest(HsCredentialsContextsController.class)
@Import({ StrictMapper.class, JsonObjectMapperConfiguration.class, DisableSecurityConfig.class, MessageTranslator.class})
@ActiveProfiles("test")
class HsCredentialsContextsControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @Autowired
    @SuppressWarnings("unused") // not used in test, but in controller class
    StrictMapper mapper;

    @MockitoBean
    EntityManagerWrapper em;

    @MockitoBean
    EntityManagerFactory emf;

    @MockitoBean
    HsCredentialsContextRbacRepository loginContextRbacRepo;


    @TestConfiguration
    public static class TestConfig {

        @Bean
        public EntityManager entityManager() {
            return mock(EntityManager.class);
        }

    }

    @BeforeEach
    void init() {
        when(emf.createEntityManager()).thenReturn(em);
        when(emf.createEntityManager(any(Map.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class), any(Map.class))).thenReturn(em);
    }

    @Test
    void getListOfLoginContextsReturnsOkWithEmptyList() throws Exception {

        // given
        when(loginContextRbacRepo.findAll()).thenReturn(List.of(
                HsCredentialsContextRbacEntity.builder()
                        .uuid(UUID.randomUUID())
                        .type("HSADMIN")
                        .qualifier("prod")
                        .build(),
                HsCredentialsContextRbacEntity.builder()
                        .uuid(UUID.randomUUID())
                        .type("SSH")
                        .qualifier("prod")
                        .build()
        ));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                .get("/api/hs/accounts/contexts")
                .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$", lenientlyEquals("""
                                [
                                    {
                                      "type": "HSADMIN",
                                      "qualifier": "prod"
                                    },
                                    {
                                      "type": "SSH",
                                      "qualifier": "prod"
                                    }
                                ]
                                """
                )));
    }
}
