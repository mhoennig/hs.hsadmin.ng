package net.hostsharing.hsadminng.hs.office.coopassets;

import net.hostsharing.hsadminng.rbac.generator.RbacViewMermaidFlowchartGenerator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static net.hostsharing.hsadminng.hs.office.membership.TestHsMembership.TEST_MEMBERSHIP;
import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeCoopAssetsTransactionEntityUnitTest {

    final HsOfficeCoopAssetsTransactionEntity givenCoopAssetTransaction = HsOfficeCoopAssetsTransactionEntity.builder()
            .membership(TEST_MEMBERSHIP)
            .reference("some-ref")
            .valueDate(LocalDate.parse("2020-01-01"))
            .transactionType(HsOfficeCoopAssetsTransactionType.DEPOSIT)
            .assetValue(new BigDecimal("128.00"))
            .comment("some comment")
            .build();


    final HsOfficeCoopAssetsTransactionEntity givenCoopAssetAdjustmentTransaction = HsOfficeCoopAssetsTransactionEntity.builder()
            .membership(TEST_MEMBERSHIP)
            .reference("some-ref")
            .valueDate(LocalDate.parse("2020-01-15"))
            .transactionType(HsOfficeCoopAssetsTransactionType.ADJUSTMENT)
            .assetValue(new BigDecimal("-128.00"))
            .comment("some comment")
            .adjustedAssetTx(givenCoopAssetTransaction)
            .build();

    final HsOfficeCoopAssetsTransactionEntity givenEmptyCoopAssetsTransaction = HsOfficeCoopAssetsTransactionEntity.builder().build();

    @Test
    void toStringContainsAllNonNullProperties() {
        final var result = givenCoopAssetTransaction.toString();

        assertThat(result).isEqualTo("CoopAssetsTransaction(M-1000101: 2020-01-01, DEPOSIT, 128.00, some-ref, some comment)");
    }

    @Test
    void toStringWithReverseEntryContainsReverseEntry() {
        givenCoopAssetTransaction.setAdjustedAssetTx(givenCoopAssetAdjustmentTransaction);

        final var result = givenCoopAssetTransaction.toString();

        assertThat(result).isEqualTo("CoopAssetsTransaction(M-1000101: 2020-01-01, DEPOSIT, 128.00, some-ref, some comment, M-1000101:ADJ:-128.00)");
    }

    @Test
    void toShortStringContainsOnlyMemberNumberSuffixAndSharesCountOnly() {
        final var result = givenCoopAssetTransaction.toShortString();

        assertThat(result).isEqualTo("M-1000101:DEP:+128.00");
    }

    @Test
    void toStringWithEmptyTransactionDoesNotThrowException() {
        final var result = givenEmptyCoopAssetsTransaction.toString();

        assertThat(result).isEqualTo("CoopAssetsTransaction(M-???????: )");
    }

    @Test
    void toShortStringEmptyTransactionDoesNotThrowException() {
        final var result = givenEmptyCoopAssetsTransaction.toShortString();

        assertThat(result).isEqualTo("M-???????:nul:+0.00");
    }

    @Test
    void definesRbac() {
        final var rbacFlowchart = new RbacViewMermaidFlowchartGenerator(HsOfficeCoopAssetsTransactionEntity.rbac()).toString();
        assertThat(rbacFlowchart).isEqualTo("""
                %%{init:{'flowchart':{'htmlLabels':false}}}%%
                flowchart TB
                
                subgraph coopAssetsTransaction["`**coopAssetsTransaction**`"]
                    direction TB
                    style coopAssetsTransaction fill:#dd4901,stroke:#274d6e,stroke-width:8px
                
                    subgraph coopAssetsTransaction:permissions[ ]
                        style coopAssetsTransaction:permissions fill:#dd4901,stroke:white
                
                        perm:coopAssetsTransaction:INSERT{{coopAssetsTransaction:INSERT}}
                        perm:coopAssetsTransaction:UPDATE{{coopAssetsTransaction:UPDATE}}
                        perm:coopAssetsTransaction:SELECT{{coopAssetsTransaction:SELECT}}
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
                role:membership:ADMIN ==> perm:coopAssetsTransaction:INSERT
                role:membership:ADMIN ==> perm:coopAssetsTransaction:UPDATE
                role:membership:AGENT ==> perm:coopAssetsTransaction:SELECT
                """);
    }
}
