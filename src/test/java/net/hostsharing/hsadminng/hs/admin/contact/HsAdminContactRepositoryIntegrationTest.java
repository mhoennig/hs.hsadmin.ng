package net.hostsharing.hsadminng.hs.admin.contact;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.modelmapper.internal.bytebuddy.utility.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.function.Supplier;

import static net.hostsharing.hsadminng.hs.admin.contact.TestHsAdminContact.hsAdminContact;
import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { HsAdminContactRepository.class, Context.class, JpaAttempt.class })
@DirtiesContext
class HsAdminContactRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    HsAdminContactRepository contactRepo;

    @Autowired
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
            context("alex@hostsharing.net");
            final var count = contactRepo.count();

            // when

            final var result = attempt(em, () -> contactRepo.save(
                    hsAdminContact("a new contact", "contact-admin@www.example.com")));

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsAdminContactEntity::getUuid).isNotNull();
            assertThatContactIsPersisted(result.returnedValue());
            assertThat(contactRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void arbitraryUser_canCreateNewContact() {
            // given
            context("drew@hostsharing.org");
            final var count = contactRepo.count();

            // when
            final var result = attempt(em, () -> contactRepo.save(
                    hsAdminContact("another new contact", "another-new-contact@example.com")));

            // then
            result.assertSuccessful();
            assertThat(result.returnedValue()).isNotNull().extracting(HsAdminContactEntity::getUuid).isNotNull();
            assertThatContactIsPersisted(result.returnedValue());
            assertThat(contactRepo.count()).isEqualTo(count + 1);
        }

        private void assertThatContactIsPersisted(final HsAdminContactEntity saved) {
            final var found = contactRepo.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().usingRecursiveComparison().isEqualTo(saved);
        }
    }

    @Nested
    class FindAllContacts {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllContacts() {
            // given
            context("alex@hostsharing.net");

            // when
            final var result = contactRepo.findContactByOptionalLabelLike(null);

            // then
            allTheseContactsAreReturned(result, "first contact", "second contact", "third contact");
        }

        @Test
        public void arbitraryUser_canViewOnlyItsOwnContact() {
            // given:
            final var givenContact = givenSomeTemporaryContact("drew@hostsharing.org");

            // when:
            context("drew@hostsharing.org");
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
            context("alex@hostsharing.net", null);

            // when
            final var result = contactRepo.findContactByOptionalLabelLike("second");

            // then
            exactlyTheseContactsAreReturned(result, "second contact");
        }

        @Test
        public void arbitraryUser_withoutAssumedRole_canViewOnlyItsOwnContact() {
            // given:
            final var givenContact = givenSomeTemporaryContact("drew@hostsharing.org");

            // when:
            context("drew@hostsharing.org");
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
            final var givenContact = givenSomeTemporaryContact("drew@hostsharing.org");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("alex@hostsharing.net", null);
                contactRepo.deleteByUuid(givenContact.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("alex@hostsharing.net", null);
                return contactRepo.findContactByOptionalLabelLike(givenContact.getLabel());
            }).assertSuccessful().returnedValue()).hasSize(0);
        }

        @Test
        public void arbitraryUser_withoutAssumedRole_canDeleteAContactCreatedByItself() {
            // given
            final var givenContact = givenSomeTemporaryContact("drew@hostsharing.org");

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("drew@hostsharing.org", null);
                contactRepo.deleteByUuid(givenContact.getUuid());
            });

            // then
            result.assertSuccessful();
            assertThat(jpaAttempt.transacted(() -> {
                context("alex@hostsharing.net", null);
                return contactRepo.findContactByOptionalLabelLike(givenContact.getLabel());
            }).assertSuccessful().returnedValue()).hasSize(0);
        }
    }

    private HsAdminContactEntity givenSomeTemporaryContact(
            final String createdByUser,
            Supplier<HsAdminContactEntity> entitySupplier) {
        return jpaAttempt.transacted(() -> {
            context(createdByUser);
            return contactRepo.save(entitySupplier.get());
        }).assumeSuccessful().returnedValue();
    }

    @AfterEach
    void cleanup() {
        context("alex@hostsharing.net", null);
        final var result = contactRepo.findContactByOptionalLabelLike("some temporary contact");
        result.forEach(tempPerson -> {
            System.out.println("DELETING contact: " + tempPerson.getLabel());
            contactRepo.deleteByUuid(tempPerson.getUuid());
        });
    }

    private HsAdminContactEntity givenSomeTemporaryContact(final String createdByUser) {
        final var random = RandomString.make(12);
        return givenSomeTemporaryContact(createdByUser, () ->
                hsAdminContact(
                        "some temporary contact #" + random,
                        "some-temporary-contact" + random + "@example.com"));
    }

    void exactlyTheseContactsAreReturned(final List<HsAdminContactEntity> actualResult, final String... contactLabels) {
        assertThat(actualResult)
                .extracting(HsAdminContactEntity::getLabel)
                .containsExactlyInAnyOrder(contactLabels);
    }

    void allTheseContactsAreReturned(final List<HsAdminContactEntity> actualResult, final String... contactLabels) {
        assertThat(actualResult)
                .extracting(HsAdminContactEntity::getLabel)
                .contains(contactLabels);
    }
}
