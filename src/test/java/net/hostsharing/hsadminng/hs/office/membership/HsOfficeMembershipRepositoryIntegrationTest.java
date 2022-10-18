package net.hostsharing.hsadminng.hs.office.membership;

import com.vladmihalcea.hibernate.type.range.Range;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRepository;
import net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleRepository;
import net.hostsharing.test.Array;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.annotation.DirtiesContext;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.*;

import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.grantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.roleNamesOf;
import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { HsOfficeMembershipRepository.class, Context.class, JpaAttempt.class })
@DirtiesContext
class HsOfficeMembershipRepositoryIntegrationTest extends ContextBasedTest {

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

    @Autowired
    EntityManager em;

    @Autowired
    JpaAttempt jpaAttempt;

    @MockBean
    HttpServletRequest request;

    Set<HsOfficeMembershipEntity> tempEntities = new HashSet<>();

    @Nested
    class CreateMembership {

        @Test
        public void globalAdmin_withoutAssumedRole_canCreateNewMembership() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = membershipRepo.count();
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("First").get(0);
            final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike("First").get(0);

            // when
            final var result = attempt(em, () -> {
                final var newMembership = toCleanup(HsOfficeMembershipEntity.builder()
                        .uuid(UUID.randomUUID())
                        .memberNumber(20001)
                        .partner(givenPartner)
                        .mainDebitor(givenDebitor)
                        .validity(Range.closedInfinite(LocalDate.parse("2020-01-01")))
                        .build());
                return membershipRepo.save(newMembership);
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
            final var initialRoleNames = roleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = grantDisplaysOf(rawGrantRepo.findAll()).stream()
                    .map(s -> s.replace("GmbH-firstcontact", ""))
                    .map(s -> s.replace("hs_office_", ""))
                    .toList();

            // when
            attempt(em, () -> {
                final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("First").get(0);
                final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike("First").get(0);
                final var newMembership = toCleanup(HsOfficeMembershipEntity.builder()
                        .uuid(UUID.randomUUID())
                        .memberNumber(20002)
                        .partner(givenPartner)
                        .mainDebitor(givenDebitor)
                        .validity(Range.closedInfinite(LocalDate.parse("2020-01-01")))
                        .build());
                return membershipRepo.save(newMembership);
            });

            // then
            final var all = rawRoleRepo.findAll();
            assertThat(roleNamesOf(all)).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_office_membership#20002FirstGmbH-firstcontact.admin",
                    "hs_office_membership#20002FirstGmbH-firstcontact.agent",
                    "hs_office_membership#20002FirstGmbH-firstcontact.guest",
                    "hs_office_membership#20002FirstGmbH-firstcontact.owner",
                    "hs_office_membership#20002FirstGmbH-firstcontact.tenant"));
            assertThat(grantDisplaysOf(rawGrantRepo.findAll()))
                    .map(s -> s.replace("GmbH-firstcontact", ""))
                    .map(s -> s.replace("hs_office_", ""))
                    .containsExactlyInAnyOrder(Array.fromFormatted(
                            initialGrantNames,

                            // owner
                            "{ grant perm * on membership#20002First        to role membership#20002First.owner     by system and assume }",
                            "{ grant role membership#20002First.owner       to role global#global.admin             by system and assume }",

                            // admin
                            "{ grant perm edit on membership#20002First     to role membership#20002First.admin     by system and assume }",
                            "{ grant role membership#20002First.admin       to role membership#20002First.owner     by system and assume }",

                            // agent
                            "{ grant role membership#20002First.agent       to role membership#20002First.admin     by system and assume }",
                            "{ grant role partner#First.tenant              to role membership#20002First.agent     by system and assume }",
                            "{ grant role membership#20002First.agent       to role debitor#10001First.admin        by system and assume }",
                            "{ grant role membership#20002First.agent       to role partner#First.admin             by system and assume }",
                            "{ grant role debitor#10001First.tenant         to role membership#20002First.agent     by system and assume }",

                            // tenant
                            "{ grant role membership#20002First.tenant      to role membership#20002First.agent     by system and assume }",
                            "{ grant role partner#First.guest               to role membership#20002First.tenant    by system and assume }",
                            "{ grant role debitor#10001First.guest          to role membership#20002First.tenant    by system and assume }",
                            "{ grant role membership#20002First.tenant      to role debitor#10001First.agent        by system and assume }",
                            "{ grant role membership#20002First.tenant      to role partner#First.agent             by system and assume }",

                            // guest
                            "{ grant perm view on membership#20002First     to role membership#20002First.guest     by system and assume }",
                            "{ grant role membership#20002First.guest       to role membership#20002First.tenant    by system and assume }",
                            "{ grant role membership#20002First.guest       to role partner#First.tenant            by system and assume }",
                            "{ grant role membership#20002First.guest       to role debitor#10001First.tenant       by system and assume }",

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
            final var result = membershipRepo.findMembershipsByOptionalPartnerUuidAndOptionalMemberNumber(null, null);

            // then
            exactlyTheseMembershipsAreReturned(
                    result,
                    "Membership(10001, First GmbH, 10001, [2022-10-01,), NONE)",
                    "Membership(10002, Second e.K., 10002, [2022-10-01,), NONE)",
                    "Membership(10003, Third OHG, 10003, [2022-10-01,), NONE)");
        }

        @Test
        public void globalAdmin_withoutAssumedRole_canFindAllMembershipByPartnerUuid() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("First").get(0);

            // when
            final var result = membershipRepo.findMembershipsByOptionalPartnerUuidAndOptionalMemberNumber(
                    givenPartner.getUuid(),
                    null);

            // then
            exactlyTheseMembershipsAreReturned(result, "Membership(10001, First GmbH, 10001, [2022-10-01,), NONE)");
        }

