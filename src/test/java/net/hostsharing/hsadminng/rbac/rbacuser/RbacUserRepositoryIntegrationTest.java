package net.hostsharing.hsadminng.rbac.rbacuser;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.test.Array;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.UUID;

import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { RbacUserRepository.class, Context.class, JpaAttempt.class })
@DirtiesContext
class RbacUserRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    RbacUserRepository rbacUserRepository;

    @Autowired
    JpaAttempt jpaAttempt;

    @Autowired EntityManager em;

    @Nested
    class CreateUser {

        @Test
        public void anyoneCanCreateTheirOwnUser() {
            // given
            final var givenNewUserName = "test-user-" + System.currentTimeMillis() + "@example.com";
            context(givenNewUserName, null);

            // when
            final var result = rbacUserRepository.create(
                new RbacUserEntity(null, givenNewUserName));

            // then the persisted user is returned
            assertThat(result).isNotNull().extracting(RbacUserEntity::getName).isEqualTo(givenNewUserName);

            // and the new user entity can be fetched by the user itself
            context(givenNewUserName);
            assertThat(em.find(RbacUserEntity.class, result.getUuid()))
                .isNotNull().extracting(RbacUserEntity::getName).isEqualTo(givenNewUserName);
        }

        @Test
        @Transactional(propagation = Propagation.NEVER)
        void anyoneCanCreateTheirOwnUser_committed() {

            // given:
            final var givenUuid = UUID.randomUUID();
            final var newUserName = "test-user-" + System.currentTimeMillis() + "@example.com";

            // when:
            final var result = jpaAttempt.transacted(() -> {
                context("admin@aaa.example.com");
                return rbacUserRepository.create(new RbacUserEntity(givenUuid, newUserName));
            });

            // then:
            assertThat(result.wasSuccessful()).isTrue();
            assertThat(result.returnedValue()).isNotNull()
                .extracting(RbacUserEntity::getUuid).isEqualTo(givenUuid);
            jpaAttempt.transacted(() -> {
                context(newUserName);
                assertThat(em.find(RbacUserEntity.class, givenUuid))
                    .isNotNull().extracting(RbacUserEntity::getName).isEqualTo(newUserName);
            });
        }
    }

    @Nested
    class FindByOptionalNameLike {

        private static final String[] ALL_TEST_DATA_USERS = Array.of(
            // @formatter:off
            "mike@hostsharing.net", "sven@hostsharing.net",
            "admin@aaa.example.com",
            "aaa00@aaa.example.com", "aaa01@aaa.example.com", "aaa02@aaa.example.com",
            "admin@aab.example.com",
            "aab00@aab.example.com", "aab01@aab.example.com", "aab02@aab.example.com",
            "admin@aac.example.com",
            "aac00@aac.example.com", "aac01@aac.example.com", "aac02@aac.example.com"
            // @formatter:on
        );

        @Test
        public void hostsharingAdmin_withoutAssumedRole_canViewAllRbacUsers() {
            // given
            context("mike@hostsharing.net");

            // when
            final var result = rbacUserRepository.findByOptionalNameLike(null);

            // then
            exactlyTheseRbacUsersAreReturned(result, ALL_TEST_DATA_USERS);
        }

        @Test
        public void hostsharingAdmin_withAssumedHostsharingAdminRole_canViewAllRbacUsers() {
            given:
            context("mike@hostsharing.net", "global#hostsharing.admin");

            // when
            final var result = rbacUserRepository.findByOptionalNameLike(null);

            then:
            exactlyTheseRbacUsersAreReturned(result, ALL_TEST_DATA_USERS);
        }

        @Test
        public void hostsharingAdmin_withAssumedCustomerAdminRole_canViewOnlyUsersHavingRolesInThatCustomersRealm() {
            given:
            context("mike@hostsharing.net", "customer#aaa.admin");

            // when
            final var result = rbacUserRepository.findByOptionalNameLike(null);

            then:
            exactlyTheseRbacUsersAreReturned(
                result,
                "admin@aaa.example.com",
                "aaa00@aaa.example.com", "aaa01@aaa.example.com", "aaa02@aaa.example.com"
            );
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewOnlyUsersHavingRolesInThatCustomersRealm() {
            // given:
            context("admin@aaa.example.com");

            // when:
            final var result = rbacUserRepository.findByOptionalNameLike(null);

            // then:
            exactlyTheseRbacUsersAreReturned(
                result,
                "admin@aaa.example.com",
                "aaa00@aaa.example.com", "aaa01@aaa.example.com", "aaa02@aaa.example.com"
            );
        }

        @Test
        public void customerAdmin_withAssumedOwnedPackageAdminRole_canViewOnlyUsersHavingRolesInThatPackage() {
            context("admin@aaa.example.com", "package#aaa00.admin");

            final var result = rbacUserRepository.findByOptionalNameLike(null);

            exactlyTheseRbacUsersAreReturned(result, "aaa00@aaa.example.com");
        }

        @Test
        public void packageAdmin_withoutAssumedRole_canViewOnlyUsersHavingRolesInThatPackage() {
            context("aaa00@aaa.example.com");

            final var result = rbacUserRepository.findByOptionalNameLike(null);

            exactlyTheseRbacUsersAreReturned(result, "aaa00@aaa.example.com");
        }

    }

    @Nested
    class ListUserPermissions {

        private static final String[] ALL_USER_PERMISSIONS = Array.of(
            // @formatter:off
            "global#hostsharing.admin -> global#hostsharing: add-customer",

            "customer#aaa.admin -> customer#aaa: add-package",
            "customer#aaa.admin -> customer#aaa: view",
            "customer#aaa.owner -> customer#aaa: *",
            "customer#aaa.tenant -> customer#aaa: view",
            "package#aaa00.admin -> package#aaa00: add-domain",
            "package#aaa00.admin -> package#aaa00: add-unixuser",
            "package#aaa00.tenant -> package#aaa00: view",
            "package#aaa01.admin -> package#aaa01: add-domain",
            "package#aaa01.admin -> package#aaa01: add-unixuser",
            "package#aaa01.tenant -> package#aaa01: view",
            "package#aaa02.admin -> package#aaa02: add-domain",
            "package#aaa02.admin -> package#aaa02: add-unixuser",
            "package#aaa02.tenant -> package#aaa02: view",

            "customer#aab.admin -> customer#aab: add-package",
            "customer#aab.admin -> customer#aab: view",
            "customer#aab.owner -> customer#aab: *",
            "customer#aab.tenant -> customer#aab: view",
            "package#aab00.admin -> package#aab00: add-domain",
            "package#aab00.admin -> package#aab00: add-unixuser",
            "package#aab00.tenant -> package#aab00: view",
            "package#aab01.admin -> package#aab01: add-domain",
            "package#aab01.admin -> package#aab01: add-unixuser",
            "package#aab01.tenant -> package#aab01: view",
            "package#aab02.admin -> package#aab02: add-domain",
            "package#aab02.admin -> package#aab02: add-unixuser",
            "package#aab02.tenant -> package#aab02: view",

            "customer#aac.admin -> customer#aac: add-package",
            "customer#aac.admin -> customer#aac: view",
            "customer#aac.owner -> customer#aac: *",
            "customer#aac.tenant -> customer#aac: view",
            "package#aac00.admin -> package#aac00: add-domain",
            "package#aac00.admin -> package#aac00: add-unixuser",
            "package#aac00.tenant -> package#aac00: view",
            "package#aac01.admin -> package#aac01: add-domain",
            "package#aac01.admin -> package#aac01: add-unixuser",
            "package#aac01.tenant -> package#aac01: view",
            "package#aac02.admin -> package#aac02: add-domain",
            "package#aac02.admin -> package#aac02: add-unixuser",
            "package#aac02.tenant -> package#aac02: view"
            // @formatter:on
        );

        @Test
        public void hostsharingAdmin_withoutAssumedRole_canViewTheirOwnPermissions() {
            // given
            context("mike@hostsharing.net");

            // when
            final var result = rbacUserRepository.findPermissionsOfUser("mike@hostsharing.net");

            // then
            allTheseRbacPermissionsAreReturned(result, ALL_USER_PERMISSIONS);
        }

        @Test
        public void hostsharingAdmin_withAssumedHostmastersRole_willThrowException() {
            // given
            context("mike@hostsharing.net", "global#hostsharing.admin");

            // when
            final var result = attempt(em, () ->
                rbacUserRepository.findPermissionsOfUser("mike@hostsharing.net")
            );

            // then
            result.assertExceptionWithRootCauseMessage(
                JpaSystemException.class,
                "[400] grantedPermissions(...) does not support assumed roles");
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewTheirOwnPermissions() {
            // given
            context("admin@aaa.example.com");

            // when
            final var result = rbacUserRepository.findPermissionsOfUser("admin@aaa.example.com");

            // then
            allTheseRbacPermissionsAreReturned(
                result,
                // @formatter:off
                "customer#aaa.admin -> customer#aaa: add-package",
                "customer#aaa.admin -> customer#aaa: view",
                "customer#aaa.tenant -> customer#aaa: view",

                "package#aaa00.admin -> package#aaa00: add-domain",
                "package#aaa00.admin -> package#aaa00: add-unixuser",
                "package#aaa00.tenant -> package#aaa00: view",
                "unixuser#aaa00-aaaa.owner -> unixuser#aaa00-aaaa: *",

                "package#aaa01.admin -> package#aaa01: add-domain",
                "package#aaa01.admin -> package#aaa01: add-unixuser",
                "package#aaa01.tenant -> package#aaa01: view",
                "unixuser#aaa01-aaaa.owner -> unixuser#aaa01-aaaa: *",

                "package#aaa02.admin -> package#aaa02: add-domain",
                "package#aaa02.admin -> package#aaa02: add-unixuser",
                "package#aaa02.tenant -> package#aaa02: view",
                "unixuser#aaa02-aaaa.owner -> unixuser#aaa02-aaaa: *"
                // @formatter:on
            );
            noneOfTheseRbacPermissionsAreReturned(
                result,
                // @formatter:off
                "customer#aab.admin -> customer#aab: add-package",
                "customer#aab.admin -> customer#aab: view",
                "customer#aab.tenant -> customer#aab: view"
                // @formatter:on
            );
        }

        @Test
        public void customerAdmin_withoutAssumedRole_isNotAllowedToViewGlobalAdminsPermissions() {
            // given
            context("admin@aaa.example.com");

            // when
            final var result = attempt(em, () ->
                rbacUserRepository.findPermissionsOfUser("mike@hostsharing.net")
            );

            // then
            result.assertExceptionWithRootCauseMessage(
                JpaSystemException.class,
                "[403] permissions of user \"mike@hostsharing.net\" are not accessible to user \"admin@aaa.example.com\"");
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewAllPermissionsWithinThePacketsRealm() {
            // given
            context("admin@aaa.example.com");

            // when
            final var result = rbacUserRepository.findPermissionsOfUser("aaa00@aaa.example.com");

            // then
            allTheseRbacPermissionsAreReturned(
                result,
                // @formatter:off
                "customer#aaa.tenant -> customer#aaa: view",
                // "customer#aaa.admin -> customer#aaa: view" - Not permissions through the customer admin!
                "package#aaa00.admin -> package#aaa00: add-unixuser",
                "package#aaa00.admin -> package#aaa00: add-domain",
                "package#aaa00.tenant -> package#aaa00: view",
                "unixuser#aaa00-aaaa.owner -> unixuser#aaa00-aaaa: *",
                "unixuser#aaa00-aaab.owner -> unixuser#aaa00-aaab: *"
                // @formatter:on
            );
            noneOfTheseRbacPermissionsAreReturned(
                result,
                // @formatter:off
                "customer#aab.admin -> customer#aab: add-package",
                "customer#aab.admin -> customer#aab: view",
                "customer#aab.tenant -> customer#aab: view",
                "package#aab00.admin -> package#aab00: add-unixuser",
                "package#aab00.admin -> package#aab00: add-domain",
                "package#aab00.tenant -> package#aab00: view",
                "unixuser#aab00-aaaa.owner -> unixuser#aab00-aaaa: *",
                "unixuser#aab00-aaab.owner -> unixuser#aab00-aaab: *"
                // @formatter:on
            );
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canNotViewPermissionsOfUnrelatedUsers() {
            // given
            context("admin@aaa.example.com");

            // when
            final var result = rbacUserRepository.findPermissionsOfUser("aab00@aab.example.com");

            // then
            noRbacPermissionsAreReturned(result);
        }

        @Test
        public void packetAdmin_withoutAssumedRole_canViewAllPermissionsWithinThePacketsRealm() {
            // given
            context("aaa00@aaa.example.com");

            // when
            final var result = rbacUserRepository.findPermissionsOfUser("aaa00@aaa.example.com");

            // then
            allTheseRbacPermissionsAreReturned(
                result,
                // @formatter:off
                "customer#aaa.tenant -> customer#aaa: view",
                // "customer#aaa.admin -> customer#aaa: view" - Not permissions through the customer admin!
                "package#aaa00.admin -> package#aaa00: add-unixuser",
                "package#aaa00.admin -> package#aaa00: add-domain",
                "package#aaa00.tenant -> package#aaa00: view"
                // @formatter:on
            );
            noneOfTheseRbacPermissionsAreReturned(
                result,
                // @formatter:off
                // no customer admin permissions
                "customer#aaa.admin -> customer#aaa: add-package",
                // no permissions on other customer's objects
                "customer#aab.admin -> customer#aab: add-package",
                "customer#aab.admin -> customer#aab: view",
                "customer#aab.tenant -> customer#aab: view",
                "package#aab00.admin -> package#aab00: add-unixuser",
                "package#aab00.admin -> package#aab00: add-domain",
                "package#aab00.tenant -> package#aab00: view",
                "unixuser#aab00-aaaa.owner -> unixuser#aab00-aaaa: *",
                "unixuser#aab00-aaab.owner -> unixuser#aab00-aaab: *"
                // @formatter:on
            );
        }
    }

    void exactlyTheseRbacUsersAreReturned(final List<RbacUserEntity> actualResult, final String... expectedUserNames) {
        assertThat(actualResult)
            .filteredOn(u -> !u.getName().startsWith("test-user-"))
            .extracting(RbacUserEntity::getName)
            .containsExactlyInAnyOrder(expectedUserNames);
    }

    void noRbacPermissionsAreReturned(
        final List<RbacUserPermission> actualResult) {
        assertThat(actualResult)
            .extracting(p -> p.getRoleName() + " -> " + p.getObjectTable() + "#" + p.getObjectIdName() + ": " + p.getOp())
            .containsExactlyInAnyOrder();
    }

    void exactlyTheseRbacPermissionsAreReturned(
        final List<RbacUserPermission> actualResult,
        final String... expectedRoleNames) {
        assertThat(actualResult)
            .extracting(p -> p.getRoleName() + " -> " + p.getObjectTable() + "#" + p.getObjectIdName() + ": " + p.getOp())
            .containsExactlyInAnyOrder(expectedRoleNames);
    }

    void allTheseRbacPermissionsAreReturned(
        final List<RbacUserPermission> actualResult,
        final String... expectedRoleNames) {
        assertThat(actualResult)
            .extracting(p -> p.getRoleName() + " -> " + p.getObjectTable() + "#" + p.getObjectIdName() + ": " + p.getOp())
            .contains(expectedRoleNames);
    }

    void noneOfTheseRbacPermissionsAreReturned(
        final List<RbacUserPermission> actualResult,
        final String... unexpectedRoleNames) {
        assertThat(actualResult)
            .extracting(p -> p.getRoleName() + " -> " + p.getObjectTable() + "#" + p.getObjectIdName() + ": " + p.getOp())
            .doesNotContain(unexpectedRoleNames);
    }

}
