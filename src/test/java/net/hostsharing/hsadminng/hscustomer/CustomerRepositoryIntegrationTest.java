package net.hostsharing.hsadminng.hscustomer;

import net.hostsharing.hsadminng.context.Context;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.orm.jpa.JpaSystemException;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ComponentScan(basePackageClasses = { Context.class, CustomerRepository.class })
class CustomerRepositoryIntegrationTest {

    final static String adminUser = "mike@hostsharing.net";
    final static String customerAaa = "admin@aaa.example.com";

    @Autowired
    Context context;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired EntityManager em;

    @Test
    @Transactional
    void hostsharingAdminWithoutAssumedRoleCanViewAllCustomers() {
        // given
        context.setCurrentUser(adminUser);

        // when
        final var actual = customerRepository.findAll();

        // then

        assertThat(actual).hasSize(3)
            .extracting(CustomerEntity::getPrefix)
            .containsExactlyInAnyOrder("aaa", "aab", "aac");
    }

    @Test
    @Transactional
    void hostsharingAdminWithAssumedHostsharingAdminRoleCanViewAllCustomers() {
        // given
        context.setCurrentUser(adminUser);
        context.assumeRoles("global#hostsharing.admin");

        // when
        final var actual = customerRepository.findAll();

        // then

        assertThat(actual).hasSize(3)
            .extracting(CustomerEntity::getPrefix)
            .containsExactlyInAnyOrder("aaa", "aab", "aac");
    }

    @Test
    @Transactional
    void customerAdminWithoutAssumedRoleCanViewItsOwnCustomer() {
        // given
        context.setCurrentUser(customerAaa);

        // when
        final var actual = customerRepository.findAll();

        // then

        assertThat(actual).hasSize(1)
            .extracting(CustomerEntity::getPrefix)
            .containsExactly("aaa");
    }

    @Test
    @Transactional
    void customerAdminWithAssumedOwnedPackageAdminRoleCanViewItsOwnCustomer() {
        // given
        context.setCurrentUser(customerAaa);
        context.assumeRoles("package#aaa00.admin");

        // when
        final var actual = customerRepository.findAll();

        // then
        assertThat(actual).hasSize(1)
            .extracting(CustomerEntity::getPrefix)
            .containsExactly("aaa");
    }

    @Test
    @Transactional
    void customerAdminWithAssumedAlienPackageAdminRoleCanViewItsOwnCustomer() {
        // given
        context.setCurrentUser(customerAaa);
        context.assumeRoles("package#aab00.admin");

        // when
        final JpaSystemException thrown =
            assertThrows(JpaSystemException.class, () -> customerRepository.findAll());

        // then
        assertThat(firstRootCauseMessageLineOf(thrown)).matches(
            ".* user admin@aaa.example.com .* has no permission to assume role package#aab00#admin .*"
        );
   }

    @Test
    @Transactional
    void unknownUserWithoutAssumedRoleCannotViewAnyCustomers() {
        // given
        context.setCurrentUser("unknown@example.org");

        // when
        final JpaSystemException thrown =
            assertThrows(JpaSystemException.class, () -> customerRepository.findAll());

        // then
        assertThat(firstRootCauseMessageLineOf(thrown)).matches(
            ".* user unknown@example.org does not exist.*"
        );
    }

    @Test
    @Transactional
    void unknownUserWithAssumedRoleCannotViewAnyCustomers() {
        // given
        context.setCurrentUser("unknown@example.org");
        assertThat(context.getCurrentUser()).isEqualTo("unknown@example.org");
        context.assumeRoles("customer#aaa.admin");


        // when
        final JpaSystemException thrown =
            assertThrows(JpaSystemException.class, () -> customerRepository.findAll());

        // then
        assertThat(firstRootCauseMessageLineOf(thrown)).matches(
            ".* user unknown@example.org does not exist.*"
        );
    }

    private String firstRootCauseMessageLineOf(final JpaSystemException throwable) {
        return Optional.ofNullable(throwable.getRootCause())
            .map(Throwable::getMessage)
            .map( message -> message.split("\\r|\\n|\\r\\n", 0)[0])
        .orElse(null);
    }
}