        @Test
        public void globalAdmin_withoutAssumedRole_canFindAllMembershipByMemberNumber() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = membershipRepo.findMembershipsByOptionalPartnerUuidAndOptionalMemberNumber(null, 10002);

            // then
            exactlyTheseMembershipsAreReturned(result, "Membership(10002, Second e.K., 10002, [2022-10-01,), NONE)");
        }
    }

    @Nested
    class UpdateMembership {

        @Test
        public void globalAdmin_canUpdateValidityOfArbitraryMembership() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenMembership = givenSomeTemporaryMembership("First", "First");
            assertThatMembershipIsVisibleForUserWithRole(
                    givenMembership,
                    "hs_office_debitor#10001FirstGmbH-firstcontact.admin");
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
        public void debitorAdmin_canViewButNotUpdateRelatedMembership() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenMembership = givenSomeTemporaryMembership("First", "First");
            assertThatMembershipIsVisibleForUserWithRole(
                    givenMembership,
                    "hs_office_debitor#10001FirstGmbH-firstcontact.admin");
            assertThatMembershipExistsAndIsAccessibleToCurrentContext(givenMembership);
            final var newValidityEnd = LocalDate.now();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_office_debitor#10001FirstGmbH-firstcontact.admin");
                givenMembership.setValidity(Range.closedOpen(
                        givenMembership.getValidity().lower(), newValidityEnd));
                return membershipRepo.save(givenMembership);
            });

            // then
            result.assertExceptionWithRootCauseMessage(JpaSystemException.class,
                    "[403] Subject ", " is not allowed to update hs_office_membership uuid");
        }

        private void assertThatMembershipExistsAndIsAccessibleToCurrentContext(final HsOfficeMembershipEntity saved) {
            final var found = membershipRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().isNotSameAs(saved).usingRecursiveComparison().isEqualTo(saved);
        }

        private void assertThatMembershipIsVisibleForUserWithRole(
                final HsOfficeMembershipEntity entity,
                final String assumedRoles) {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", assumedRoles);
                assertThatMembershipExistsAndIsAccessibleToCurrentContext(entity);
            }).assertSuccessful();
        }

        private void assertThatMembershipIsNotVisibleForUserWithRole(
                final HsOfficeMembershipEntity entity,
                final String assumedRoles) {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", assumedRoles);
                final var found = membershipRepo.findByUuid(entity.getUuid());
                assertThat(found).isEmpty();
            }).assertSuccessful();
        }
    }

    @Nested
    class DeleteByUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canDeleteAnyMembership() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenMembership = givenSomeTemporaryMembership("First", "Second");

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
        public void nonGlobalAdmin_canNotDeleteTheirRelatedMembership() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenMembership = givenSomeTemporaryMembership("First", "Third");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_office_debitor#10003ThirdOHG-thirdcontact.admin");
                assumeThat(membershipRepo.findByUuid(givenMembership.getUuid())).isPresent();

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
            final var initialRoleNames = Array.from(roleNamesOf(rawRoleRepo.findAll()));
            final var initialGrantNames = Array.from(grantDisplaysOf(rawGrantRepo.findAll()));
            final var givenMembership = givenSomeTemporaryMembership("First", "First");
            assertThat(rawRoleRepo.findAll().size()).as("precondition failed: unexpected number of roles created")
                    .isEqualTo(initialRoleNames.length + 5);
            assertThat(rawGrantRepo.findAll().size()).as("precondition failed: unexpected number of grants created")
                    .isEqualTo(initialGrantNames.length + 18);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return membershipRepo.deleteByUuid(givenMembership.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isEqualTo(1);
            assertThat(roleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(initialRoleNames);
            assertThat(grantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(initialGrantNames);
        }
    }

    @Test
    public void auditJournalLogIsAvailable() {
        // given
        final var query = em.createNativeQuery("""
                select c.currenttask, j.targettable, j.targetop
                    from tx_journal j
                    join tx_context c on j.contextId = c.contextId
                    where targettable = 'hs_office_membership';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating Membership test-data FirstGmbH10001, hs_office_membership, INSERT]",
                "[creating Membership test-data Seconde.K.10002, hs_office_membership, INSERT]");
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        tempEntities.forEach(tempMembership -> {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", null);
                System.out.println("DELETING temporary membership: " + tempMembership.toString());
                membershipRepo.deleteByUuid(tempMembership.getUuid());
            });
        });
        jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net", null);
            em.createQuery("DELETE FROM HsOfficeMembershipEntity WHERE memberNumber >= 20000");
        });
    }

    private HsOfficeMembershipEntity givenSomeTemporaryMembership(final String partnerTradeName, final String debitorName) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike(partnerTradeName).get(0);
            final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike(debitorName).get(0);
            final var newMembership = HsOfficeMembershipEntity.builder()
                    .uuid(UUID.randomUUID())
                    .memberNumber(20002)
                    .partner(givenPartner)
                    .mainDebitor(givenDebitor)
                    .validity(Range.closedInfinite(LocalDate.parse("2020-01-01")))
                    .build();

            toCleanup(newMembership);

            return membershipRepo.save(newMembership);
        }).assertSuccessful().returnedValue();
    }

    private HsOfficeMembershipEntity toCleanup(final HsOfficeMembershipEntity tempEntity) {
        tempEntities.add(tempEntity);
        return tempEntity;
    }

    void exactlyTheseMembershipsAreReturned(
            final List<HsOfficeMembershipEntity> actualResult,
            final String... membershipNames) {
        assertThat(actualResult)
                .extracting(membershipEntity -> membershipEntity.toString())
                .containsExactlyInAnyOrder(membershipNames);
    }

    void allTheseMembershipsAreReturned(final List<HsOfficeMembershipEntity> actualResult, final String... membershipNames) {
        assertThat(actualResult)
                .extracting(membershipEntity -> membershipEntity.toString())
                .contains(membershipNames);
    }
}
