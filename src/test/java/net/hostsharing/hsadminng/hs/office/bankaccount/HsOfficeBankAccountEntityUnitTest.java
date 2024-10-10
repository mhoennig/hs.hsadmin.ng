package net.hostsharing.hsadminng.hs.office.bankaccount;

import net.hostsharing.hsadminng.rbac.generator.RbacViewMermaidFlowchartGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeBankAccountEntityUnitTest {

    @Test
    void toStringReturnsNullForNullBankAccount() {
        final HsOfficeBankAccountEntity givenBankAccount = null;
        assertThat("" + givenBankAccount).isEqualTo("null");
    }

    @Test
    void toStringReturnsAllProperties() {
        final var givenBankAccount = HsOfficeBankAccountEntity.builder()
                .holder("given holder")
                .iban("DE02370502990000684712")
                .bic("COKSDE33")
                .build();
        assertThat(givenBankAccount.toString()).isEqualTo("bankAccount(DE02370502990000684712: holder='given holder', bic='COKSDE33')");
    }

    @Test
    void toShotStringReturnsHolder() {
        final var givenBankAccount = HsOfficeBankAccountEntity.builder()
                .holder("given holder")
                .iban("DE02370502990000684712")
                .bic("COKSDE33")
                .build();
        assertThat(givenBankAccount.toShortString()).isEqualTo("given holder");
    }

    @Test
    void definesRbac() {
        final var rbacFlowchart = new RbacViewMermaidFlowchartGenerator(HsOfficeBankAccountEntity.rbac()).toString();
        assertThat(rbacFlowchart).isEqualTo("""
                %%{init:{'flowchart':{'htmlLabels':false}}}%%
                flowchart TB
                
                subgraph bankAccount["`**bankAccount**`"]
                    direction TB
                    style bankAccount fill:#dd4901,stroke:#274d6e,stroke-width:8px
                
                    subgraph bankAccount:roles[ ]
                        style bankAccount:roles fill:#dd4901,stroke:white
                
                        role:bankAccount:OWNER[[bankAccount:OWNER]]
                        role:bankAccount:ADMIN[[bankAccount:ADMIN]]
                        role:bankAccount:REFERRER[[bankAccount:REFERRER]]
                    end
                
                    subgraph bankAccount:permissions[ ]
                        style bankAccount:permissions fill:#dd4901,stroke:white
                
                        perm:bankAccount:INSERT{{bankAccount:INSERT}}
                        perm:bankAccount:DELETE{{bankAccount:DELETE}}
                        perm:bankAccount:UPDATE{{bankAccount:UPDATE}}
                        perm:bankAccount:SELECT{{bankAccount:SELECT}}
                    end
                end
                
                %% granting roles to users
                user:creator ==> role:bankAccount:OWNER
                
                %% granting roles to roles
                role:rbac.global:ADMIN ==> role:bankAccount:OWNER
                role:bankAccount:OWNER ==> role:bankAccount:ADMIN
                role:bankAccount:ADMIN ==> role:bankAccount:REFERRER
                
                %% granting permissions to roles
                role:rbac.global:GUEST ==> perm:bankAccount:INSERT
                role:bankAccount:OWNER ==> perm:bankAccount:DELETE
                role:bankAccount:ADMIN ==> perm:bankAccount:UPDATE
                role:bankAccount:REFERRER ==> perm:bankAccount:SELECT
                """);
    }
}
