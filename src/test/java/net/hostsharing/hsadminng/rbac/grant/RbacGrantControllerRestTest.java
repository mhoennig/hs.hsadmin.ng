package net.hostsharing.hsadminng.rbac.grant;

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
import org.springframework.test.util.ReflectionTestUtils;
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

@WebMvcTest(RbacGrantController.class)
@Import({ StrictMapper.class,
          MessageTranslator.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class RbacGrantControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RbacGrantController controller;

    @MockitoBean
    Context contextMock;

    @MockitoBean
    RbacGrantRepository rbacGrantRepository;

    @MockitoBean
    EntityManagerWrapper emw;

    @MockitoBean
    EntityManager em;

    @MockitoBean
    EntityManagerFactory emf;

    @BeforeEach
    void beforeEach() {
        when(emf.createEntityManager()).thenReturn(em);
        when(emf.createEntityManager(any(Map.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class))).thenReturn(em);
        when(emf.createEntityManager(any(SynchronizationType.class), any(Map.class))).thenReturn(em);
        ReflectionTestUtils.setField(controller, "em", em);
    }

    @Test
    void getListOfSubjectGrantsReturnsAllGrants() throws Exception {
        // given
        val grantedRoleUuid = UUID.randomUUID();
        val granteeSubjectUuid = UUID.randomUUID();
        given(rbacGrantRepository.findAll()).willReturn(List.of(givenGrant(grantedRoleUuid, granteeSubjectUuid)));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/rbac/grants")
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .header("Hostsharing-Assumed-Roles", "rbac.global#global:ADMIN")
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].grantedRoleIdName", is("rbactest.package#pac00:ADMIN")))
                .andExpect(jsonPath("$[0].['grantedRole.uuid']", is(grantedRoleUuid.toString())))
                .andExpect(jsonPath("$[0].granteeSubjectName", is("some-user@example.org")))
                .andExpect(jsonPath("$[0].['granteeSubject.uuid']", is(granteeSubjectUuid.toString())))
                .andExpect(jsonPath("$[0].assumed", is(true)));

        // then
        verify(contextMock).assumeRoles("rbac.global#global:ADMIN");
    }

    @Test
    void getListOfGrantsByUuidReturnsNotFoundIfMissing() throws Exception {
        // given
        val grantedRoleUuid = UUID.randomUUID();
        val granteeSubjectUuid = UUID.randomUUID();

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/rbac/grants/{grantedRoleUuid}/{granteeSubjectUuid}", grantedRoleUuid, granteeSubjectUuid)
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isNotFound());
    }

    @Test
    void getListOfGrantsByUuidReturnsGrantIfFound() throws Exception {
        // given
        val grantedRoleUuid = UUID.randomUUID();
        val granteeSubjectUuid = UUID.randomUUID();
        given(rbacGrantRepository.findById(new RbacGrantId(granteeSubjectUuid, grantedRoleUuid)))
                .willReturn(givenGrant(grantedRoleUuid, granteeSubjectUuid));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/rbac/grants/{grantedRoleUuid}/{granteeSubjectUuid}", grantedRoleUuid, granteeSubjectUuid)
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("grantedRoleIdName", is("rbactest.package#pac00:ADMIN")))
                .andExpect(jsonPath("['grantedRole.uuid']", is(grantedRoleUuid.toString())))
                .andExpect(jsonPath("['granteeSubject.uuid']", is(granteeSubjectUuid.toString())));
    }

    @Test
    void postNewRoleGrantToSubjectSavesGrant() throws Exception {
        // given
        val grantedRoleUuid = UUID.randomUUID();
        val granteeSubjectUuid = UUID.randomUUID();
        given(rbacGrantRepository.save(any())).willAnswer(invocation -> invocation.<RbacGrantEntity>getArgument(0));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/rbac/grants")
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "grantedRole.uuid": "%s",
                                    "granteeSubject.uuid": "%s",
                                    "assumed": true
                                }
                                """.formatted(grantedRoleUuid, granteeSubjectUuid))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/rbac.yaml/grants/" + grantedRoleUuid))
                .andExpect(jsonPath("['grantedRole.uuid']", is(grantedRoleUuid.toString())))
                .andExpect(jsonPath("['granteeSubject.uuid']", is(granteeSubjectUuid.toString())))
                .andExpect(jsonPath("assumed", is(true)));

        // then
        verify(rbacGrantRepository).save(argThat(entity ->
                entity.getGrantedRoleUuid().equals(grantedRoleUuid) &&
                        entity.getGranteeSubjectUuid().equals(granteeSubjectUuid) &&
                        entity.isAssumed()));
    }

    @Test
    void deleteRoleGrantFromSubjectDeletesGrant() throws Exception {
        // given
        val grantedRoleUuid = UUID.randomUUID();
        val granteeSubjectUuid = UUID.randomUUID();

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/api/rbac/grants/{grantedRoleUuid}/{granteeSubjectUuid}", grantedRoleUuid, granteeSubjectUuid)
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isNoContent());

        // then
        verify(rbacGrantRepository).deleteByRbacGrantId(new RbacGrantId(granteeSubjectUuid, grantedRoleUuid));
    }

    private RbacGrantEntity givenGrant(final UUID grantedRoleUuid, final UUID granteeSubjectUuid) {
        return RbacGrantEntity.builder()
                .grantedByRoleIdName("rbac.global#global:ADMIN")
                .grantedByRoleUuid(UUID.randomUUID())
                .grantedRoleIdName("rbactest.package#pac00:ADMIN")
                .grantedRoleUuid(grantedRoleUuid)
                .granteeSubjectName("some-user@example.org")
                .granteeSubjectUuid(granteeSubjectUuid)
                .assumed(true)
                .build();
    }
}
