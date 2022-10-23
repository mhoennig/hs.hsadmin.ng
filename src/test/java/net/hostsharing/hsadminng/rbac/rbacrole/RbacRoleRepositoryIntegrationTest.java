package net.hostsharing.hsadminng.rbac.rbacrole;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.test.Array;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.annotation.DirtiesContext;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static net.hostsharing.test.JpaAttempt.attempt;
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
            "global#global.admin",
                "test_customer#xxx.admin", "test_customer#xxx.owner", "test_customer#xxx.tenant",
                    "test_package#xxx00.admin", "test_package#xxx00.owner", "test_package#xxx00.tenant",
                    "test_package#xxx01.admin", "test_package#xxx01.owner", "test_package#xxx01.tenant",
                    "test_package#xxx02.admin", "test_package#xxx02.owner", "test_package#xxx02.tenant",
                "test_customer#yyy.admin", "test_customer#yyy.owner", "test_customer#yyy.tenant",
                    "test_package#yyy00.admin", "test_package#yyy00.owner", "test_package#yyy00.tenant",
                    "test_package#yyy01.admin", "test_package#yyy01.owner", "test_package#yyy01.tenant",
                    "test_package#yyy02.admin", "test_package#yyy02.owner", "test_package#yyy02.tenant",
                "test_customer#zzz.admin", "test_customer#zzz.owner", "test_customer#zzz.tenant",
                    "test_package#zzz00.admin", "test_package#zzz00.owner", "test_package#zzz00.tenant",
                    "test_package#zzz01.admin", "test_package#zzz01.owner", "test_package#zzz01.tenant",
                    "test_package#zzz02.admin", "test_package#zzz02.owner", "test_package#zzz02.tenant"
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
            context.define("superuser-alex@hostsharing.net", "global#global.admin");

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
                "test_customer#xxx.admin",
                "test_customer#xxx.tenant",
                "test_package#xxx00.admin",
                "test_package#xxx00.owner",
                "test_package#xxx00.tenant",
                "test_package#xxx01.admin",
                "test_package#xxx01.owner",
                "test_package#xxx01.tenant",
                // ...
                "test_domain#xxx00-aaaa.admin",
                "test_domain#xxx00-aaaa.owner",
                // ..
                "test_domain#xxx01-aaab.admin",
                "test_domain#xxx01-aaab.owner"
                // @formatter:on
            );
            noneOfTheseRbacRolesIsReturned(
                    result,
                    // @formatter:off
                "global#global.admin",
                "test_customer#xxx.owner",
                "test_package#yyy00.admin",
                "test_package#yyy00.owner",
                "test_package#yyy00.tenant"
                // @formatter:on
            );
        }

        @Test
        public void customerAdmin_withAssumedOwnedPackageAdminRole_canViewOnlyItsOwnRbacRole() {
            context.define("customer-admin@xxx.example.com", "test_package#xxx00.admin");

            final var result = rbacRoleRepository.findAll();

            exactlyTheseRbacRolesAreReturned(
                    result,
                    "test_customer#xxx.tenant",
                    "test_package#xxx00.admin",
                    "test_package#xxx00.tenant",
                    "test_domain#xxx00-aaaa.admin",
                    "test_domain#xxx00-aaaa.owner",
                    "test_domain#xxx00-aaab.admin",
                    "test_domain#xxx00-aaab.owner");
        }

        @Test
        void anonymousUser_withoutAssumedRole_cannotViewAnyRbacRoles() {
            context.define(null);

            final var result = attempt(
                    em,
                    () -> rbacRoleRepository.findAll());

            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[401] currentSubjectsUuids cannot be determined, please call `defineContext(...)` with a valid user");
        }
    }

    @Nested
    class FindByName {

        @Test
        void customerAdmin_withoutAssumedRole_canFindItsOwnRolesByName() {
            context.define("customer-admin@xxx.example.com");

            final var result = rbacRoleRepository.findByRoleName("test_customer#xxx.admin");

            assertThat(result).isNotNull();
            assertThat(result.getObjectTable()).isEqualTo("test_customer");
            assertThat(result.getObjectIdName()).isEqualTo("xxx");
            assertThat(result.getRoleType()).isEqualTo(RbacRoleType.admin);
        }

        @Test
        void customerAdmin_withoutAssumedRole_canNotFindAlienRolesByName() {
            context.define("customer-admin@xxx.example.com");

            final var result = rbacRoleRepository.findByRoleName("test_customer#bbb.admin");

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
