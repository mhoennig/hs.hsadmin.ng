package net.hostsharing.hsadminng.hs.office.contact;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleRepository;
import net.hostsharing.test.Array;
import net.hostsharing.test.JpaAttempt;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static net.hostsharing.hsadminng.hs.office.contact.TestHsOfficeContact.hsOfficeContact;
import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
class HsOfficeContactRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    HsOfficeContactRepository contactRepo;

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
    class CreateContact {

        @Test
        public void globalAdmin_withoutAssumedRole_canCreateNewContact() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = contactRepo.count();

            // when

            final var result = attempt(em, () -> toCleanup(contactRepo.save(
                    hsOfficeContact("a new contact", "contact-admin@www.example.com"))));

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficeContactEntity::getUuid).isNotNull();
            assertThatContactIsPersisted(result.returnedValue());
            assertThat(contactRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void arbitraryUser_canCreateNewContact() {
            // given
            context("selfregistered-user-drew@hostsharing.org");
            final var count = contactRepo.count();

            // when
            final var result = attempt(em, () -> toCleanup(contactRepo.save(
                    hsOfficeContact("another new contact", "another-new-contact@example.com"))));

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficeContactEntity::getUuid).isNotNull();
            assertThatContactIsPersisted(result.returnedValue());
            assertThat(contactRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void createsAndGrantsRoles() {
            // given
            context("selfregistered-user-drew@hostsharing.org");
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll());

            // when
            attempt(em, () -> toCleanup(contactRepo.save(
                    hsOfficeContact("another new contact", "another-new-contact@example.com")))
            ).assumeSuccessful();

            // then
            final var roles = rawRoleRepo.findAll();
            assertThat(distinctRoleNamesOf(roles)).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_office_contact#anothernewcontact:OWNER",
                    "hs_office_contact#anothernewcontact:ADMIN",
                    "hs_office_contact#anothernewcontact:REFERRER"
            ));
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(Array.fromFormatted(
                    initialGrantNames,
                    "{ grant role:hs_office_contact#anothernewcontact:OWNER     to role:global#global:ADMIN                          by system and assume }",
                    "{ grant perm:hs_office_contact#anothernewcontact:UPDATE    to role:hs_office_contact#anothernewcontact:ADMIN    by system and assume }",
                    "{ grant role:hs_office_contact#anothernewcontact:OWNER     to user:selfregistered-user-drew@hostsharing.org     by hs_office_contact#anothernewcontact:OWNER and assume }",
                    "{ grant perm:hs_office_contact#anothernewcontact:DELETE     to role:hs_office_contact#anothernewcontact:OWNER    by system and assume }",
                    "{ grant role:hs_office_contact#anothernewcontact:ADMIN     to role:hs_office_contact#anothernewcontact:OWNER    by system and assume }",

                    "{ grant perm:hs_office_contact#anothernewcontact:SELECT    to role:hs_office_contact#anothernewcontact:REFERRER by system and assume }",
                    "{ grant role:hs_office_contact#anothernewcontact:REFERRER  to role:hs_office_contact#anothernewcontact:ADMIN    by system and assume }"
            ));
        }

        private void assertThatContactIsPersisted(final HsOfficeContactEntity saved) {
            final var found = contactRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().usingRecursiveComparison().isEqualTo(saved);
        }
    }

    @Nested
    class FindAllContacts {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllContacts() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = contactRepo.findContactByOptionalLabelLike(null);

            // then
            allTheseContactsAreReturned(result, "first contact", "second contact", "third contact");
        }

        @Test
        public void arbitraryUser_canViewOnlyItsOwnContact() {
            // given:
            final var givenContact = givenSomeTemporaryContact("selfregistered-user-drew@hostsharing.org");

            // when:
            context("selfregistered-user-drew@hostsharing.org");
            final var result = contactRepo.findContactByOptionalLabelLike(null);

            // then:
            exactlyTheseContactsAreReturned(result, givenContact.getLabel());
        }
    }

    @Nested
    class FindByLabelLike {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllContacts() {
            // given
            context("superuser-alex@hostsharing.net", null);

            // when
            final var result = contactRepo.findContactByOptionalLabelLike("second");

            // then
            exactlyTheseContactsAreReturned(result, "second contact");
        }

        @Test
        public void arbitraryUser_withoutAssumedRole_canViewOnlyItsOwnContact() {
            // given:
            final var givenContact = givenSomeTemporaryContact("selfregistered-user-drew@hostsharing.org");

            // when:
            context("selfregistered-user-drew@hostsharing.org");
            final var result = contactRepo.findContactByOptionalLabelLike(givenContact.getLabel());

            // then:
            exactlyTheseContactsAreReturned(result, givenContact.getLabel());
        }
    }

    @Nested
    class DeleteByUuid {

        @Test
        public void globalAdmin_withoutAssumedRole_canDeleteAnyContact() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var givenContact = givenSomeTemporaryContact("selfregistered-user-drew@hostsharing.org");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", null);
                contactRepo.deleteByUuid(givenContact.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", null);
                return contactRepo.findContactByOptionalLabelLike(givenContact.getLabel());
            }).assertSuccessful().returnedValue()).hasSize(0);
        }

        @Test
        public void arbitraryUser_withoutAssumedRole_canDeleteAContactCreatedByItself() {
            // given
            final var givenContact = givenSomeTemporaryContact("selfregistered-user-drew@hostsharing.org");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("selfregistered-user-drew@hostsharing.org", null);
                contactRepo.deleteByUuid(givenContact.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", null);
                return contactRepo.findContactByOptionalLabelLike(givenContact.getLabel());
            }).assertSuccessful().returnedValue()).hasSize(0);
        }

        @Test
        public void deletingAContactAlsoDeletesRelatedRolesAndGrants() {
            // given
            context("selfregistered-user-drew@hostsharing.org", null);
            final var initialRoleNames = distinctRoleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = distinctGrantDisplaysOf(rawGrantRepo.findAll());
            final var givenContact = givenSomeTemporaryContact("selfregistered-user-drew@hostsharing.org");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("selfregistered-user-drew@hostsharing.org", null);
                return contactRepo.deleteByUuid(givenContact.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isEqualTo(1);
            assertThat(distinctRoleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames
            ));
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(Array.from(
                    initialGrantNames
            ));
        }
    }

    @Test
    public void auditJournalLogIsAvailable() {
        // given
        final var query = em.createNativeQuery("""
                select currentTask, targetTable, targetOp
                    from tx_journal_v
                    where targettable = 'hs_office_contact';
                    """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating contact test-data first contact, hs_office_contact, INSERT]",
                "[creating contact test-data second contact, hs_office_contact, INSERT]");
    }

    private HsOfficeContactEntity givenSomeTemporaryContact(
            final String createdByUser,
            Supplier<HsOfficeContactEntity> entitySupplier) {
        return jpaAttempt.transacted(() -> {
            context(createdByUser);
            return toCleanup(contactRepo.save(entitySupplier.get()));
        }).assumeSuccessful().returnedValue();
    }

    private HsOfficeContactEntity givenSomeTemporaryContact(final String createdByUser) {
        final var random = RandomStringUtils.randomAlphabetic(12);
        return givenSomeTemporaryContact(createdByUser, () ->
                hsOfficeContact(
                        "some temporary contact #" + random,
                        "some-temporary-contact" + random + "@example.com"));
    }

    void exactlyTheseContactsAreReturned(final List<HsOfficeContactEntity> actualResult, final String... contactLabels) {
        assertThat(actualResult)
                .extracting(HsOfficeContactEntity::getLabel)
                .containsExactlyInAnyOrder(contactLabels);
    }

    void allTheseContactsAreReturned(final List<HsOfficeContactEntity> actualResult, final String... contactLabels) {
        assertThat(actualResult)
                .extracting(HsOfficeContactEntity::getLabel)
                .contains(contactLabels);
    }
}
