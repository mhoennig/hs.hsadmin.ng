package net.hostsharing.hsadminng.hs.office.debitor;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountRepository;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRepository;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRepository;
import net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleRepository;
import net.hostsharing.test.Array;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.grantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.roleNamesOf;
import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
class HsOfficeDebitorRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    HsOfficeDebitorRepository debitorRepo;

    @Autowired
    HsOfficePartnerRepository partnerRepo;

    @Autowired
    HsOfficeContactRepository contactRepo;

    @Autowired
    HsOfficeBankAccountRepository bankAccountRepo;

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
    class CreateDebitor {

        @Test
        public void globalAdmin_canCreateNewDebitor() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = debitorRepo.count();
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("First GmbH").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("first contact").get(0);

            // when
            final var result = attempt(em, () -> {
                final var newDebitor = HsOfficeDebitorEntity.builder()
                        .debitorNumberSuffix((byte)21)
                        .partner(givenPartner)
                        .billingContact(givenContact)
                        .defaultPrefix("abc")
                        .billable(false)
                        .build();
                return debitorRepo.save(newDebitor);
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficeDebitorEntity::getUuid).isNotNull();
            assertThatDebitorIsPersisted(result.returnedValue());
            assertThat(debitorRepo.count()).isEqualTo(count + 1);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "a", "ab", "a12", "123", "12a"})
        @Transactional
        public void canNotCreateNewDebitorWithInvalidDefaultPrefix(final String givenPrefix) {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = debitorRepo.count();
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("First GmbH").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("first contact").get(0);

            // when
            final var result = attempt(em, () -> {
                final var newDebitor = HsOfficeDebitorEntity.builder()
                        .debitorNumberSuffix((byte)21)
                        .partner(givenPartner)
                        .billingContact(givenContact)
                        .billable(true)
                        .vatReverseCharge(false)
                        .vatBusiness(false)
                        .defaultPrefix(givenPrefix)
                        .build();
                return debitorRepo.save(newDebitor);
            });

            // then
            result.assertExceptionWithRootCauseMessage(org.hibernate.exception.ConstraintViolationException.class);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = roleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = grantDisplaysOf(rawGrantRepo.findAll()).stream()
                    // some search+replace to make the output fit into the screen width
                    .map(s -> s.replace("superuser-alex@hostsharing.net", "superuser-alex"))
                    .map(s -> s.replace("22Fourthe.G.-forthcontact", "FeG"))
                    .map(s -> s.replace("Fourthe.G.-forthcontact", "FeG"))
                    .map(s -> s.replace("forthcontact", "4th"))
                    .map(s -> s.replace("hs_office_", ""))
                    .toList();

            // when
            attempt(em, () -> {
                final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("Fourth").get(0);
                final var givenContact = contactRepo.findContactByOptionalLabelLike("forth contact").get(0);
                final var newDebitor = HsOfficeDebitorEntity.builder()
                        .debitorNumberSuffix((byte)22)
                        .partner(givenPartner)
                        .billingContact(givenContact)
                        .defaultPrefix("abc")
                        .billable(false)
                        .build();
                return debitorRepo.save(newDebitor);
            }).assertSuccessful();

            // then
            assertThat(roleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_office_debitor#1000422:Fourthe.G.-forthcontact.owner",
                    "hs_office_debitor#1000422:Fourthe.G.-forthcontact.admin",
                    "hs_office_debitor#1000422:Fourthe.G.-forthcontact.agent",
                    "hs_office_debitor#1000422:Fourthe.G.-forthcontact.tenant",
                    "hs_office_debitor#1000422:Fourthe.G.-forthcontact.guest"));
            assertThat(grantDisplaysOf(rawGrantRepo.findAll()))
                    .map(s -> s.replace("superuser-alex@hostsharing.net", "superuser-alex"))
                    .map(s -> s.replace("22Fourthe.G.-forthcontact", "FeG"))
                    .map(s -> s.replace("Fourthe.G.-forthcontact", "FeG"))
                    .map(s -> s.replace("forthcontact", "4th"))
                    .map(s -> s.replace("hs_office_", ""))
                    .containsExactlyInAnyOrder(Array.fromFormatted(
                            initialGrantNames,
                            // owner
                            "{ grant perm * on debitor#1000422:FeG      to role debitor#1000422:FeG.owner   by system and assume }",
                            "{ grant role debitor#1000422:FeG.owner     to role global#global.admin by system and assume }",
                            "{ grant role debitor#1000422:FeG.owner     to user superuser-alex      by global#global.admin and assume }",

                            // admin
                            "{ grant perm edit on debitor#1000422:FeG   to role debitor#1000422:FeG.admin   by system and assume }",
                            "{ grant role debitor#1000422:FeG.admin     to role debitor#1000422:FeG.owner   by system and assume }",

                            // agent
                            "{ grant role debitor#1000422:FeG.agent     to role debitor#1000422:FeG.admin   by system and assume }",
                            "{ grant role debitor#1000422:FeG.agent     to role contact#4th.admin   by system and assume }",
                            "{ grant role debitor#1000422:FeG.agent     to role partner#10004:FeG.admin   by system and assume }",

                            // tenant
                            "{ grant role contact#4th.guest     to role debitor#1000422:FeG.tenant  by system and assume }",
                            "{ grant role debitor#1000422:FeG.tenant    to role debitor#1000422:FeG.agent   by system and assume }",
                            "{ grant role debitor#1000422:FeG.tenant    to role partner#10004:FeG.agent   by system and assume }",
                            "{ grant role partner#10004:FeG.tenant    to role debitor#1000422:FeG.tenant  by system and assume }",

                            // guest
                            "{ grant perm view on debitor#1000422:FeG   to role debitor#1000422:FeG.guest   by system and assume }",
                            "{ grant role debitor#1000422:FeG.guest     to role debitor#1000422:FeG.tenant  by system and assume }",

                            null));
        }

        private void assertThatDebitorIsPersisted(final HsOfficeDebitorEntity saved) {
            final var found = debitorRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().usingRecursiveComparison().isEqualTo(saved);
        }
    }

    @Nested
    class FindByOptionalName {

        @Test
        public void globalAdmin_canViewAllDebitors() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = debitorRepo.findDebitorByOptionalNameLike(null);

            // then
            allTheseDebitorsAreReturned(
                    result,
                    "debitor(1000111: LP First GmbH: fir)",
                    "debitor(1000212: LP Second e.K.: sec)",
                    "debitor(1000313: IF Third OHG: thi)");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "hs_office_partner#10001:FirstGmbH-firstcontact.admin",
                "hs_office_person#FirstGmbH.admin",
                "hs_office_contact#firstcontact.admin",
        })
        public void relatedPersonAdmin_canViewRelatedDebitors(final String assumedRole) {
            // given:
            context("superuser-alex@hostsharing.net", assumedRole);

            // when:
            final var result = debitorRepo.findDebitorByOptionalNameLike(null);

            // then:
            exactlyTheseDebitorsAreReturned(result,
                    "debitor(1000111: LP First GmbH: fir)",
                    "debitor(1000120: LP First GmbH: fif)");
        }

        @Test
        public void unrelatedUser_canNotViewAnyDebitor() {
            // given:
            context("selfregistered-test-user@hostsharing.org");

            // when:
            final var result = debitorRepo.findDebitorByOptionalNameLike(null);

            // then:
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByDebitorNumberLike {

        @Test
        public void globalAdmin_canViewAllDebitors() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = debitorRepo.findDebitorByDebitorNumber(1000313);

            // then
            exactlyTheseDebitorsAreReturned(result, "debitor(1000313: IF Third OHG: thi)");
        }
    }

    @Nested
    class FindByNameLike {

        @Test
        public void globalAdmin_canViewAllDebitors() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = debitorRepo.findDebitorByOptionalNameLike("third contact");

            // then
            exactlyTheseDebitorsAreReturned(result, "debitor(1000313: IF Third OHG: thi)");
        }
    }

    @Nested
    class UpdateDebitor {

        @Test
        public void globalAdmin_canUpdateArbitraryDebitor() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "fifth contact", "Fourth", "fif");
            assertThatDebitorIsVisibleForUserWithRole(
                    givenDebitor,
                    "hs_office_partner#10004:Fourthe.G.-forthcontact.admin");
            assertThatDebitorActuallyInDatabase(givenDebitor);
            final var givenNewPartner = partnerRepo.findPartnerByOptionalNameLike("First").get(0);
            final var givenNewContact = contactRepo.findContactByOptionalLabelLike("sixth contact").get(0);
            final var givenNewBankAccount = bankAccountRepo.findByOptionalHolderLike("first").get(0);
            final String givenNewVatId = "NEW-VAT-ID";
            final String givenNewVatCountryCode = "NC";
            final boolean givenNewVatBusiness = !givenDebitor.isVatBusiness();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                givenDebitor.setPartner(givenNewPartner);
                givenDebitor.setBillingContact(givenNewContact);
                givenDebitor.setRefundBankAccount(givenNewBankAccount);
                givenDebitor.setVatId(givenNewVatId);
                givenDebitor.setVatCountryCode(givenNewVatCountryCode);
                givenDebitor.setVatBusiness(givenNewVatBusiness);
                return debitorRepo.save(givenDebitor);
            });

            // then
            result.assertSuccessful();
            assertThatDebitorIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "global#global.admin");

            // ... partner role was reassigned:
            assertThatDebitorIsNotVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_partner#10004:Fourthe.G.-forthcontact.agent");
            assertThatDebitorIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_partner#10001:FirstGmbH-firstcontact.agent");

            // ... contact role was reassigned:
            assertThatDebitorIsNotVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_contact#fifthcontact.admin");
            assertThatDebitorIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_contact#sixthcontact.admin");

            // ... bank-account role was reassigned:
            assertThatDebitorIsNotVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_bankaccount#Fourthe.G..admin");
            assertThatDebitorIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_bankaccount#FirstGmbH.admin");
        }

        @Test
        public void globalAdmin_canUpdateNullRefundBankAccountToNotNullBankAccountForArbitraryDebitor() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "fifth contact", null, "fig");
            assertThatDebitorIsVisibleForUserWithRole(
                    givenDebitor,
                    "hs_office_partner#10004:Fourthe.G.-forthcontact.admin");
            assertThatDebitorActuallyInDatabase(givenDebitor);
            final var givenNewBankAccount = bankAccountRepo.findByOptionalHolderLike("first").get(0);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                givenDebitor.setRefundBankAccount(givenNewBankAccount);
                return debitorRepo.save(givenDebitor);
            });

            // then
            result.assertSuccessful();
            assertThatDebitorIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "global#global.admin");

            // ... bank-account role was assigned:
            assertThatDebitorIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_bankaccount#FirstGmbH.admin");
        }

        @Test
        public void globalAdmin_canUpdateRefundBankAccountToNullForArbitraryDebitor() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "fifth contact", "Fourth", "fih");
            assertThatDebitorIsVisibleForUserWithRole(
                    givenDebitor,
                    "hs_office_partner#10004:Fourthe.G.-forthcontact.admin");
            assertThatDebitorActuallyInDatabase(givenDebitor);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                givenDebitor.setRefundBankAccount(null);
                return debitorRepo.save(givenDebitor);
            });

            // then
            result.assertSuccessful();
            assertThatDebitorIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "global#global.admin");

            // ... bank-account role was removed from previous bank-account admin:
            assertThatDebitorIsNotVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_bankaccount#Fourthe.G..admin");
        }

        @Test
        public void partnerAdmin_canNotUpdateRelatedDebitor() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "eighth", "Fourth", "eig");
            assertThatDebitorIsVisibleForUserWithRole(
                    givenDebitor,
                    "hs_office_partner#10004:Fourthe.G.-forthcontact.admin");
            assertThatDebitorActuallyInDatabase(givenDebitor);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_office_partner#10004:Fourthe.G.-forthcontact.admin");
                givenDebitor.setVatId("NEW-VAT-ID");
                return debitorRepo.save(givenDebitor);
            });

            // then
            result.assertExceptionWithRootCauseMessage(JpaSystemException.class,
                    "[403] Subject ", " is not allowed to update hs_office_debitor uuid");
        }

        @Test
        public void contactAdmin_canNotUpdateRelatedDebitor() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "ninth", "Fourth", "nin");
            assertThatDebitorIsVisibleForUserWithRole(
                    givenDebitor,
                    "hs_office_contact#ninthcontact.admin");
            assertThatDebitorActuallyInDatabase(givenDebitor);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_office_contact#ninthcontact.admin");
                givenDebitor.setVatId("NEW-VAT-ID");
                return debitorRepo.save(givenDebitor);
            });

            // then
            result.assertExceptionWithRootCauseMessage(JpaSystemException.class,
                    "[403] Subject ", " is not allowed to update hs_office_debitor uuid");
        }

        private void assertThatDebitorActuallyInDatabase(final HsOfficeDebitorEntity saved) {
            final var found = debitorRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().isNotSameAs(saved)
                    .extracting(Object::toString).isEqualTo(saved.toString());
        }

        private void assertThatDebitorIsVisibleForUserWithRole(
                final HsOfficeDebitorEntity entity,
                final String assumedRoles) {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", assumedRoles);
                assertThatDebitorActuallyInDatabase(entity);
            }).assertSuccessful();
        }

        private void assertThatDebitorIsNotVisibleForUserWithRole(
                final HsOfficeDebitorEntity entity,
                final String assumedRoles) {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", assumedRoles);
                final var found = debitorRepo.findByUuid(entity.getUuid());
                assertThat(found).isEmpty();
            }).assertSuccessful();
        }
    }

    @Nested
    class DeleteByUuid {

        @Test
        public void globalAdmin_canDeleteAnyDebitor() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "tenth", "Fourth", "ten");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                debitorRepo.deleteByUuid(givenDebitor.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-fran@hostsharing.net", null);
                return debitorRepo.findByUuid(givenDebitor.getUuid());
            }).assertSuccessful().returnedValue()).isEmpty();
        }

        @Test
        public void relatedPerson_canNotDeleteTheirRelatedDebitor() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "eleventh", "Fourth", "ele");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("person-Fourthe.G.@example.com");
                assertThat(debitorRepo.findByUuid(givenDebitor.getUuid())).isPresent();

                debitorRepo.deleteByUuid(givenDebitor.getUuid());
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] Subject ", " not allowed to delete hs_office_debitor");
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return debitorRepo.findByUuid(givenDebitor.getUuid());
            }).assertSuccessful().returnedValue()).isPresent(); // still there
        }

        @Test
        public void deletingADebitorAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = Array.from(roleNamesOf(rawRoleRepo.findAll()));
            final var initialGrantNames = Array.from(grantDisplaysOf(rawGrantRepo.findAll()));
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "twelfth", "Fourth", "twe");
            assertThat(rawRoleRepo.findAll().size()).as("precondition failed: unexpected number of roles created")
                    .isEqualTo(initialRoleNames.length + 5);
            assertThat(rawGrantRepo.findAll().size()).as("precondition failed: unexpected number of grants created")
                    .isEqualTo(initialGrantNames.length + 17);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return debitorRepo.deleteByUuid(givenDebitor.getUuid());
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
                    where targettable = 'hs_office_debitor';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating debitor test-data FirstGmbH-firstcontact, hs_office_debitor, INSERT]",
                "[creating debitor test-data Seconde.K.-secondcontact, hs_office_debitor, INSERT]");
    }

    private HsOfficeDebitorEntity givenSomeTemporaryDebitor(
            final String partner,
            final String contact,
            final String bankAccount,
            final String defaultPrefix) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike(partner).get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike(contact).get(0);
            final var givenBankAccount =
                    bankAccount != null ? bankAccountRepo.findByOptionalHolderLike(bankAccount).get(0) : null;
            final var newDebitor = HsOfficeDebitorEntity.builder()
                    .debitorNumberSuffix((byte)20)
                    .partner(givenPartner)
                    .billingContact(givenContact)
                    .refundBankAccount(givenBankAccount)
                    .defaultPrefix(defaultPrefix)
                    .billable(true)
                    .build();

            return debitorRepo.save(newDebitor);
        }).assertSuccessful().returnedValue();
    }

    void exactlyTheseDebitorsAreReturned(final List<HsOfficeDebitorEntity> actualResult, final String... debitorNames) {
        assertThat(actualResult)
                .extracting(HsOfficeDebitorEntity::toString)
                .containsExactlyInAnyOrder(debitorNames);
    }

    void allTheseDebitorsAreReturned(final List<HsOfficeDebitorEntity> actualResult, final String... debitorNames) {
        assertThat(actualResult)
                .extracting(HsOfficeDebitorEntity::toString)
                .contains(debitorNames);
    }
}
