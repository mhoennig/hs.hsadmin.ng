package net.hostsharing.hsadminng.hs.hspackage;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.hscustomer.CustomerRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { Context.class, CustomerRepository.class })
@DirtiesContext
class PackageRepositoryIntegrationTest {

    @Autowired
    Context context;

    @Autowired
    PackageRepository packageRepository;

    @Autowired EntityManager em;

    @Nested
    class FindAllByOptionalNameLike {

        @Test
        public void hostsharingAdmin_withoutAssumedRole_canNotViewAnyPackages_becauseThoseGrantsAreNotassumedd() {
            // given
            currentUser("mike@hostsharing.net");

            // when
            final var result = packageRepository.findAllByOptionalNameLike(null);

            // then
            noPackagesAreReturned(result);
        }

        @Test
        public void hostsharingAdmin_withAssumedHostsharingAdminRole__canNotViewAnyPackages_becauseThoseGrantsAreNotassumedd() {
            given:
            currentUser("mike@hostsharing.net");
            assumedRoles("global#hostsharing.admin");

            // when
            final var result = packageRepository.findAllByOptionalNameLike(null);

            then:
            noPackagesAreReturned(result);
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewOnlyItsOwnPackages() {
            // given:
            currentUser("admin@aaa.example.com");

            // when:
            final var result = packageRepository.findAllByOptionalNameLike(null);

            // then:
            exactlyThesePackagesAreReturned(result, "aaa00", "aaa01", "aaa02");
        }

        @Test
        public void customerAdmin_withAssumedOwnedPackageAdminRole_canViewOnlyItsOwnPackages() {
            currentUser("admin@aaa.example.com");
            assumedRoles("package#aaa00.admin");

            final var result = packageRepository.findAllByOptionalNameLike(null);

            exactlyThesePackagesAreReturned(result, "aaa00");
        }

        @Test
        public void customerAdmin_withAssumedAlienPackageAdminRole_cannotViewAnyPackages() {
            // given:
            currentUser("admin@aaa.example.com");
            assumedRoles("package#aab00.admin");

            // when
            final var result = attempt(
                em,
                () -> packageRepository.findAllByOptionalNameLike(null));

            // then
            result.assertExceptionWithRootCauseMessage(
                JpaSystemException.class,
                "[403] user admin@aaa.example.com", "has no permission to assume role package#aab00#admin");
        }

        @Test
        void unknownUser_withoutAssumedRole_cannotViewAnyPackages() {
            currentUser("unknown@example.org");

            final var result = attempt(
                em,
                () -> packageRepository.findAllByOptionalNameLike(null));

            result.assertExceptionWithRootCauseMessage(
                JpaSystemException.class,
                "hsadminng.currentUser defined as unknown@example.org, but does not exists");
        }

        @Test
        @Transactional
        void unknownUser_withAssumedCustomerRole_cannotViewAnyPackages() {
            currentUser("unknown@example.org");
            assumedRoles("customer#aaa.admin");

            final var result = attempt(
                em,
                () -> packageRepository.findAllByOptionalNameLike(null));

            result.assertExceptionWithRootCauseMessage(
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

    void noPackagesAreReturned(final List<PackageEntity> actualResult) {
        assertThat(actualResult)
            .extracting(PackageEntity::getName)
            .isEmpty();
    }

    void exactlyThesePackagesAreReturned(final List<PackageEntity> actualResult, final String... packageNames) {
        assertThat(actualResult)
            .extracting(PackageEntity::getName)
            .containsExactlyInAnyOrder(packageNames);
    }

}
