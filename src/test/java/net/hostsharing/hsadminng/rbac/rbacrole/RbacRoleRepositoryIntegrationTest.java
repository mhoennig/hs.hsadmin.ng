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
                "customer#aaa.admin", "customer#aaa.owner", "customer#aaa.tenant",
                    "package#aaa00.admin", "package#aaa00.owner", "package#aaa00.tenant",
                    "package#aaa01.admin", "package#aaa01.owner", "package#aaa01.tenant",
                    "package#aaa02.admin", "package#aaa02.owner", "package#aaa02.tenant",
                "customer#aab.admin", "customer#aab.owner", "customer#aab.tenant",
                    "package#aab00.admin", "package#aab00.owner", "package#aab00.tenant",
                    "package#aab01.admin", "package#aab01.owner", "package#aab01.tenant",
                    "package#aab02.admin", "package#aab02.owner", "package#aab02.tenant",
                "customer#aac.admin", "customer#aac.owner", "customer#aac.tenant",
                    "package#aac00.admin", "package#aac00.owner", "package#aac00.tenant",
                    "package#aac01.admin", "package#aac01.owner", "package#aac01.tenant",
                    "package#aac02.admin", "package#aac02.owner", "package#aac02.tenant"
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
            currentUser("admin@aaa.example.com");

            // when:
            final var result = rbacRoleRepository.findAll();

            // then:
            allTheseRbacRolesAreReturned(
                result,
                // @formatter:off
                "customer#aaa.admin",
                "customer#aaa.tenant",
                "package#aaa00.admin",
                "package#aaa00.owner",
                "package#aaa00.tenant",
                "package#aaa01.admin",
                "package#aaa01.owner",
                "package#aaa01.tenant",
                // ...
                "unixuser#aaa00-aaaa.admin",
                "unixuser#aaa00-aaaa.owner",
                // ..
                "unixuser#aaa01-aaaa.admin",
                "unixuser#aaa01-aaaa.owner"
                // @formatter:on
            );
            noneOfTheseRbacRolesIsReturned(
                result,
                // @formatter:off
                "global#hostsharing.admin",
                "customer#aaa.owner",
                "package#aab00.admin",
                "package#aab00.owner",
                "package#aab00.tenant"
                // @formatter:on
            );
        }

        @Test
        public void customerAdmin_withAssumedOwnedPackageAdminRole_canViewOnlyItsOwnRbacRole() {
            currentUser("admin@aaa.example.com");
            assumedRoles("package#aaa00.admin");

            final var result = rbacRoleRepository.findAll();

            exactlyTheseRbacRolesAreReturned(
                result,
                "customer#aaa.tenant",
                "package#aaa00.admin",
                "package#aaa00.tenant",
                "unixuser#aaa00-aaaa.admin",
                "unixuser#aaa00-aaaa.owner",
                "unixuser#aaa00-aaab.admin",
                "unixuser#aaa00-aaab.owner");
        }

        @Test
        public void customerAdmin_withAssumedAlienPackageAdminRole_cannotViewAnyRbacRole() {
            // given:
            currentUser("admin@aaa.example.com");
            assumedRoles("package#aab00.admin");

            // when
            final var result = attempt(
                em,
                () -> rbacRoleRepository.findAll());

            // then
            result.assertExceptionWithRootCauseMessage(
                JpaSystemException.class,
                "[403] user admin@aaa.example.com", "has no permission to assume role package#aab00#admin");
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
            assumedRoles("RbacRole#aaa.admin");

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
            currentUser("admin@aaa.example.com");

            final var result = rbacRoleRepository.findByRoleName("customer#aaa.admin");

            assertThat(result).isNotNull();
            assertThat(result.getObjectTable()).isEqualTo("customer");
            assertThat(result.getObjectIdName()).isEqualTo("aaa");
            assertThat(result.getRoleType()).isEqualTo(RbacRoleType.admin);
        }

        @Test
        void customerAdmin_withoutAssumedRole_canNotFindAlienRolesByName() {
            currentUser("admin@aaa.example.com");

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
