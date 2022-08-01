package net.hostsharing.hsadminng.hscustomer;

import net.hostsharing.hsadminng.context.Context;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.NestedRuntimeException;
import org.springframework.orm.jpa.JpaSystemException;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

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
    class FindAll {

        private final Given given = new Given();
        private When<List<CustomerEntity>> when;
        private final Then then = new Then();

        @Test
        public void hostsharingAdminWithoutAssumedRoleCanViewAllCustomers() {
            given.currentUser("mike@hostsharing.net");

            when(() -> customerRepository.findAll());

            then.exactlyTheseCustomersAreReturned("aaa", "aab", "aac");
        }

        @Test
        public void hostsharingAdminWithAssumedHostsharingAdminRoleCanViewAllCustomers() {
            given.currentUser("mike@hostsharing.net").
                and().assumedRoles("global#hostsharing.admin");

            when(() -> customerRepository.findAll());

            then.exactlyTheseCustomersAreReturned("aaa", "aab", "aac");
        }

        @Test
        public void customerAdminWithoutAssumedRoleCanViewOnlyItsOwnCustomer() {
            given.currentUser("admin@aaa.example.com");

            when(() -> customerRepository.findAll());

            then.exactlyTheseCustomersAreReturned("aaa");
        }

        @Test
        public void customerAdminWithAssumedOwnedPackageAdminRoleCanViewOnlyItsOwnCustomer() {
            given.currentUser("admin@aaa.example.com").
                and().assumedRoles("package#aaa00.admin");

            when(() -> customerRepository.findAll());

            then.exactlyTheseCustomersAreReturned("aaa");
        }

        @Test
        public void customerAdmin_withAssumedAlienPackageAdminRole_cannotViewAnyCustomer() {
            given.currentUser("admin@aaa.example.com").
                and().assumedRoles("package#aab00.admin");

            when(() -> customerRepository.findAll());

            then.expectJpaSystemExceptionHasBeenThrown().
                and()
                .expectRootCauseMessageMatches(
                    ".* user admin@aaa.example.com .* has no permission to assume role package#aab00#admin .*");
        }

        @Test
        void unknownUser_withoutAssumedRole_cannotViewAnyCustomers() {
            given.currentUser("unknown@example.org");

            when(() -> customerRepository.findAll());

            then.expectJpaSystemExceptionHasBeenThrown().
                and().expectRootCauseMessageMatches(".* user unknown@example.org does not exist.*");
        }

        @Test
        @Transactional
        void unknownUserWithAssumedCustomerRoleCannotViewAnyCustomers() {
            given.currentUser("unknown@example.org").
                and().assumedRoles("customer#aaa.admin");

            when(() -> customerRepository.findAll());

            then.expectJpaSystemExceptionHasBeenThrown().
                and().expectRootCauseMessageMatches(".* user unknown@example.org does not exist.*");
        }

        void when(final Supplier<List<CustomerEntity>> code) {
            try {
                when = new When<>(code.get());
            } catch (final NestedRuntimeException exc) {
                when = new When<>(exc);
            }
        }

        private class Then {

            Then and() {
                return this;
            }

            void exactlyTheseCustomersAreReturned(final String... customerPrefixes) {
                assertThat(when.actualResult)
                    .hasSize(customerPrefixes.length)
                    .extracting(CustomerEntity::getPrefix)
                    .containsExactlyInAnyOrder(customerPrefixes);
            }

            Then expectJpaSystemExceptionHasBeenThrown() {
                assertThat(when.actualException).isInstanceOf(JpaSystemException.class);
                return this;
            }

            void expectRootCauseMessageMatches(final String expectedMessage) {
                assertThat(firstRootCauseMessageLineOf(when.actualException)).matches(expectedMessage);
            }
        }
    }

    private String firstRootCauseMessageLineOf(final NestedRuntimeException exception) {
        return Optional.ofNullable(exception.getRootCause())
            .map(Throwable::getMessage)
            .map(message -> message.split("\\r|\\n|\\r\\n", 0)[0])
            .orElse(null);
    }

    private class Given {

        Given and() {
            return this;
        }

        Given currentUser(final String currentUser) {
            context.setCurrentUser(currentUser);
            assertThat(context.getCurrentUser()).as("precondition").isEqualTo(currentUser);
            return this;
        }

        void assumedRoles(final String assumedRoles) {
            context.assumeRoles(assumedRoles);
            assertThat(context.getAssumedRoles()).as("precondition").containsExactly(assumedRoles.split(";"));
        }
    }

    private static class When<T> {

        T actualResult;
        NestedRuntimeException actualException;

        When(final T actualResult) {
            this.actualResult = actualResult;
        }

        When(final NestedRuntimeException exception) {
            this.actualException = exception;
        }
    }
}
