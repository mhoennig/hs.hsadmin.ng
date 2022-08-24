package net.hostsharing.hsadminng.hs.hscustomer;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.util.List;
import java.util.UUID;

import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { Context.class, CustomerRepository.class })
@DirtiesContext
class CustomerRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    CustomerRepository customerRepository;

    @Autowired EntityManager em;

    @Nested
    class CreateCustomer {

        @Test
        public void hostsharingAdmin_withoutAssumedRole_canCreateNewCustomer() {
            // given
            context("mike@hostsharing.net", null);
            final var count = customerRepository.count();

            // when

            final var result = attempt(em, () -> {
                final var newCustomer = new CustomerEntity(
                    UUID.randomUUID(), "xxx", 90001, "admin@xxx.example.com");
                return customerRepository.save(newCustomer);
            });

            // then
            assertThat(result.wasSuccessful()).isTrue();
            assertThat(result.returnedValue()).isNotNull().extracting(CustomerEntity::getUuid).isNotNull();
            assertThatCustomerIsPersisted(result.returnedValue());
            assertThat(customerRepository.count()).isEqualTo(count + 1);
        }

        @Test
        public void hostsharingAdmin_withAssumedCustomerRole_cannotCreateNewCustomer() {
            // given
            context("mike@hostsharing.net", "customer#aaa.admin");

            // when
            final var result = attempt(em, () -> {
                final var newCustomer = new CustomerEntity(
                    UUID.randomUUID(), "xxx", 90001, "admin@xxx.example.com");
                return customerRepository.save(newCustomer);
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                PersistenceException.class,
                "add-customer not permitted for customer#aaa.admin");
        }

        @Test
        public void customerAdmin_withoutAssumedRole_cannotCreateNewCustomer() {
            // given
            context("admin@aaa.example.com", null);

            // when
            final var result = attempt(em, () -> {
                final var newCustomer = new CustomerEntity(
                    UUID.randomUUID(), "yyy", 90002, "admin@yyy.example.com");
                return customerRepository.save(newCustomer);
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                PersistenceException.class,
                "add-customer not permitted for admin@aaa.example.com");

        }

        private void assertThatCustomerIsPersisted(final CustomerEntity saved) {
            final var found = customerRepository.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().usingRecursiveComparison().isEqualTo(saved);
        }
    }

    @Nested
    class FindAllCustomers {

        @Test
        public void hostsharingAdmin_withoutAssumedRole_canViewAllCustomers() {
            // given
            context("mike@hostsharing.net", null);

            // when
            final var result = customerRepository.findCustomerByOptionalPrefixLike(null);

            // then
            exactlyTheseCustomersAreReturned(result, "aaa", "aab", "aac");
        }

        @Test
        public void hostsharingAdmin_withAssumedHostsharingAdminRole_canViewAllCustomers() {
            given:
            context("mike@hostsharing.net", "global#hostsharing.admin");

            // when
            final var result = customerRepository.findCustomerByOptionalPrefixLike(null);

            then:
            exactlyTheseCustomersAreReturned(result, "aaa", "aab", "aac");
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewOnlyItsOwnCustomer() {
            // given:
            context("admin@aaa.example.com", null);

            // when:
            final var result = customerRepository.findCustomerByOptionalPrefixLike(null);

            // then:
            exactlyTheseCustomersAreReturned(result, "aaa");
        }

        @Test
        public void customerAdmin_withAssumedOwnedPackageAdminRole_canViewOnlyItsOwnCustomer() {
            context("admin@aaa.example.com", "package#aaa00.admin");

            final var result = customerRepository.findCustomerByOptionalPrefixLike(null);

            exactlyTheseCustomersAreReturned(result, "aaa");
        }

        @Test
        public void customerAdmin_withAssumedAlienPackageAdminRole_cannotViewAnyCustomer() {
            // given:
            context("admin@aaa.example.com", "package#aab00.admin");

            // when
            final var result = attempt(
                em,
                () -> customerRepository.findCustomerByOptionalPrefixLike(null));

            // then
            result.assertExceptionWithRootCauseMessage(
                JpaSystemException.class,
                "[403] user admin@aaa.example.com", "has no permission to assume role package#aab00#admin");
        }

        @Test
        void unknownUser_withoutAssumedRole_cannotViewAnyCustomers() {
            context("unknown@example.org", null);

            final var result = attempt(
                em,
                () -> customerRepository.findCustomerByOptionalPrefixLike(null));

            result.assertExceptionWithRootCauseMessage(
                JpaSystemException.class,
                "hsadminng.currentUser defined as unknown@example.org, but does not exists");
        }

        @Test
        @Transactional
        void unknownUser_withAssumedCustomerRole_cannotViewAnyCustomers() {
            context("unknown@example.org", "customer#aaa.admin");

            final var result = attempt(
                em,
                () -> customerRepository.findCustomerByOptionalPrefixLike(null));

            result.assertExceptionWithRootCauseMessage(
                JpaSystemException.class,
                "hsadminng.currentUser defined as unknown@example.org, but does not exists");
        }

    }

    @Nested
    class FindByPrefixLike {

        @Test
        public void hostsharingAdmin_withoutAssumedRole_canViewAllCustomers() {
            // given
            context("mike@hostsharing.net", null);

            // when
            final var result = customerRepository.findCustomerByOptionalPrefixLike("aab");

            // then
            exactlyTheseCustomersAreReturned(result, "aab");
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewOnlyItsOwnCustomer() {
            // given:
            context("admin@aaa.example.com", null);

            // when:
            final var result = customerRepository.findCustomerByOptionalPrefixLike("aab");

            // then:
            exactlyTheseCustomersAreReturned(result);
        }
    }

    void exactlyTheseCustomersAreReturned(final List<CustomerEntity> actualResult, final String... customerPrefixes) {
        assertThat(actualResult)
            .hasSize(customerPrefixes.length)
            .extracting(CustomerEntity::getPrefix)
            .containsExactlyInAnyOrder(customerPrefixes);
    }

}
