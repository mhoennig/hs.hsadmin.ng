package net.hostsharing.hsadminng.rbac.subject;

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

import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static net.hostsharing.hsadminng.rbac.test.IsValidUuidMatcher.isUuidValid;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RbacSubjectController.class)
@Import({ StrictMapper.class,
          MessageTranslator.class,
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
    RbacSubjectListService rbacSubjectListService;

    @MockitoBean
    EntityManagerWrapper em;

    @BeforeEach
    void beforeEach() {
        given(rbacSubjectRepository.create(any())).willAnswer(invocation ->
            invocation.<RbacSubjectEntity>getArgument(0)
        );
    }

    @Test
    void postNewSubjectUsesGivenUuid() throws Exception {
        // given
        val givenUuid = UUID.randomUUID();

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/rbac/subjects")
                        .header("Authorization", bearer("selfregistered-user-drew@hostsharing.org"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "uuid": "%s"
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
    void postNewSubjectGeneratesRandomUuidIfNotGiven() throws Exception {
        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/rbac/subjects")
                        .header("Authorization", bearer("selfregistered-user-drew@hostsharing.org"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
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
        when(rbacSubjectListService.findByOptionalNameLikeAndOptionalType("some-user", null)).thenReturn(List.of(
                RbacSubjectEntity.builder()
                        .uuid(givenSubjectUuid)
                        .name("some-user@example.org")
                        .build()));

        // when
        val result = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/rbac/subjects?name=some-user")
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
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

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/rbac/subjects/{subjectUuid}", givenSubjectUuid)
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isNotFound());
    }

    @Test
    void getSingleSubjectByUuidReturnsSubjectIfFound() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();
        when(rbacSubjectRepository.findByUuid(givenSubjectUuid)).thenReturn(
                RbacSubjectEntity.builder()
                        .uuid(givenSubjectUuid)
                        .name("some-user@example.org")
                        .build());

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/rbac/subjects/{subjectUuid}", givenSubjectUuid)
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
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
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
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
    void deleteSubjectByUuidDeletesSubject() throws Exception {
        // given
        val givenSubjectUuid = UUID.randomUUID();

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/api/rbac/subjects/{subjectUuid}", givenSubjectUuid)
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isNoContent());

        // then
        verify(rbacSubjectRepository).deleteByUuid(givenSubjectUuid);
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
        given(rbacSubjectListService.findByOptionalNameLikeAndOptionalType(null, SubjectType.GROUP))
                .willReturn(List.of(RbacSubjectEntity.builder()
                        .uuid(givenUuid)
                        .name("/xyz-Team")
                        .type(SubjectType.GROUP)
                        .build()));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/rbac/subjects?type=GROUP")
                        .header("Authorization", bearer("superuser-alex@hostsharing.net"))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("[0].uuid", is(givenUuid.toString())))
                .andExpect(jsonPath("[0].name", is("/xyz-Team")))
                .andExpect(jsonPath("[0].type", is("GROUP")));

        // then
        verify(rbacSubjectListService).findByOptionalNameLikeAndOptionalType(null, SubjectType.GROUP);
    }

    @Test
    void getListOfSubjectsDelegatesGroupVisibilityToService() throws Exception {
        // given
        final var givenUuid = UUID.randomUUID();
        given(rbacSubjectListService.findByOptionalNameLikeAndOptionalType(
                "/xyz-Team",
                SubjectType.GROUP))
                .willReturn(List.of(RealSubjectEntity.builder()
                        .uuid(givenUuid)
                        .name("/xyz-Team")
                        .type(SubjectType.GROUP)
                        .build()));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/rbac/subjects?name=/xyz-Team&type=GROUP")
                        .header("Authorization", bearer(
                                "person-FirbySusan@example.com",
                                List.of("/xyz-Team")))
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("[0].uuid", is(givenUuid.toString())))
                .andExpect(jsonPath("[0].name", is("/xyz-Team")))
                .andExpect(jsonPath("[0].type", is("GROUP")));

        // then
        verify(rbacSubjectListService).findByOptionalNameLikeAndOptionalType(
                "/xyz-Team",
                SubjectType.GROUP);
    }
}
