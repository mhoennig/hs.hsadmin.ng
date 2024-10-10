package net.hostsharing.hsadminng.hs.office.sepamandate;

import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationRbacEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacViewMermaidFlowchartGenerator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static net.hostsharing.hsadminng.hs.office.debitor.TestHsOfficeDebitor.TEST_DEBITOR;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.toPostgresDateRange;
import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeSepaMandateEntityUnitTest {
    public static final LocalDate GIVEN_VALID_FROM = LocalDate.parse("2020-01-01");
    public static final LocalDate GIVEN_VALID_TO = LocalDate.parse("2030-12-31");

    final HsOfficeSepaMandateEntity givenSepaMandate = HsOfficeSepaMandateEntity.builder()
            .debitor(TEST_DEBITOR)
            .reference("some-ref")
            .validity(toPostgresDateRange(GIVEN_VALID_FROM, GIVEN_VALID_TO))
            .bankAccount(HsOfficeBankAccountEntity.builder().iban("some caption").build())
            .build();

    @Test
    void toStringContainsReferenceAndBankAccount() {
        final var result = givenSepaMandate.toString();

        assertThat(result).isEqualTo("SEPA-Mandate(some caption, some-ref, [2020-01-01,2031-01-01))");
    }

    @Test
    void toShortStringContainsReferenceOnly() {
        final var result = givenSepaMandate.toShortString();

        assertThat(result).isEqualTo("some-ref");
    }

    @Test
    void settingValidFromKeepsValidTo() {
        givenSepaMandate.setValidFrom(LocalDate.parse("2023-12-31"));
        assertThat(givenSepaMandate.getValidFrom()).isEqualTo(LocalDate.parse("2023-12-31"));
        assertThat(givenSepaMandate.getValidTo()).isEqualTo(GIVEN_VALID_TO);

    }

    @Test
    void settingValidToKeepsValidFrom() {
        givenSepaMandate.setValidTo(LocalDate.parse("2024-12-31"));
        assertThat(givenSepaMandate.getValidFrom()).isEqualTo(GIVEN_VALID_FROM);
        assertThat(givenSepaMandate.getValidTo()).isEqualTo(LocalDate.parse("2024-12-31"));
    }

    @Test
    void definesRbac() {
        final var rbacFlowchart = new RbacViewMermaidFlowchartGenerator(HsOfficeSepaMandateEntity.rbac()).toString();
        assertThat(rbacFlowchart).isEqualTo("""
                %%{init:{'flowchart':{'htmlLabels':false}}}%%
                flowchart TB
                
                subgraph bankAccount["`**bankAccount**`"]
                    direction TB
                    style bankAccount fill:#99bcdb,stroke:#274d6e,stroke-width:8px
                
                    subgraph bankAccount:roles[ ]
                        style bankAccount:roles fill:#99bcdb,stroke:white
                
                        role:bankAccount:OWNER[[bankAccount:OWNER]]
                        role:bankAccount:ADMIN[[bankAccount:ADMIN]]
                        role:bankAccount:REFERRER[[bankAccount:REFERRER]]
                    end
                end
                
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
                
                subgraph debitorRel.anchorPerson["`**debitorRel.anchorPerson**`"]
                    direction TB
                    style debitorRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px
                
                    subgraph debitorRel.anchorPerson:roles[ ]
                        style debitorRel.anchorPerson:roles fill:#99bcdb,stroke:white
                
                        role:debitorRel.anchorPerson:OWNER[[debitorRel.anchorPerson:OWNER]]
                        role:debitorRel.anchorPerson:ADMIN[[debitorRel.anchorPerson:ADMIN]]
                        role:debitorRel.anchorPerson:REFERRER[[debitorRel.anchorPerson:REFERRER]]
                    end
                end
                
                subgraph debitorRel.contact["`**debitorRel.contact**`"]
                    direction TB
                    style debitorRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px
                
                    subgraph debitorRel.contact:roles[ ]
                        style debitorRel.contact:roles fill:#99bcdb,stroke:white
                
                        role:debitorRel.contact:OWNER[[debitorRel.contact:OWNER]]
                        role:debitorRel.contact:ADMIN[[debitorRel.contact:ADMIN]]
                        role:debitorRel.contact:REFERRER[[debitorRel.contact:REFERRER]]
                    end
                end
                
                subgraph debitorRel.holderPerson["`**debitorRel.holderPerson**`"]
                    direction TB
                    style debitorRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px
                
                    subgraph debitorRel.holderPerson:roles[ ]
                        style debitorRel.holderPerson:roles fill:#99bcdb,stroke:white
                
                        role:debitorRel.holderPerson:OWNER[[debitorRel.holderPerson:OWNER]]
                        role:debitorRel.holderPerson:ADMIN[[debitorRel.holderPerson:ADMIN]]
                        role:debitorRel.holderPerson:REFERRER[[debitorRel.holderPerson:REFERRER]]
                    end
                end
                
                subgraph sepaMandate["`**sepaMandate**`"]
                    direction TB
                    style sepaMandate fill:#dd4901,stroke:#274d6e,stroke-width:8px
                
                    subgraph sepaMandate:roles[ ]
                        style sepaMandate:roles fill:#dd4901,stroke:white
                
                        role:sepaMandate:OWNER[[sepaMandate:OWNER]]
                        role:sepaMandate:ADMIN[[sepaMandate:ADMIN]]
                        role:sepaMandate:AGENT[[sepaMandate:AGENT]]
                        role:sepaMandate:REFERRER[[sepaMandate:REFERRER]]
                    end
                
                    subgraph sepaMandate:permissions[ ]
                        style sepaMandate:permissions fill:#dd4901,stroke:white
                
                        perm:sepaMandate:DELETE{{sepaMandate:DELETE}}
                        perm:sepaMandate:UPDATE{{sepaMandate:UPDATE}}
                        perm:sepaMandate:SELECT{{sepaMandate:SELECT}}
                        perm:sepaMandate:INSERT{{sepaMandate:INSERT}}
                    end
                end
                
                %% granting roles to users
                user:creator ==> role:sepaMandate:OWNER
                
                %% granting roles to roles
                role:rbac.global:ADMIN -.-> role:debitorRel.anchorPerson:OWNER
                role:debitorRel.anchorPerson:OWNER -.-> role:debitorRel.anchorPerson:ADMIN
                role:debitorRel.anchorPerson:ADMIN -.-> role:debitorRel.anchorPerson:REFERRER
                role:rbac.global:ADMIN -.-> role:debitorRel.holderPerson:OWNER
                role:debitorRel.holderPerson:OWNER -.-> role:debitorRel.holderPerson:ADMIN
                role:debitorRel.holderPerson:ADMIN -.-> role:debitorRel.holderPerson:REFERRER
                role:rbac.global:ADMIN -.-> role:debitorRel.contact:OWNER
                role:debitorRel.contact:OWNER -.-> role:debitorRel.contact:ADMIN
                role:debitorRel.contact:ADMIN -.-> role:debitorRel.contact:REFERRER
                role:rbac.global:ADMIN -.-> role:debitorRel:OWNER
                role:debitorRel:OWNER -.-> role:debitorRel:ADMIN
                role:debitorRel:ADMIN -.-> role:debitorRel:AGENT
                role:debitorRel:AGENT -.-> role:debitorRel:TENANT
                role:debitorRel.contact:ADMIN -.-> role:debitorRel:TENANT
                role:debitorRel:TENANT -.-> role:debitorRel.anchorPerson:REFERRER
                role:debitorRel:TENANT -.-> role:debitorRel.holderPerson:REFERRER
                role:debitorRel:TENANT -.-> role:debitorRel.contact:REFERRER
                role:debitorRel.anchorPerson:ADMIN -.-> role:debitorRel:OWNER
                role:debitorRel.holderPerson:ADMIN -.-> role:debitorRel:AGENT
                role:rbac.global:ADMIN -.-> role:bankAccount:OWNER
                role:bankAccount:OWNER -.-> role:bankAccount:ADMIN
                role:bankAccount:ADMIN -.-> role:bankAccount:REFERRER
                role:rbac.global:ADMIN ==> role:sepaMandate:OWNER
                role:sepaMandate:OWNER ==> role:sepaMandate:ADMIN
                role:sepaMandate:ADMIN ==> role:sepaMandate:AGENT
                role:sepaMandate:AGENT ==> role:bankAccount:REFERRER
                role:sepaMandate:AGENT ==> role:debitorRel:AGENT
                role:sepaMandate:AGENT ==> role:sepaMandate:REFERRER
                role:bankAccount:ADMIN ==> role:sepaMandate:REFERRER
                role:debitorRel:AGENT ==> role:sepaMandate:REFERRER
                role:sepaMandate:REFERRER ==> role:debitorRel:TENANT
                
                %% granting permissions to roles
                role:sepaMandate:OWNER ==> perm:sepaMandate:DELETE
                role:sepaMandate:ADMIN ==> perm:sepaMandate:UPDATE
                role:sepaMandate:REFERRER ==> perm:sepaMandate:SELECT
                role:debitorRel:ADMIN ==> perm:sepaMandate:INSERT
                """);
    }
}
