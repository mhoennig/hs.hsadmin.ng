package net.hostsharing.hsadminng.hs.office.relation;

import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
import net.hostsharing.hsadminng.rbac.generator.RbacViewMermaidFlowchartGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeRelationUnitTest {

    private HsOfficePersonRealEntity anchor = HsOfficePersonRealEntity.builder()
            .personType(HsOfficePersonType.LEGAL_PERSON)
            .tradeName("some trade name")
            .build();
    private HsOfficePersonRealEntity holder = HsOfficePersonRealEntity.builder()
            .personType(HsOfficePersonType.NATURAL_PERSON)
            .familyName("Meier")
            .givenName("Mellie")
            .build();

    @Test
    void toStringReturnsAllProperties() {
        final var given = HsOfficeRelationRbacEntity.builder()
                .type(HsOfficeRelationType.SUBSCRIBER)
                .mark("members-announce")
                .anchor(anchor)
                .holder(holder)
                .build();

        assertThat(given.toString()).isEqualTo("rel(anchor='LP some trade name', type='SUBSCRIBER', mark='members-announce', holder='NP Meier, Mellie')");
    }

    @Test
    void toShortString() {
        final var given = HsOfficeRelationRbacEntity.builder()
                .type(HsOfficeRelationType.REPRESENTATIVE)
                .anchor(anchor)
                .holder(holder)
                .build();

        assertThat(given.toShortString()).isEqualTo("rel(anchor='LP some trade name', type='REPRESENTATIVE', holder='NP Meier, Mellie')");
    }

    @Test
    void definesRbac() {
        final var rbacFlowchart = new RbacViewMermaidFlowchartGenerator(HsOfficeRelationRbacEntity.rbac()).toString();
        assertThat(rbacFlowchart).isEqualTo("""
                %%{init:{'flowchart':{'htmlLabels':false}}}%%
                flowchart TB
                
                subgraph anchorPerson["`**anchorPerson**`"]
                    direction TB
                    style anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px
                
                    subgraph anchorPerson:roles[ ]
                        style anchorPerson:roles fill:#99bcdb,stroke:white
                
                        role:anchorPerson:OWNER[[anchorPerson:OWNER]]
                        role:anchorPerson:ADMIN[[anchorPerson:ADMIN]]
                        role:anchorPerson:REFERRER[[anchorPerson:REFERRER]]
                    end
                end
                
                subgraph contact["`**contact**`"]
                    direction TB
                    style contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px
                
                    subgraph contact:roles[ ]
                        style contact:roles fill:#99bcdb,stroke:white
                
                        role:contact:OWNER[[contact:OWNER]]
                        role:contact:ADMIN[[contact:ADMIN]]
                        role:contact:REFERRER[[contact:REFERRER]]
                    end
                end
                
                subgraph holderPerson["`**holderPerson**`"]
                    direction TB
                    style holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px
                
                    subgraph holderPerson:roles[ ]
                        style holderPerson:roles fill:#99bcdb,stroke:white
                
                        role:holderPerson:OWNER[[holderPerson:OWNER]]
                        role:holderPerson:ADMIN[[holderPerson:ADMIN]]
                        role:holderPerson:REFERRER[[holderPerson:REFERRER]]
                    end
                end
                
                subgraph relation["`**relation**`"]
                    direction TB
                    style relation fill:#dd4901,stroke:#274d6e,stroke-width:8px
                
                    subgraph relation:roles[ ]
                        style relation:roles fill:#dd4901,stroke:white
                
                        role:relation:OWNER[[relation:OWNER]]
                        role:relation:ADMIN[[relation:ADMIN]]
                        role:relation:AGENT[[relation:AGENT]]
                        role:relation:TENANT[[relation:TENANT]]
                    end
                
                    subgraph relation:permissions[ ]
                        style relation:permissions fill:#dd4901,stroke:white
                
                        perm:relation:DELETE{{relation:DELETE}}
                        perm:relation:UPDATE{{relation:UPDATE}}
                        perm:relation:SELECT{{relation:SELECT}}
                        perm:relation:INSERT{{relation:INSERT}}
                    end
                end
                
                %% granting roles to users
                user:creator ==> role:relation:OWNER
                
                %% granting roles to roles
                role:rbac.global:ADMIN -.-> role:anchorPerson:OWNER
                role:anchorPerson:OWNER -.-> role:anchorPerson:ADMIN
                role:anchorPerson:ADMIN -.-> role:anchorPerson:REFERRER
                role:rbac.global:ADMIN -.-> role:holderPerson:OWNER
                role:holderPerson:OWNER -.-> role:holderPerson:ADMIN
                role:holderPerson:ADMIN -.-> role:holderPerson:REFERRER
                role:rbac.global:ADMIN -.-> role:contact:OWNER
                role:contact:OWNER -.-> role:contact:ADMIN
                role:contact:ADMIN -.-> role:contact:REFERRER
                role:rbac.global:ADMIN ==> role:relation:OWNER
                role:relation:OWNER ==> role:relation:ADMIN
                role:relation:ADMIN ==> role:relation:AGENT
                role:relation:AGENT ==> role:relation:TENANT
                role:contact:ADMIN ==> role:relation:TENANT
                role:relation:TENANT ==> role:anchorPerson:REFERRER
                role:relation:TENANT ==> role:holderPerson:REFERRER
                role:relation:TENANT ==> role:contact:REFERRER
                
                %% granting permissions to roles
                role:relation:OWNER ==> perm:relation:DELETE
                role:relation:ADMIN ==> perm:relation:UPDATE
                role:relation:TENANT ==> perm:relation:SELECT
                role:anchorPerson:ADMIN ==> perm:relation:INSERT
                """);
    }
}
