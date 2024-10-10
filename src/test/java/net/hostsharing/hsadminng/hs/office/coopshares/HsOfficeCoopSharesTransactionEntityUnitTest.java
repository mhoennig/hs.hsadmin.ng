package net.hostsharing.hsadminng.hs.office.coopshares;

import net.hostsharing.hsadminng.rbac.generator.RbacViewMermaidFlowchartGenerator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static net.hostsharing.hsadminng.hs.office.membership.TestHsMembership.TEST_MEMBERSHIP;
import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeCoopSharesTransactionEntityUnitTest {

    final HsOfficeCoopSharesTransactionEntity givenCoopSharesTransaction = HsOfficeCoopSharesTransactionEntity.builder()
            .membership(TEST_MEMBERSHIP)
            .reference("some-ref")
            .valueDate(LocalDate.parse("2020-01-01"))
            .transactionType(HsOfficeCoopSharesTransactionType.SUBSCRIPTION)
            .shareCount(4)
            .comment("some comment")
            .build();


    final HsOfficeCoopSharesTransactionEntity givenCoopShareAdjustmentTransaction = HsOfficeCoopSharesTransactionEntity.builder()
        .membership(TEST_MEMBERSHIP)
        .reference("some-ref")
        .valueDate(LocalDate.parse("2020-01-15"))
        .transactionType(HsOfficeCoopSharesTransactionType.ADJUSTMENT)
        .shareCount(-4)
        .comment("some comment")
        .adjustedShareTx(givenCoopSharesTransaction)
        .build();

    final HsOfficeCoopSharesTransactionEntity givenEmptyCoopSharesTransaction = HsOfficeCoopSharesTransactionEntity.builder().build();

    @Test
    void toStringContainsAllNonNullProperties() {
        final var result = givenCoopSharesTransaction.toString();

        assertThat(result).isEqualTo("CoopShareTransaction(M-1000101: 2020-01-01, SUBSCRIPTION, 4, some-ref, some comment)");
    }

    @Test
    void toStringWithReverseEntryContainsReverseEntry() {
        givenCoopSharesTransaction.setAdjustedShareTx(givenCoopShareAdjustmentTransaction);

        final var result = givenCoopSharesTransaction.toString();

        assertThat(result).isEqualTo("CoopShareTransaction(M-1000101: 2020-01-01, SUBSCRIPTION, 4, some-ref, some comment, M-1000101:ADJ:-4)");
    }

    @Test
    void toShortStringContainsOnlyAbbreviatedString() {
        final var result = givenCoopSharesTransaction.toShortString();

        assertThat(result).isEqualTo("M-1000101:SUB:+4");
    }

    @Test
    void toStringEmptyTransactionDoesNotThrowException() {
        final var result = givenEmptyCoopSharesTransaction.toString();

        assertThat(result).isEqualTo("CoopShareTransaction(null: 0)");
    }

    @Test
    void toShortStringEmptyTransactionDoesNotThrowException() {
        final var result = givenEmptyCoopSharesTransaction.toShortString();

        assertThat(result).isEqualTo("null:nul:+0");
    }

    @Test
    void definesRbac() {
        final var rbacFlowchart = new RbacViewMermaidFlowchartGenerator(HsOfficeCoopSharesTransactionEntity.rbac()).toString();
        assertThat(rbacFlowchart).isEqualTo("""
                %%{init:{'flowchart':{'htmlLabels':false}}}%%
                flowchart TB
                
                subgraph coopSharesTransaction["`**coopSharesTransaction**`"]
                    direction TB
                    style coopSharesTransaction fill:#dd4901,stroke:#274d6e,stroke-width:8px
                
                    subgraph coopSharesTransaction:permissions[ ]
                        style coopSharesTransaction:permissions fill:#dd4901,stroke:white
                
                        perm:coopSharesTransaction:INSERT{{coopSharesTransaction:INSERT}}
                        perm:coopSharesTransaction:UPDATE{{coopSharesTransaction:UPDATE}}
                        perm:coopSharesTransaction:SELECT{{coopSharesTransaction:SELECT}}
                    end
                end
                
                subgraph membership["`**membership**`"]
                    direction TB
                    style membership fill:#99bcdb,stroke:#274d6e,stroke-width:8px
                
                    subgraph membership:roles[ ]
                        style membership:roles fill:#99bcdb,stroke:white
                
                        role:membership:OWNER[[membership:OWNER]]
                        role:membership:ADMIN[[membership:ADMIN]]
                        role:membership:AGENT[[membership:AGENT]]
                    end
                end
                
                subgraph membership.partnerRel["`**membership.partnerRel**`"]
                    direction TB
                    style membership.partnerRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px
                
                    subgraph membership.partnerRel:roles[ ]
                        style membership.partnerRel:roles fill:#99bcdb,stroke:white
                
                        role:membership.partnerRel:OWNER[[membership.partnerRel:OWNER]]
                        role:membership.partnerRel:ADMIN[[membership.partnerRel:ADMIN]]
                        role:membership.partnerRel:AGENT[[membership.partnerRel:AGENT]]
                        role:membership.partnerRel:TENANT[[membership.partnerRel:TENANT]]
                    end
                end
                
                subgraph membership.partnerRel.anchorPerson["`**membership.partnerRel.anchorPerson**`"]
                    direction TB
                    style membership.partnerRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px
                
                    subgraph membership.partnerRel.anchorPerson:roles[ ]
                        style membership.partnerRel.anchorPerson:roles fill:#99bcdb,stroke:white
                
                        role:membership.partnerRel.anchorPerson:OWNER[[membership.partnerRel.anchorPerson:OWNER]]
                        role:membership.partnerRel.anchorPerson:ADMIN[[membership.partnerRel.anchorPerson:ADMIN]]
                        role:membership.partnerRel.anchorPerson:REFERRER[[membership.partnerRel.anchorPerson:REFERRER]]
                    end
                end
                
                subgraph membership.partnerRel.contact["`**membership.partnerRel.contact**`"]
                    direction TB
                    style membership.partnerRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px
                
                    subgraph membership.partnerRel.contact:roles[ ]
                        style membership.partnerRel.contact:roles fill:#99bcdb,stroke:white
                
                        role:membership.partnerRel.contact:OWNER[[membership.partnerRel.contact:OWNER]]
                        role:membership.partnerRel.contact:ADMIN[[membership.partnerRel.contact:ADMIN]]
                        role:membership.partnerRel.contact:REFERRER[[membership.partnerRel.contact:REFERRER]]
                    end
                end
                
                subgraph membership.partnerRel.holderPerson["`**membership.partnerRel.holderPerson**`"]
                    direction TB
                    style membership.partnerRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px
                
                    subgraph membership.partnerRel.holderPerson:roles[ ]
                        style membership.partnerRel.holderPerson:roles fill:#99bcdb,stroke:white
                
                        role:membership.partnerRel.holderPerson:OWNER[[membership.partnerRel.holderPerson:OWNER]]
                        role:membership.partnerRel.holderPerson:ADMIN[[membership.partnerRel.holderPerson:ADMIN]]
                        role:membership.partnerRel.holderPerson:REFERRER[[membership.partnerRel.holderPerson:REFERRER]]
                    end
                end
                
                %% granting roles to roles
                role:rbac.global:ADMIN -.-> role:membership.partnerRel.anchorPerson:OWNER
                role:membership.partnerRel.anchorPerson:OWNER -.-> role:membership.partnerRel.anchorPerson:ADMIN
                role:membership.partnerRel.anchorPerson:ADMIN -.-> role:membership.partnerRel.anchorPerson:REFERRER
                role:rbac.global:ADMIN -.-> role:membership.partnerRel.holderPerson:OWNER
                role:membership.partnerRel.holderPerson:OWNER -.-> role:membership.partnerRel.holderPerson:ADMIN
                role:membership.partnerRel.holderPerson:ADMIN -.-> role:membership.partnerRel.holderPerson:REFERRER
                role:rbac.global:ADMIN -.-> role:membership.partnerRel.contact:OWNER
                role:membership.partnerRel.contact:OWNER -.-> role:membership.partnerRel.contact:ADMIN
                role:membership.partnerRel.contact:ADMIN -.-> role:membership.partnerRel.contact:REFERRER
                role:rbac.global:ADMIN -.-> role:membership.partnerRel:OWNER
                role:membership.partnerRel:OWNER -.-> role:membership.partnerRel:ADMIN
                role:membership.partnerRel:ADMIN -.-> role:membership.partnerRel:AGENT
                role:membership.partnerRel:AGENT -.-> role:membership.partnerRel:TENANT
                role:membership.partnerRel.contact:ADMIN -.-> role:membership.partnerRel:TENANT
                role:membership.partnerRel:TENANT -.-> role:membership.partnerRel.anchorPerson:REFERRER
                role:membership.partnerRel:TENANT -.-> role:membership.partnerRel.holderPerson:REFERRER
                role:membership.partnerRel:TENANT -.-> role:membership.partnerRel.contact:REFERRER
                role:membership.partnerRel.anchorPerson:ADMIN -.-> role:membership.partnerRel:OWNER
                role:membership.partnerRel.holderPerson:ADMIN -.-> role:membership.partnerRel:AGENT
                role:membership:OWNER -.-> role:membership:ADMIN
                role:membership.partnerRel:ADMIN -.-> role:membership:ADMIN
                role:membership:ADMIN -.-> role:membership:AGENT
                role:membership.partnerRel:AGENT -.-> role:membership:AGENT
                role:membership:AGENT -.-> role:membership.partnerRel:TENANT
                
                %% granting permissions to roles
                role:membership:ADMIN ==> perm:coopSharesTransaction:INSERT
                role:membership:ADMIN ==> perm:coopSharesTransaction:UPDATE
                role:membership:AGENT ==> perm:coopSharesTransaction:SELECT
                """);
    }
}
