package net.hostsharing.hsadminng.hs.admin.contact;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static net.hostsharing.hsadminng.hs.admin.contact.TestHsAdminContact.hsAdminContact;
import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { Context.class, HsAdminContactRepository.class })
@DirtiesContext
class HsAdminContactRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    HsAdminContactRepository contactRepo;

    @Autowired
    EntityManager em;

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
            assertThat(result.wasSuccessful()).isTrue();
            assertThat(result.returnedValue()).isNotNull().extracting(HsAdminContactEntity::getUuid).isNotNull();
            assertThatContactIsPersisted(result.returnedValue());
            assertThat(contactRepo.count()).isEqualTo(count + 1);
        }

        @Test
        public void arbitraryUser_canCreateNewContact() {
            // given
            context("pac-admin-xxx00@xxx.example.com");
            final var count = contactRepo.count();

            // when
            final var result = attempt(em, () -> contactRepo.save(
                    hsAdminContact("another new contact", "another-new-contact@example.com")));

            // then
            assertThat(result.wasSuccessful()).isTrue();
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
            context("customer-admin@secondcontact.example.com");

            final var result = contactRepo.findContactByOptionalLabelLike(null);

            exactlyTheseContactsAreReturned(result, "second contact");
        }
    }

    @Nested
    class FindByPrefixLike {

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
            context("customer-admin@secondcontact.example.com", null);

            // when:
            final var result = contactRepo.findContactByOptionalLabelLike("second contact");

            // then:
            exactlyTheseContactsAreReturned(result, "second contact");
        }
    }

    void exactlyTheseContactsAreReturned(final List<HsAdminContactEntity> actualResult, final String... contactLabels) {
        assertThat(actualResult)
                .hasSize(contactLabels.length)
                .extracting(HsAdminContactEntity::getLabel)
                .containsExactlyInAnyOrder(contactLabels);
    }

    void allTheseContactsAreReturned(final List<HsAdminContactEntity> actualResult, final String... contactLabels) {
        assertThat(actualResult)
                .extracting(HsAdminContactEntity::getLabel)
                .contains(contactLabels);
    }
}
