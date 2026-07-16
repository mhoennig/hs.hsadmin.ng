package net.hostsharing.hsadminng.rbac.context;

import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;

import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.role.RbacRoleEntity;
import net.hostsharing.hsadminng.rbac.role.RbacRoleRepository;
import net.hostsharing.hsadminng.rbac.role.RbacRoleType;
import net.hostsharing.hsadminng.rbac.subject.RealSubjectEntity;
import net.hostsharing.hsadminng.rbac.subject.RealSubjectRepository;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectRepository;
import net.hostsharing.hsadminng.rbac.subject.SubjectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Stream;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RbacContextController.class)
@Import({ StrictMapper.class,
          MessageTranslator.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class RbacContextControllerRestTest {

    private static final String GIVEN_SUBJECT_NAME = "hsh-alex_superuser";
    private static final SubjectType GIVEN_SUBJECT_TYPE = SubjectType.USER;
    private static final boolean GIVEN_GLOBAL_ADMIN = true;
    private static final String GIVEN_ASSUMED_ROLES = "rbactest.package#xxx00:OWNER;rbactest.package#yyy00:OWNER";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @MockitoBean
    RbacRoleRepository rbacRoleRepository;

    @MockitoBean
    RbacSubjectRepository rbacSubjectRepository;

    @MockitoBean
    RealSubjectRepository realSubjectRepository;

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
        final var fakeSubject = new RealSubjectEntity();
        fakeSubject.setUuid(mockUuid);
        fakeSubject.setName(GIVEN_SUBJECT_NAME);
        fakeSubject.setType(GIVEN_SUBJECT_TYPE);
        when(realSubjectRepository.findCurrentSubject()).thenReturn(fakeSubject);

        // claimed groups stem from the JWT and may include groups not (yet) synchronized as effective subjects
        when(contextMock.fetchClaimedSubjectGroupNames()).thenReturn(List.of("/xyz-Team", "/abc-External"));

        when(realSubjectRepository.findEffectiveSubjectGroups()).thenReturn(List.of(
                givenGroupSubject("/xyz-Team"),
                givenGroupSubject("/xyz-Service")));
    }

    @ParameterizedTest
    @MethodSource("validAssumedRolesHeaderCombinations")
    void apiContextWillReturnCurrentContext(
            final String assumedRoles,
            final String assumedRolesDeprecated,
            final String expectedAssumedRoles) throws Exception {

        // given
        when(contextMock.isGlobalAdmin()).thenReturn(GIVEN_GLOBAL_ADMIN);
        final var givenAssumedRoles = Arrays.stream(GIVEN_ASSUMED_ROLES.split(";"))
                .map(RbacRoleDescriptor::fromRoleName)
                .map(roleDesc -> {
                    final var objectUuid = UUID.randomUUID();
                    return new RbacRoleEntity(
                            UUID.randomUUID(), objectUuid,
                            roleDesc.tableName, roleDesc.objectIdName, roleDesc.roleType,
                            roleDesc.tableName + "#" + objectUuid + ":" + roleDesc.roleType,
                            roleDesc.roleName);
                })
                .toList();
        when(rbacRoleRepository.fetchAssumedRoles()).thenReturn(givenAssumedRoles);

        // when
        final var request = MockMvcRequestBuilders
                        .get("/api/rbac/context")
                        .header("Authorization", bearer(GIVEN_SUBJECT_NAME))
                        .accept(MediaType.APPLICATION_JSON);
        if (assumedRoles != null) {
            request.header("Hostsharing-Assumed-Roles", assumedRoles);
        }
        if (assumedRolesDeprecated != null) {
            request.header("assumed-roles", assumedRolesDeprecated);
        }

        mockMvc.perform(request)
                .andDo(print())

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.name", is(GIVEN_SUBJECT_NAME)))
                .andExpect(jsonPath("$.subject.type", is(GIVEN_SUBJECT_TYPE.name())))
                .andExpect(jsonPath("$.globalAdmin", is(GIVEN_GLOBAL_ADMIN)))
                .andExpect(jsonPath("$.claimedGroups", hasSize(2)))
                .andExpect(jsonPath("$.claimedGroups[0]", is("/xyz-Team")))
                .andExpect(jsonPath("$.claimedGroups[1]", is("/abc-External")))
                .andExpect(jsonPath("$.effectiveGroups", hasSize(2)))
                .andExpect(jsonPath("$.effectiveGroups[0].name", is("/xyz-Team")))
                .andExpect(jsonPath("$.effectiveGroups[0].type").doesNotExist())
                .andExpect(jsonPath("$.effectiveGroups[1].name", is("/xyz-Service")))
                .andExpect(jsonPath("$.effectiveGroups[1].type").doesNotExist())
                .andExpect(jsonPath("$.assumedRoles", hasSize(2)))
                .andExpect(jsonPath("$.assumedRoles[0].roleName", is(givenAssumedRoles.get(0).getRoleName())))
                .andExpect(jsonPath("$.assumedRoles[0].roleIdName", is("rbactest.package#xxx00:OWNER")))
                .andExpect(jsonPath("$.assumedRoles[1].roleName", is(givenAssumedRoles.get(1).getRoleName())))
                .andExpect(jsonPath("$.assumedRoles[1].roleIdName", is("rbactest.package#yyy00:OWNER")));

        verify(contextMock).assumeRoles(expectedAssumedRoles);
    }

    private static RealSubjectEntity givenGroupSubject(final String name) {
        final var subject = new RealSubjectEntity();
        subject.setUuid(UUID.randomUUID());
        subject.setName(name);
        subject.setType(SubjectType.GROUP);
        return subject;
    }

    static Stream<Arguments> validAssumedRolesHeaderCombinations() {
        return Stream.of(
                Arguments.of(null, null, null),
                Arguments.of(GIVEN_ASSUMED_ROLES, null, GIVEN_ASSUMED_ROLES),
                Arguments.of(null, GIVEN_ASSUMED_ROLES, GIVEN_ASSUMED_ROLES),
                Arguments.of(GIVEN_ASSUMED_ROLES, GIVEN_ASSUMED_ROLES, GIVEN_ASSUMED_ROLES)
        );
    }

    @Test
    void apiContextWillRespondNotFoundIfAuthenticatedSubjectDoesNotExist() throws Exception {

        // given
        final var unknownSubjectUuid = UUID.randomUUID();
        doThrow(new NoSuchElementException("cannot find Subject by uuid: " + unknownSubjectUuid))
                .when(contextMock).define();

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/rbac/context")
                        .header("Authorization", bearer(GIVEN_SUBJECT_NAME))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())

                // then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(
                        "ERROR: [404] cannot find Subject by uuid: " + unknownSubjectUuid)));
    }

    @ParameterizedTest
    @MethodSource("conflictingAssumedRolesHeaderCombinations")
    void apiContextWillRespondBadRequestForConflictingAssumedRolesHeaders(
            final String assumedRoles,
            final String assumedRolesDeprecated) throws Exception {

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/rbac/context")
                        .header("Authorization", bearer(GIVEN_SUBJECT_NAME))
                        .header("Hostsharing-Assumed-Roles", assumedRoles)
                        .header("assumed-roles", assumedRolesDeprecated)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())

                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(
                        "ERROR: [400] headers 'Hostsharing-Assumed-Roles' and 'assumed-roles' must either match or only one may be used")));

        verify(contextMock, never()).assumeRoles(any());
    }

    static Stream<Arguments> conflictingAssumedRolesHeaderCombinations() {
        return Stream.of(
                Arguments.of(GIVEN_ASSUMED_ROLES, "rbactest.package#zzz00:OWNER")
        );
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
