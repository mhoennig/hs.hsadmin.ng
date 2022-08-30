package net.hostsharing.hsadminng.hs.hspackage;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.hscustomer.CustomerRepository;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

    @MockBean
    HttpServletRequest request;

    @Nested
    class FindAllByOptionalNameLike {

        @Test
        public void hostsharingAdmin_withoutAssumedRole_canNotViewAnyPackages_becauseThoseGrantsAreNotassumedd() {
            // given
            context.define("mike@hostsharing.net");

            // when
            final var result = packageRepository.findAllByOptionalNameLike(null);

            // then
            noPackagesAreReturned(result);
        }

        @Test
        public void hostsharingAdmin_withAssumedHostsharingAdminRole__canNotViewAnyPackages_becauseThoseGrantsAreNotassumedd() {
            given:
            context.define("mike@hostsharing.net", "global#hostsharing.admin");

            // when
            final var result = packageRepository.findAllByOptionalNameLike(null);

            then:
            noPackagesAreReturned(result);
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewOnlyItsOwnPackages() {
            // given:
            context.define("customer-admin@xxx.example.com");

            // when:
            final var result = packageRepository.findAllByOptionalNameLike(null);

            // then:
            exactlyThesePackagesAreReturned(result, "xxx00", "xxx01", "xxx02");
        }

        @Test
        public void customerAdmin_withAssumedOwnedPackageAdminRole_canViewOnlyItsOwnPackages() {
            context.define("customer-admin@xxx.example.com", "package#xxx00.admin");

            final var result = packageRepository.findAllByOptionalNameLike(null);

            exactlyThesePackagesAreReturned(result, "xxx00");
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
        context.define("mike@hostsharing.net", assumedRoles);
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
