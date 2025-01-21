package net.hostsharing.hsadminng.rbac.subject;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.mapper.Array;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

import static java.util.Comparator.comparing;
import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
@Tag("generalIntegrationTest")
class RbacSubjectRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    RbacSubjectRepository rbacSubjectRepository;

    @Autowired
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @MockitoBean
    HttpServletRequest request;

    @Nested
    class PostNewSubject {

        @Test
        @Transactional(propagation = Propagation.NEVER)
        void anyoneCanCreateTheirOwnUser() {

            // given:
            final var givenUuid = UUID.randomUUID();
            final var newUserName = "test-user-" + System.currentTimeMillis() + "@example.com";

            // when:
            final var result = jpaAttempt.transacted(() -> {
                context(null);
                return rbacSubjectRepository.create(new RbacSubjectEntity(givenUuid, newUserName));
            });

            // then:
            assertThat(result.wasSuccessful()).isTrue();
            assertThat(result.returnedValue()).isNotNull()
                    .extracting(RbacSubjectEntity::getUuid).isEqualTo(givenUuid);
            assertThat(rbacSubjectRepository.findByName(result.returnedValue().getName())).isNotNull();
        }
    }

    @Nested
    class DeleteSubject {

        @Test
        @Transactional(propagation = Propagation.NEVER)
        public void anyoneCanDeleteTheirOwnUser() {
            // given
            final RbacSubjectEntity givenUser = givenANewSubject();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context(givenUser.getName());
                rbacSubjectRepository.deleteByUuid(givenUser.getUuid());
            });

            // then the user is deleted
            result.assertSuccessful();
            assertThat(rbacSubjectRepository.findByName(givenUser.getName())).isNull();
        }
    }

    @Nested
    class FindByOptionalNameLike {

        private static final String[] ALL_TEST_DATA_USERS = Array.of(
                // @formatter:off
            "superuser-alex@hostsharing.net", "superuser-fran@hostsharing.net",
            "customer-admin@xxx.example.com",
            "pac-admin-xxx00@xxx.example.com", "pac-admin-xxx01@xxx.example.com", "pac-admin-xxx02@xxx.example.com",
            "customer-admin@yyy.example.com",
            "pac-admin-yyy00@yyy.example.com", "pac-admin-yyy01@yyy.example.com", "pac-admin-yyy02@yyy.example.com",
            "customer-admin@zzz.example.com",
            "pac-admin-zzz00@zzz.example.com", "pac-admin-zzz01@zzz.example.com", "pac-admin-zzz02@zzz.example.com"
            // @formatter:on
        );

        @Test
        public void globalAdmin_withoutAssumedRole_canViewAllRbacSubjects() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = rbacSubjectRepository.findByOptionalNameLike(null);

            // then
            allTheseRbacSubjectsAreReturned(result, ALL_TEST_DATA_USERS);
        }

        @Test
        public void globalAdmin_withAssumedglobalAdminRole_canViewAllRbacSubjects() {
            given:
            context("superuser-alex@hostsharing.net", "rbac.global#global:ADMIN");

            // when
            final var result = rbacSubjectRepository.findByOptionalNameLike(null);

            then:
            allTheseRbacSubjectsAreReturned(result, ALL_TEST_DATA_USERS);
        }

        @Test
        public void globalAdmin_withAssumedCustomerAdminRole_canViewOnlyUsersHavingRolesInThatCustomersRealm() {
            given:
            context("superuser-alex@hostsharing.net", "rbactest.customer#xxx:ADMIN");

            // when
            final var result = rbacSubjectRepository.findByOptionalNameLike(null);

            then:
            exactlyTheseRbacSubjectsAreReturned(
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
            final var result = rbacSubjectRepository.findByOptionalNameLike(null);

            // then:
            exactlyTheseRbacSubjectsAreReturned(
                    result,
                    "customer-admin@xxx.example.com",
                    "pac-admin-xxx00@xxx.example.com", "pac-admin-xxx01@xxx.example.com", "pac-admin-xxx02@xxx.example.com"
            );
        }

        @Test
        public void customerAdmin_withAssumedOwnedPackageAdminRole_canViewOnlyUsersHavingRolesInThatPackage() {
            context("customer-admin@xxx.example.com", "rbactest.package#xxx00:ADMIN");

            final var result = rbacSubjectRepository.findByOptionalNameLike(null);

            exactlyTheseRbacSubjectsAreReturned(result, "pac-admin-xxx00@xxx.example.com");
        }

        @Test
        public void packageAdmin_withoutAssumedRole_canViewOnlyUsersHavingRolesInThatPackage() {
            context("pac-admin-xxx00@xxx.example.com");

            final var result = rbacSubjectRepository.findByOptionalNameLike(null);

            exactlyTheseRbacSubjectsAreReturned(result, "pac-admin-xxx00@xxx.example.com");
        }

    }

    @Nested
    class GetListOfSubjectPermissions {

        private static final String[] ALL_USER_PERMISSIONS = Array.of(
                // @formatter:off
                "rbactest.customer#xxx:ADMIN -> rbactest.customer#xxx: SELECT",
                "rbactest.customer#xxx:OWNER -> rbactest.customer#xxx: DELETE",
                "rbactest.customer#xxx:TENANT -> rbactest.customer#xxx: SELECT",
                "rbactest.customer#xxx:ADMIN -> rbactest.customer#xxx: INSERT:rbactest.package",
                "rbactest.package#xxx00:ADMIN -> rbactest.package#xxx00: INSERT:rbactest.domain",
                "rbactest.package#xxx00:ADMIN -> rbactest.package#xxx00: INSERT:rbactest.domain",
                "rbactest.package#xxx00:TENANT -> rbactest.package#xxx00: SELECT",
                "rbactest.package#xxx01:ADMIN -> rbactest.package#xxx01: INSERT:rbactest.domain",
                "rbactest.package#xxx01:ADMIN -> rbactest.package#xxx01: INSERT:rbactest.domain",
                "rbactest.package#xxx01:TENANT -> rbactest.package#xxx01: SELECT",
                "rbactest.package#xxx02:ADMIN -> rbactest.package#xxx02: INSERT:rbactest.domain",
                "rbactest.package#xxx02:ADMIN -> rbactest.package#xxx02: INSERT:rbactest.domain",
                "rbactest.package#xxx02:TENANT -> rbactest.package#xxx02: SELECT",

                "rbactest.customer#yyy:ADMIN -> rbactest.customer#yyy: SELECT",
                "rbactest.customer#yyy:OWNER -> rbactest.customer#yyy: DELETE",
                "rbactest.customer#yyy:TENANT -> rbactest.customer#yyy: SELECT",
                "rbactest.customer#yyy:ADMIN -> rbactest.customer#yyy: INSERT:rbactest.package",
                "rbactest.package#yyy00:ADMIN -> rbactest.package#yyy00: INSERT:rbactest.domain",
                "rbactest.package#yyy00:ADMIN -> rbactest.package#yyy00: INSERT:rbactest.domain",
                "rbactest.package#yyy00:TENANT -> rbactest.package#yyy00: SELECT",
                "rbactest.package#yyy01:ADMIN -> rbactest.package#yyy01: INSERT:rbactest.domain",
                "rbactest.package#yyy01:ADMIN -> rbactest.package#yyy01: INSERT:rbactest.domain",
                "rbactest.package#yyy01:TENANT -> rbactest.package#yyy01: SELECT",
                "rbactest.package#yyy02:ADMIN -> rbactest.package#yyy02: INSERT:rbactest.domain",
                "rbactest.package#yyy02:ADMIN -> rbactest.package#yyy02: INSERT:rbactest.domain",
                "rbactest.package#yyy02:TENANT -> rbactest.package#yyy02: SELECT",

                "rbactest.customer#zzz:ADMIN -> rbactest.customer#zzz: SELECT",
                "rbactest.customer#zzz:OWNER -> rbactest.customer#zzz: DELETE",
                "rbactest.customer#zzz:TENANT -> rbactest.customer#zzz: SELECT",
                "rbactest.customer#zzz:ADMIN -> rbactest.customer#zzz: INSERT:rbactest.package",
                "rbactest.package#zzz00:ADMIN -> rbactest.package#zzz00: INSERT:rbactest.domain",
                "rbactest.package#zzz00:ADMIN -> rbactest.package#zzz00: INSERT:rbactest.domain",
                "rbactest.package#zzz00:TENANT -> rbactest.package#zzz00: SELECT",
                "rbactest.package#zzz01:ADMIN -> rbactest.package#zzz01: INSERT:rbactest.domain",
                "rbactest.package#zzz01:ADMIN -> rbactest.package#zzz01: INSERT:rbactest.domain",
                "rbactest.package#zzz01:TENANT -> rbactest.package#zzz01: SELECT",
                "rbactest.package#zzz02:ADMIN -> rbactest.package#zzz02: INSERT:rbactest.domain",
                "rbactest.package#zzz02:ADMIN -> rbactest.package#zzz02: INSERT:rbactest.domain",
                "rbactest.package#zzz02:TENANT -> rbactest.package#zzz02: SELECT"
                // @formatter:on
        );

        @Test
        public void globalAdmin_withoutAssumedRole_canViewTheirOwnPermissions() {
            // given
            context("superuser-alex@hostsharing.net");

            // when
            final var result = rbacSubjectRepository.findPermissionsOfUserByUuid(subjectUuid("superuser-fran@hostsharing.net"))
                    .stream().filter(p -> p.getObjectTable().contains("rbactest."))
                    .sorted(comparing(RbacSubjectPermission::toString)).toList();

            // then
            allTheseRbacPermissionsAreReturned(result, ALL_USER_PERMISSIONS);
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewTheirOwnPermissions() {
            // given
            context("customer-admin@xxx.example.com");

            // when
            final var result = rbacSubjectRepository.findPermissionsOfUserByUuid(subjectUuid("customer-admin@xxx.example.com"));

            // then
            allTheseRbacPermissionsAreReturned(
                    result,
                    // @formatter:off
                "rbactest.customer#xxx:ADMIN -> rbactest.customer#xxx: INSERT:rbactest.package",
                "rbactest.customer#xxx:ADMIN -> rbactest.customer#xxx: SELECT",
                "rbactest.customer#xxx:TENANT -> rbactest.customer#xxx: SELECT",

                "rbactest.package#xxx00:ADMIN -> rbactest.package#xxx00: INSERT:rbactest.domain",
                "rbactest.package#xxx00:ADMIN -> rbactest.package#xxx00: INSERT:rbactest.domain",
                "rbactest.package#xxx00:TENANT -> rbactest.package#xxx00: SELECT",
                "rbactest.domain#xxx00-aaaa:OWNER -> rbactest.domain#xxx00-aaaa: DELETE",

                "rbactest.package#xxx01:ADMIN -> rbactest.package#xxx01: INSERT:rbactest.domain",
                "rbactest.package#xxx01:ADMIN -> rbactest.package#xxx01: INSERT:rbactest.domain",
                "rbactest.package#xxx01:TENANT -> rbactest.package#xxx01: SELECT",
                "rbactest.domain#xxx01-aaaa:OWNER -> rbactest.domain#xxx01-aaaa: DELETE",

                "rbactest.package#xxx02:ADMIN -> rbactest.package#xxx02: INSERT:rbactest.domain",
                "rbactest.package#xxx02:ADMIN -> rbactest.package#xxx02: INSERT:rbactest.domain",
                "rbactest.package#xxx02:TENANT -> rbactest.package#xxx02: SELECT",
                "rbactest.domain#xxx02-aaaa:OWNER -> rbactest.domain#xxx02-aaaa: DELETE"
                // @formatter:on
            );
            noneOfTheseRbacPermissionsAreReturned(
                    result,
                    // @formatter:off
                "rbactest.customer#yyy:ADMIN -> rbactest.customer#yyy: INSERT:rbactest.package",
                "rbactest.customer#yyy:ADMIN -> rbactest.customer#yyy: SELECT",
                "rbactest.customer#yyy:TENANT -> rbactest.customer#yyy: SELECT"
                // @formatter:on
            );
        }

        @Test
        public void customerAdmin_withoutAssumedRole_isNotAllowedToViewGlobalAdminsPermissions() {
            // given
            context("customer-admin@xxx.example.com");
            final UUID subjectUuid = subjectUuid("superuser-alex@hostsharing.net");

            // when
            final var result = attempt(em, () ->
                    rbacSubjectRepository.findPermissionsOfUserByUuid(subjectUuid)
            );

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] permissions of user \"" + subjectUuid
                            + "\" are not accessible to user \"customer-admin@xxx.example.com\"");
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewAllPermissionsWithinThePacketsRealm() {
            // given
            context("customer-admin@xxx.example.com");

            // when
            final var result = rbacSubjectRepository.findPermissionsOfUserByUuid(subjectUuid("pac-admin-xxx00@xxx.example.com"));

            // then
            allTheseRbacPermissionsAreReturned(
                    result,
                    // @formatter:off
                "rbactest.customer#xxx:TENANT -> rbactest.customer#xxx: SELECT",
                // "rbactest.customer#xxx:ADMIN -> rbactest.customer#xxx: view" - Not permissions through the customer admin!
                "rbactest.package#xxx00:ADMIN -> rbactest.package#xxx00: INSERT:rbactest.domain",
                "rbactest.package#xxx00:ADMIN -> rbactest.package#xxx00: INSERT:rbactest.domain",
                "rbactest.package#xxx00:TENANT -> rbactest.package#xxx00: SELECT",
                "rbactest.domain#xxx00-aaaa:OWNER -> rbactest.domain#xxx00-aaaa: DELETE",
                "rbactest.domain#xxx00-aaab:OWNER -> rbactest.domain#xxx00-aaab: DELETE"
                // @formatter:on
            );
            noneOfTheseRbacPermissionsAreReturned(
                    result,
                    // @formatter:off
                "rbactest.customer#yyy:ADMIN -> rbactest.customer#yyy: INSERT:rbactest.package",
                "rbactest.customer#yyy:ADMIN -> rbactest.customer#yyy: SELECT",
                "rbactest.customer#yyy:TENANT -> rbactest.customer#yyy: SELECT",
                "rbactest.package#yyy00:ADMIN -> rbactest.package#yyy00: INSERT:rbactest.domain",
                "rbactest.package#yyy00:ADMIN -> rbactest.package#yyy00: INSERT:rbactest.domain",
                "rbactest.package#yyy00:TENANT -> rbactest.package#yyy00: SELECT",
                "rbactest.domain#yyy00-aaaa:OWNER -> rbactest.domain#yyy00-aaaa: DELETE",
                "rbactest.domain#yyy00-aaab:OWNER -> rbactest.domain#yyy00-aaab: DELETE"
                // @formatter:on
            );
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canNotViewPermissionsOfUnrelatedUsers() {
            // given
            context("customer-admin@xxx.example.com");

            // when
            final var result = rbacSubjectRepository.findPermissionsOfUserByUuid(subjectUuid("pac-admin-yyy00@yyy.example.com"));

            // then
            noRbacPermissionsAreReturned(result);
        }

        @Test
        public void packetAdmin_withoutAssumedRole_canViewAllPermissionsWithinThePacketsRealm() {
            // given
            context("pac-admin-xxx00@xxx.example.com");

            // when
            final var result = rbacSubjectRepository.findPermissionsOfUserByUuid(subjectUuid("pac-admin-xxx00@xxx.example.com"));

            // then
            allTheseRbacPermissionsAreReturned(
                    result,
                    // @formatter:off
                "rbactest.customer#xxx:TENANT -> rbactest.customer#xxx: SELECT",
                // "rbactest.customer#xxx:ADMIN -> rbactest.customer#xxx: view" - Not permissions through the customer admin!
                "rbactest.package#xxx00:ADMIN -> rbactest.package#xxx00: INSERT:rbactest.domain",
                "rbactest.package#xxx00:TENANT -> rbactest.package#xxx00: SELECT"
                // @formatter:on
            );
            noneOfTheseRbacPermissionsAreReturned(
                    result,
                    // @formatter:off
                // no customer admin permissions
                "rbactest.customer#xxx:ADMIN -> rbactest.customer#xxx: add-package",
                // no permissions on other customer's objects
                "rbactest.customer#yyy:ADMIN -> rbactest.customer#yyy: add-package",
                "rbactest.customer#yyy:ADMIN -> rbactest.customer#yyy: SELECT",
                "rbactest.customer#yyy:TENANT -> rbactest.customer#yyy: SELECT",
                "rbactest.package#yyy00:ADMIN -> rbactest.package#yyy00: INSERT:rbactest.domain",
                "rbactest.package#yyy00:ADMIN -> rbactest.package#yyy00: INSERT:rbactest.domain",
                "rbactest.package#yyy00:TENANT -> rbactest.package#yyy00: SELECT",
                "rbactest.domain#yyy00-aaaa:OWNER -> rbactest.domain#yyy00-aaaa: DELETE",
                "rbactest.domain#yyy00-xxxb:OWNER -> rbactest.domain#yyy00-xxxb: DELETE"
                // @formatter:on
            );
        }
    }

    UUID subjectUuid(final String userName) {
        return rbacSubjectRepository.findByName(userName).getUuid();
    }

    RbacSubjectEntity givenANewSubject() {
        final var givenUserName = "test-user-" + System.currentTimeMillis() + "@example.com";
        final var givenUser = jpaAttempt.transacted(() -> {
            context(null);
            return rbacSubjectRepository.create(new RbacSubjectEntity(UUID.randomUUID(), givenUserName));
        }).assumeSuccessful().returnedValue();
        assertThat(rbacSubjectRepository.findByName(givenUser.getName())).isNotNull();
        return givenUser;
    }

    void exactlyTheseRbacSubjectsAreReturned(final List<RbacSubjectEntity> actualResult, final String... expectedUserNames) {
        assertThat(actualResult)
                .extracting(RbacSubjectEntity::getName)
                .filteredOn(n -> !n.startsWith("test-user"))
                .containsExactlyInAnyOrder(expectedUserNames);
    }

    void allTheseRbacSubjectsAreReturned(final List<RbacSubjectEntity> actualResult, final String... expectedUserNames) {
        assertThat(actualResult)
                .extracting(RbacSubjectEntity::getName)
                .filteredOn(n -> !n.startsWith("test-user"))
                .contains(expectedUserNames);
    }

    void noRbacPermissionsAreReturned(
            final List<RbacSubjectPermission> actualResult) {
        assertThat(actualResult)
                .extracting(p -> p.getRoleName() + " -> " + p.getObjectTable() + "#" + p.getObjectIdName() + ": " + p.getOp())
                .containsExactlyInAnyOrder();
    }

    void allTheseRbacPermissionsAreReturned(
            final List<RbacSubjectPermission> actualResult,
            final String... expectedRoleNames) {
        assertThat(actualResult)
                .extracting(p -> p.getRoleName() + " -> " + p.getObjectTable() + "#" + p.getObjectIdName() + ": " + p.getOp()
                    + (p.getOpTableName() != null ? (":"+p.getOpTableName()) : "" ))
                .contains(expectedRoleNames);
    }

    void noneOfTheseRbacPermissionsAreReturned(
            final List<RbacSubjectPermission> actualResult,
            final String... unexpectedRoleNames) {
        assertThat(actualResult)
                .extracting(p -> p.getRoleName() + " -> " + p.getObjectTable() + "#" + p.getObjectIdName() + ": " + p.getOp())
                .doesNotContain(unexpectedRoleNames);
    }

}
