package net.hostsharing.hsadminng.hs.hscustomer;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.hscustomer.CustomerEntity;
import net.hostsharing.hsadminng.hs.hscustomer.CustomerRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.orm.jpa.JpaSystemException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { Context.class, CustomerRepository.class })
class CustomerRepositoryIntegrationTest {

    @Autowired
    Context context;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired EntityManager em;

    @Nested
    class CreateCustomer {

        @Test
        public void hostsharingAdmin_withoutAssumedRole_canCreateNewCustomer() {
            // given
            currentUser("mike@hostsharing.net");

            // when
            final var newCustomer = new CustomerEntity(
                UUID.randomUUID(), "xxx", 90001, "admin@xxx.example.com");
            final var result = customerRepository.save(newCustomer);

            // then
            assertThat(result).isNotNull().extracting(CustomerEntity::getUuid).isNotNull();
            assertThatCustomerIsPersisted(result);
        }

        @Test
        public void hostsharingAdmin_withAssumedCustomerRole_cannotCreateNewCustomer() {
            // given
            currentUser("mike@hostsharing.net");
            assumedRoles("customer#aaa.admin");

            // when
            final var attempt = attempt(em, () -> {
                final var newCustomer = new CustomerEntity(
                    UUID.randomUUID(), "xxx", 90001, "admin@xxx.example.com");
                return customerRepository.save(newCustomer);
            });

            // then
            attempt.assertExceptionWithRootCauseMessage(
                PersistenceException.class,
                "add-customer not permitted for customer#aaa.admin");
        }

        @Test
        public void customerAdmin_withoutAssumedRole_cannotCreateNewCustomer() {
            // given
            currentUser("admin@aaa.example.com");

            // when
            final var attempt = attempt(em, () -> {
                final var newCustomer = new CustomerEntity(
                    UUID.randomUUID(), "yyy", 90002, "admin@yyy.example.com");
                return customerRepository.save(newCustomer);
            });

            // then
            attempt.assertExceptionWithRootCauseMessage(
                PersistenceException.class,
                "add-customer not permitted for admin@aaa.example.com");

        }

        private void assertThatCustomerIsPersisted(final CustomerEntity saved) {
            final var found = customerRepository.findById(saved.getUuid());
            assertThat(found).hasValue(saved);
        }
    }

    @Nested
    class FindAllCustomers {

        @Test
        public void hostsharingAdmin_withoutAssumedRole_canViewAllCustomers() {
            // given
            currentUser("mike@hostsharing.net");

            // when
            final var result = customerRepository.findAll();

            // then
            exactlyTheseCustomersAreReturned(result, "aaa", "aab", "aac");
        }

        @Test
        public void hostsharingAdmin_withAssumedHostsharingAdminRole_canViewAllCustomers() {
            given:
            currentUser("mike@hostsharing.net");
            assumedRoles("global#hostsharing.admin");

            // when
            final var result = customerRepository.findAll();

            then:
            exactlyTheseCustomersAreReturned(result, "aaa", "aab", "aac");
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewOnlyItsOwnCustomer() {
            // given:
            currentUser("admin@aaa.example.com");

            // when:
            final var result = customerRepository.findAll();

            // then:
            exactlyTheseCustomersAreReturned(result, "aaa");
        }

        @Test
        public void customerAdmin_withAssumedOwnedPackageAdminRole_canViewOnlyItsOwnCustomer() {
            currentUser("admin@aaa.example.com");
            assumedRoles("package#aaa00.admin");

            final var result = customerRepository.findAll();

            exactlyTheseCustomersAreReturned(result, "aaa");
        }

        @Test
        public void customerAdmin_withAssumedAlienPackageAdminRole_cannotViewAnyCustomer() {
            // given:
            currentUser("admin@aaa.example.com");
            assumedRoles("package#aab00.admin");

            // when
            final var attempt = attempt(
                em,
                () -> customerRepository.findAll());

            // then
            attempt.assertExceptionWithRootCauseMessage(
                JpaSystemException.class,
                "user admin@aaa.example.com .* has no permission to assume role package#aab00#admin");
        }

        @Test
        void unknownUser_withoutAssumedRole_cannotViewAnyCustomers() {
            currentUser("unknown@example.org");

            final var attempt = attempt(
                em,
                () -> customerRepository.findAll());

            attempt.assertExceptionWithRootCauseMessage(
                JpaSystemException.class,
                "hsadminng.currentUser defined as unknown@example.org, but does not exists");
        }

        @Test
        @Transactional
        void unknownUser_withAssumedCustomerRole_cannotViewAnyCustomers() {
            currentUser("unknown@example.org");
            assumedRoles("customer#aaa.admin");

            final var attempt = attempt(
                em,
                () -> customerRepository.findAll());

            attempt.assertExceptionWithRootCauseMessage(
                JpaSystemException.class,
                "hsadminng.currentUser defined as unknown@example.org, but does not exists");
        }

    }

    void currentUser(final String currentUser) {
        context.setCurrentUser(currentUser);
        assertThat(context.getCurrentUser()).as("precondition").isEqualTo(currentUser);
    }

    void assumedRoles(final String assumedRoles) {
        context.assumeRoles(assumedRoles);
        assertThat(context.getAssumedRoles()).as("precondition").containsExactly(assumedRoles.split(";"));
    }

    void exactlyTheseCustomersAreReturned(final List<CustomerEntity> actualResult, final String... customerPrefixes) {
        assertThat(actualResult)
            .hasSize(customerPrefixes.length)
            .extracting(CustomerEntity::getPrefix)
            .containsExactlyInAnyOrder(customerPrefixes);
    }

}
