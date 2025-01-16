package net.hostsharing.hsadminng.rbac.test.pac;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
class TestPackageRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    TestPackageRepository testPackageRepository;

    @PersistenceContext
    EntityManager em;

    @Autowired
    JpaAttempt jpaAttempt;

    @MockitoBean
    HttpServletRequest request;

    @Nested
    class FindAllByOptionalNameLike {

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllPackagesDueToBypassoOfRecursiveCteRbacQuery() {
            // given
            // alex is not just rbac.global-admin but also the creating user, thus we use fran
            context.define("superuser-fran@hostsharing.net");

            // when
            final var result = testPackageRepository.findAllByOptionalNameLike(null);

            // then

            exactlyThesePackagesAreReturned(result,
                    "xxx00", "xxx01", "xxx02", "yyy00", "yyy01", "yyy02", "zzz00", "zzz01", "zzz02");
        }

        @Test
        public void globalAdmin_withAssumedGlobalAdminRole__canNotViewAnyPackages_becauseThoseGrantsAreNotAssumed() {
            // given
            context.define("superuser-alex@hostsharing.net", "rbac.global#global:ADMIN");

            // when
            final var result = testPackageRepository.findAllByOptionalNameLike(null);

            // then
            noPackagesAreReturned(result);
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewOnlyItsOwnPackages() {
            // given:
            context.define("customer-admin@xxx.example.com");

            // when:
            final var result = testPackageRepository.findAllByOptionalNameLike(null);

            // then:
            exactlyThesePackagesAreReturned(result, "xxx00", "xxx01", "xxx02");
        }

        @Test
        public void customerAdmin_withAssumedOwnedPackageAdminRole_canViewOnlyItsOwnPackages() {
            context.define("customer-admin@xxx.example.com", "rbactest.package#xxx00:ADMIN");

            final var result = testPackageRepository.findAllByOptionalNameLike(null);

            exactlyThesePackagesAreReturned(result, "xxx00");
        }
    }

    @Nested
    class OptimisticLocking {

        @Test
        public void supportsOptimisticLocking() {
            // given
            globalAdminWithAssumedRole("rbactest.package#xxx00:ADMIN");
            final var pac = testPackageRepository.findAllByOptionalNameLike("%").get(0);

            // when
            final var result1 = jpaAttempt.transacted(() -> {
                globalAdminWithAssumedRole("rbactest.package#xxx00:OWNER");
                pac.setDescription("description set by thread 1");
                testPackageRepository.save(pac);
            });
            final var result2 = jpaAttempt.transacted(() -> {
                globalAdminWithAssumedRole("rbactest.package#xxx00:OWNER");
                pac.setDescription("description set by thread 2");
                testPackageRepository.save(pac);
                sleep(1500);
            });

            // then
            assertThat(result1.caughtException()).isNull();
            assertThat(result2.caughtException()).isInstanceOf(ObjectOptimisticLockingFailureException.class);
            em.refresh(pac);
            assertThat(pac.getDescription()).isEqualTo("description set by thread 1");
        }

        private void sleep(final int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void globalAdminWithAssumedRole(final String assumedRoles) {
        context.define("superuser-alex@hostsharing.net", assumedRoles);
    }

    void noPackagesAreReturned(final List<TestPackageEntity> actualResult) {
        assertThat(actualResult)
                .extracting(TestPackageEntity::getName)
                .isEmpty();
    }

    void exactlyThesePackagesAreReturned(final List<TestPackageEntity> actualResult, final String... packageNames) {
        assertThat(actualResult)
                .extracting(TestPackageEntity::getName)
                .containsExactlyInAnyOrder(packageNames);
    }

}
