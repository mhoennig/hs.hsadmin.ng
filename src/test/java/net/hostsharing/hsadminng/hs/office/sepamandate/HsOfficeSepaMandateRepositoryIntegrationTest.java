package net.hostsharing.hsadminng.hs.office.sepamandate;

import io.hypersistence.utils.hibernate.type.range.Range;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountRepository;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.grant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.role.RawRbacRoleRepository;
import net.hostsharing.hsadminng.mapper.Array;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaSystemException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static net.hostsharing.hsadminng.rbac.grant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.role.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.hsadminng.mapper.Array.fromFormatted;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({ Context.class, JpaAttempt.class })
@Tag("officeIntegrationTest")
class HsOfficeSepaMandateRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

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
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @MockitoBean
    HttpServletRequest request;

    @Nested
    class CreateSepaMandate {

        @Test
        public void testHostsharingAdmin_withoutAssumedRole_canCreateNewSepaMandate() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = sepaMandateRepo.count();
            final var givenDebitor = debitorRepo.findDebitorsByOptionalNameLike("First").get(0);
            final var givenBankAccount = bankAccountRepo.findByOptionalHolderLike("Paul Winkler").get(0);

            // when
            final var result = attempt(em, () -> {
                final var newSepaMandate = HsOfficeSepaMandateEntity.builder()
                        .debitor(givenDebitor)
                        .bankAccount(givenBankAccount)
                        .reference("temp ref A")
                        .agreement(LocalDate.parse("2020-01-02"))
                        .validity(Range.closedOpen(
                                LocalDate.parse("2020-01-01"), LocalDate.parse("2023-01-01")))
                        .build();
                return toCleanup(sepaMandateRepo.save(newSepaMandate));
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
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll()).stream()
                    .map(s -> s.replace("hs_office.", ""))
                    .toList();

            // when
            attempt(em, () -> {
                final var givenDebitor = debitorRepo.findDebitorsByOptionalNameLike("First").get(0);
                final var givenBankAccount = bankAccountRepo.findByOptionalHolderLike("Paul Winkler").get(0);
                final var newSepaMandate = HsOfficeSepaMandateEntity.builder()
                        .debitor(givenDebitor)
                        .bankAccount(givenBankAccount)
                        .reference("temp ref B")
                        .agreement(LocalDate.parse("2020-01-02"))
                        .validity(Range.closedOpen(
                                LocalDate.parse("2020-01-01"), LocalDate.parse("2023-01-01")))
                        .build();
                return toCleanup(sepaMandateRepo.save(newSepaMandate));
            });

            // then
            final var all = rawRoleRepo.findAll();
            assertThat(distinctRoleNamesOf(all)).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_office.sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):ADMIN",
                    "hs_office.sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):AGENT",
                    "hs_office.sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):OWNER",
                    "hs_office.sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):REFERRER"));
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll()))
                    .map(s -> s.replace("hs_office.", ""))
                    .containsExactlyInAnyOrder(fromFormatted(
                            initialGrantNames,

                            // owner
                            "{ grant perm:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):DELETE to role:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):OWNER by system and assume }",
                            "{ grant role:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):OWNER to role:rbac.global#global:ADMIN by system and assume }",
                            "{ grant role:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):OWNER to user:superuser-alex@hostsharing.net by sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):OWNER and assume }",

                            // admin
                            "{ grant perm:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):UPDATE to role:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):ADMIN by system and assume }",
                            "{ grant role:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):ADMIN to role:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):OWNER by system and assume }",

                            // agent
                            "{ grant role:bankaccount#DE02600501010002034304:REFERRER to role:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):AGENT by system and assume }",
                            "{ grant role:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):AGENT to role:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):ADMIN by system and assume }",
                            "{ grant role:relation#FirstGmbH-with-DEBITOR-FirstGmbH:AGENT to role:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):AGENT by system and assume }",

                            // referrer
                            "{ grant perm:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):SELECT to role:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):REFERRER by system and assume }",
                            "{ grant role:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):REFERRER to role:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):AGENT by system and assume }",
                            "{ grant role:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):REFERRER to role:bankaccount#DE02600501010002034304:ADMIN by system and assume }",
                            "{ grant role:relation#FirstGmbH-with-DEBITOR-FirstGmbH:TENANT to role:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):REFERRER by system and assume }",
                            "{ grant role:sepamandate#DE02600501010002034304-[2020-01-01,2023-01-01):REFERRER to role:relation#FirstGmbH-with-DEBITOR-FirstGmbH:AGENT by system and assume }",

                            null));
        }

        private void assertThatSepaMandateIsPersisted(final HsOfficeSepaMandateEntity saved) {
            final var found = sepaMandateRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().extracting(Object::toString).isEqualTo(saved.toString());
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
                    "SEPA-Mandate(DE02100500000054540402, ref-10002-12, 2022-09-30, [2022-10-01,2027-01-01))",
                    "SEPA-Mandate(DE02120300000000202051, ref-10001-11, 2022-09-30, [2022-10-01,2027-01-01))",
                    "SEPA-Mandate(DE02300209000106531065, ref-10003-13, 2022-09-30, [2022-10-01,2027-01-01))");
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
                    "SEPA-Mandate(DE02120300000000202051, ref-10001-11, 2022-09-30, [2022-10-01,2027-01-01))");
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
                    "SEPA-Mandate(DE02100500000054540402, ref-10002-12, 2022-09-30, [2022-10-01,2027-01-01))",
                    "SEPA-Mandate(DE02120300000000202051, ref-10001-11, 2022-09-30, [2022-10-01,2027-01-01))",
                    "SEPA-Mandate(DE02300209000106531065, ref-10003-13, 2022-09-30, [2022-10-01,2027-01-01))");
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
                    "SEPA-Mandate(DE02300209000106531065, ref-10003-13, 2022-09-30, [2022-10-01,2027-01-01))");
        }
    }

    @Nested
    class UpdateSepaMandate {

        @Test
        public void hostsharingAdmin_canUpdateArbitrarySepaMandate() {
            // given
            final var givenSepaMandate = givenSomeTemporarySepaMandate("DE02600501010002034304");
            assertThatSepaMandateIsVisibleForUserWithRole(
                    givenSepaMandate,
                    "hs_office.bankaccount#DE02600501010002034304:ADMIN");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                givenSepaMandate.setReference("temp ref X - updated");
                givenSepaMandate.setAgreement(LocalDate.parse("2019-05-13"));
                givenSepaMandate.setValidity(Range.closedOpen(
                        LocalDate.parse("2019-05-17"), LocalDate.parse("2023-01-01")));
                return toCleanup(sepaMandateRepo.save(givenSepaMandate));
            });

            // then
            result.assertSuccessful();
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                assertThat(sepaMandateRepo.findByUuid(givenSepaMandate.getUuid())).isNotEmpty().get()
                        .extracting(Object::toString).isEqualTo(givenSepaMandate.toString());
            }).assertSuccessful();
        }

        @Test
        public void bankAccountAdmin_canViewButNotUpdateRelatedSepaMandate() {
            // given
            context("superuser-alex@hostsharing.net");

            final var givenSepaMandate = givenSomeTemporarySepaMandate("DE02300606010002474689");
            assertThatSepaMandateIsVisibleForUserWithRole(
                    givenSepaMandate,
                    "hs_office.bankaccount#DE02300606010002474689:ADMIN");
            assertThatSepaMandateActuallyInDatabase(givenSepaMandate);
            final var newValidityEnd = LocalDate.now();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_office.bankaccount#DE02300606010002474689:ADMIN");

                givenSepaMandate.setValidity(Range.closedOpen(
                        givenSepaMandate.getValidity().lower(), newValidityEnd));
                return toCleanup(sepaMandateRepo.save(givenSepaMandate));
            });

            // then
            result.assertExceptionWithRootCauseMessage(JpaSystemException.class,
                    "[403] Subject ", " is not allowed to update hs_office.sepamandate uuid");
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
            final var givenSepaMandate = givenSomeTemporarySepaMandate("DE02200505501015871393");

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
            final var givenSepaMandate = givenSomeTemporarySepaMandate("DE02300209000106531065");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("bankaccount-admin@ThirdOHG.example.com");
                assertThat(sepaMandateRepo.findByUuid(givenSepaMandate.getUuid())).isPresent();

                sepaMandateRepo.deleteByUuid(givenSepaMandate.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " not allowed to delete hs_office.sepamandate");
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return sepaMandateRepo.findByUuid(givenSepaMandate.getUuid());
            }).assertSuccessful().returnedValue()).isPresent(); // still there
        }

        @Test
        public void deletingASepaMandateAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = Array.from(distinctRoleNamesOf(rawRoleRepo.findAll()));
            final var initialGrantNames = Array.from(distinctGrantDisplaysOf(rawGrantRepo.findAll()));
            final var givenSepaMandate = givenSomeTemporarySepaMandate("DE02600501010002034304");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return sepaMandateRepo.deleteByUuid(givenSepaMandate.getUuid());
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
                select currentTask, targetTable, targetOp, targetdelta->>'reference'
                    from base.tx_journal_v
                    where targettable = 'hs_office.sepamandate';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating SEPA-mandate test-data, hs_office.sepamandate, INSERT, ref-10001-11]",
                "[creating SEPA-mandate test-data, hs_office.sepamandate, INSERT, ref-10002-12]",
                "[creating SEPA-mandate test-data, hs_office.sepamandate, INSERT, ref-10003-13]");
    }

    private HsOfficeSepaMandateEntity givenSomeTemporarySepaMandate(final String iban) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorsByOptionalNameLike("First").get(0);
            final var givenBankAccount = bankAccountRepo.findByIbanOrderByIbanAsc(iban).get(0);
            final var newSepaMandate = HsOfficeSepaMandateEntity.builder()
                    .debitor(givenDebitor)
                    .bankAccount(givenBankAccount)
                    .reference("temp ref X")
                    .agreement(LocalDate.parse("2020-01-02"))
                    .validity(Range.closedOpen(
                            LocalDate.parse("2020-01-01"), LocalDate.parse("2023-01-01")))
                    .build();

            return toCleanup(sepaMandateRepo.save(newSepaMandate));
        }).assertSuccessful().returnedValue();
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
