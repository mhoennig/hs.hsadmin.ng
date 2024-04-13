package net.hostsharing.hsadminng.rbac.test.cust;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import jakarta.persistence.PersistenceException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
class TestCustomerRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    TestCustomerRepository testCustomerRepository;

    @MockBean
    HttpServletRequest request;

    @Nested
    class CreateCustomer {

        @Test
        public void globalAdmin_withoutAssumedRole_canCreateNewCustomer() {
            // given
            context("superuser-alex@hostsharing.net", null);
            final var count = testCustomerRepository.count();

            // when
            final var result = attempt(em, () -> {
                final var newCustomer = new TestCustomerEntity(
                        UUID.randomUUID(), 0, "www", 90001, "customer-admin@www.example.com");
                return testCustomerRepository.save(newCustomer);
            });

            // then
            assertThat(result.wasSuccessful()).isTrue();
            assertThat(result.returnedValue()).isNotNull().extracting(TestCustomerEntity::getUuid).isNotNull();
            assertThatCustomerIsPersisted(result.returnedValue());
            assertThat(testCustomerRepository.count()).isEqualTo(count + 1);
        }

        @Test
        public void globalAdmin_withAssumedCustomerRole_cannotCreateNewCustomer() {
            // given
            context("superuser-alex@hostsharing.net", "test_customer#xxx:ADMIN");

            // when
            final var result = attempt(em, () -> {
                final var newCustomer = new TestCustomerEntity(
                        UUID.randomUUID(), 0, "www", 90001, "customer-admin@www.example.com");
                return testCustomerRepository.save(newCustomer);
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    PersistenceException.class,
                    "ERROR: [403] insert into test_customer not allowed for current subjects {test_customer#xxx:ADMIN}");
        }

        @Test
        public void customerAdmin_withoutAssumedRole_cannotCreateNewCustomer() {
            // given
            context("customer-admin@xxx.example.com", null);

            // when
            final var result = attempt(em, () -> {
                final var newCustomer = new TestCustomerEntity(
                        UUID.randomUUID(), 0, "www", 90001, "customer-admin@www.example.com");
                return testCustomerRepository.save(newCustomer);
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    PersistenceException.class,
                    "ERROR: [403] insert into test_customer not allowed for current subjects {customer-admin@xxx.example.com}");

        }

        private void assertThatCustomerIsPersisted(final TestCustomerEntity saved) {
            final var found = testCustomerRepository.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().usingRecursiveComparison().isEqualTo(saved);
        }
    }

    @Nested
    class FindAllCustomers {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllCustomers() {
            // given
            context("superuser-alex@hostsharing.net", null);

            // when
            final var result = testCustomerRepository.findCustomerByOptionalPrefixLike(null);

            // then
            allTheseCustomersAreReturned(result, "xxx", "yyy", "zzz");
        }

        @Test
        public void globalAdmin_withAssumedCustomerOwnerRole_canViewExactlyThatCustomer() {
            given:
            context("superuser-alex@hostsharing.net", "test_customer#yyy:OWNER");

            // when
            final var result = testCustomerRepository.findCustomerByOptionalPrefixLike(null);

            then:
            allTheseCustomersAreReturned(result, "yyy");
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewOnlyItsOwnCustomer() {
            // given:
            context("customer-admin@xxx.example.com", null);

            // when:
            final var result = testCustomerRepository.findCustomerByOptionalPrefixLike(null);

            // then:
            exactlyTheseCustomersAreReturned(result, "xxx");
        }

        @Test
        public void customerAdmin_withAssumedOwnedPackageAdminRole_canViewOnlyItsOwnCustomer() {
            context("customer-admin@xxx.example.com");

            context("customer-admin@xxx.example.com", "test_package#xxx00:ADMIN");

            final var result = testCustomerRepository.findCustomerByOptionalPrefixLike(null);

            exactlyTheseCustomersAreReturned(result, "xxx");
        }
    }

    @Nested
    class FindByPrefixLike {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllCustomers() {
            // given
            context("superuser-alex@hostsharing.net", null);

            // when
            final var result = testCustomerRepository.findCustomerByOptionalPrefixLike("yyy");

            // then
            exactlyTheseCustomersAreReturned(result, "yyy");
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewOnlyItsOwnCustomer() {
            // given:
            context("customer-admin@xxx.example.com", null);

            // when:
            final var result = testCustomerRepository.findCustomerByOptionalPrefixLike("yyy");

            // then:
            exactlyTheseCustomersAreReturned(result);
        }
    }

    void exactlyTheseCustomersAreReturned(final List<TestCustomerEntity> actualResult, final String... customerPrefixes) {
        assertThat(actualResult)
                .hasSize(customerPrefixes.length)
                .extracting(TestCustomerEntity::getPrefix)
                .containsExactlyInAnyOrder(customerPrefixes);
    }

    void allTheseCustomersAreReturned(final List<TestCustomerEntity> actualResult, final String... customerPrefixes) {
        assertThat(actualResult)
                .extracting(TestCustomerEntity::getPrefix)
                .contains(customerPrefixes);
    }
}
