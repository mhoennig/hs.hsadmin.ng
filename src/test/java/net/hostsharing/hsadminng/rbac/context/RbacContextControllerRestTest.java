package net.hostsharing.hsadminng.rbac.context;

import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.role.RbacRoleEntity;
import net.hostsharing.hsadminng.rbac.role.RbacRoleRepository;
import net.hostsharing.hsadminng.rbac.role.RbacRoleType;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectRepository;
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

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SynchronizationType;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RbacContextController.class)
@Import({ StrictMapper.class, DisableSecurityConfig.class, MessageTranslator.class })
@ActiveProfiles("test")
class RbacContextControllerRestTest {

    private static final String GIVEN_SUBJECT_NAME = "superuser-alex@hostsharing.net";
    private static final boolean GIVEN_GLOBAL_ADMIN = true;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @MockitoBean
    RbacRoleRepository rbacRoleRepository;

    @MockitoBean
    RbacSubjectRepository rbacSubjectRepository;

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

        // current subject uuid mock
        final var mockUuid = UUID.randomUUID();
        when(contextMock.fetchCurrentSubjectUuid()).thenReturn(mockUuid);

        // find by uuid mock
        final var mockSubject = new RbacSubjectEntity();
        mockSubject.setUuid(mockUuid);
        mockSubject.setName(GIVEN_SUBJECT_NAME);
        when(rbacSubjectRepository.findByUuid(mockUuid)).thenReturn(mockSubject);
    }

    @Test
    void apiContextWillReturnCurrentContext() throws Exception {

        // given
        final var rolesToAssume = "rbactest.package#xxx00:OWNER;rbactest.package#yyy00:OWNER";
        when(contextMock.isGlobalAdmin()).thenReturn(GIVEN_GLOBAL_ADMIN);
        when(rbacRoleRepository.fetchAssumedRoles()).thenReturn(
                Arrays.stream(rolesToAssume.split(";"))
                        .map(RbacRoleDescriptor::fromRoleName)
                        .map(roleDesc -> new RbacRoleEntity(
                                UUID.randomUUID(), UUID.randomUUID(),
                                roleDesc.tableName, roleDesc.objectIdName, roleDesc.roleType,
                                roleDesc.roleName))
                        .toList()
        );

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/rbac/context")
                        .header("Authorization", "Bearer " + GIVEN_SUBJECT_NAME)
                        .header("assumed-roles", rolesToAssume)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.name", is(GIVEN_SUBJECT_NAME)))
                .andExpect(jsonPath("$.globalAdmin", is(GIVEN_GLOBAL_ADMIN)))
                .andExpect(jsonPath("$.assumedRoles", hasSize(2)))
                .andExpect(jsonPath("$.assumedRoles[0].roleName", is("rbactest.package#xxx00:OWNER")))
                .andExpect(jsonPath("$.assumedRoles[1].roleName", is("rbactest.package#yyy00:OWNER")));
    }

    record RbacRoleDescriptor(String roleName, String tableName, String objectIdName, RbacRoleType roleType) {

        private static RbacRoleDescriptor fromRoleName(final String roleName) {
            final var tablePlus = roleName.split("#");
            final var tableName = tablePlus[0];
            final var objectId = tablePlus[1].split(":")[0];
            final var roleType = RbacRoleType.valueOf(tablePlus[1].split(":")[1]);
            return new RbacRoleDescriptor(roleName, tableName, objectId, roleType);
        }
    }
}
