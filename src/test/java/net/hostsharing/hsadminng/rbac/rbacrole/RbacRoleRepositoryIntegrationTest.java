package net.hostsharing.hsadminng.rbac.rbacrole;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.test.Array;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.annotation.DirtiesContext;

import javax.persistence.EntityManager;
import java.util.List;

import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { Context.class, RbacRoleRepository.class })
@DirtiesContext
class RbacRoleRepositoryIntegrationTest {

    @Autowired
    Context context;

    @Autowired
    RbacRoleRepository rbacRoleRepository;

    @Autowired EntityManager em;

    @Nested
    class FindAllRbacRoles {

        private static final String[] ALL_TEST_DATA_ROLES = Array.of(
            // @formatter:off
            "global#hostsharing.admin",
                "customer#xxx.admin", "customer#xxx.owner", "customer#xxx.tenant",
                    "package#xxx00.admin", "package#xxx00.owner", "package#xxx00.tenant",
                    "package#xxx01.admin", "package#xxx01.owner", "package#xxx01.tenant",
                    "package#xxx02.admin", "package#xxx02.owner", "package#xxx02.tenant",
                "customer#yyy.admin", "customer#yyy.owner", "customer#yyy.tenant",
                    "package#yyy00.admin", "package#yyy00.owner", "package#yyy00.tenant",
                    "package#yyy01.admin", "package#yyy01.owner", "package#yyy01.tenant",
                    "package#yyy02.admin", "package#yyy02.owner", "package#yyy02.tenant",
                "customer#zzz.admin", "customer#zzz.owner", "customer#zzz.tenant",
                    "package#zzz00.admin", "package#zzz00.owner", "package#zzz00.tenant",
                    "package#zzz01.admin", "package#zzz01.owner", "package#zzz01.tenant",
                    "package#zzz02.admin", "package#zzz02.owner", "package#zzz02.tenant"
            // @formatter:on
        );

        @Test
        public void hostsharingAdmin_withoutAssumedRole_canViewAllRbacRoles() {
            // given
            currentUser("mike@hostsharing.net");

            // when
            final var result = rbacRoleRepository.findAll();

            // then
            allTheseRbacRolesAreReturned(result, ALL_TEST_DATA_ROLES);
        }

        @Test
        public void hostsharingAdmin_withAssumedHostsharingAdminRole_canViewAllRbacRoles() {
            given:
            currentUser("mike@hostsharing.net");
            assumedRoles("global#hostsharing.admin");

            // when
            final var result = rbacRoleRepository.findAll();

            then:
            allTheseRbacRolesAreReturned(result, ALL_TEST_DATA_ROLES);
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewOnlyItsOwnRbacRole() {
            // given:
            currentUser("customer-admin@xxx.example.com");

            // when:
            final var result = rbacRoleRepository.findAll();

            // then:
            allTheseRbacRolesAreReturned(
                result,
                // @formatter:off
                "customer#xxx.admin",
                "customer#xxx.tenant",
                "package#xxx00.admin",
                "package#xxx00.owner",
                "package#xxx00.tenant",
                "package#xxx01.admin",
                "package#xxx01.owner",
                "package#xxx01.tenant",
                // ...
                "unixuser#xxx00-aaaa.admin",
                "unixuser#xxx00-aaaa.owner",
                // ..
                "unixuser#xxx01-aaab.admin",
                "unixuser#xxx01-aaab.owner"
                // @formatter:on
            );
            noneOfTheseRbacRolesIsReturned(
                result,
                // @formatter:off
                "global#hostsharing.admin",
                "customer#xxx.owner",
                "package#yyy00.admin",
                "package#yyy00.owner",
                "package#yyy00.tenant"
                // @formatter:on
            );
        }

        @Test
        public void customerAdmin_withAssumedOwnedPackageAdminRole_canViewOnlyItsOwnRbacRole() {
            currentUser("customer-admin@xxx.example.com");
            assumedRoles("package#xxx00.admin");

            final var result = rbacRoleRepository.findAll();

            exactlyTheseRbacRolesAreReturned(
                result,
                "customer#xxx.tenant",
                "package#xxx00.admin",
                "package#xxx00.tenant",
                "unixuser#xxx00-aaaa.admin",
                "unixuser#xxx00-aaaa.owner",
                "unixuser#xxx00-aaab.admin",
                "unixuser#xxx00-aaab.owner");
        }

        @Test
        public void customerAdmin_withAssumedAlienPackageAdminRole_cannotViewAnyRbacRole() {
            // given:
            currentUser("customer-admin@xxx.example.com");
            assumedRoles("package#yyy00.admin");

            // when
            final var result = attempt(
                em,
                () -> rbacRoleRepository.findAll());

            // then
            result.assertExceptionWithRootCauseMessage(
                JpaSystemException.class,
                "[403] user customer-admin@xxx.example.com", "has no permission to assume role package#yyy00#admin");
        }

        @Test
        void unknownUser_withoutAssumedRole_cannotViewAnyRbacRoles() {
            currentUser("unknown@example.org");

            final var result = attempt(
                em,
                () -> rbacRoleRepository.findAll());

            result.assertExceptionWithRootCauseMessage(
                JpaSystemException.class,
                "hsadminng.currentUser defined as unknown@example.org, but does not exists");
        }

        @Test
        void unknownUser_withAssumedRbacRoleRole_cannotViewAnyRbacRoles() {
            currentUser("unknown@example.org");
            assumedRoles("RbacRole#xxx.admin");

            final var result = attempt(
                em,
                () -> rbacRoleRepository.findAll());

            result.assertExceptionWithRootCauseMessage(
                JpaSystemException.class,
                "hsadminng.currentUser defined as unknown@example.org, but does not exists");
        }
    }

    @Nested
    class FindByName {

        @Test
        void customerAdmin_withoutAssumedRole_canFindItsOwnRolesByName() {
            currentUser("customer-admin@xxx.example.com");

            final var result = rbacRoleRepository.findByRoleName("customer#xxx.admin");

            assertThat(result).isNotNull();
            assertThat(result.getObjectTable()).isEqualTo("customer");
            assertThat(result.getObjectIdName()).isEqualTo("xxx");
            assertThat(result.getRoleType()).isEqualTo(RbacRoleType.admin);
        }

        @Test
        void customerAdmin_withoutAssumedRole_canNotFindAlienRolesByName() {
            currentUser("customer-admin@xxx.example.com");

            final var result = rbacRoleRepository.findByRoleName("customer#bbb.admin");

            assertThat(result).isNull();
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

    void exactlyTheseRbacRolesAreReturned(final List<RbacRoleEntity> actualResult, final String... expectedRoleNames) {
        assertThat(actualResult)
            .extracting(RbacRoleEntity::getRoleName)
            .containsExactlyInAnyOrder(expectedRoleNames);
    }

    void allTheseRbacRolesAreReturned(final List<RbacRoleEntity> actualResult, final String... expectedRoleNames) {
        assertThat(actualResult)
            .extracting(RbacRoleEntity::getRoleName)
            .contains(expectedRoleNames);
    }

    void noneOfTheseRbacRolesIsReturned(final List<RbacRoleEntity> actualResult, final String... unexpectedRoleNames) {
        assertThat(actualResult)
            .extracting(RbacRoleEntity::getRoleName)
            .doesNotContain(unexpectedRoleNames);
    }

}
