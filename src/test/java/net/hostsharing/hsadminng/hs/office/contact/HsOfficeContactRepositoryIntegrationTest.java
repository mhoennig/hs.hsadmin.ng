package net.hostsharing.hsadminng.hs.office.contact;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantRepository;
import net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleRepository;
import net.hostsharing.test.Array;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.modelmapper.internal.bytebuddy.utility.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static net.hostsharing.hsadminng.hs.office.contact.TestHsOfficeContact.hsOfficeContact;
import static net.hostsharing.hsadminng.rbac.rbacgrant.RawRbacGrantEntity.grantDisplaysOf;
import static net.hostsharing.hsadminng.rbac.rbacrole.RawRbacRoleEntity.roleNamesOf;
import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { HsOfficeContactRepository.class, Context.class, JpaAttempt.class })
@DirtiesContext
class HsOfficeContactRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    HsOfficeContactRepository contactRepo;

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

    @Container
    Container postgres;

    @Nested
    class CreateContact {

        @Test
        public void globalAdmin_withoutAssumedRole_canCreateNewContact() {
            // given
            context("superuser-alex@hostsharing.net");
            final var count = contactRepo.count();

            // when

            final var result = attempt(em, () -> contactRepo.save(
                    hsOfficeContact("a new contact", "contact-admin@www.example.com")));

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
            final var result = attempt(em, () -> contactRepo.save(
                    hsOfficeContact("another new contact", "another-new-contact@example.com")));

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
            final var initialRoleNames = roleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = grantDisplaysOf(rawGrantRepo.findAll());

            // when
            attempt(em, () -> contactRepo.save(
                    hsOfficeContact("another new contact", "another-new-contact@example.com"))
            ).assumeSuccessful();

            // then
            final var roles = rawRoleRepo.findAll();
            assertThat(roleNamesOf(roles)).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames,
                    "hs_office_contact#anothernewcontact.owner",
                    "hs_office_contact#anothernewcontact.admin",
                    "hs_office_contact#anothernewcontact.tenant",
                    "hs_office_contact#anothernewcontact.guest"
            ));
            assertThat(grantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(Array.from(
                    initialGrantNames,
                    "{ grant role hs_office_contact#anothernewcontact.owner to role global#global.admin by system and assume }",
                    "{ grant perm edit on hs_office_contact#anothernewcontact to role hs_office_contact#anothernewcontact.admin by system and assume }",
                    "{ grant role hs_office_contact#anothernewcontact.tenant to role hs_office_contact#anothernewcontact.admin by system and assume }",
                    "{ grant perm * on hs_office_contact#anothernewcontact to role hs_office_contact#anothernewcontact.owner by system and assume }",
                    "{ grant role hs_office_contact#anothernewcontact.admin to role hs_office_contact#anothernewcontact.owner by system and assume }",
                    "{ grant perm view on hs_office_contact#anothernewcontact to role hs_office_contact#anothernewcontact.guest by system and assume }",
                    "{ grant role hs_office_contact#anothernewcontact.guest to role hs_office_contact#anothernewcontact.tenant by system and assume }",
                    "{ grant role hs_office_contact#anothernewcontact.owner to user selfregistered-user-drew@hostsharing.org by global#global.admin and assume }"
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
            final var initialRoleNames = roleNamesOf(rawRoleRepo.findAll());
            final var initialGrantNames = grantDisplaysOf(rawGrantRepo.findAll());
            final var givenContact = givenSomeTemporaryContact("selfregistered-user-drew@hostsharing.org");
            assumeThat(rawRoleRepo.findAll().size()).as("unexpected number of roles created")
                    .isEqualTo(initialRoleNames.size() + 3);
            assumeThat(rawGrantRepo.findAll().size()).as("unexpected number of grants created")
                    .isEqualTo(initialGrantNames.size() + 7);

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("selfregistered-user-drew@hostsharing.org", null);
                return contactRepo.deleteByUuid(givenContact.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isEqualTo(1);
            assertThat(roleNamesOf(rawRoleRepo.findAll())).containsExactlyInAnyOrder(Array.from(
                    initialRoleNames
            ));
            assertThat(grantDisplaysOf(rawGrantRepo.findAll())).containsExactlyInAnyOrder(Array.from(
                    initialGrantNames
            ));
        }
    }

    @Test
    public void auditJournalLogIsAvailable() {
        // given
        final var query = em.createNativeQuery("""
                select c.currenttask, j.targettable, j.targetop
                    from tx_journal j
                    join tx_context c on j.contextId = c.contextId
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
            return contactRepo.save(entitySupplier.get());
        }).assumeSuccessful().returnedValue();
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        context("superuser-alex@hostsharing.net", null);
        final var result = contactRepo.findContactByOptionalLabelLike("some temporary contact");
        result.forEach(tempPerson -> {
            System.out.println("DELETING temporary contact: " + tempPerson.getLabel());
            contactRepo.deleteByUuid(tempPerson.getUuid());
        });
    }

    private HsOfficeContactEntity givenSomeTemporaryContact(final String createdByUser) {
        final var random = RandomString.make(12);
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
