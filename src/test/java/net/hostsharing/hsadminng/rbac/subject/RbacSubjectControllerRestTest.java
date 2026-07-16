package net.hostsharing.hsadminng.rbac.subject;

import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.MessagesResourceConfig;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import net.hostsharing.hsadminng.errors.RestResponseEntityExceptionHandler;
import net.hostsharing.hsadminng.rbac.context.RbacTranslations;
import net.hostsharing.hsadminng.mapper.StrictBodyConverter;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacGroupSubjectInsertResource;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacUserSubjectInsertResource;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static net.hostsharing.hsadminng.rbac.test.IsValidUuidMatcher.isUuidValid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RbacSubjectController.class)
@Import({ StrictMapper.class,
          StrictBodyConverter.class,
          MessagesResourceConfig.class,
          MessageTranslator.class,
          RestResponseEntityExceptionHandler.class,
          RbacTranslations.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class RbacSubjectControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @MockitoBean
    RbacSubjectRepository rbacSubjectRepository;

    @MockitoBean
    RealSubjectRepository realSubjectRepository;

    @MockitoBean
    EntityManagerWrapper em;

    @BeforeEach
    void beforeEach() {
        given(rbacSubjectRepository.create(any())).willAnswer(invocation ->
            invocation.<RbacSubjectEntity>getArgument(0)
        );
        lenient().doCallRealMethod().when(contextMock).requireGlobalAdmin(anyString());
    }

    @Test
    void postNewSubjectUsesGivenUuid() throws Exception {
        // given
        val givenUuid = UUID.randomUUID();
        when(contextMock.isGlobalAdmin()).thenReturn(true);

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/rbac/subjects")
                        .header("Authorization", bearer("tst-drew_selfregistered"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "uuid": "%s",
                                    "name": "tst-somebody_new"
                                }
                                """.formatted(givenUuid))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/api/rbac/subjects/" + givenUuid))
            .andExpect(jsonPath("uuid", is(givenUuid.toString())))
                .andExpect(jsonPath("type", is("USER")));
        verify(rbacSubjectRepository).create(argThat(entity -> entity.getUuid().equals(givenUuid)));
    }

    @Test
    void postNewSubjectByNonGlobalAdminReturnsForbidden() throws Exception {
        // given
        when(contextMock.isGlobalAdmin()).thenReturn(false);

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/rbac/subjects")
                        .header("Authorization", bearer("tst-drew_selfregistered"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "tst-somebody_new"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isForbidden());
        verify(rbacSubjectRepository, never()).create(any());
    }

    @Test
    void postNewSubjectGeneratesRandomUuidIfNotGiven() throws Exception {
        // given
        when(contextMock.isGlobalAdmin()).thenReturn(true);

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/rbac/subjects")
                        .header("Authorization", bearer("tst-drew_selfregistered"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "tst-somebody_new"
                                }""")
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
            .andExpect(status().isCreated())
            .andExpect(jsonPath("uuid", isUuidValid()));
        verify(rbacSubjectRepository).create(argThat(entity -> entity.getUuid() != null));
    }

    @Test
    void getListOfSubjectsReturnsSubjectsFromRepository() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        when(realSubjectRepository.findVisibleSubjectsByOptionalNameLikeAndOptionalType(
                        "some-user",
                        null)
                .stream()
                .<Subject<?>>map(subject -> subject)
                .toList()).thenReturn(List.of(
                RbacSubjectEntity.builder()
                        .uuid(givenSubjectUuid)
                        .name("some-user@example.org")
                        .build()));

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/rbac/subjects?name=some-user")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .header("Hostsharing-Assumed-Roles", "rbac.global#global:ADMIN")
                        .accept(MediaType.APPLICATION_JSON));

        // then
        verify(contextMock).assumeRoles("rbac.global#global:ADMIN");
        result
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].uuid", is(givenSubjectUuid.toString())))
            .andExpect(jsonPath("$[0].name", is("some-user@example.org")));
    }

    @Test
    void getSingleSubjectByUuidReturnsNotFoundIfMissing() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        when(realSubjectRepository.findVisibleSubjectByUuid(givenSubjectUuid)
                .<Subject<?>>map(subject -> subject)).thenReturn(Optional.empty());

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/rbac/subjects/{subjectUuid}", givenSubjectUuid)
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isNotFound());
    }

    @Test
    void getSingleSubjectByUuidReturnsSubjectIfFound() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        when(realSubjectRepository.findVisibleSubjectByUuid(givenSubjectUuid)
                .<Subject<?>>map(subject -> subject)).thenReturn(
                Optional.of(RealSubjectEntity.builder()
                        .uuid(givenSubjectUuid)
                        .name("some-user@example.org")
                        .build()));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/rbac/subjects/{subjectUuid}", givenSubjectUuid)
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid", is(givenSubjectUuid.toString())))
                .andExpect(jsonPath("name", is("some-user@example.org")));
    }

    @Test
    void getListOfSubjectPermissionsReturnsPermissionsFromRepository() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        val givenRoleUuid = UUID.randomUUID();
        val givenPermissionUuid = UUID.randomUUID();
        val givenObjectUuid = UUID.randomUUID();
        when(rbacSubjectRepository.findPermissionsOfUserByUuid(givenSubjectUuid)).thenReturn(List.of(
                givenPermission(givenRoleUuid, givenPermissionUuid, givenObjectUuid)));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/rbac/subjects/{subjectUuid}/permissions", givenSubjectUuid)
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].['role.uuid']", is(givenRoleUuid.toString())))
                .andExpect(jsonPath("$[0].roleName", is("rbactest.package#pac00:ADMIN")))
                .andExpect(jsonPath("$[0].['permission.uuid']", is(givenPermissionUuid.toString())))
                .andExpect(jsonPath("$[0].op", is("SELECT")))
                .andExpect(jsonPath("$[0].objectTable", is("rbactest.package")))
                .andExpect(jsonPath("$[0].objectIdName", is("pac00")))
                .andExpect(jsonPath("$[0].['object.uuid']", is(givenObjectUuid.toString())));
    }

    @Test
    void deleteSubjectByUuidDeactivatesSubjectByDefault() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        when(contextMock.isGlobalAdmin()).thenReturn(true);

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/api/rbac/subjects/{subjectUuid}", givenSubjectUuid)
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isNoContent());

        // then the default (purge omitted) goes through the soft-delete/deactivation path
        verify(rbacSubjectRepository).deactivateByUuid(givenSubjectUuid);
        verify(rbacSubjectRepository, never()).deleteByUuid(any());
    }

    @Test
    void deleteSubjectByUuidByNonGlobalAdminReturnsForbidden() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        when(contextMock.isGlobalAdmin()).thenReturn(false);

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/api/rbac/subjects/{subjectUuid}", givenSubjectUuid)
                        .header("Authorization", bearer("tst-drew_selfregistered"))
                        .accept(MediaType.APPLICATION_JSON))

                // then the whole delete operation is restricted to global-admins
                .andExpect(status().isForbidden());

        // then neither delete path is taken
        verify(rbacSubjectRepository, never()).deactivateByUuid(any());
        verify(rbacSubjectRepository, never()).deleteByUuid(any());
    }

    @Test
    void deleteSubjectByUuidWithPurgeTrueAsGlobalAdminPhysicallyDeletesSubject() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        when(contextMock.isGlobalAdmin()).thenReturn(true);

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/api/rbac/subjects/{subjectUuid}", givenSubjectUuid)
                        .param("purge", "true")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isNoContent());

        // then the physical-delete path is used and the soft-delete/deactivation path is not
        verify(rbacSubjectRepository).deleteByUuid(givenSubjectUuid);
        verify(rbacSubjectRepository, never()).deactivateByUuid(any());
    }

    @Test
    void deleteSubjectByUuidWithPurgeTrueAsNonGlobalAdminReturnsForbidden() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        when(contextMock.isGlobalAdmin()).thenReturn(false);

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/api/rbac/subjects/{subjectUuid}", givenSubjectUuid)
                        .param("purge", "true")
                        .header("Authorization", bearer("tst-drew_selfregistered"))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isForbidden());

        // then neither delete path is taken
        verify(rbacSubjectRepository, never()).deleteByUuid(any());
        verify(rbacSubjectRepository, never()).deactivateByUuid(any());
    }

    @Test
    void putCreatesNewUserSubjectReturnsCreated() throws Exception {
        // given
        val givenUuid = UUID.randomUUID();
        when(contextMock.isGlobalAdmin()).thenReturn(true);
        when(rbacSubjectRepository.upsert(givenUuid, "xyz-alice", "USER")).thenReturn("created");

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .put("/api/rbac/subjects/{subjectUuid}", givenUuid)
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "xyz-alice"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/rbac/subjects/" + givenUuid))
                .andExpect(jsonPath("uuid", is(givenUuid.toString())))
                .andExpect(jsonPath("name", is("xyz-alice")))
                .andExpect(jsonPath("type", is("USER")));
        verify(rbacSubjectRepository).upsert(givenUuid, "xyz-alice", "USER");
    }

    @Test
    void putCreatesNewGroupSubjectReturnsCreated() throws Exception {
        // given
        val givenUuid = UUID.randomUUID();
        when(contextMock.isGlobalAdmin()).thenReturn(true);
        when(rbacSubjectRepository.upsert(givenUuid, "/xyz-Team", "GROUP")).thenReturn("created");

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .put("/api/rbac/subjects/{subjectUuid}", givenUuid)
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "/xyz-Team",
                                    "type": "GROUP"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isCreated())
                .andExpect(jsonPath("type", is("GROUP")));
    }

    @Test
    void putUpdatesExistingSubjectReturnsOk() throws Exception {
        // given
        val givenUuid = UUID.randomUUID();
        when(contextMock.isGlobalAdmin()).thenReturn(true);
        when(rbacSubjectRepository.upsert(givenUuid, "xyz-alicia", "USER")).thenReturn("updated");

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .put("/api/rbac/subjects/{subjectUuid}", givenUuid)
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "xyz-alicia"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid", is(givenUuid.toString())))
                .andExpect(jsonPath("name", is("xyz-alicia")));
    }

    @Test
    void putByNonGlobalAdminReturnsForbidden() throws Exception {
        // given
        val givenUuid = UUID.randomUUID();
        when(contextMock.isGlobalAdmin()).thenReturn(false);

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .put("/api/rbac/subjects/{subjectUuid}", givenUuid)
                        .header("Authorization", bearer("tst-drew_selfregistered"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "xyz-alice"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isForbidden());
        verify(rbacSubjectRepository, never()).upsert(any(), any(), any());
    }

    @Test
    void putWithMismatchedBodyUuidReturnsBadRequest() throws Exception {
        // given
        val pathUuid = UUID.randomUUID();
        val otherUuid = UUID.randomUUID();
        when(contextMock.isGlobalAdmin()).thenReturn(true);

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .put("/api/rbac/subjects/{subjectUuid}", pathUuid)
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "uuid": "%s",
                                    "name": "xyz-alice"
                                }
                                """.formatted(otherUuid))
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isBadRequest());
        verify(rbacSubjectRepository, never()).upsert(any(), any(), any());
    }

    private RbacSubjectPermission givenPermission(
            final UUID roleUuid,
            final UUID permissionUuid,
            final UUID objectUuid) {
        return new RbacSubjectPermission() {
            @Override
            public UUID getRoleUuid() {
                return roleUuid;
            }

            @Override
            public String getRoleName() {
                return "rbactest.package#pac00:ADMIN";
            }

            @Override
            public UUID getPermissionUuid() {
                return permissionUuid;
            }

            @Override
            public String getOp() {
                return "SELECT";
            }

            @Override
            public String getOpTableName() {
                return "rbactest.package";
            }

            @Override
            public String getObjectTable() {
                return "rbactest.package";
            }

            @Override
            public String getObjectIdName() {
                return "pac00";
            }

            @Override
            public UUID getObjectUuid() {
                return objectUuid;
            }
        };
    }

    @Test
    void getListOfSubjectsFiltersByType() throws Exception {
        // given
        final var givenUuid = UUID.randomUUID();
        given(realSubjectRepository.findVisibleSubjectsByOptionalNameLikeAndOptionalType(
                        null,
                        SubjectType.GROUP)
                .stream()
                .<Subject<?>>map(subject1 -> subject1)
                .toList())
                .willReturn(List.of(RbacSubjectEntity.builder()
                        .uuid(givenUuid)
                        .name("/xyz-Team")
                        .type(SubjectType.GROUP)
                        .build()));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/rbac/subjects?type=GROUP")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("[0].uuid", is(givenUuid.toString())))
                .andExpect(jsonPath("[0].name", is("/xyz-Team")))
                .andExpect(jsonPath("[0].type", is("GROUP")));

        // then
        Mockito.verify(realSubjectRepository).findVisibleSubjectsByOptionalNameLikeAndOptionalType(
                        (String) null,
                        SubjectType.GROUP);
    }

    @Test
    void getListOfSubjectsDelegatesGroupVisibilityToService() throws Exception {
        // given
        final var givenUuid = UUID.randomUUID();
        given(realSubjectRepository.findVisibleSubjectsByOptionalNameLikeAndOptionalType(
                        "/xyz-Team",
                        SubjectType.GROUP)
                .stream()
                .<Subject<?>>map(subject1 -> subject1)
                .toList())
                .willReturn(List.of(RealSubjectEntity.builder()
                        .uuid(givenUuid)
                        .name("/xyz-Team")
                        .type(SubjectType.GROUP)
                        .build()));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/rbac/subjects?name=/xyz-Team&type=GROUP")
                        .header("Authorization", bearer(
                                "tst-person_firbysusan",
                                List.of("/xyz-Team")))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("[0].uuid", is(givenUuid.toString())))
                .andExpect(jsonPath("[0].name", is("/xyz-Team")))
                .andExpect(jsonPath("[0].type", is("GROUP")));

        // then
        Mockito.verify(realSubjectRepository).findVisibleSubjectsByOptionalNameLikeAndOptionalType(
                        "/xyz-Team",
                        SubjectType.GROUP);
    }

    @Test
    void postNewUserSubjectWithValidNameSucceeds() throws Exception {
        // given
        when(contextMock.isGlobalAdmin()).thenReturn(true);

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/rbac/subjects")
                        .header("Authorization", bearer("tst-drew_selfregistered"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "tst-valid_user"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isCreated());
    }

    @Test
    void postNewUserSubjectWithInvalidNameReturnsBadRequest() throws Exception {
        // given
        when(contextMock.isGlobalAdmin()).thenReturn(true);

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/rbac/subjects")
                        .header("Authorization", bearer("tst-drew_selfregistered"))
                        .header("Accept-Language", "de")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "invaliduser@example.com"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString(
                        "USER-Subjekt 'invaliduser@example.com' entspricht nicht dem erforderlichen Muster")));
        verify(rbacSubjectRepository, never()).create(any());
    }

    @Test
    void postNewUserSubjectWithGroupNameReturnsBadRequest() throws Exception {
        // given
        when(contextMock.isGlobalAdmin()).thenReturn(true);

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/rbac/subjects")
                        .header("Authorization", bearer("tst-drew_selfregistered"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "/xyz-Team",
                                    "type": "USER"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isBadRequest());
        verify(rbacSubjectRepository, never()).create(any());
    }

    @Test
    void postNewGroupSubjectWithNonUserPatternNameSucceeds() throws Exception {
        // given
        when(contextMock.isGlobalAdmin()).thenReturn(true);

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/rbac/subjects")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "/xyz-Team",
                                    "type": "GROUP"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isCreated());
    }

    @Test
    void postNewGroupSubjectWithUserdNameReturnsBadRequest() throws Exception {
        // given
        when(contextMock.isGlobalAdmin()).thenReturn(true);

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/rbac/subjects")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "xyz-Team",
                                    "type": "GROUP"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString(
                        "GROUP subject name 'xyz-Team' does not match required pattern")));
        verify(rbacSubjectRepository, never()).create(any());
    }

    @Test
    void postNewSubjectWithoutNameReturnsBadRequest() throws Exception {
        // given
        when(contextMock.isGlobalAdmin()).thenReturn(true);

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/rbac/subjects")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "type": "USER"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString("name")));
        verify(rbacSubjectRepository, never()).create(any());
    }

    @Test
    void postNewSubjectWithUnknownPropertyReturnsBadRequest() throws Exception {
        // given
        when(contextMock.isGlobalAdmin()).thenReturn(true);

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/rbac/subjects")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "tst-somebody_new",
                                    "admin": true
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString("admin")));
        verify(rbacSubjectRepository, never()).create(any());
    }

    @Test
    void postNewSubjectWithInvalidTypeReturnsBadRequest() throws Exception {
        // given
        when(contextMock.isGlobalAdmin()).thenReturn(true);

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/rbac/subjects")
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "tst-somebody_new",
                                    "type": "ROBOT"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isBadRequest());
        verify(rbacSubjectRepository, never()).create(any());
    }

    @Test
    void putSubjectWithInvalidUserNameReturnsBadRequest() throws Exception {
        // given
        val givenUuid = UUID.randomUUID();
        when(contextMock.isGlobalAdmin()).thenReturn(true);

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .put("/api/rbac/subjects/{subjectUuid}", givenUuid)
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "invaliduser@example.com"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString(
                        "USER subject name 'invaliduser@example.com' does not match required pattern")));
        verify(rbacSubjectRepository, never()).upsert(any(), any(), any());
    }

    @Test
    void putGroupNameWithoutTypeReturnsBadRequest() throws Exception {
        // given a group-style name but no type, which defaults to USER and thus fails the USER name pattern
        val givenUuid = UUID.randomUUID();
        when(contextMock.isGlobalAdmin()).thenReturn(true);

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .put("/api/rbac/subjects/{subjectUuid}", givenUuid)
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "/xyz-Team"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message", containsString(
                        "USER subject name '/xyz-Team' does not match required pattern")));
        verify(rbacSubjectRepository, never()).upsert(any(), any(), any());
    }

    @Test
    void openApiUserSubjectNamePatternMatchesGeneratedValidationAndDatabaseFunction() throws Exception {
        // given
        final Map<String, Object> yaml = new Yaml().load(Files.newInputStream(
                Path.of("src/main/resources/api-definition/rbac/rbac-subject-schemas.yaml")));
        final var schemas = map(map(yaml.get("components")).get("schemas"));
        final var anyOf = (List<Map<String, String>>) map(schemas.get("RbacSubjectInsert")).get("anyOf");
        final var userInsertNameSchema = map(map(map(schemas.get("RbacUserSubjectInsert"))
                .get("properties"))
                .get("name"));
        final var groupInsertNameSchema = map(map(map(schemas.get("RbacGroupSubjectInsert"))
                .get("properties"))
                .get("name"));
        final var userPattern = (String) userInsertNameSchema.get("pattern");
        final var groupPattern = (String) groupInsertNameSchema.get("pattern");

        final var sql = Files.readString(Path.of("src/main/resources/db/changelog/1-rbac/1050-rbac-base.sql"));
        final var sqlUserPattern = functionPattern(sql, "is_valid_user_subject_name");
        final var sqlGroupPattern = functionPattern(sql, "is_valid_group_subject_name");

        // then
        assertThat(anyOf).extracting(ref -> ref.get("$ref"))
                .containsExactly(
                        "#/components/schemas/RbacUserSubjectInsert",
                        "#/components/schemas/RbacGroupSubjectInsert");
        assertThat(userInsertNameSchema).doesNotContainKey("x-user-subject-name-pattern");
        assertThat(RbacUserSubjectInsertResource.class.getDeclaredField("name")
                .getAnnotation(jakarta.validation.constraints.Pattern.class)
                .regexp()).isEqualTo(userPattern);
        assertThat(RbacGroupSubjectInsertResource.class.getDeclaredField("name")
                .getAnnotation(jakarta.validation.constraints.Pattern.class)
                .regexp()).isEqualTo(groupPattern);
        assertThat(sqlUserPattern).isEqualTo(userPattern);
        assertThat(sqlGroupPattern).isEqualTo(groupPattern);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(final Object yamlNode) {
        return (Map<String, Object>) yamlNode;
    }

    private static String functionPattern(final String sql, final String functionName) {
        final var matcher = Pattern.compile(
                "create or replace function rbac\\." + functionName + "\\(subjectName varchar\\).*?subjectName ~ '([^']+)'",
                Pattern.DOTALL).matcher(sql);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
