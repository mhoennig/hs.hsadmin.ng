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
                "rbactest.customer#xxx:ADMIN", "rbactest.customer#xxx:OWNER", "rbactest.customer#xxx:TENANT",
                    "rbactest.package#xxx00:ADMIN", "rbactest.package#xxx00:OWNER", "rbactest.package#xxx00:TENANT",
                    "rbactest.package#xxx01:ADMIN", "rbactest.package#xxx01:OWNER", "rbactest.package#xxx01:TENANT",
                    "rbactest.package#xxx02:ADMIN", "rbactest.package#xxx02:OWNER", "rbactest.package#xxx02:TENANT",
                "rbactest.customer#yyy:ADMIN", "rbactest.customer#yyy:OWNER", "rbactest.customer#yyy:TENANT",
                    "rbactest.package#yyy00:ADMIN", "rbactest.package#yyy00:OWNER", "rbactest.package#yyy00:TENANT",
                    "rbactest.package#yyy01:ADMIN", "rbactest.package#yyy01:OWNER", "rbactest.package#yyy01:TENANT",
                    "rbactest.package#yyy02:ADMIN", "rbactest.package#yyy02:OWNER", "rbactest.package#yyy02:TENANT",
                "rbactest.customer#zzz:ADMIN", "rbactest.customer#zzz:OWNER", "rbactest.customer#zzz:TENANT",
                    "rbactest.package#zzz00:ADMIN", "rbactest.package#zzz00:OWNER", "rbactest.package#zzz00:TENANT",
                    "rbactest.package#zzz01:ADMIN", "rbactest.package#zzz01:OWNER", "rbactest.package#zzz01:TENANT",
                    "rbactest.package#zzz02:ADMIN", "rbactest.package#zzz02:OWNER", "rbactest.package#zzz02:TENANT"
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
                "rbactest.customer#xxx:ADMIN",
                "rbactest.customer#xxx:TENANT",
                "rbactest.package#xxx00:ADMIN",
                "rbactest.package#xxx00:OWNER",
                "rbactest.package#xxx00:TENANT",
                "rbactest.package#xxx01:ADMIN",
                "rbactest.package#xxx01:OWNER",
                "rbactest.package#xxx01:TENANT",
                // ...
                "rbactest.domain#xxx00-aaaa:ADMIN",
                "rbactest.domain#xxx00-aaaa:OWNER",
                // ..
                "rbactest.domain#xxx01-aaab:ADMIN",
                "rbactest.domain#xxx01-aaab:OWNER"
                // @formatter:on
            );
            noneOfTheseRbacRolesIsReturned(
                    result,
                    // @formatter:off
                "rbac.global#global:ADMIN",
                "rbactest.customer#xxx:OWNER",
                "rbactest.package#yyy00:ADMIN",
                "rbactest.package#yyy00:OWNER",
                "rbactest.package#yyy00:TENANT"
                // @formatter:on
            );
        }

        @Test
        public void customerAdmin_withAssumedOwnedPackageAdminRole_canViewOnlyItsOwnRbacRole() {
            context.define("customer-admin@xxx.example.com", "rbactest.package#xxx00:ADMIN");

            final var result = rbacRoleRepository.findAll();

            exactlyTheseRbacRolesAreReturned(
                    result,
                    "rbactest.customer#xxx:TENANT",
                    "rbactest.package#xxx00:ADMIN",
                    "rbactest.package#xxx00:TENANT",
                    "rbactest.domain#xxx00-aaaa:ADMIN",
                    "rbactest.domain#xxx00-aaaa:OWNER",
                    "rbactest.domain#xxx00-aaab:ADMIN",
                    "rbactest.domain#xxx00-aaab:OWNER");
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

            final var result = rbacRoleRepository.findByRoleName("rbactest.customer#xxx:ADMIN");

            assertThat(result).isNotNull();
            assertThat(result.getObjectTable()).isEqualTo("rbactest.customer");
            assertThat(result.getObjectIdName()).isEqualTo("xxx");
            assertThat(result.getRoleType()).isEqualTo(RbacRoleType.ADMIN);
        }

        @Test
        void customerAdmin_withoutAssumedRole_canNotFindAlienRolesByName() {
            context.define("customer-admin@xxx.example.com");

            final var result = rbacRoleRepository.findByRoleName("rbactest.customer#bbb:ADMIN");

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
