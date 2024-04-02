package net.hostsharing.hsadminng.hs.office.membership;

import com.vladmihalcea.hibernate.type.range.Range;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRepository;
import net.hostsharing.hsadminng.hs.office.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleRepository;
import net.hostsharing.test.Array;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaSystemException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
class HsOfficeMembershipRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    HsOfficeMembershipRepository membershipRepo;

    @Autowired
    HsOfficePartnerRepository partnerRepo;

    @Autowired
    HsOfficeDebitorRepository debitorRepo;

    @Autowired
    RawRbacRoleRepository rawRoleRepo;

    @Autowired
    RawRbacGrantRepository rawGrantRepo;

    @PersistenceContext
    EntityManager em;

    @Autowired
    JpaAttempt jpaAttempt;

    @MockBean
    HttpServletRequest request;

    @Nested
    class CreateMembership {

        @Test
        public void globalAdmin_withoutAssumedRole_canCreateNewMembership() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = membershipRepo.count();
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("First").get(0);

            // when
            final var result = attempt(em, () -> {
                final var newMembership = HsOfficeMembershipEntity.builder()
                        .memberNumberSuffix("11")
                        .partner(givenPartner)
                        .validity(Range.closedInfinite(LocalDate.parse("2020-01-01")))
                        .membershipFeeBillable(true)
                        .build();
                return toCleanup(membershipRepo.save(newMembership));
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficeMembershipEntity::getUuid).isNotNull();
            assertThatMembershipIsPersisted(result.returnedValue());
            assertThat(membershipRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll()).stream()
                    .map(s -> s.replace("GmbH-firstcontact", ""))
                    .map(s -> s.replace("hs_office_", ""))
                    .toList();

            // when
            attempt(em, () -> {
                final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("First").get(0);
                final var newMembership = HsOfficeMembershipEntity.builder()
                        .memberNumberSuffix("17")
                        .partner(givenPartner)
                        .validity(Range.closedInfinite(LocalDate.parse("2020-01-01")))
                        .membershipFeeBillable(true)
                        .build();
                return toCleanup(membershipRepo.save(newMembership));
            }).assertSuccessful();

            // then
            final var all = rawRoleRepo.findAll();
            assertThat(distinctRoleNamesOf(all)).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_office_membership#M-1000117.admin",
                    "hs_office_membership#M-1000117.owner",
                    "hs_office_membership#M-1000117.agent"));
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll()))
                    .map(s -> s.replace("GmbH-firstcontact", ""))
                    .map(s -> s.replace("hs_office_", ""))
                    .containsExactlyInAnyOrder(Array.fromFormatted(
                            initialGrantNames,

                            // insert
                            "{ grant perm INSERT into coopassetstransaction with membership#M-1000117 to role membership#M-1000117.admin by system and assume }",
                            "{ grant perm INSERT into coopsharestransaction with membership#M-1000117 to role membership#M-1000117.admin by system and assume }",

                            // owner
                            "{ grant perm DELETE on membership#M-1000117 to role membership#M-1000117.admin by system and assume }",
                            "{ grant role membership#M-1000117.owner to user superuser-alex@hostsharing.net by membership#M-1000117.owner and assume }",

                            // admin
                            "{ grant perm UPDATE on membership#M-1000117 to role membership#M-1000117.admin by system and assume }",
                            "{ grant role membership#M-1000117.admin to role membership#M-1000117.owner by system and assume }",
                            "{ grant role membership#M-1000117.admin to role relation#HostsharingeG-with-PARTNER-FirstGmbH.admin by system and assume }",

                            // agent
                            "{ grant perm SELECT on membership#M-1000117 to role membership#M-1000117.agent by system and assume }",
                            "{ grant role membership#M-1000117.agent to role membership#M-1000117.admin by system and assume }",
                            "{ grant role membership#M-1000117.agent to role relation#HostsharingeG-with-PARTNER-FirstGmbH.agent by system and assume }",
                            "{ grant role relation#HostsharingeG-with-PARTNER-FirstGmbH.tenant to role membership#M-1000117.agent by system and assume }",

                            null));
        }

        private void assertThatMembershipIsPersisted(final HsOfficeMembershipEntity saved) {
            final var found = membershipRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().usingRecursiveComparison().isEqualTo(saved);
        }
    }

    @Nested
    class ListMemberships {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllMemberships() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = membershipRepo.findMembershipsByOptionalPartnerUuid(null);

            // then
            exactlyTheseMembershipsAreReturned(
                    result,
                    "Membership(M-1000101, P-10001, [2022-10-01,), NONE)",
                    "Membership(M-1000202, P-10002, [2022-10-01,), NONE)",
                    "Membership(M-1000303, P-10003, [2022-10-01,), NONE)");
        }

        @Test
        public void globalAdmin_withoutAssumedRole_canFindAllMembershipByPartnerUuid() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("First").get(0);

            // when
            final var result = membershipRepo.findMembershipsByOptionalPartnerUuid(givenPartner.getUuid());

            // then
            exactlyTheseMembershipsAreReturned(result,
                    "Membership(M-1000101, P-10001, [2022-10-01,), NONE)");
        }

        @Test
        public void globalAdmin_withoutAssumedRole_canFindAllMemberships() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = membershipRepo.findMembershipByMemberNumber(1000202);

            // then
            assertThat(result)
                    .isNotNull()
                    .extracting(Object::toString)
                    .isEqualTo("Membership(M-1000202, P-10002, [2022-10-01,), NONE)");
        }
    }

    @Nested
    class UpdateMembership {

        @Test
        public void globalAdmin_canUpdateValidityOfArbitraryMembership() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenMembership = givenSomeTemporaryMembership("First", "11");
            assertThatMembershipExistsAndIsAccessibleToCurrentContext(givenMembership);
            final var newValidityEnd = LocalDate.now();

            // when
            context("superuser-alex@hostsharing.net");
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                givenMembership.setValidity(Range.closedOpen(
                        givenMembership.getValidity().lower(), newValidityEnd));
                givenMembership.setReasonForTermination(HsOfficeReasonForTermination.CANCELLATION);
                return toCleanup(membershipRepo.save(givenMembership));
            });

            // then
            result.assertSuccessful();

            membershipRepo.deleteByUuid(givenMembership.getUuid());
        }

        @Test
        public void membershipAgent_canViewButNotUpdateRelatedMembership() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenMembership = givenSomeTemporaryMembership("First", "13");
            assertThatMembershipExistsAndIsAccessibleToCurrentContext(givenMembership);
            assertThatMembershipIsVisibleForRole(
                    givenMembership,
                    "hs_office_membership#M-1000113.agent");
            final var newValidityEnd = LocalDate.now();

            // when
            final var result = jpaAttempt.transacted(() -> {
                // TODO: we should test with debitor- and partner-admin as well
                context("superuser-alex@hostsharing.net", "hs_office_membership#M-1000113.agent");
                givenMembership.setValidity(
                        Range.closedOpen(givenMembership.getValidity().lower(), newValidityEnd));
                return membershipRepo.save(givenMembership);
            });

            // then
            result.assertExceptionWithRootCauseMessage(JpaSystemException.class,
                    "[403] Subject ", " is not allowed to update hs_office_membership uuid");
        }

        private void assertThatMembershipExistsAndIsAccessibleToCurrentContext(final HsOfficeMembershipEntity saved) {
            final var found = membershipRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().isNotSameAs(saved)
                    .extracting(Object::toString).isEqualTo(saved.toString());
        }

        private void assertThatMembershipIsVisibleForRole(
                final HsOfficeMembershipEntity entity,
                final String assumedRoles) {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", assumedRoles);
                assertThatMembershipExistsAndIsAccessibleToCurrentContext(entity);
            }).assertSuccessful();
        }
    }

    @Nested
    class DeleteByUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canDeleteAnyMembership() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenMembership = givenSomeTemporaryMembership("First", "12");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                membershipRepo.deleteByUuid(givenMembership.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-fran@hostsharing.net", null);
                return membershipRepo.findByUuid(givenMembership.getUuid());
            }).assertSuccessful().returnedValue()).isEmpty();
        }

        @Test
        public void partnerRelationAgent_canNotDeleteTheirRelatedMembership() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenMembership = givenSomeTemporaryMembership("First", "14");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_office_relation#HostsharingeG-with-PARTNER-FirstGmbH.agent");
                assertThat(membershipRepo.findByUuid(givenMembership.getUuid())).isPresent();

                membershipRepo.deleteByUuid(givenMembership.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " not allowed to delete hs_office_membership");
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return membershipRepo.findByUuid(givenMembership.getUuid());
            }).assertSuccessful().returnedValue()).isPresent(); // still there
        }

        @Test
        public void deletingAMembershipAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = Array.from(distinctRoleNamesOf(rawRoleRepo.findAll()));
            final var initialGrantNames = Array.from(distinctGrantDisplaysOf(rawGrantRepo.findAll()));
            final var givenMembership = givenSomeTemporaryMembership("First", "15");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return membershipRepo.deleteByUuid(givenMembership.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isEqualTo(1);
            assertThat(distinctRoleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(initialRoleNames);
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(initialGrantNames);
        }
    }

    @Test
    public void auditJournalLogIsAvailable() {
        // given
        final var query = em.createNativeQuery("""
                select currentTask, targetTable, targetOp
                    from tx_journal_v
                    where targettable = 'hs_office_membership';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating Membership test-data P-10001M-...01, hs_office_membership, INSERT]",
                "[creating Membership test-data P-10002M-...02, hs_office_membership, INSERT]",
                "[creating Membership test-data P-10003M-...03, hs_office_membership, INSERT]");
    }

    private HsOfficeMembershipEntity givenSomeTemporaryMembership(final String partnerTradeName, final String memberNumberSuffix) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike(partnerTradeName).get(0);
            final var newMembership = HsOfficeMembershipEntity.builder()
                    .memberNumberSuffix(memberNumberSuffix)
                    .partner(givenPartner)
                    .validity(Range.closedInfinite(LocalDate.parse("2020-01-01")))
                    .membershipFeeBillable(true)
                    .build();

            return toCleanup(membershipRepo.save(newMembership));
        }).assertSuccessful().returnedValue();
    }

    void exactlyTheseMembershipsAreReturned(
            final List<HsOfficeMembershipEntity> actualResult,
            final String... membershipNames) {
        assertThat(actualResult)
                .extracting(membershipEntity -> membershipEntity.toString())
                .containsExactlyInAnyOrder(membershipNames);
    }
}
