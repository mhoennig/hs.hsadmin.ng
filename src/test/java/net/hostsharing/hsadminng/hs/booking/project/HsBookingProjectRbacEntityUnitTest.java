package net.hostsharing.hsadminng.hs.booking.project;

import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacViewMermaidFlowchartGenerator;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HsBookingProjectRbacEntityUnitTest {

    @Test
    void toStringForEmptyInstance() {
        final var givenEntity = HsBookingProjectRbacEntity.builder().build();
        assertThat(givenEntity.toString()).isEqualTo("HsBookingProject()");
    }

    @Test
    void toStringForFullyInitializedInstance() {
        final var givenDebitor = HsBookingDebitorEntity.builder()
            .debitorNumber(123456)
            .build();
        final var givenUuid = UUID.randomUUID();
        final var givenEntity = HsBookingProjectRbacEntity.builder()
            .uuid(givenUuid)
            .debitor(givenDebitor)
            .caption("some project")
            .build();
        assertThat(givenEntity.toString()).isEqualTo("HsBookingProject(D-123456, some project)");
    }

    @Test
    void definesRbac() {
        final var rbacFlowchart = new RbacViewMermaidFlowchartGenerator(HsBookingProjectRbacEntity.rbac()).toString();
        assertThat(rbacFlowchart).isEqualTo("""
            %%{init:{'flowchart':{'htmlLabels':false}}}%%
            flowchart TB

            subgraph debitorRel["`**debitorRel**`"]
                direction TB
                style debitorRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px

                subgraph debitorRel:roles[ ]
                    style debitorRel:roles fill:#99bcdb,stroke:white

                    role:debitorRel:OWNER[[debitorRel:OWNER]]
                    role:debitorRel:ADMIN[[debitorRel:ADMIN]]
                    role:debitorRel:AGENT[[debitorRel:AGENT]]
                    role:debitorRel:TENANT[[debitorRel:TENANT]]
                end
            end

            subgraph project["`**project**`"]
                direction TB
                style project fill:#dd4901,stroke:#274d6e,stroke-width:8px

                subgraph project:roles[ ]
                    style project:roles fill:#dd4901,stroke:white

                    role:project:OWNER[[project:OWNER]]
                    role:project:ADMIN[[project:ADMIN]]
                    role:project:AGENT[[project:AGENT]]
                    role:project:TENANT[[project:TENANT]]
                end

                subgraph project:permissions[ ]
                    style project:permissions fill:#dd4901,stroke:white

                    perm:project:INSERT{{project:INSERT}}
                    perm:project:DELETE{{project:DELETE}}
                    perm:project:UPDATE{{project:UPDATE}}
                    perm:project:SELECT{{project:SELECT}}
                end
            end

            %% granting roles to roles
            role:rbac.global:ADMIN -.-> role:debitorRel:OWNER
            role:debitorRel:OWNER -.-> role:debitorRel:ADMIN
            role:debitorRel:ADMIN -.-> role:debitorRel:AGENT
            role:debitorRel:AGENT -.-> role:debitorRel:TENANT
            role:debitorRel:AGENT ==>|XX| role:project:OWNER
            role:project:OWNER ==> role:project:ADMIN
            role:project:ADMIN ==> role:project:AGENT
            role:project:AGENT ==> role:project:TENANT
            role:project:TENANT ==> role:debitorRel:TENANT

            %% granting permissions to roles
            role:debitorRel:ADMIN ==> perm:project:INSERT
            role:rbac.global:ADMIN ==> perm:project:DELETE
            role:project:ADMIN ==> perm:project:UPDATE
            role:project:TENANT ==> perm:project:SELECT
            """);
    }
}
