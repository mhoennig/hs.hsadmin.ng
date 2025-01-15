package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealEntity;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonType;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRealEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType;
import net.hostsharing.hsadminng.rbac.generator.RbacViewMermaidFlowchartGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficePartnerEntityUnitTest {

    private final HsOfficePartnerRbacEntity givenPartner = HsOfficePartnerRbacEntity.builder()
            .partnerNumber(12345)
            .partnerRel(HsOfficeRelationRealEntity.builder()
                    .anchor(HsOfficePersonRealEntity.builder()
                            .personType(HsOfficePersonType.LEGAL_PERSON)
                            .tradeName("Hostsharing eG")
                            .build())
                    .type(HsOfficeRelationType.PARTNER)
                    .holder(HsOfficePersonRealEntity.builder()
                            .personType(HsOfficePersonType.LEGAL_PERSON)
                            .tradeName("some trade name")
                            .build())
                    .contact(HsOfficeContactRealEntity.builder().caption("some caption").build())
                    .build())
            .build();

    @Test
    void toStringContainsPartnerNumberPersonAndContact() {
        final var result = givenPartner.toString();
        assertThat(result).isEqualTo("partner(P-12345: LP some trade name, some caption)");
    }

    @Test
    void toShortStringContainsPartnerNumber() {
        final var result = givenPartner.toShortString();
        assertThat(result).isEqualTo("P-12345");
    }

    @Test
    void definesRbac() {
        final var rbacFlowchart = new RbacViewMermaidFlowchartGenerator(HsOfficePartnerRbacEntity.rbac()).toString();
        assertThat(rbacFlowchart).isEqualTo("""
                %%{init:{'flowchart':{'htmlLabels':false}}}%%
                flowchart TB
                
                subgraph partner["`**partner**`"]
                    direction TB
                    style partner fill:#dd4901,stroke:#274d6e,stroke-width:8px
                
                    subgraph partner:permissions[ ]
                        style partner:permissions fill:#dd4901,stroke:white
                
                        perm:partner:INSERT{{partner:INSERT}}
                        perm:partner:DELETE{{partner:DELETE}}
                        perm:partner:UPDATE{{partner:UPDATE}}
                        perm:partner:SELECT{{partner:SELECT}}
                    end
                
                    subgraph partnerRel["`**partnerRel**`"]
                        direction TB
                        style partnerRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px
                
                        subgraph partnerRel:roles[ ]
                            style partnerRel:roles fill:#99bcdb,stroke:white
                
                            role:partnerRel:OWNER[[partnerRel:OWNER]]
                            role:partnerRel:ADMIN[[partnerRel:ADMIN]]
                            role:partnerRel:AGENT[[partnerRel:AGENT]]
                            role:partnerRel:TENANT[[partnerRel:TENANT]]
                        end
                    end
                end
                
                subgraph partnerDetails["`**partnerDetails**`"]
                    direction TB
                    style partnerDetails fill:#feb28c,stroke:#274d6e,stroke-width:8px
                
                    subgraph partnerDetails:permissions[ ]
                        style partnerDetails:permissions fill:#feb28c,stroke:white
                
                        perm:partnerDetails:DELETE{{partnerDetails:DELETE}}
                        perm:partnerDetails:UPDATE{{partnerDetails:UPDATE}}
                        perm:partnerDetails:SELECT{{partnerDetails:SELECT}}
                    end
                end
                
                subgraph partnerRel.anchorPerson["`**partnerRel.anchorPerson**`"]
                    direction TB
                    style partnerRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px
                
                    subgraph partnerRel.anchorPerson:roles[ ]
                        style partnerRel.anchorPerson:roles fill:#99bcdb,stroke:white
                
                        role:partnerRel.anchorPerson:OWNER[[partnerRel.anchorPerson:OWNER]]
                        role:partnerRel.anchorPerson:ADMIN[[partnerRel.anchorPerson:ADMIN]]
                        role:partnerRel.anchorPerson:REFERRER[[partnerRel.anchorPerson:REFERRER]]
                    end
                end
                
                subgraph partnerRel.contact["`**partnerRel.contact**`"]
                    direction TB
                    style partnerRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px
                
                    subgraph partnerRel.contact:roles[ ]
                        style partnerRel.contact:roles fill:#99bcdb,stroke:white
                
                        role:partnerRel.contact:OWNER[[partnerRel.contact:OWNER]]
                        role:partnerRel.contact:ADMIN[[partnerRel.contact:ADMIN]]
                        role:partnerRel.contact:REFERRER[[partnerRel.contact:REFERRER]]
                    end
                end
                
                subgraph partnerRel.holderPerson["`**partnerRel.holderPerson**`"]
                    direction TB
                    style partnerRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px
                
                    subgraph partnerRel.holderPerson:roles[ ]
                        style partnerRel.holderPerson:roles fill:#99bcdb,stroke:white
                
                        role:partnerRel.holderPerson:OWNER[[partnerRel.holderPerson:OWNER]]
                        role:partnerRel.holderPerson:ADMIN[[partnerRel.holderPerson:ADMIN]]
                        role:partnerRel.holderPerson:REFERRER[[partnerRel.holderPerson:REFERRER]]
                    end
                end
                
                %% granting roles to roles
                role:rbac.global:ADMIN -.-> role:partnerRel.anchorPerson:OWNER
                role:partnerRel.anchorPerson:OWNER -.-> role:partnerRel.anchorPerson:ADMIN
                role:partnerRel.anchorPerson:ADMIN -.-> role:partnerRel.anchorPerson:REFERRER
                role:rbac.global:ADMIN -.-> role:partnerRel.holderPerson:OWNER
                role:partnerRel.holderPerson:OWNER -.-> role:partnerRel.holderPerson:ADMIN
                role:partnerRel.holderPerson:ADMIN -.-> role:partnerRel.holderPerson:REFERRER
                role:rbac.global:ADMIN -.-> role:partnerRel.contact:OWNER
                role:partnerRel.contact:OWNER -.-> role:partnerRel.contact:ADMIN
                role:partnerRel.contact:ADMIN -.-> role:partnerRel.contact:REFERRER
                role:rbac.global:ADMIN -.-> role:partnerRel:OWNER
                role:partnerRel:OWNER -.-> role:partnerRel:ADMIN
                role:partnerRel:ADMIN -.-> role:partnerRel:AGENT
                role:partnerRel:AGENT -.-> role:partnerRel:TENANT
                role:partnerRel.contact:ADMIN -.-> role:partnerRel:TENANT
                role:partnerRel:TENANT -.-> role:partnerRel.anchorPerson:REFERRER
                role:partnerRel:TENANT -.-> role:partnerRel.holderPerson:REFERRER
                role:partnerRel:TENANT -.-> role:partnerRel.contact:REFERRER
                role:partnerRel.anchorPerson:ADMIN -.-> role:partnerRel:OWNER
                role:partnerRel.holderPerson:ADMIN -.-> role:partnerRel:AGENT
                
                %% granting permissions to roles
                role:rbac.global:ADMIN ==> perm:partner:INSERT
                role:partnerRel:OWNER ==> perm:partner:DELETE
                role:partnerRel:ADMIN ==> perm:partner:UPDATE
                role:partnerRel:TENANT ==> perm:partner:SELECT
                role:partnerRel:OWNER ==> perm:partnerDetails:DELETE
                role:partnerRel:AGENT ==> perm:partnerDetails:UPDATE
                role:partnerRel:AGENT ==> perm:partnerDetails:SELECT
                """);
    }
}
