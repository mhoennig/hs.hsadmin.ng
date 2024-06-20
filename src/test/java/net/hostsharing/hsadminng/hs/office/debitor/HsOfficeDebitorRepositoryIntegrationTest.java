package net.hostsharing.hsadminng.hs.office.debitor;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountRepository;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRepository;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRepository;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationEntity;
import net.hostsharing.hsadminng.hs.office.relation.HsOfficeRelationType;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacgrant.RbacGrantsDiagramService;
import net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleRepository;
import net.hostsharing.hsadminng.mapper.Array;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

import static net.hostsharing.hsadminng.rbac.test.EntityList.one;
import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class, RbacGrantsDiagramService.class })
class HsOfficeDebitorRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    HsOfficeDebitorRepository debitorRepo;

    @Autowired
    HsOfficePartnerRepository partnerRepo;

    @Autowired
    HsOfficeContactRepository contactRepo;

    @Autowired
    HsOfficePersonRepository personRepo;

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

    @Autowired
    RbacGrantsDiagramService mermaidService;

    @MockBean
    HttpServletRequest request;
    @Nested
    class CreateDebitor {

        @Test
        public void globalAdmin_canCreateNewDebitor() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = debitorRepo.count();
            final var givenPartnerPerson = one(personRepo.findPersonByOptionalNameLike("First GmbH"));
            final var givenContact = one(contactRepo.findContactByOptionalCaptionLike("first contact"));

            // when
            final var result = attempt(em, () -> {
                final var newDebitor = HsOfficeDebitorEntity.builder()
                        .debitorNumberSuffix("21")
                        .debitorRel(HsOfficeRelationEntity.builder()
                                .type(HsOfficeRelationType.DEBITOR)
                                .anchor(givenPartnerPerson)
                                .holder(givenPartnerPerson)
                                .contact(givenContact)
                                .build())
                        .defaultPrefix("abc")
                        .billable(false)
                        .build();
                return toCleanup(debitorRepo.save(newDebitor));
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficeDebitorEntity::getUuid).isNotNull();
            assertThatDebitorIsPersisted(result.returnedValue());
            assertThat(debitorRepo.count()).isEqualTo(count + 1);
        }

        @Transactional
        @ParameterizedTest
        @ValueSource(strings = {"", "a", "ab", "a12", "123", "12a"})
        public void canNotCreateNewDebitorWithInvalidDefaultPrefix(final String givenPrefix) {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenPartnerPerson = one(personRepo.findPersonByOptionalNameLike("First GmbH"));
            final var givenContact = one(contactRepo.findContactByOptionalCaptionLike("first contact"));

            // when
            final var result = attempt(em, () -> {
                final var newDebitor = HsOfficeDebitorEntity.builder()
                        .debitorNumberSuffix("21")
                        .debitorRel(HsOfficeRelationEntity.builder()
                                .type(HsOfficeRelationType.DEBITOR)
                                .anchor(givenPartnerPerson)
                                .holder(givenPartnerPerson)
                                .contact(givenContact)
                                .build())
                        .billable(true)
                        .vatReverseCharge(false)
                        .vatBusiness(false)
                        .defaultPrefix(givenPrefix)
                        .build();
                return toCleanup(debitorRepo.save(newDebitor));
            });

            // then
            result.assertExceptionWithRootCauseMessage(org.hibernate.exception.ConstraintViolationException.class);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("superuser-alex@hostsharing.net");
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll()).stream()
                    // some search+replace to make the output fit into the screen width
                    .map(s -> s.replace("hs_office_", ""))
                    .toList();

            // when
            attempt(em, () -> {
                final var givenPartnerPerson = one(personRepo.findPersonByOptionalNameLike("First GmbH"));
                final var givenDebitorPerson = one(personRepo.findPersonByOptionalNameLike("Fourth eG"));
                final var givenContact = one(contactRepo.findContactByOptionalCaptionLike("fourth contact"));
                final var newDebitor = HsOfficeDebitorEntity.builder()
                        .debitorNumberSuffix("22")
                        .debitorRel(HsOfficeRelationEntity.builder()
                                .type(HsOfficeRelationType.DEBITOR)
                                .anchor(givenPartnerPerson)
                                .holder(givenDebitorPerson)
                                .contact(givenContact)
                                .build())
                        .defaultPrefix("abc")
                        .billable(false)
                        .build();
                return toCleanup(debitorRepo.save(newDebitor));
            }).assertSuccessful();

            // then
            assertThat(distinctRoleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_office_relation#FirstGmbH-with-DEBITOR-FourtheG:OWNER",
                    "hs_office_relation#FirstGmbH-with-DEBITOR-FourtheG:ADMIN",
                    "hs_office_relation#FirstGmbH-with-DEBITOR-FourtheG:AGENT",
                    "hs_office_relation#FirstGmbH-with-DEBITOR-FourtheG:TENANT"));
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll()))
                .map(s -> s.replace("hs_office_", ""))
                .containsExactlyInAnyOrder(Array.fromFormatted(
                    initialGrantNames,
                    "{ grant perm:relation#FirstGmbH-with-DEBITOR-FourtheG:INSERT>sepamandate to role:relation#FirstGmbH-with-DEBITOR-FourtheG:ADMIN by system and assume }",
                    "{ grant perm:relation#FirstGmbH-with-DEBITOR-FourtheG:INSERT>hs_booking_project to role:relation#FirstGmbH-with-DEBITOR-FourtheG:ADMIN by system and assume }",

                    // owner
                    "{ grant perm:debitor#D-1000122:DELETE                          to role:relation#FirstGmbH-with-DEBITOR-FourtheG:OWNER by system and assume }",
                    "{ grant perm:relation#FirstGmbH-with-DEBITOR-FourtheG:DELETE   to role:relation#FirstGmbH-with-DEBITOR-FourtheG:OWNER by system and assume }",
                    "{ grant role:relation#FirstGmbH-with-DEBITOR-FourtheG:OWNER    to role:global#global:ADMIN by system and assume }",
                    "{ grant role:relation#FirstGmbH-with-DEBITOR-FourtheG:OWNER    to role:person#FirstGmbH:ADMIN by system and assume }",
                    "{ grant role:relation#FirstGmbH-with-DEBITOR-FourtheG:OWNER    to user:superuser-alex@hostsharing.net by relation#FirstGmbH-with-DEBITOR-FourtheG:OWNER and assume }",

                    // admin
                    "{ grant perm:debitor#D-1000122:UPDATE                          to role:relation#FirstGmbH-with-DEBITOR-FourtheG:ADMIN by system and assume }",
                    "{ grant perm:relation#FirstGmbH-with-DEBITOR-FourtheG:UPDATE   to role:relation#FirstGmbH-with-DEBITOR-FourtheG:ADMIN by system and assume }",
                    "{ grant role:relation#FirstGmbH-with-DEBITOR-FourtheG:ADMIN    to role:relation#FirstGmbH-with-DEBITOR-FourtheG:OWNER by system and assume }",
                    "{ grant role:relation#FirstGmbH-with-DEBITOR-FourtheG:ADMIN    to role:relation#HostsharingeG-with-PARTNER-FirstGmbH:ADMIN by system and assume }",

                    // agent
                    "{ grant role:relation#FirstGmbH-with-DEBITOR-FourtheG:AGENT    to role:person#FourtheG:ADMIN by system and assume }",
                    "{ grant role:relation#FirstGmbH-with-DEBITOR-FourtheG:AGENT    to role:relation#FirstGmbH-with-DEBITOR-FourtheG:ADMIN by system and assume }",
                    "{ grant role:relation#FirstGmbH-with-DEBITOR-FourtheG:AGENT    to role:relation#HostsharingeG-with-PARTNER-FirstGmbH:AGENT by system and assume }",

                    // tenant
                    "{ grant perm:debitor#D-1000122:SELECT                          to role:relation#FirstGmbH-with-DEBITOR-FourtheG:TENANT by system and assume }",
                    "{ grant perm:relation#FirstGmbH-with-DEBITOR-FourtheG:SELECT   to role:relation#FirstGmbH-with-DEBITOR-FourtheG:TENANT by system and assume }",
                    "{ grant role:relation#HostsharingeG-with-PARTNER-FirstGmbH:TENANT to role:relation#FirstGmbH-with-DEBITOR-FourtheG:AGENT by system and assume }",
                    "{ grant role:contact#fourthcontact:REFERRER                    to role:relation#FirstGmbH-with-DEBITOR-FourtheG:TENANT by system and assume }",
                    "{ grant role:person#FirstGmbH:REFERRER                         to role:relation#FirstGmbH-with-DEBITOR-FourtheG:TENANT by system and assume }",
                    "{ grant role:person#FourtheG:REFERRER                          to role:relation#FirstGmbH-with-DEBITOR-FourtheG:TENANT by system and assume }",
                    "{ grant role:relation#FirstGmbH-with-DEBITOR-FourtheG:TENANT   to role:contact#fourthcontact:ADMIN by system and assume }",
                    "{ grant role:relation#FirstGmbH-with-DEBITOR-FourtheG:TENANT   to role:relation#FirstGmbH-with-DEBITOR-FourtheG:AGENT by system and assume }",

                    null));
        }

        private void assertThatDebitorIsPersisted(final HsOfficeDebitorEntity saved) {
            final var savedRefreshed = refresh(saved);
            final var found = debitorRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().usingRecursiveComparison().isEqualTo(savedRefreshed);
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
                    "debitor(D-1000111: rel(anchor='LP First GmbH', type='DEBITOR', holder='LP First GmbH'), fir)",
                    "debitor(D-1000212: rel(anchor='LP Second e.K.', type='DEBITOR', holder='LP Second e.K.'), sec)",
                    "debitor(D-1000313: rel(anchor='IF Third OHG', type='DEBITOR', holder='IF Third OHG'), thi)");
        }

        @ParameterizedTest
        @Disabled // TODO: reactivate once partner.person + partner.contact  are removed
        @ValueSource(strings = {
                "hs_office_partner#10001:FirstGmbH-firstcontact:ADMIN",
                "hs_office_person#FirstGmbH:ADMIN",
                "hs_office_contact#firstcontact:ADMIN",
        })
        public void relatedPersonAdmin_canViewRelatedDebitors(final String assumedRole) {
            // given:
            context("superuser-alex@hostsharing.net", assumedRole);

            // when:
            final var result = debitorRepo.findDebitorByOptionalNameLike("");

            // then:
            exactlyTheseDebitorsAreReturned(result,
                    "debitor(D-1000111: P-10001, fir)",
                    "debitor(D-1000120: P-10001, fif)");
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
            exactlyTheseDebitorsAreReturned(result,
                    "debitor(D-1000313: rel(anchor='IF Third OHG', type='DEBITOR', holder='IF Third OHG'), thi)");
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
            exactlyTheseDebitorsAreReturned(result, "debitor(D-1000313: rel(anchor='IF Third OHG', type='DEBITOR', holder='IF Third OHG'), thi)");
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
                    "hs_office_relation#FourtheG-with-DEBITOR-FourtheG:ADMIN", true);
            final var givenNewPartnerPerson = one(personRepo.findPersonByOptionalNameLike("First"));
            final var givenNewBillingPerson = one(personRepo.findPersonByOptionalNameLike("Firby"));
            final var givenNewContact = one(contactRepo.findContactByOptionalCaptionLike("sixth contact"));
            final var givenNewBankAccount = one(bankAccountRepo.findByOptionalHolderLike("first"));
            final String givenNewVatId = "NEW-VAT-ID";
            final String givenNewVatCountryCode = "NC";
            final boolean givenNewVatBusiness = !givenDebitor.isVatBusiness();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                givenDebitor.setDebitorRel(HsOfficeRelationEntity.builder()
                        .type(HsOfficeRelationType.DEBITOR)
                        .anchor(givenNewPartnerPerson)
                        .holder(givenNewBillingPerson)
                        .contact(givenNewContact)
                        .build());
                givenDebitor.setRefundBankAccount(givenNewBankAccount);
                givenDebitor.setVatId(givenNewVatId);
                givenDebitor.setVatCountryCode(givenNewVatCountryCode);
                givenDebitor.setVatBusiness(givenNewVatBusiness);
                return toCleanup(debitorRepo.save(givenDebitor));
            });

            // then
            result.assertSuccessful();
            assertThatDebitorIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "global#global:ADMIN", true);

            // ... partner role was reassigned:
            assertThatDebitorIsNotVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_relation#FourtheG-with-DEBITOR-FourtheG:ADMIN");
            assertThatDebitorIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_relation#FirstGmbH-with-DEBITOR-FirbySusan:AGENT", true);

            // ... contact role was reassigned:
            assertThatDebitorIsNotVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_contact#fifthcontact:ADMIN");
            assertThatDebitorIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_contact#sixthcontact:ADMIN", false);

            // ... bank-account role was reassigned:
            assertThatDebitorIsNotVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_bankaccount#DE02200505501015871393:ADMIN");
            assertThatDebitorIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_bankaccount#DE02120300000000202051:ADMIN", true);
        }

        @Test
        public void globalAdmin_canUpdateNullRefundBankAccountToNotNullBankAccountForArbitraryDebitor() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "fifth contact", null, "fig");
            assertThatDebitorIsVisibleForUserWithRole(
                    givenDebitor,
                    "hs_office_relation#FourtheG-with-DEBITOR-FourtheG:ADMIN", true);
            assertThatDebitorActuallyInDatabase(givenDebitor, true);
            final var givenNewBankAccount = one(bankAccountRepo.findByOptionalHolderLike("first"));

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                givenDebitor.setRefundBankAccount(givenNewBankAccount);
                return toCleanup(debitorRepo.save(givenDebitor));
            });

            // then
            result.assertSuccessful();
            assertThatDebitorIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "global#global:ADMIN", true);

            // ... bank-account role was assigned:
            assertThatDebitorIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_bankaccount#DE02120300000000202051:ADMIN", true);
        }

        @Test
        public void globalAdmin_canUpdateRefundBankAccountToNullForArbitraryDebitor() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "fifth contact", "Fourth", "fih");
            assertThatDebitorIsVisibleForUserWithRole(
                    givenDebitor,
                    "hs_office_relation#HostsharingeG-with-PARTNER-FourtheG:AGENT", true);
            assertThatDebitorActuallyInDatabase(givenDebitor, true);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                givenDebitor.setRefundBankAccount(null);
                return toCleanup(debitorRepo.save(givenDebitor));
            });

            // then
            result.assertSuccessful();
            assertThatDebitorIsVisibleForUserWithRole(
                    result.returnedValue(),
                    "global#global:ADMIN", true);

            // ... bank-account role was removed from previous bank-account admin:
            assertThatDebitorIsNotVisibleForUserWithRole(
                    result.returnedValue(),
                    "hs_office_bankaccount#DE02200505501015871393:ADMIN");
        }

        @Test
        public void partnerAgent_canNotUpdateRelatedDebitor() {
            // given
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "eighth", "Fourth", "eig");
            assertThatDebitorIsVisibleForUserWithRole(
                    givenDebitor,
                    "hs_office_relation#HostsharingeG-with-PARTNER-FourtheG:AGENT", true);
            assertThatDebitorActuallyInDatabase(givenDebitor, true);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_office_relation#HostsharingeG-with-PARTNER-FourtheG:AGENT");
                givenDebitor.setVatId("NEW-VAT-ID");
                return toCleanup(debitorRepo.save(givenDebitor));
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
            assertThatDebitorActuallyInDatabase(givenDebitor, true);
            assertThatDebitorIsVisibleForUserWithRole(
                    givenDebitor,
                    "hs_office_contact#ninthcontact:ADMIN", false);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_office_contact#ninthcontact:ADMIN");
                givenDebitor.setVatId("NEW-VAT-ID");
                return toCleanup(debitorRepo.save(givenDebitor));
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaObjectRetrievalFailureException.class,
                    // this technical error message gets translated to a [403] error at the controller level
                    "Unable to find net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity with id ");
        }

        private void assertThatDebitorActuallyInDatabase(final HsOfficeDebitorEntity saved, final boolean withPartner) {
            final var found = debitorRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty();
            found.ifPresent(foundEntity -> {
                em.refresh(foundEntity);
                Hibernate.initialize(foundEntity);
                assertThat(foundEntity).isNotSameAs(saved);
                if (withPartner) {
                    assertThat(foundEntity.getPartner()).isNotNull();
                }
                assertThat(foundEntity.getDebitorRel()).extracting(HsOfficeRelationEntity::toString)
                        .isEqualTo(saved.getDebitorRel().toString());
            });
        }

        private void assertThatDebitorIsVisibleForUserWithRole(
                final HsOfficeDebitorEntity entity,
                final String assumedRoles,
                final boolean withPartner) {
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", assumedRoles);
                assertThatDebitorActuallyInDatabase(entity, withPartner);
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
        public void debitorAgent_canViewButNotDeleteTheirRelatedDebitor() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "eleventh", "Fourth", "ele");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", "hs_office_relation#FourtheG-with-DEBITOR-FourtheG:ADMIN");
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
            final var initialRoleNames = Array.from(distinctRoleNamesOf(rawRoleRepo.findAll()));
            final var initialGrantNames = Array.from(distinctGrantDisplaysOf(rawGrantRepo.findAll()));
            final var givenDebitor = givenSomeTemporaryDebitor("Fourth", "twelfth", "Fourth", "twi");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net");
                return debitorRepo.deleteByUuid(givenDebitor.getUuid());
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
            final String partnerName,
            final String contactCaption,
            final String bankAccountHolder,
            final String defaultPrefix) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net");
            final var givenPartnerPerson = one(personRepo.findPersonByOptionalNameLike(partnerName));
            final var givenContact = one(contactRepo.findContactByOptionalCaptionLike(contactCaption));
            final var givenBankAccount =
                    bankAccountHolder != null ? one(bankAccountRepo.findByOptionalHolderLike(bankAccountHolder)) : null;
            final var newDebitor = HsOfficeDebitorEntity.builder()
                    .debitorNumberSuffix("20")
                    .debitorRel(HsOfficeRelationEntity.builder()
                            .type(HsOfficeRelationType.DEBITOR)
                            .anchor(givenPartnerPerson)
                            .holder(givenPartnerPerson)
                            .contact(givenContact)
                            .build())
                    .refundBankAccount(givenBankAccount)
                    .defaultPrefix(defaultPrefix)
                    .billable(true)
                    .build();

            return toCleanup(debitorRepo.save(newDebitor));
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
