package net.hostsharing.hsadminng.rbac.test.cust;

import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;

import jakarta.persistence.PersistenceException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
@Tag("generalIntegrationTest")
class TestCustomerRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    TestCustomerRepository testCustomerRepository;

    @MockitoBean
    HttpServletRequest request;

    @Nested
    class CreateCustomer {

        @Test
        public void globalAdmin_withoutAssumedRole_canCreateNewCustomer() {
            // given
            context("hsh-alex_superuser", null);
            final var count = testCustomerRepository.count();

            // when
            final var result = attempt(em, () -> {
                final var newCustomer = new TestCustomerEntity(
                        null, 0, "www", 90001, "tst-customer_admin_www");
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
            context("hsh-alex_superuser", "rbactest.customer#xxx:ADMIN");

            // when
            final var result = attempt(em, () -> {
                final var newCustomer = new TestCustomerEntity(
                        null, 0, "www", 90001, "tst-customer_admin_www");
                return testCustomerRepository.save(newCustomer);
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    PersistenceException.class,
                    "ERROR: [403] insert into rbactest.customer ",
                    "not allowed for current subjects {rbactest.customer#xxx:ADMIN}");
        }

        @Test
        public void customerAdmin_withoutAssumedRole_cannotCreateNewCustomer() {
            // given
            context("tst-customer_admin_xxx", null);

            // when
            final var result = attempt(em, () -> {
                final var newCustomer = new TestCustomerEntity(
                        null, 0, "www", 90001, "tst-customer_admin_www");
                return testCustomerRepository.save(newCustomer);
            });

            // then
            result.assertExceptionWithRootCauseMessage(
                    PersistenceException.class,
                    "ERROR: [403] insert into rbactest.customer ",
                    " not allowed for current subjects {tst-customer_admin_xxx}");

        }

        private void assertThatCustomerIsPersisted(final TestCustomerEntity saved) {
            final var found = testCustomerRepository.findByUuid(saved.getUuid());
            assertThat(found).isNotEmpty().get().extracting(Object::toString).isEqualTo(saved.toString());
        }
    }

    @Nested
    class FindAllCustomers {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllCustomers() {
            // given
            context("hsh-alex_superuser", null);

            // when
            final var result = testCustomerRepository.findCustomerByOptionalPrefixLike(null);

            // then
            allTheseCustomersAreReturned(result, "xxx", "yyy", "zzz");
        }

        @Test
        public void globalAdmin_withAssumedCustomerOwnerRole_canViewExactlyThatCustomer() {
            given:
            context("hsh-alex_superuser", "rbactest.customer#yyy:OWNER");

            // when
            final var result = testCustomerRepository.findCustomerByOptionalPrefixLike(null);

            then:
            allTheseCustomersAreReturned(result, "yyy");
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewOnlyItsOwnCustomer() {
            // given:
            context("tst-customer_admin_xxx", null);

            // when:
            final var result = testCustomerRepository.findCustomerByOptionalPrefixLike(null);

            // then:
            exactlyTheseCustomersAreReturned(result, "xxx");
        }

        @Test
        public void customerAdmin_withAssumedOwnedPackageAdminRole_canViewOnlyItsOwnCustomer() {
            context("tst-customer_admin_xxx");

            context("tst-customer_admin_xxx", "rbactest.package#xxx00:ADMIN");

            final var result = testCustomerRepository.findCustomerByOptionalPrefixLike(null);

            exactlyTheseCustomersAreReturned(result, "xxx");
        }
    }

    @Nested
    class FindByPrefixLike {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllCustomers() {
            // given
            context("hsh-alex_superuser", null);

            // when
            final var result = testCustomerRepository.findCustomerByOptionalPrefixLike("yyy");

            // then
            exactlyTheseCustomersAreReturned(result, "yyy");
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewOnlyItsOwnCustomer() {
            // given:
            context("tst-customer_admin_xxx", null);

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
