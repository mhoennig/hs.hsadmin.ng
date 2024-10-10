package net.hostsharing.hsadminng.hs.office.contact;

import net.hostsharing.hsadminng.rbac.generator.RbacViewMermaidFlowchartGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeContactUnitTest {

    @Test
    void toStringReturnsNullForNullContact() {
        final HsOfficeContactRbacEntity givenContact = null;
      assertThat("" + givenContact).isEqualTo("null");
    }

    @Test
    void toStringReturnsCaption() {
        final var givenContact = HsOfficeContactRbacEntity.builder().caption("given caption").build();
        assertThat("" + givenContact).isEqualTo("contact(caption='given caption')");
    }

    @Test
    void definesRbac() {
        final var rbacFlowchart = new RbacViewMermaidFlowchartGenerator(HsOfficeContactRbacEntity.rbac()).toString();
        assertThat(rbacFlowchart).isEqualTo("""
                %%{init:{'flowchart':{'htmlLabels':false}}}%%
                flowchart TB
                
                subgraph contact["`**contact**`"]
                    direction TB
                    style contact fill:#dd4901,stroke:#274d6e,stroke-width:8px
                
                    subgraph contact:roles[ ]
                        style contact:roles fill:#dd4901,stroke:white
                
                        role:contact:OWNER[[contact:OWNER]]
                        role:contact:ADMIN[[contact:ADMIN]]
                        role:contact:REFERRER[[contact:REFERRER]]
                    end
                
                    subgraph contact:permissions[ ]
                        style contact:permissions fill:#dd4901,stroke:white
                
                        perm:contact:DELETE{{contact:DELETE}}
                        perm:contact:UPDATE{{contact:UPDATE}}
                        perm:contact:SELECT{{contact:SELECT}}
                        perm:contact:INSERT{{contact:INSERT}}
                    end
                end
                
                %% granting roles to users
                user:creator ==> role:contact:OWNER
                
                %% granting roles to roles
                role:rbac.global:ADMIN ==> role:contact:OWNER
                role:contact:OWNER ==> role:contact:ADMIN
                role:contact:ADMIN ==> role:contact:REFERRER
                
                %% granting permissions to roles
                role:contact:OWNER ==> perm:contact:DELETE
                role:contact:ADMIN ==> perm:contact:UPDATE
                role:contact:REFERRER ==> perm:contact:SELECT
                role:rbac.global:GUEST ==> perm:contact:INSERT
                """);
    }
}
