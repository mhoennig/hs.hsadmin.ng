package net.hostsharing.hsadminng.hs.hspackage;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.hscustomer.CustomerRepository;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { Context.class, CustomerRepository.class, JpaAttempt.class })
@DirtiesContext
class PackageRepositoryIntegrationTest {

    @Autowired
    Context context;

    @Autowired
    PackageRepository packageRepository;

    @Autowired
    EntityManager em;

    @Autowired
    JpaAttempt jpaAttempt;

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
            currentUser("customer-admin@xxx.example.com");

            // when:
            final var result = packageRepository.findAllByOptionalNameLike(null);

            // then:
            exactlyThesePackagesAreReturned(result, "xxx00", "xxx01", "xxx02");
        }

        @Test
        public void customerAdmin_withAssumedOwnedPackageAdminRole_canViewOnlyItsOwnPackages() {
            currentUser("customer-admin@xxx.example.com");
            assumedRoles("package#xxx00.admin");

            final var result = packageRepository.findAllByOptionalNameLike(null);

            exactlyThesePackagesAreReturned(result, "xxx00");
        }

        @Test
        public void customerAdmin_withAssumedAlienPackageAdminRole_cannotViewAnyPackages() {
            // given:
            currentUser("customer-admin@xxx.example.com");
            assumedRoles("package#yyy00.admin");

            // when
            final var result = attempt(
                    em,
                    () -> packageRepository.findAllByOptionalNameLike(null));

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] user customer-admin@xxx.example.com", "has no permission to assume role package#yyy00#admin");
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
            assumedRoles("customer#xxx.admin");

            final var result = attempt(
                    em,
                    () -> packageRepository.findAllByOptionalNameLike(null));

            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "hsadminng.currentUser defined as unknown@example.org, but does not exists");
        }

    }

    @Nested
    class OptimisticLocking {

        @Test
        public void supportsOptimisticLocking() throws InterruptedException {
            // given
            hostsharingAdminWithAssumedRole("package#xxx00.admin");
            final var pac = packageRepository.findAllByOptionalNameLike("%").get(0);

            // when
            final var result1 = jpaAttempt.transacted(() -> {
                hostsharingAdminWithAssumedRole("package#xxx00.admin");
                pac.setDescription("description set by thread 1");
                packageRepository.save(pac);
            });
            final var result2 = jpaAttempt.transacted(() -> {
                hostsharingAdminWithAssumedRole("package#xxx00.admin");
                pac.setDescription("description set by thread 2");
                packageRepository.save(pac);
                sleep(1500);
            });

            // then
            em.refresh(pac);
            assertThat(pac.getDescription()).isEqualTo("description set by thread 1");
            assertThat(result1.caughtException()).isNull();
            assertThat(result2.caughtException()).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        }

        private void sleep(final int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void hostsharingAdminWithAssumedRole(final String assumedRoles) {
        currentUser("mike@hostsharing.net");
        assumedRoles(assumedRoles);
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
