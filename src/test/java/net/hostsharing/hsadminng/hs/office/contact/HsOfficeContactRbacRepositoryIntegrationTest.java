package net.hostsharing.hsadminng.hs.office.contact;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.grant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.role.RawRbacRoleRepository;
import net.hostsharing.hsadminng.mapper.Array;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRbacTestEntity.hsOfficeContact;
import static net.hostsharing.hsadminng.rbac.grant.RawRbacGrantEntity.distinctGrantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.role.RawRbacRoleEntity.distinctRoleNamesOf;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
class HsOfficeContactRbacRepositoryIntegrationTest extends ContextBasedTestWithCleanup {

    @Autowired
    HsOfficeContactRbacRepository contactRepo;

    @Autowired
    RawRbacRoleRepository rawRoleRepo;

    @Autowired
    RawRbacGrantRepository rawGrantRepo;

    @PersistenceContext
    EntityManager em;

    @Autowired
    JpaAttempt jpaAttempt;

    @MockitoBean
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
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficeContactRbacEntity::getUuid).isNotNull();
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
            assertThat(result.returnedValue()).isNotNull().extracting(HsOfficeContactRbacEntity::getUuid).isNotNull();
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
                    "hs_office.contact#anothernewcontact:OWNER",
                    "hs_office.contact#anothernewcontact:ADMIN",
                    "hs_office.contact#anothernewcontact:REFERRER"
            ));
            assertThat(distinctGrantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(Array.fromFormatted(
                    initialGrantNames,
                    "{ grant role:hs_office.contact#anothernewcontact:OWNER     to role:rbac.global#global:ADMIN                          by system and assume }",
                    "{ grant perm:hs_office.contact#anothernewcontact:UPDATE    to role:hs_office.contact#anothernewcontact:ADMIN    by system and assume }",
                    "{ grant role:hs_office.contact#anothernewcontact:OWNER     to user:selfregistered-user-drew@hostsharing.org     by hs_office.contact#anothernewcontact:OWNER and assume }",
                    "{ grant perm:hs_office.contact#anothernewcontact:DELETE     to role:hs_office.contact#anothernewcontact:OWNER    by system and assume }",
                    "{ grant role:hs_office.contact#anothernewcontact:ADMIN     to role:hs_office.contact#anothernewcontact:OWNER    by system and assume }",

                    "{ grant perm:hs_office.contact#anothernewcontact:SELECT    to role:hs_office.contact#anothernewcontact:REFERRER by system and assume }",
                    "{ grant role:hs_office.contact#anothernewcontact:REFERRER  to role:hs_office.contact#anothernewcontact:ADMIN    by system and assume }"
            ));
        }

        private void assertThatContactIsPersisted(final HsOfficeContactRbacEntity saved) {
            final var found = contactRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().extracting(Object::toString).isEqualTo(saved.toString());
        }
    }

    @Nested
    class FindContacts {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllContacts() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = contactRepo.findContactByOptionalCaptionLike(null);

            // then
            allTheseContactsAreReturned(result, "first contact", "second contact", "third contact");
        }

        @Test
        public void arbitraryUser_canViewOnlyItsOwnContact() {
            // given:
            final var givenContact = givenSomeTemporaryContact("selfregistered-user-drew@hostsharing.org");

            // when:
            context("selfregistered-user-drew@hostsharing.org");
            final var result = contactRepo.findContactByOptionalCaptionLike(null);

            // then:
            exactlyTheseContactsAreReturned(result, givenContact.getCaption());
        }
    }

    @Nested
    class FindByCaptionLike {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllContacts() {
            // given
            context("superuser-alex@hostsharing.net", null);

            // when
            final var result = contactRepo.findContactByOptionalCaptionLike("second");

            // then
            exactlyTheseContactsAreReturned(result, "second contact");
        }

        @Test
        public void arbitraryUser_withoutAssumedRole_canViewOnlyItsOwnContact() {
            // given:
            final var givenContact = givenSomeTemporaryContact("selfregistered-user-drew@hostsharing.org");

            // when:
            context("selfregistered-user-drew@hostsharing.org");
            final var result = contactRepo.findContactByOptionalCaptionLike(givenContact.getCaption());

            // then:
            exactlyTheseContactsAreReturned(result, givenContact.getCaption());
        }
    }

    @Nested
    class FindByEmailAddress {

        @Test
        public void globalAdmin_withoutAssumedRole_canFindContactsByEmailAddress() {
            // given
            context("superuser-alex@hostsharing.net", null);

            // when
            final var result = contactRepo.findContactByEmailAddress("%@secondcontact.example.com");

            // then
            exactlyTheseContactsAreReturned(result, "second contact");
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
                return contactRepo.findContactByOptionalCaptionLike(givenContact.getCaption());
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
                return contactRepo.findContactByOptionalCaptionLike(givenContact.getCaption());
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
                select currentTask, targetTable, targetOp, targetdelta->>'caption'
                    from base.tx_journal_v
                    where targettable = 'hs_office.contact';
                """);

        // when
        @SuppressWarnings("unchecked") final List<Object[]> customerLogEntries = query.getResultList();

        // then
        assertThat(customerLogEntries).map(Arrays::toString).contains(
                "[creating contact test-data, hs_office.contact, INSERT, first contact]",
                "[creating contact test-data, hs_office.contact, INSERT, second contact]",
                "[creating contact test-data, hs_office.contact, INSERT, third contact]");
    }

    private HsOfficeContactRbacEntity givenSomeTemporaryContact(
            final String createdByUser,
            Supplier<HsOfficeContactRbacEntity> entitySupplier) {
        return jpaAttempt.transacted(() -> {
            context(createdByUser);
            return toCleanup(contactRepo.save(entitySupplier.get()));
        }).assumeSuccessful().returnedValue();
    }

    private HsOfficeContactRbacEntity givenSomeTemporaryContact(final String createdByUser) {
        final var random = RandomStringUtils.randomAlphabetic(12);
        return givenSomeTemporaryContact(createdByUser, () ->
                hsOfficeContact(
                        "some temporary contact #" + random,
                        "some-temporary-contact" + random + "@example.com"));
    }

    void exactlyTheseContactsAreReturned(final List<HsOfficeContactRbacEntity> actualResult, final String... contactCaptions) {
        assertThat(actualResult)
                .extracting(HsOfficeContactRbacEntity::getCaption)
                .containsExactlyInAnyOrder(contactCaptions);
    }

    void allTheseContactsAreReturned(final List<HsOfficeContactRbacEntity> actualResult, final String... contactCaptions) {
        assertThat(actualResult)
                .extracting(HsOfficeContactRbacEntity::getCaption)
                .contains(contactCaptions);
    }
}
