package net.hostsharing.hsadminng.rbac.role;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.mapper.Array;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaSystemException;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
class RbacRoleRepositoryIntegrationTest {

    @Autowired
    Context context;

    @Autowired
    RbacRoleRepository rbacRoleRepository;

    @Autowired
    EntityManager em;

    @MockBean
    HttpServletRequest request;

    @Nested
    class FindAllRbacRoles {

        private static final String[] ALL_TEST_DATA_ROLES = Array.of(
            // @formatter:off
            "rbac.global#global:ADMIN",
                "test_customer#xxx:ADMIN", "test_customer#xxx:OWNER", "test_customer#xxx:TENANT",
                    "test_package#xxx00:ADMIN", "test_package#xxx00:OWNER", "test_package#xxx00:TENANT",
                    "test_package#xxx01:ADMIN", "test_package#xxx01:OWNER", "test_package#xxx01:TENANT",
                    "test_package#xxx02:ADMIN", "test_package#xxx02:OWNER", "test_package#xxx02:TENANT",
                "test_customer#yyy:ADMIN", "test_customer#yyy:OWNER", "test_customer#yyy:TENANT",
                    "test_package#yyy00:ADMIN", "test_package#yyy00:OWNER", "test_package#yyy00:TENANT",
                    "test_package#yyy01:ADMIN", "test_package#yyy01:OWNER", "test_package#yyy01:TENANT",
                    "test_package#yyy02:ADMIN", "test_package#yyy02:OWNER", "test_package#yyy02:TENANT",
                "test_customer#zzz:ADMIN", "test_customer#zzz:OWNER", "test_customer#zzz:TENANT",
                    "test_package#zzz00:ADMIN", "test_package#zzz00:OWNER", "test_package#zzz00:TENANT",
                    "test_package#zzz01:ADMIN", "test_package#zzz01:OWNER", "test_package#zzz01:TENANT",
                    "test_package#zzz02:ADMIN", "test_package#zzz02:OWNER", "test_package#zzz02:TENANT"
            // @formatter:on
        );

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllRbacRoles() {
            // given
            context.define("superuser-alex@hostsharing.net");

            // when
            final var result = rbacRoleRepository.findAll();

            // then
            allTheseRbacRolesAreReturned(result, ALL_TEST_DATA_ROLES);
        }

        @Test
        public void globalAdmin_withAssumedglobalAdminRole_canViewAllRbacRoles() {
            given:
            context.define("superuser-alex@hostsharing.net", "rbac.global#global:ADMIN");

            // when
            final var result = rbacRoleRepository.findAll();

            then:
            allTheseRbacRolesAreReturned(result, ALL_TEST_DATA_ROLES);
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewOnlyItsOwnRbacRole() {
            // given:
            context.define("customer-admin@xxx.example.com");

            // when:
            final var result = rbacRoleRepository.findAll();

            // then:
            allTheseRbacRolesAreReturned(
                    result,
                    // @formatter:off
                "test_customer#xxx:ADMIN",
                "test_customer#xxx:TENANT",
                "test_package#xxx00:ADMIN",
                "test_package#xxx00:OWNER",
                "test_package#xxx00:TENANT",
                "test_package#xxx01:ADMIN",
                "test_package#xxx01:OWNER",
                "test_package#xxx01:TENANT",
                // ...
                "test_domain#xxx00-aaaa:ADMIN",
                "test_domain#xxx00-aaaa:OWNER",
                // ..
                "test_domain#xxx01-aaab:ADMIN",
                "test_domain#xxx01-aaab:OWNER"
                // @formatter:on
            );
            noneOfTheseRbacRolesIsReturned(
                    result,
                    // @formatter:off
                "rbac.global#global:ADMIN",
                "test_customer#xxx:OWNER",
                "test_package#yyy00:ADMIN",
                "test_package#yyy00:OWNER",
                "test_package#yyy00:TENANT"
                // @formatter:on
            );
        }

        @Test
        public void customerAdmin_withAssumedOwnedPackageAdminRole_canViewOnlyItsOwnRbacRole() {
            context.define("customer-admin@xxx.example.com", "test_package#xxx00:ADMIN");

            final var result = rbacRoleRepository.findAll();

            exactlyTheseRbacRolesAreReturned(
                    result,
                    "test_customer#xxx:TENANT",
                    "test_package#xxx00:ADMIN",
                    "test_package#xxx00:TENANT",
                    "test_domain#xxx00-aaaa:ADMIN",
                    "test_domain#xxx00-aaaa:OWNER",
                    "test_domain#xxx00-aaab:ADMIN",
                    "test_domain#xxx00-aaab:OWNER");
        }

        @Test
        void anonymousUser_withoutAssumedRole_cannotViewAnyRbacRoles() {
            context.define(null);

            final var result = attempt(
                    em,
                    () -> rbacRoleRepository.findAll());

            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[401] currentSubjectOrAssumedRolesUuids cannot be determined, please call `base.defineContext(...)` with a valid subject");
        }
    }

    @Nested
    class FindByName {

        @Test
        void customerAdmin_withoutAssumedRole_canFindItsOwnRolesByName() {
            context.define("customer-admin@xxx.example.com");

            final var result = rbacRoleRepository.findByRoleName("test_customer#xxx:ADMIN");

            assertThat(result).isNotNull();
            assertThat(result.getObjectTable()).isEqualTo("test_customer");
            assertThat(result.getObjectIdName()).isEqualTo("xxx");
            assertThat(result.getRoleType()).isEqualTo(RbacRoleType.ADMIN);
        }

        @Test
        void customerAdmin_withoutAssumedRole_canNotFindAlienRolesByName() {
            context.define("customer-admin@xxx.example.com");

            final var result = rbacRoleRepository.findByRoleName("test_customer#bbb:ADMIN");

            assertThat(result).isNull();
        }
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
