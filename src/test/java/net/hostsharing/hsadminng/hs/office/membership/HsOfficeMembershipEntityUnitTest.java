package net.hostsharing.hsadminng.hs.office.membership;

import io.hypersistence.utils.hibernate.type.range.Range;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.rbac.generator.RbacViewMermaidFlowchartGenerator;
import org.junit.jupiter.api.Test;

import jakarta.persistence.PrePersist;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.Arrays;

import static net.hostsharing.hsadminng.hs.office.partner.TestHsOfficePartner.TEST_PARTNER;
import static org.assertj.core.api.Assertions.assertThat;

class HsOfficeMembershipEntityUnitTest {

    public static final LocalDate GIVEN_VALID_FROM = LocalDate.parse("2020-01-01");

    final HsOfficeMembershipEntity givenMembership = HsOfficeMembershipEntity.builder()
            .memberNumberSuffix("01")
            .partner(TEST_PARTNER)
            .validity(Range.closedInfinite(GIVEN_VALID_FROM))
            .build();

    @Test
    void toStringContainsAllProps() {
        final var result = givenMembership.toString();
        assertThat(result).isEqualTo("Membership(M-1000101, P-10001, [2020-01-01,))");
    }

    @Test
    void toShortStringContainsMemberNumberSuffixOnly() {
        final var result = givenMembership.toShortString();
        assertThat(result).isEqualTo("M-1000101");
    }

    @Test
    void getMemberNumberWithPartnerAndSuffix() {
        final var result = givenMembership.getMemberNumber();
        assertThat(result).isEqualTo(1000101);
    }

    @Test
    void getMemberNumberWithPartnerButWithoutSuffix() {
        givenMembership.setMemberNumberSuffix(null);
        final var result = givenMembership.getMemberNumber();
        assertThat(result).isEqualTo(null);
    }

    @Test
    void getMemberNumberWithoutPartnerButWithSuffix() {
        givenMembership.setPartner(null);
        final var result = givenMembership.getMemberNumber();
        assertThat(result).isEqualTo(null);
    }

    @Test
    void getMemberNumberWithoutPartnerNumberButWithSuffix() {
        givenMembership.setPartner(HsOfficePartnerEntity.builder().build());
        final var result = givenMembership.getMemberNumber();
        assertThat(result).isEqualTo(null);
    }

    @Test
    void getEmptyValidtyIfNull() {
        givenMembership.setValidity(null);
        final var result = givenMembership.getValidity();
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void initializesStatusInPrePersistIfNull() throws Exception {
        final var givenUninitializedMembership = new HsOfficeMembershipEntity();
        assertThat(givenUninitializedMembership.getStatus()).as("precondition failed").isNull();

        invokePrePersist(givenUninitializedMembership);
        assertThat(givenUninitializedMembership.getStatus()).isEqualTo(HsOfficeMembershipStatus.INVALID);
    }

    @Test
    void doesNotOverwriteStatusInPrePersistIfNotNull() throws Exception {
        givenMembership.setStatus(HsOfficeMembershipStatus.CANCELLED);

        invokePrePersist(givenMembership);
        assertThat(givenMembership.getStatus()).isEqualTo(HsOfficeMembershipStatus.CANCELLED);
    }

    @Test
    void settingValidFromKeepsValidTo() {
        givenMembership.setValidFrom(LocalDate.parse("2020-01-01"));
        assertThat(givenMembership.getValidFrom()).isEqualTo(LocalDate.parse("2020-01-01"));
        assertThat(givenMembership.getValidTo()).isNull();

    }

    @Test
    void settingValidToKeepsValidFrom() {
        givenMembership.setValidTo(LocalDate.parse("2024-12-31"));
        assertThat(givenMembership.getValidFrom()).isEqualTo(GIVEN_VALID_FROM);
        assertThat(givenMembership.getValidTo()).isEqualTo(LocalDate.parse("2024-12-31"));
    }


    @Test
    void definesRbac() {
        final var rbacFlowchart = new RbacViewMermaidFlowchartGenerator(HsOfficeMembershipEntity.rbac()).toString();
        assertThat(rbacFlowchart).isEqualTo("""
                %%{init:{'flowchart':{'htmlLabels':false}}}%%
                flowchart TB
                
                subgraph membership["`**membership**`"]
                    direction TB
                    style membership fill:#dd4901,stroke:#274d6e,stroke-width:8px
                
                    subgraph membership:roles[ ]
                        style membership:roles fill:#dd4901,stroke:white
                
                        role:membership:OWNER[[membership:OWNER]]
                        role:membership:ADMIN[[membership:ADMIN]]
                        role:membership:AGENT[[membership:AGENT]]
                    end
                
                    subgraph membership:permissions[ ]
                        style membership:permissions fill:#dd4901,stroke:white
                
                        perm:membership:INSERT{{membership:INSERT}}
                        perm:membership:DELETE{{membership:DELETE}}
                        perm:membership:UPDATE{{membership:UPDATE}}
                        perm:membership:SELECT{{membership:SELECT}}
                    end
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
                
                %% granting roles to users
                user:creator ==> role:membership:OWNER
                
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
                role:membership:OWNER ==> role:membership:ADMIN
                role:partnerRel:ADMIN ==> role:membership:ADMIN
                role:membership:ADMIN ==> role:membership:AGENT
                role:partnerRel:AGENT ==> role:membership:AGENT
                role:membership:AGENT ==> role:partnerRel:TENANT
                
                %% granting permissions to roles
                role:rbac.global:ADMIN ==> perm:membership:INSERT
                role:membership:ADMIN ==> perm:membership:DELETE
                role:membership:ADMIN ==> perm:membership:UPDATE
                role:membership:AGENT ==> perm:membership:SELECT
                """);
    }

    private static void invokePrePersist(final HsOfficeMembershipEntity membershipEntity)
            throws IllegalAccessException, InvocationTargetException {
        final var prePersistMethod = Arrays.stream(HsOfficeMembershipEntity.class.getDeclaredMethods())
                .filter(f -> f.getAnnotation(PrePersist.class) != null)
                .findFirst();
        assertThat(prePersistMethod).as("@PrePersist method not found").isPresent();

        prePersistMethod.get().invoke(membershipEntity);
    }
}
