package net.hostsharing.hsadminng.hs.office.sepamandate;

import com.vladmihalcea.hibernate.type.range.Range;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountRepository;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
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
@ComponentScan(basePackageClasses = { HsOfficeSepaMandateRepository.class, Context.class, JpaAttempt.class })
@DirtiesContext
class HsOfficeSepaMandateRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    HsOfficeSepaMandateRepository sepaMandateRepo;

    @Autowired
    HsOfficeDebitorRepository debitorRepo;

    @Autowired
    HsOfficeBankAccountRepository bankAccountRepo;

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

    Set<HsOfficeSepaMandateEntity> tempEntities = new HashSet<>();

    @Nested
    class CreateSepaMandate {

        @Test
        public void testHostsharingAdmin_withoutAssumedRole_canCreateNewSepaMandate() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = sepaMandateRepo.count();
            final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike("First").get(0);
            final var givenBankAccount = bankAccountRepo.findByOptionalHolderLike("Paul Winkler").get(0);

            // when
            final var result = attempt(em, () -> {
                final var newSepaMandate = toCleanup(HsOfficeSepaMandateEntity.builder()
                        .uuid(UUID.randomUUID())
                        .debitor(givenDebitor)
                        .bankAccount(givenBankAccount)
                        .reference("temp ref A")
                        .validity(Range.closedOpen(
                                LocalDate.parse("2020-01-01"), LocalDate.parse("2023-01-01")))
                        .build());
                return sepaMandateRepo.save(newSepaMandate);
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficeSepaMandateEntity::getUuid).isNotNull();
            assertThatSepaMandateIsPersisted(result.returnedValue());
            assertThat(sepaMandateRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = roleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = grantDisplaysOf(rawGrantRepo.findAll()).stream()
                    .map(s -> s.replace("-firstcontact", "-..."))
                    .map(s -> s.replace("PaulWinkler", "Paul..."))
                    .map(s -> s.replace("hs_office_", ""))
                    .toList();

            // when
            attempt(em, () -> {
                final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike("First").get(0);
                final var givenBankAccount = bankAccountRepo.findByOptionalHolderLike("Paul Winkler").get(0);
                final var newSepaMandate = toCleanup(HsOfficeSepaMandateEntity.builder()
                        .uuid(UUID.randomUUID())
                        .debitor(givenDebitor)
                        .bankAccount(givenBankAccount)
                        .reference("temp ref B")
                        .validity(Range.closedOpen(
                                LocalDate.parse("2020-01-01"), LocalDate.parse("2023-01-01")))
                        .build());
                return sepaMandateRepo.save(newSepaMandate);
            });

            // then
            final var all = rawRoleRepo.findAll();
            assertThat(roleNamesOf(all)).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_office_sepamandate#temprefB.owner",
                    "hs_office_sepamandate#temprefB.admin",
                    "hs_office_sepamandate#temprefB.agent",
                    "hs_office_sepamandate#temprefB.tenant",
                    "hs_office_sepamandate#temprefB.guest"));
            assertThat(grantDisplaysOf(rawGrantRepo.findAll()))
                    .map(s -> s.replace("-firstcontact", "-..."))
                    .map(s -> s.replace("PaulWinkler", "Paul..."))
                    .map(s -> s.replace("hs_office_", ""))
                    .containsExactlyInAnyOrder(Array.fromFormatted(
                            initialGrantNames,

                            // owner
                            "{ grant perm * on sepamandate#temprefB         to role sepamandate#temprefB.owner           by system and assume }",
                            "{ grant role sepamandate#temprefB.owner        to role global#global.admin                 by system and assume }",

                            // admin
                            "{ grant perm edit on sepamandate#temprefB      to role sepamandate#temprefB.admin           by system and assume }",
                            "{ grant role sepamandate#temprefB.admin        to role sepamandate#temprefB.owner           by system and assume }",
                            "{ grant role bankaccount#Paul....tenant        to role sepamandate#temprefB.admin           by system and assume }",

                            // agent
                            "{ grant role sepamandate#temprefB.agent        to role sepamandate#temprefB.admin           by system and assume }",
                            "{ grant role debitor#10001FirstGmbH-....tenant to role sepamandate#temprefB.agent           by system and assume }",
                            "{ grant role sepamandate#temprefB.agent        to role bankaccount#Paul....admin           by system and assume }",
                            "{ grant role sepamandate#temprefB.agent        to role debitor#10001FirstGmbH-....admin    by system and assume }",

                            // tenant
                            "{ grant role sepamandate#temprefB.tenant       to role sepamandate#temprefB.agent           by system and assume }",
                            "{ grant role debitor#10001FirstGmbH-....guest  to role sepamandate#temprefB.tenant          by system and assume }",
                            "{ grant role bankaccount#Paul....guest         to role sepamandate#temprefB.tenant          by system and assume }",

                            // guest
                            "{ grant perm view on sepamandate#temprefB      to role sepamandate#temprefB.guest           by system and assume }",
                            "{ grant role sepamandate#temprefB.guest        to role sepamandate#temprefB.tenant          by system and assume }",
                            null));
        }

        private void assertThatSepaMandateIsPersisted(final HsOfficeSepaMandateEntity saved) {
            final var found = sepaMandateRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().usingRecursiveComparison().isEqualTo(saved);
        }
    }

    @Nested
    class FindAllSepaMandates {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllSepaMandates() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = sepaMandateRepo.findSepaMandateByOptionalIban(null);

            // then
            allTheseSepaMandatesAreReturned(
                    result,
                    "SEPA-Mandate(DE02120300000000202051, refFirstGmbH, [2022-10-01,2027-01-01))",
                    "SEPA-Mandate(DE02100500000054540402, refSeconde.K., [2022-10-01,2027-01-01))",
                    "SEPA-Mandate(DE02300209000106531065, refThirdOHG, [2022-10-01,2027-01-01))");
        }

        @Test
        public void normalUser_canViewOnlyRelatedSepaMandates() {
            // given:
            context("bankaccount-admin@FirstGmbH.example.com");

            // when:
            final var result = sepaMandateRepo.findSepaMandateByOptionalIban(null);

            // then:
            exactlyTheseSepaMandatesAreReturned(
                    result,
                    "SEPA-Mandate(DE02120300000000202051, refFirstGmbH, [2022-10-01,2027-01-01))");
        }
    }

    @Nested
    class FindByNameLike {

        @Test
        public void globalAdmin_canViewAllSepaMandates() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = sepaMandateRepo.findSepaMandateByOptionalIban(null);

            // then
            exactlyTheseSepaMandatesAreReturned(
                    result,
                    "SEPA-Mandate(DE02120300000000202051, refFirstGmbH, [2022-10-01,2027-01-01))",
                    "SEPA-Mandate(DE02100500000054540402, refSeconde.K., [2022-10-01,2027-01-01))",
                    "SEPA-Mandate(DE02300209000106531065, refThirdOHG, [2022-10-01,2027-01-01))");
        }

        @Test
        public void bankAccountAdmin_canViewRelatedSepaMandates() {
            // given
            context("bankaccount-admin@ThirdOHG.example.com");

            // when
            final var result = sepaMandateRepo.findSepaMandateByOptionalIban(null);

            // then
            exactlyTheseSepaMandatesAreReturned(
                    result,
                    "SEPA-Mandate(DE02300209000106531065, refThirdOHG, [2022-10-01,2027-01-01))");
        }
    }

    @Nested
    class UpdateSepaMandate {

        @Test
        public void hostsharingAdmin_canUpdateValidityOfArbitrarySepaMandate() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenSepaMandate = givenSomeTemporarySepaMandateBessler("Peter Smith");
            assertThatSepaMandateIsVisibleForUserWithRole(
                    givenSepaMandate,
                    "hs_office_bankaccount#PeterSmith.admin");
            assertThatSepaMandateActuallyInDatabase(givenSepaMandate);
            final var newValidityEnd = LocalDate.now();

            // when
            context("superuser-alex@hostsharing.net");
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                givenSepaMandate.setValidity(Range.closedOpen(
                        givenSepaMandate.getValidity().lower(), newValidityEnd));
                return toCleanup(sepaMandateRepo.save(givenSepaMandate));
            });

            // then
            result.assertSuccessful();

            sepaMandateRepo.deleteByUuid(givenSepaMandate.getUuid());
        }

        @Test
        public void bankAccountAdmin_canViewButNotUpdateRelatedSepaMandate() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenSepaMandate = givenSomeTemporarySepaMandateBessler("Anita Bessler");
            assertThatSepaMandateIsVisibleForUserWithRole(
                    givenSepaMandate,
                    "hs_office_bankaccount#AnitaBessler.admin");
            assertThatSepaMandateActuallyInDatabase(givenSepaMandate);
            final var newValidityEnd = LocalDate.now();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_office_bankaccount#AnitaBessler.admin");
                givenSepaMandate.setValidity(Range.closedOpen(
                        givenSepaMandate.getValidity().lower(), newValidityEnd));
                return sepaMandateRepo.save(givenSepaMandate);
            });

            // then
            result.assertExceptionWithRootCauseMessage(JpaSystemException.class,
                    "[403] Subject ", " is not allowed to update hs_office_sepamandate uuid");
        }

        private void assertThatSepaMandateActuallyInDatabase(final HsOfficeSepaMandateEntity saved) {
            final var found = sepaMandateRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().isNotSameAs(saved)
                    .extracting(Object::toString).isEqualTo(saved.toString());
        }

        private void assertThatSepaMandateIsVisibleForUserWithRole(
                final HsOfficeSepaMandateEntity entity,
                final String assumedRoles) {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", assumedRoles);
                assertThatSepaMandateActuallyInDatabase(entity);
            }).assertSuccessful();
        }

        private void assertThatSepaMandateIsNotVisibleForUserWithRole(
                final HsOfficeSepaMandateEntity entity,
                final String assumedRoles) {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", assumedRoles);
                final var found = sepaMandateRepo.findByUuid(entity.getUuid());
                assertThat(found).isEmpty();
            }).assertSuccessful();
        }
    }

    @Nested
    class DeleteByUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canDeleteAnySepaMandate() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenSepaMandate = givenSomeTemporarySepaMandateBessler("Fourth e.G.");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                sepaMandateRepo.deleteByUuid(givenSepaMandate.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-fran@hostsharing.net", null);
                return sepaMandateRepo.findByUuid(givenSepaMandate.getUuid());
            }).assertSuccessful().returnedValue()).isEmpty();
        }

        @Test
        public void nonGlobalAdmin_canNotDeleteTheirRelatedSepaMandate() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenSepaMandate = givenSomeTemporarySepaMandateBessler("Third OHG");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("bankaccount-admin@ThirdOHG.example.com");
                assumeThat(sepaMandateRepo.findByUuid(givenSepaMandate.getUuid())).isPresent();

                sepaMandateRepo.deleteByUuid(givenSepaMandate.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " not allowed to delete hs_office_sepamandate");
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return sepaMandateRepo.findByUuid(givenSepaMandate.getUuid());
            }).assertSuccessful().returnedValue()).isPresent(); // still there
        }

        @Test
        public void deletingASepaMandateAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = Array.from(roleNamesOf(rawRoleRepo.findAll()));
            final var initialGrantNames = Array.from(grantDisplaysOf(rawGrantRepo.findAll()));
            final var givenSepaMandate = givenSomeTemporarySepaMandateBessler("Mel Bessler");
            assertThat(rawRoleRepo.findAll().size()).as("precondition failed: unexpected number of roles created")
                    .isEqualTo(initialRoleNames.length + 5);
            assertThat(rawGrantRepo.findAll().size()).as("precondition failed: unexpected number of grants created")
                    .isEqualTo(initialGrantNames.length + 14);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return sepaMandateRepo.deleteByUuid(givenSepaMandate.getUuid());
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
                    where targettable = 'hs_office_sepamandate';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating SEPA-mandate test-data FirstGmbH, hs_office_sepamandate, INSERT]",
                "[creating SEPA-mandate test-data Seconde.K., hs_office_sepamandate, INSERT]");
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        tempEntities.forEach(tempSepaMandate -> {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", null);
                System.out.println("DELETING temporary sepaMandate: " + tempSepaMandate.toString());
                sepaMandateRepo.deleteByUuid(tempSepaMandate.getUuid());
            });
        });
        jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net", null);
            em.createQuery("DELETE FROM HsOfficeSepaMandateEntity WHERE reference like 'temp ref%'");
        });
    }

    private HsOfficeSepaMandateEntity givenSomeTemporarySepaMandateBessler(final String bankAccountHolder) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike("First").get(0);
            final var givenBankAccount = bankAccountRepo.findByOptionalHolderLike(bankAccountHolder).get(0);
            final var newSepaMandate = HsOfficeSepaMandateEntity.builder()
                    .uuid(UUID.randomUUID())
                    .debitor(givenDebitor)
                    .bankAccount(givenBankAccount)
                    .reference("temp ref X")
                    .validity(Range.closedOpen(
                            LocalDate.parse("2020-01-01"), LocalDate.parse("2023-01-01")))
                    .build();

            toCleanup(newSepaMandate);

            return sepaMandateRepo.save(newSepaMandate);
        }).assertSuccessful().returnedValue();
    }

    private HsOfficeSepaMandateEntity toCleanup(final HsOfficeSepaMandateEntity tempEntity) {
        tempEntities.add(tempEntity);
        return tempEntity;
    }

    void exactlyTheseSepaMandatesAreReturned(
            final List<HsOfficeSepaMandateEntity> actualResult,
            final String... sepaMandateNames) {
        assertThat(actualResult)
                .extracting(sepaMandateEntity -> sepaMandateEntity.toString())
                .containsExactlyInAnyOrder(sepaMandateNames);
    }

    void allTheseSepaMandatesAreReturned(final List<HsOfficeSepaMandateEntity> actualResult, final String... sepaMandateNames) {
        assertThat(actualResult)
                .extracting(sepaMandateEntity -> sepaMandateEntity.toString())
                .contains(sepaMandateNames);
    }
}
