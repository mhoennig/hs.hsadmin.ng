package net.hostsharing.hsadminng.rbac.rbacuser;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.test.Array;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
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

    @Autowired
    EntityManager em;

    @MockBean
    HttpServletRequest request;

    @Nested
    class CreateUser {

        @Test
        @Transactional(propagation = Propagation.NEVER)
        void anyoneCanCreateTheirOwnUser() {

            // given:
            final var givenUuid = UUID.randomUUID();
            final var newUserName = "test-user-" + System.currentTimeMillis() + "@example.com";

            // when:
            final var result = jpaAttempt.transacted(() -> {
                context(null);
                return rbacUserRepository.create(new RbacUserEntity(givenUuid, newUserName));
            });

            // then:
            assertThat(result.wasSuccessful()).isTrue();
            assertThat(result.returnedValue()).isNotNull()
                    .extracting(RbacUserEntity::getUuid).isEqualTo(givenUuid);
            assertThat(rbacUserRepository.findByName(result.returnedValue().getName())).isNotNull();
            //        jpaAttempt.transacted(() -> {
            //            context(givenUser.getName());
            //            assertThat(em.find(RbacUserEntity.class, givenUser.getUuid()))
            //                    .isNotNull().extracting(RbacUserEntity::getName).isEqualTo(givenUser.getName());
            //        }).assertSuccessful();
        }
    }

    @Nested
    class DeleteUser {

        @Test
        @Transactional(propagation = Propagation.NEVER)
        public void anyoneCanDeleteTheirOwnUser() {
            // given
            final RbacUserEntity givenUser = givenANewUser();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context(givenUser.getName());
                rbacUserRepository.deleteByUuid(givenUser.getUuid());
            });

            // then the user is deleted
            result.assertSuccessful();
            assertThat(rbacUserRepository.findByName(givenUser.getName())).isNull();
            //        jpaAttempt.transacted(() -> {
            //            assertThat(rbacUserRepository.findByName(givenUser.getName())).isNull();
            //        }).assertSuccessful();
        }
    }

    @Nested
    class FindByOptionalNameLike {

        private static final String[] ALL_TEST_DATA_USERS = Array.of(
                // @formatter:off
            "mike@example.org", "sven@example.org",
            "customer-admin@xxx.example.com",
            "pac-admin-xxx00@xxx.example.com", "pac-admin-xxx01@xxx.example.com", "pac-admin-xxx02@xxx.example.com",
            "customer-admin@yyy.example.com",
            "pac-admin-yyy00@yyy.example.com", "pac-admin-yyy01@yyy.example.com", "pac-admin-yyy02@yyy.example.com",
            "customer-admin@zzz.example.com",
            "pac-admin-zzz00@zzz.example.com", "pac-admin-zzz01@zzz.example.com", "pac-admin-zzz02@zzz.example.com"
            // @formatter:on
        );

        @Test
        public void testGlobalAdmin_withoutAssumedRole_canViewAllRbacUsers() {
            // given
            context("mike@example.org");

            // when
            final var result = rbacUserRepository.findByOptionalNameLike(null);

            // then
            allTheseRbacUsersAreReturned(result, ALL_TEST_DATA_USERS);
        }

        @Test
        public void testGlobalAdmin_withAssumedtestGlobalAdminRole_canViewAllRbacUsers() {
            given:
            context("mike@example.org", "global#test-global.admin");

            // when
            final var result = rbacUserRepository.findByOptionalNameLike(null);

            then:
            allTheseRbacUsersAreReturned(result, ALL_TEST_DATA_USERS);
        }

        @Test
        public void testGlobalAdmin_withAssumedCustomerAdminRole_canViewOnlyUsersHavingRolesInThatCustomersRealm() {
            given:
            context("mike@example.org", "test_customer#xxx.admin");

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
            context("customer-admin@xxx.example.com", "test_package#xxx00.admin");

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
            "global#test-global.admin -> global#test-global: add-customer",

            "test_customer#xxx.admin -> test_customer#xxx: add-package",
            "test_customer#xxx.admin -> test_customer#xxx: view",
            "test_customer#xxx.owner -> test_customer#xxx: *",
            "test_customer#xxx.tenant -> test_customer#xxx: view",
            "test_package#xxx00.admin -> test_package#xxx00: add-domain",
            "test_package#xxx00.admin -> test_package#xxx00: add-domain",
            "test_package#xxx00.tenant -> test_package#xxx00: view",
            "test_package#xxx01.admin -> test_package#xxx01: add-domain",
            "test_package#xxx01.admin -> test_package#xxx01: add-domain",
            "test_package#xxx01.tenant -> test_package#xxx01: view",
            "test_package#xxx02.admin -> test_package#xxx02: add-domain",
            "test_package#xxx02.admin -> test_package#xxx02: add-domain",
            "test_package#xxx02.tenant -> test_package#xxx02: view",

            "test_customer#yyy.admin -> test_customer#yyy: add-package",
            "test_customer#yyy.admin -> test_customer#yyy: view",
            "test_customer#yyy.owner -> test_customer#yyy: *",
            "test_customer#yyy.tenant -> test_customer#yyy: view",
            "test_package#yyy00.admin -> test_package#yyy00: add-domain",
            "test_package#yyy00.admin -> test_package#yyy00: add-domain",
            "test_package#yyy00.tenant -> test_package#yyy00: view",
            "test_package#yyy01.admin -> test_package#yyy01: add-domain",
            "test_package#yyy01.admin -> test_package#yyy01: add-domain",
            "test_package#yyy01.tenant -> test_package#yyy01: view",
            "test_package#yyy02.admin -> test_package#yyy02: add-domain",
            "test_package#yyy02.admin -> test_package#yyy02: add-domain",
            "test_package#yyy02.tenant -> test_package#yyy02: view",

            "test_customer#zzz.admin -> test_customer#zzz: add-package",
            "test_customer#zzz.admin -> test_customer#zzz: view",
            "test_customer#zzz.owner -> test_customer#zzz: *",
            "test_customer#zzz.tenant -> test_customer#zzz: view",
            "test_package#zzz00.admin -> test_package#zzz00: add-domain",
            "test_package#zzz00.admin -> test_package#zzz00: add-domain",
            "test_package#zzz00.tenant -> test_package#zzz00: view",
            "test_package#zzz01.admin -> test_package#zzz01: add-domain",
            "test_package#zzz01.admin -> test_package#zzz01: add-domain",
            "test_package#zzz01.tenant -> test_package#zzz01: view",
            "test_package#zzz02.admin -> test_package#zzz02: add-domain",
            "test_package#zzz02.admin -> test_package#zzz02: add-domain",
            "test_package#zzz02.tenant -> test_package#zzz02: view"
            // @formatter:on
        );

        @Test
        public void testGlobalAdmin_withoutAssumedRole_canViewTheirOwnPermissions() {
            // given
            context("mike@example.org");

            // when
            final var result = rbacUserRepository.findPermissionsOfUserByUuid(userUUID("mike@example.org"));

            // then
            allTheseRbacPermissionsAreReturned(result, ALL_USER_PERMISSIONS);
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewTheirOwnPermissions() {
            // given
            context("customer-admin@xxx.example.com");

            // when
            final var result = rbacUserRepository.findPermissionsOfUserByUuid(userUUID("customer-admin@xxx.example.com"));

            // then
            allTheseRbacPermissionsAreReturned(
                    result,
                    // @formatter:off
                "test_customer#xxx.admin -> test_customer#xxx: add-package",
                "test_customer#xxx.admin -> test_customer#xxx: view",
                "test_customer#xxx.tenant -> test_customer#xxx: view",

                "test_package#xxx00.admin -> test_package#xxx00: add-domain",
                "test_package#xxx00.admin -> test_package#xxx00: add-domain",
                "test_package#xxx00.tenant -> test_package#xxx00: view",
                "test_domain#xxx00-aaaa.owner -> test_domain#xxx00-aaaa: *",

                "test_package#xxx01.admin -> test_package#xxx01: add-domain",
                "test_package#xxx01.admin -> test_package#xxx01: add-domain",
                "test_package#xxx01.tenant -> test_package#xxx01: view",
                "test_domain#xxx01-aaaa.owner -> test_domain#xxx01-aaaa: *",

                "test_package#xxx02.admin -> test_package#xxx02: add-domain",
                "test_package#xxx02.admin -> test_package#xxx02: add-domain",
                "test_package#xxx02.tenant -> test_package#xxx02: view",
                "test_domain#xxx02-aaaa.owner -> test_domain#xxx02-aaaa: *"
                // @formatter:on
            );
            noneOfTheseRbacPermissionsAreReturned(
                    result,
                    // @formatter:off
                "test_customer#yyy.admin -> test_customer#yyy: add-package",
                "test_customer#yyy.admin -> test_customer#yyy: view",
                "test_customer#yyy.tenant -> test_customer#yyy: view"
                // @formatter:on
            );
        }

        @Test
        public void customerAdmin_withoutAssumedRole_isNotAllowedToViewGlobalAdminsPermissions() {
            // given
            context("customer-admin@xxx.example.com");
            final UUID userUuid = userUUID("mike@example.org");

            // when
            final var result = attempt(em, () ->
                    rbacUserRepository.findPermissionsOfUserByUuid(userUuid)
            );

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] permissions of user \"" + userUuid
                            + "\" are not accessible to user \"customer-admin@xxx.example.com\"");
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewAllPermissionsWithinThePacketsRealm() {
            // given
            context("customer-admin@xxx.example.com");

            // when
            final var result = rbacUserRepository.findPermissionsOfUserByUuid(userUUID("pac-admin-xxx00@xxx.example.com"));

            // then
            allTheseRbacPermissionsAreReturned(
                    result,
                    // @formatter:off
                "test_customer#xxx.tenant -> test_customer#xxx: view",
                // "test_customer#xxx.admin -> test_customer#xxx: view" - Not permissions through the customer admin!
                "test_package#xxx00.admin -> test_package#xxx00: add-domain",
                "test_package#xxx00.admin -> test_package#xxx00: add-domain",
                "test_package#xxx00.tenant -> test_package#xxx00: view",
                "test_domain#xxx00-aaaa.owner -> test_domain#xxx00-aaaa: *",
                "test_domain#xxx00-aaab.owner -> test_domain#xxx00-aaab: *"
                // @formatter:on
            );
            noneOfTheseRbacPermissionsAreReturned(
                    result,
                    // @formatter:off
                "test_customer#yyy.admin -> test_customer#yyy: add-package",
                "test_customer#yyy.admin -> test_customer#yyy: view",
                "test_customer#yyy.tenant -> test_customer#yyy: view",
                "test_package#yyy00.admin -> test_package#yyy00: add-domain",
                "test_package#yyy00.admin -> test_package#yyy00: add-domain",
                "test_package#yyy00.tenant -> test_package#yyy00: view",
                "test_domain#yyy00-aaaa.owner -> test_domain#yyy00-aaaa: *",
                "test_domain#yyy00-aaab.owner -> test_domain#yyy00-aaab: *"
                // @formatter:on
            );
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canNotViewPermissionsOfUnrelatedUsers() {
            // given
            context("customer-admin@xxx.example.com");

            // when
            final var result = rbacUserRepository.findPermissionsOfUserByUuid(userUUID("pac-admin-yyy00@yyy.example.com"));

            // then
            noRbacPermissionsAreReturned(result);
        }

        @Test
        public void packetAdmin_withoutAssumedRole_canViewAllPermissionsWithinThePacketsRealm() {
            // given
            context("pac-admin-xxx00@xxx.example.com");

            // when
            final var result = rbacUserRepository.findPermissionsOfUserByUuid(userUUID("pac-admin-xxx00@xxx.example.com"));

            // then
            allTheseRbacPermissionsAreReturned(
                    result,
                    // @formatter:off
                "test_customer#xxx.tenant -> test_customer#xxx: view",
                // "test_customer#xxx.admin -> test_customer#xxx: view" - Not permissions through the customer admin!
                "test_package#xxx00.admin -> test_package#xxx00: add-domain",
                "test_package#xxx00.admin -> test_package#xxx00: add-domain",
                "test_package#xxx00.tenant -> test_package#xxx00: view"
                // @formatter:on
            );
            noneOfTheseRbacPermissionsAreReturned(
                    result,
                    // @formatter:off
                // no customer admin permissions
                "test_customer#xxx.admin -> test_customer#xxx: add-package",
                // no permissions on other customer's objects
                "test_customer#yyy.admin -> test_customer#yyy: add-package",
                "test_customer#yyy.admin -> test_customer#yyy: view",
                "test_customer#yyy.tenant -> test_customer#yyy: view",
                "test_package#yyy00.admin -> test_package#yyy00: add-domain",
                "test_package#yyy00.admin -> test_package#yyy00: add-domain",
                "test_package#yyy00.tenant -> test_package#yyy00: view",
                "test_domain#yyy00-aaaa.owner -> test_domain#yyy00-aaaa: *",
                "test_domain#yyy00-xxxb.owner -> test_domain#yyy00-xxxb: *"
                // @formatter:on
            );
        }
    }

    UUID userUUID(final String userName) {
        return rbacUserRepository.findByName(userName).getUuid();
    }

    RbacUserEntity givenANewUser() {
        final var givenUserName = "test-user-" + System.currentTimeMillis() + "@example.com";
        final var givenUser = jpaAttempt.transacted(() -> {
            context(null);
            return rbacUserRepository.create(new RbacUserEntity(UUID.randomUUID(), givenUserName));
        }).assumeSuccessful().returnedValue();
        assertThat(rbacUserRepository.findByName(givenUser.getName())).isNotNull();
        return givenUser;
    }

    void exactlyTheseRbacUsersAreReturned(final List<RbacUserEntity> actualResult, final String... expectedUserNames) {
        assertThat(actualResult)
                .extracting(RbacUserEntity::getName)
                .filteredOn(n -> !n.startsWith("test-user"))
                .containsExactlyInAnyOrder(expectedUserNames);
    }

    void allTheseRbacUsersAreReturned(final List<RbacUserEntity> actualResult, final String... expectedUserNames) {
        assertThat(actualResult)
                .extracting(RbacUserEntity::getName)
                .filteredOn(n -> !n.startsWith("test-user"))
                .contains(expectedUserNames);
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
