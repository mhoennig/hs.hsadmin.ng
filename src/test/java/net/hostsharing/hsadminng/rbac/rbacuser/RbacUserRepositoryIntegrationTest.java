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
                context("customer-admin@xxx.example.com");
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
            "customer-admin@xxx.example.com",
            "pac-admin-xxx00@xxx.example.com", "pac-admin-xxx01@xxx.example.com", "pac-admin-xxx02@xxx.example.com",
            "customer-admin@yyy.example.com",
            "pac-admin-yyy00@yyy.example.com", "pac-admin-yyy01@yyy.example.com", "pac-admin-yyy02@yyy.example.com",
            "customer-admin@zzz.example.com",
            "pac-admin-zzz00@zzz.example.com", "pac-admin-zzz01@zzz.example.com", "pac-admin-zzz02@zzz.example.com"
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
            context("mike@hostsharing.net", "customer#xxx.admin");

            // when
            final var result = rbacUserRepository.findByOptionalNameLike(null);

            then:
            exactlyTheseRbacUsersAreReturned(
                result,
                "customer-admin@xxx.example.com",
                "pac-admin-xxx00@xxx.example.com", "pac-admin-xxx01@xxx.example.com", "pac-admin-xxx02@xxx.example.com"
            );
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewOnlyUsersHavingRolesInThatCustomersRealm() {
            // given:
            context("customer-admin@xxx.example.com");

            // when:
            final var result = rbacUserRepository.findByOptionalNameLike(null);

            // then:
            exactlyTheseRbacUsersAreReturned(
                result,
                "customer-admin@xxx.example.com",
                "pac-admin-xxx00@xxx.example.com", "pac-admin-xxx01@xxx.example.com", "pac-admin-xxx02@xxx.example.com"
            );
        }

        @Test
        public void customerAdmin_withAssumedOwnedPackageAdminRole_canViewOnlyUsersHavingRolesInThatPackage() {
            context("customer-admin@xxx.example.com", "package#xxx00.admin");

            final var result = rbacUserRepository.findByOptionalNameLike(null);

            exactlyTheseRbacUsersAreReturned(result, "pac-admin-xxx00@xxx.example.com");
        }

        @Test
        public void packageAdmin_withoutAssumedRole_canViewOnlyUsersHavingRolesInThatPackage() {
            context("pac-admin-xxx00@xxx.example.com");

            final var result = rbacUserRepository.findByOptionalNameLike(null);

            exactlyTheseRbacUsersAreReturned(result, "pac-admin-xxx00@xxx.example.com");
        }

    }

    @Nested
    class ListUserPermissions {

        private static final String[] ALL_USER_PERMISSIONS = Array.of(
            // @formatter:off
            "global#hostsharing.admin -> global#hostsharing: add-customer",

            "customer#xxx.admin -> customer#xxx: add-package",
            "customer#xxx.admin -> customer#xxx: view",
            "customer#xxx.owner -> customer#xxx: *",
            "customer#xxx.tenant -> customer#xxx: view",
            "package#xxx00.admin -> package#xxx00: add-domain",
            "package#xxx00.admin -> package#xxx00: add-unixuser",
            "package#xxx00.tenant -> package#xxx00: view",
            "package#xxx01.admin -> package#xxx01: add-domain",
            "package#xxx01.admin -> package#xxx01: add-unixuser",
            "package#xxx01.tenant -> package#xxx01: view",
            "package#xxx02.admin -> package#xxx02: add-domain",
            "package#xxx02.admin -> package#xxx02: add-unixuser",
            "package#xxx02.tenant -> package#xxx02: view",

            "customer#yyy.admin -> customer#yyy: add-package",
            "customer#yyy.admin -> customer#yyy: view",
            "customer#yyy.owner -> customer#yyy: *",
            "customer#yyy.tenant -> customer#yyy: view",
            "package#yyy00.admin -> package#yyy00: add-domain",
            "package#yyy00.admin -> package#yyy00: add-unixuser",
            "package#yyy00.tenant -> package#yyy00: view",
            "package#yyy01.admin -> package#yyy01: add-domain",
            "package#yyy01.admin -> package#yyy01: add-unixuser",
            "package#yyy01.tenant -> package#yyy01: view",
            "package#yyy02.admin -> package#yyy02: add-domain",
            "package#yyy02.admin -> package#yyy02: add-unixuser",
            "package#yyy02.tenant -> package#yyy02: view",

            "customer#zzz.admin -> customer#zzz: add-package",
            "customer#zzz.admin -> customer#zzz: view",
            "customer#zzz.owner -> customer#zzz: *",
            "customer#zzz.tenant -> customer#zzz: view",
            "package#zzz00.admin -> package#zzz00: add-domain",
            "package#zzz00.admin -> package#zzz00: add-unixuser",
            "package#zzz00.tenant -> package#zzz00: view",
            "package#zzz01.admin -> package#zzz01: add-domain",
            "package#zzz01.admin -> package#zzz01: add-unixuser",
            "package#zzz01.tenant -> package#zzz01: view",
            "package#zzz02.admin -> package#zzz02: add-domain",
            "package#zzz02.admin -> package#zzz02: add-unixuser",
            "package#zzz02.tenant -> package#zzz02: view"
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
            context("customer-admin@xxx.example.com");

            // when
            final var result = rbacUserRepository.findPermissionsOfUser("customer-admin@xxx.example.com");

            // then
            allTheseRbacPermissionsAreReturned(
                result,
                // @formatter:off
                "customer#xxx.admin -> customer#xxx: add-package",
                "customer#xxx.admin -> customer#xxx: view",
                "customer#xxx.tenant -> customer#xxx: view",

                "package#xxx00.admin -> package#xxx00: add-domain",
                "package#xxx00.admin -> package#xxx00: add-unixuser",
                "package#xxx00.tenant -> package#xxx00: view",
                "unixuser#xxx00-aaaa.owner -> unixuser#xxx00-aaaa: *",

                "package#xxx01.admin -> package#xxx01: add-domain",
                "package#xxx01.admin -> package#xxx01: add-unixuser",
                "package#xxx01.tenant -> package#xxx01: view",
                "unixuser#xxx01-aaaa.owner -> unixuser#xxx01-aaaa: *",

                "package#xxx02.admin -> package#xxx02: add-domain",
                "package#xxx02.admin -> package#xxx02: add-unixuser",
                "package#xxx02.tenant -> package#xxx02: view",
                "unixuser#xxx02-aaaa.owner -> unixuser#xxx02-aaaa: *"
                // @formatter:on
            );
            noneOfTheseRbacPermissionsAreReturned(
                result,
                // @formatter:off
                "customer#yyy.admin -> customer#yyy: add-package",
                "customer#yyy.admin -> customer#yyy: view",
                "customer#yyy.tenant -> customer#yyy: view"
                // @formatter:on
            );
        }

        @Test
        public void customerAdmin_withoutAssumedRole_isNotAllowedToViewGlobalAdminsPermissions() {
            // given
            context("customer-admin@xxx.example.com");

            // when
            final var result = attempt(em, () ->
                rbacUserRepository.findPermissionsOfUser("mike@hostsharing.net")
            );

            // then
            result.assertExceptionWithRootCauseMessage(
                JpaSystemException.class,
                "[403] permissions of user \"mike@hostsharing.net\" are not accessible to user \"customer-admin@xxx.example.com\"");
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewAllPermissionsWithinThePacketsRealm() {
            // given
            context("customer-admin@xxx.example.com");

            // when
            final var result = rbacUserRepository.findPermissionsOfUser("pac-admin-xxx00@xxx.example.com");

            // then
            allTheseRbacPermissionsAreReturned(
                result,
                // @formatter:off
                "customer#xxx.tenant -> customer#xxx: view",
                // "customer#xxx.admin -> customer#xxx: view" - Not permissions through the customer admin!
                "package#xxx00.admin -> package#xxx00: add-unixuser",
                "package#xxx00.admin -> package#xxx00: add-domain",
                "package#xxx00.tenant -> package#xxx00: view",
                "unixuser#xxx00-aaaa.owner -> unixuser#xxx00-aaaa: *",
                "unixuser#xxx00-aaab.owner -> unixuser#xxx00-aaab: *"
                // @formatter:on
            );
            noneOfTheseRbacPermissionsAreReturned(
                result,
                // @formatter:off
                "customer#yyy.admin -> customer#yyy: add-package",
                "customer#yyy.admin -> customer#yyy: view",
                "customer#yyy.tenant -> customer#yyy: view",
                "package#yyy00.admin -> package#yyy00: add-unixuser",
                "package#yyy00.admin -> package#yyy00: add-domain",
                "package#yyy00.tenant -> package#yyy00: view",
                "unixuser#yyy00-aaaa.owner -> unixuser#yyy00-aaaa: *",
                "unixuser#yyy00-aaab.owner -> unixuser#yyy00-aaab: *"
                // @formatter:on
            );
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canNotViewPermissionsOfUnrelatedUsers() {
            // given
            context("customer-admin@xxx.example.com");

            // when
            final var result = rbacUserRepository.findPermissionsOfUser("pac-admin-yyy00@yyy.example.com");

            // then
            noRbacPermissionsAreReturned(result);
        }

        @Test
        public void packetAdmin_withoutAssumedRole_canViewAllPermissionsWithinThePacketsRealm() {
            // given
            context("pac-admin-xxx00@xxx.example.com");

            // when
            final var result = rbacUserRepository.findPermissionsOfUser("pac-admin-xxx00@xxx.example.com");

            // then
            allTheseRbacPermissionsAreReturned(
                result,
                // @formatter:off
                "customer#xxx.tenant -> customer#xxx: view",
                // "customer#xxx.admin -> customer#xxx: view" - Not permissions through the customer admin!
                "package#xxx00.admin -> package#xxx00: add-unixuser",
                "package#xxx00.admin -> package#xxx00: add-domain",
                "package#xxx00.tenant -> package#xxx00: view"
                // @formatter:on
            );
            noneOfTheseRbacPermissionsAreReturned(
                result,
                // @formatter:off
                // no customer admin permissions
                "customer#xxx.admin -> customer#xxx: add-package",
                // no permissions on other customer's objects
                "customer#yyy.admin -> customer#yyy: add-package",
                "customer#yyy.admin -> customer#yyy: view",
                "customer#yyy.tenant -> customer#yyy: view",
                "package#yyy00.admin -> package#yyy00: add-unixuser",
                "package#yyy00.admin -> package#yyy00: add-domain",
                "package#yyy00.tenant -> package#yyy00: view",
                "unixuser#yyy00-aaaa.owner -> unixuser#yyy00-aaaa: *",
                "unixuser#yyy00-xxxb.owner -> unixuser#yyy00-xxxb: *"
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
