package net.hostsharing.hsadminng.rbac.grant;

import net.hostsharing.hsadminng.rbac.context.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.grant.RbacGrantsDiagramService.Include.DETAILS;
import static net.hostsharing.hsadminng.rbac.grant.RbacGrantsDiagramService.Include.NOT_ASSUMED;
import static net.hostsharing.hsadminng.rbac.grant.RbacGrantsDiagramService.Include.PERMISSIONS;
import static net.hostsharing.hsadminng.rbac.grant.RbacGrantsDiagramService.Include.TEST_ENTITIES;
import static net.hostsharing.hsadminng.rbac.grant.RbacGrantsDiagramService.Include.USERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RbacGrantsDiagramServiceUnitTest {

    private static final UUID SUBJECT_UUID = UUID.fromString("10000000-0000-0000-0000-000000000000");
    private static final UUID OWNER_ROLE_UUID = UUID.fromString("20000000-0000-0000-0000-000000000000");
    private static final UUID ADMIN_ROLE_UUID = UUID.fromString("30000000-0000-0000-0000-000000000000");
    private static final UUID PERMISSION_UUID = UUID.fromString("40000000-0000-0000-0000-000000000000");
    private static final UUID TARGET_OBJECT_UUID = UUID.fromString("50000000-0000-0000-0000-000000000000");
    private static final UUID TENANT_ROLE_UUID = UUID.fromString("60000000-0000-0000-0000-000000000000");
    private static final UUID NON_TEST_ROLE_UUID = UUID.fromString("70000000-0000-0000-0000-000000000000");
    private static final UUID GRANTING_ROLE_UUID = UUID.fromString("80000000-0000-0000-0000-000000000000");

    @TempDir
    private Path tempDir;

    @Mock
    private Context context;

    @Mock
    private RawRbacGrantRepository rawGrantRepo;

    @Mock
    private EntityManager em;

    @Mock
    private Query query;

    @InjectMocks
    private RbacGrantsDiagramService rbacGrantsDiagramService;

    @Test
    void rendersCurrentSubjectGrantGraphWithoutPermissions() {

        // given
        given(context.fetchCurrentSubjectOrAssumedRolesUuids()).willReturn(new UUID[] { SUBJECT_UUID });
        given(rawGrantRepo.findByAscendingUuid(SUBJECT_UUID)).willReturn(List.of(
                as("role:rbac.global#global:ADMIN", GRANTING_ROLE_UUID)
                        .role("role:rbactest.customer#xxx:OWNER", OWNER_ROLE_UUID)
                        .wasGrantedTo("user:alice@example.org", SUBJECT_UUID)
                        .assumed(),
                as("role:rbac.global#global:ADMIN", GRANTING_ROLE_UUID)
                        .role("perm:rbactest.customer#xxx:SELECT", PERMISSION_UUID)
                        .wasGrantedTo("user:alice@example.org", SUBJECT_UUID)
                        .assumed()));
        given(rawGrantRepo.findByAscendingUuid(OWNER_ROLE_UUID)).willReturn(List.of(
                as("role:rbac.global#global:ADMIN", GRANTING_ROLE_UUID)
                        .role("role:rbactest.customer#xxx:ADMIN", ADMIN_ROLE_UUID)
                        .wasGrantedTo("role:rbactest.customer#xxx:OWNER", OWNER_ROLE_UUID)
                        .assumed(),
                as("role:rbac.global#global:ADMIN", GRANTING_ROLE_UUID)
                        .role("role:hs_office.person#p-123:OWNER", NON_TEST_ROLE_UUID)
                        .wasGrantedTo("role:rbactest.customer#xxx:OWNER", OWNER_ROLE_UUID)
                        .assumed()));
        given(rawGrantRepo.findByAscendingUuid(ADMIN_ROLE_UUID)).willReturn(List.of());

        // when
        final var graph = rbacGrantsDiagramService.allGrantsToCurrentSubject(EnumSet.of(TEST_ENTITIES));

        // then
        assertThat(graph).isEqualTo("""
                flowchart TB
                
                role:rbactest.customer#xxx:OWNER --> role:rbactest.customer#xxx:ADMIN
                user:alice --> role:rbactest.customer#xxx:OWNER""");
    }

    @Test
    void rendersGrantGraphFromTargetObjectPermission() {

        // given
        given(em.createNativeQuery(
                "select uuid from rbac.permission where objectUuid=:targetObject and op=:op",
                List.class)).willReturn(query);
        given(query.setParameter("targetObject", TARGET_OBJECT_UUID)).willReturn(query);
        given(query.setParameter("op", "SELECT")).willReturn(query);
        given(query.getResultList()).willReturn(List.of(List.of(PERMISSION_UUID)));

        given(rawGrantRepo.findByDescendantUuid(PERMISSION_UUID)).willReturn(List.of(
                as("role:rbac.global#global:ADMIN", GRANTING_ROLE_UUID)
                        .role("perm:rbactest.customer#xxx:SELECT", PERMISSION_UUID)
                        .wasGrantedTo("role:rbactest.customer#xxx:TENANT", TENANT_ROLE_UUID)
                        .assumed()));
        given(rawGrantRepo.findByDescendantUuid(TENANT_ROLE_UUID)).willReturn(List.of(
                as("role:rbac.global#global:ADMIN", GRANTING_ROLE_UUID)
                        .role("role:rbactest.customer#xxx:TENANT", TENANT_ROLE_UUID)
                        .wasGrantedTo("role:rbactest.customer#xxx:OWNER", OWNER_ROLE_UUID)
                        .notAssumed(),
                as("role:rbac.global#global:ADMIN", GRANTING_ROLE_UUID)
                        .role("role:rbactest.customer#xxx:TENANT", TENANT_ROLE_UUID)
                        .wasGrantedTo("user:alice@example.org", SUBJECT_UUID)
                        .assumed()));
        given(rawGrantRepo.findByDescendantUuid(OWNER_ROLE_UUID)).willReturn(List.of());
        given(rawGrantRepo.findByDescendantUuid(SUBJECT_UUID)).willReturn(List.of());

        // when
        final var graph = rbacGrantsDiagramService.allGrantsFrom(
                TARGET_OBJECT_UUID,
                "SELECT",
                EnumSet.of(USERS, NOT_ASSUMED, PERMISSIONS, TEST_ENTITIES));

        // then
        assertThat(graph).isEqualTo("""
                flowchart TB
                
                role:rbactest.customer#xxx:OWNER -->|XX| role:rbactest.customer#xxx:TENANT
                role:rbactest.customer#xxx:TENANT --> perm:rbactest.customer#xxx:SELECT
                user:alice --> role:rbactest.customer#xxx:TENANT""");
        verify(query).setParameter("targetObject", TARGET_OBJECT_UUID);
        verify(query).setParameter("op", "SELECT");
    }

    @Test
    void skipsUserAscendantsWhenUsersAreExcluded() {

        // given
        given(em.createNativeQuery(
                "select uuid from rbac.permission where objectUuid=:targetObject and op=:op",
                List.class)).willReturn(query);
        given(query.setParameter("targetObject", TARGET_OBJECT_UUID)).willReturn(query);
        given(query.setParameter("op", "SELECT")).willReturn(query);
        given(query.getResultList()).willReturn(List.of(List.of(TENANT_ROLE_UUID)));

        given(rawGrantRepo.findByDescendantUuid(TENANT_ROLE_UUID)).willReturn(List.of(
                as("role:rbac.global#global:ADMIN", GRANTING_ROLE_UUID)
                        .role("role:rbactest.customer#xxx:TENANT", TENANT_ROLE_UUID)
                        .wasGrantedTo("user:alice@example.org", SUBJECT_UUID)
                        .assumed(),
                as("role:rbac.global#global:ADMIN", GRANTING_ROLE_UUID)
                        .role("role:rbactest.customer#xxx:TENANT", TENANT_ROLE_UUID)
                        .wasGrantedTo("role:rbactest.customer#xxx:OWNER", OWNER_ROLE_UUID)
                        .assumed()));
        given(rawGrantRepo.findByDescendantUuid(OWNER_ROLE_UUID)).willReturn(List.of());

        // when
        final var graph = rbacGrantsDiagramService.allGrantsFrom(
                TARGET_OBJECT_UUID,
                "SELECT",
                EnumSet.of(TEST_ENTITIES));

        // then
        assertThat(graph).isEqualTo("""
                flowchart TB
                
                role:rbactest.customer#xxx:OWNER --> role:rbactest.customer#xxx:TENANT""");
    }

    @Test
    void rendersDetailedFlowchartWithSubgraphs() {

        // given
        given(context.fetchCurrentSubjectOrAssumedRolesUuids()).willReturn(new UUID[] { SUBJECT_UUID });
        given(rawGrantRepo.findByAscendingUuid(SUBJECT_UUID)).willReturn(List.of(
                as("role:rbac.global#global:ADMIN", GRANTING_ROLE_UUID)
                        .role("role:rbactest.customer#xxx:OWNER", OWNER_ROLE_UUID)
                        .wasGrantedTo("user:alice@example.org", SUBJECT_UUID)
                        .assumed()));
        given(rawGrantRepo.findByAscendingUuid(OWNER_ROLE_UUID)).willReturn(List.of(
                as("role:rbac.global#global:ADMIN", GRANTING_ROLE_UUID)
                        .role("perm:rbactest.customer#xxx:SELECT", PERMISSION_UUID)
                        .wasGrantedTo("role:rbactest.customer#xxx:OWNER", OWNER_ROLE_UUID)
                        .assumed()));
        given(rawGrantRepo.findByAscendingUuid(PERMISSION_UUID)).willReturn(List.of());

        // when
        final var graph = rbacGrantsDiagramService.allGrantsToCurrentSubject(
                EnumSet.of(DETAILS, PERMISSIONS, TEST_ENTITIES));

        // then
        assertThat(graph).startsWith("""
                %%{init:{'flowchart':{'htmlLabels':false}}}%%
                
                flowchart TB
                """);
        assertThat(graph)
                .contains("subgraph rbactest.customer#xxx[rbactest.customer#xxx]")
                .contains("role:rbactest.customer#xxx:OWNER[OWNER\n    ref:" + OWNER_ROLE_UUID + "]")
                .contains("perm:rbactest.customer#xxx:SELECT{{SELECT\n    ref:" + PERMISSION_UUID + "}}")
                .contains("subgraph users[users]")
                .contains("user:alice(alice@example.org\n    ref:" + SUBJECT_UUID + ")")
                .contains("role:rbactest.customer#xxx:OWNER --> perm:rbactest.customer#xxx:SELECT")
                .contains("user:alice --> role:rbactest.customer#xxx:OWNER");
    }

    @Test
    void reportsCroppedGraphWhenGrantLimitIsReached() {

        // given
        final var grants = new ArrayList<RawRbacGrantEntity>();
        for (int index = 0; index <= 500; ++index) {
            grants.add(as("role:rbac.global#global:ADMIN", GRANTING_ROLE_UUID)
                    .role(
                            "role:rbactest.customer#xxx" + index + ":OWNER",
                            UUID.nameUUIDFromBytes(("role-" + index).getBytes()))
                    .wasGrantedTo("user:alice@example.org", SUBJECT_UUID)
                    .notAssumed());
        }
        given(context.fetchCurrentSubjectOrAssumedRolesUuids()).willReturn(new UUID[] { SUBJECT_UUID });
        given(rawGrantRepo.findByAscendingUuid(SUBJECT_UUID)).willReturn(grants);

        // when
        final var graph = rbacGrantsDiagramService.allGrantsToCurrentSubject(EnumSet.of(TEST_ENTITIES));

        // then
        assertThat(graph).startsWith("""
                %% too many grants, graph is cropped
                flowchart TB
                """);
    }

    @Test
    void writesMarkdownFileWithMermaidGraph() throws Exception {

        // given
        final var file = tempDir.resolve("grants.md");

        // when
        RbacGrantsDiagramService.writeToFile("given subject", "flowchart TB\nuser --> role", file.toString());

        // then
        assertThat(Files.readString(file)).isEqualTo("""
                ### all grants to given subject
                
                ```mermaid
                flowchart TB
                user --> role
                ```
                """);
    }

    private static GrantByRole as(final String grantingRoleIdName, final UUID grantingRoleUuid) {
        return new GrantByRole(grantingRoleIdName, grantingRoleUuid);
    }

    private static class GrantByRole {

        private final String grantingRoleIdName;
        private final UUID grantingRoleUuid;

        private GrantByRole(final String grantingRoleIdName, final UUID grantingRoleUuid) {
            this.grantingRoleIdName = grantingRoleIdName;
            this.grantingRoleUuid = grantingRoleUuid;
        }

        Granting role(final String grantedIdName, final UUID grantedUuid) {
            return new Granting(grantingRoleIdName, grantingRoleUuid, grantedIdName, grantedUuid);
        }
    }

    private static class Granting {

        private final String grantingRoleIdName;
        private final UUID grantingRoleUuid;
        private final String grantedIdName;
        private final UUID grantedUuid;

        private Granting(
                final String grantingRoleIdName,
                final UUID grantingRoleUuid,
                final String grantedIdName,
                final UUID grantedUuid) {
            this.grantingRoleIdName = grantingRoleIdName;
            this.grantingRoleUuid = grantingRoleUuid;
            this.grantedIdName = grantedIdName;
            this.grantedUuid = grantedUuid;
        }

        GrantAssumption wasGrantedTo(final String granteeIdName, final UUID granteeUuid) {
            return new GrantAssumption(
                    grantingRoleIdName,
                    grantingRoleUuid,
                    grantedIdName,
                    grantedUuid,
                    granteeIdName,
                    granteeUuid);
        }
    }

    private static class GrantAssumption {

        private final String grantingRoleIdName;
        private final UUID grantingRoleUuid;
        private final String grantedIdName;
        private final UUID grantedUuid;
        private final String granteeIdName;
        private final UUID granteeUuid;

        private GrantAssumption(
                final String grantingRoleIdName,
                final UUID grantingRoleUuid,
                final String grantedIdName,
                final UUID grantedUuid,
                final String granteeIdName,
                final UUID granteeUuid) {
            this.grantingRoleIdName = grantingRoleIdName;
            this.grantingRoleUuid = grantingRoleUuid;
            this.grantedIdName = grantedIdName;
            this.grantedUuid = grantedUuid;
            this.granteeIdName = granteeIdName;
            this.granteeUuid = granteeUuid;
        }

        RawRbacGrantEntity assumed() {
            return grant(true);
        }

        RawRbacGrantEntity notAssumed() {
            return grant(false);
        }

        private RawRbacGrantEntity grant(final boolean assumed) {
            return RawRbacGrantEntity.builder()
                    .uuid(UUID.randomUUID())
                    .grantedByRoleIdName(grantingRoleIdName)
                    .grantedByRoleUuid(grantingRoleUuid)
                    .ascendantIdName(granteeIdName)
                    .ascendingUuid(granteeUuid)
                    .descendantIdName(grantedIdName)
                    .descendantUuid(grantedUuid)
                    .assumed(assumed)
                    .build();
        }
    }
}
