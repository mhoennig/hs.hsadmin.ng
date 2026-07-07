package net.hostsharing.hsadminng.rbac.subject;

import net.hostsharing.hsadminng.rbac.context.Context;
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
import static net.hostsharing.hsadminng.rbac.subject.SubjectType.GROUP;
import static net.hostsharing.hsadminng.rbac.subject.SubjectType.USER;
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
        void subjectCreatedWithoutExplicitType_getsTypeUser() {

            // given:
            final var givenUuid = UUID.randomUUID();
            final var newUserName = "tst-user_" + System.currentTimeMillis();

            // when:
            final var result = jpaAttempt.transacted(() -> {
                context(null);
                return rbacSubjectRepository.create(RbacSubjectEntity.builder().uuid(givenUuid).name(newUserName).build());
            });

            // then:
            assertThat(result.wasSuccessful()).isTrue();
            assertThat(rbacSubjectRepository.findByName(newUserName).getType()).isEqualTo(USER);
        }

        @Test
        @Transactional(propagation = Propagation.NEVER)
        void subjectCreatedWithExplicitUserType_getsTypeUser() {

            // given:
            final var givenUuid = UUID.randomUUID();
            final var newUserName = "tst-user_" + System.currentTimeMillis();

            // when:
            final var result = jpaAttempt.transacted(() -> {
                context(null);
                return rbacSubjectRepository.create(RbacSubjectEntity.builder().uuid(givenUuid).name(newUserName).type(USER).build());
            });

            // then:
            assertThat(result.wasSuccessful()).isTrue();
            assertThat(rbacSubjectRepository.findByName(newUserName).getType()).isEqualTo(USER);
        }

        @Test
        @Transactional(propagation = Propagation.NEVER)
        void subjectCreatedWithExplicitGroupType_getsTypeGroup() {

            // given:
            final var givenUuid = UUID.randomUUID();
            final var newGroupName = "/test-group-" + System.currentTimeMillis();

            // when:
            final var result = jpaAttempt.transacted(() -> {
                context(null);
                return rbacSubjectRepository.create(RbacSubjectEntity.builder().uuid(givenUuid).name(newGroupName).type(GROUP).build());
            });

            // then:
            assertThat(result.wasSuccessful()).isTrue();
            assertThat(rbacSubjectRepository.findByName(newGroupName).getType()).isEqualTo(GROUP);
        }

        @Test
        @Transactional(propagation = Propagation.NEVER)
        void userSubjectNameValidationFunctionMatchesConstraint() {

            // given:
            final var givenUuid = UUID.randomUUID();

            // when:
            final var result = jpaAttempt.transacted(() -> {
                context(null);
                return rbacSubjectRepository.create(RbacSubjectEntity.builder()
                        .uuid(givenUuid)
                        .name("invalid-user@example.com")
                        .type(USER)
                        .build());
            });

            // then:
            result.assertExceptionWithRootCauseMessage(
                    org.postgresql.util.PSQLException.class,
                    """
                    violates check constraint "check_valid_user_subject_name"
                    """.stripTrailing());
        }

        @Test
        @Transactional(propagation = Propagation.NEVER)
        void groupSubjectNameValidationFunctionMatchesConstraint() {

            // given:
            final var givenUuid = UUID.randomUUID();

            // when:
            final var result = jpaAttempt.transacted(() -> {
                context(null);
                return rbacSubjectRepository.create(RbacSubjectEntity.builder()
                        .uuid(givenUuid)
                        .name("invalid-group")
                        .type(GROUP)
                        .build());
            });

            // then:
            result.assertExceptionWithRootCauseMessage(
                    org.postgresql.util.PSQLException.class,
                    """
                    violates check constraint "check_valid_group_subject_name"
                    """.stripTrailing());
        }
    }

    @Nested
    class FindSubjectsByType {

        @Test
        public void testDataGroupSubjects_haveTypeGroup() {
            // when:
            final var adminGroup = rbacSubjectRepository.findByName("/hsh-Hostmasters");
            final var customerGroup = rbacSubjectRepository.findByName("/xyz-Team");

            // then:
            assertThat(adminGroup.getType()).isEqualTo(GROUP);
            assertThat(customerGroup.getType()).isEqualTo(GROUP);
        }

        @Test
        public void testDataUserSubjects_haveTypeUser() {
            // when:
            final var alex = rbacSubjectRepository.findByName("hsh-alex_superuser");

            // then:
            assertThat(alex.getType()).isEqualTo(USER);
        }

    }

    @Nested
    class DeactivateSubject {

        @Test
        @Transactional(propagation = Propagation.NEVER)
        public void deactivateByUuidSoftDeletesSubjectRetainingTheRow() {
            // given
            final RbacSubjectEntity givenSubject = givenANewSubject();

            // when
            final var result = jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser");
                rbacSubjectRepository.deactivateByUuid(givenSubject.getUuid());
            });

            // then the subject is deactivated (soft-deleted): the row is retained but no longer visible
            result.assertSuccessful();
            assertThat(rbacSubjectRepository.findByName(givenSubject.getName())).isNotNull();
            final var stillVisible = jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser");
                return rbacSubjectRepository.findByUuid(givenSubject.getUuid());
            }).returnedValue();
            assertThat(stillVisible).isNull();
        }

        @Test
        @Transactional(propagation = Propagation.NEVER)
        public void deactivateByUuidIsIdempotent() {
            // given an already deactivated subject
            final RbacSubjectEntity givenSubject = givenANewSubject();
            jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser");
                rbacSubjectRepository.deactivateByUuid(givenSubject.getUuid());
            }).assertSuccessful();

            // when it is deactivated again
            final var result = jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser");
                rbacSubjectRepository.deactivateByUuid(givenSubject.getUuid());
            });

            // then this is a no-op and the row is still retained
            result.assertSuccessful();
            assertThat(rbacSubjectRepository.findByName(givenSubject.getName())).isNotNull();
        }
    }

    @Nested
    class DeleteSubject {

        @Test
        @Transactional(propagation = Propagation.NEVER)
        public void deleteByUuidPhysicallyRemovesSubjectReferenceAndGrants() {
            // given a freshly created subject which has been granted a role, so it holds a grant
            final var givenSubject = givenANewSubject();
            grantAnArbitraryRoleTo(givenSubject.getUuid());
            assertThat(countGrantsReferencing(givenSubject.getUuid()))
                    .as("precondition: the subject must hold at least one grant")
                    .isGreaterThan(0);

            // when the subject is physically deleted through the repository (deleting its rbac.reference
            // row cascades to the subject and, via the BEFORE DELETE trigger, to all of its grants)
            final var deleteAttempt = jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser");
                rbacSubjectRepository.deleteByUuid(givenSubject.getUuid());
            });

            // then the subject row, its rbac.reference row and all its grants are gone
            deleteAttempt.assertSuccessful();
            assertThat(rbacSubjectRepository.findByName(givenSubject.getName()))
                    .as("the subject row itself is gone")
                    .isNull();
            assertThat(referenceExists(givenSubject.getUuid()))
                    .as("the rbac.reference row is removed together with the subject")
                    .isFalse();
            assertThat(countGrantsReferencing(givenSubject.getUuid()))
                    .as("all grants referencing the deleted subject are removed")
                    .isZero();
        }

        @Test
        @Transactional(propagation = Propagation.NEVER)
        public void deleteByUuidByNonGlobalAdminIsRejectedByTheDatabase() {
            // given
            final var givenSubject = givenANewSubject();

            // when a non-global-admin calls the repository purge directly, bypassing the controller's gate
            final var deleteAttempt = jpaAttempt.transacted(() -> {
                context("tst-customer_admin_xxx");
                rbacSubjectRepository.deleteByUuid(givenSubject.getUuid());
            });

            // then the database itself rejects the purge ...
            deleteAttempt.assertExceptionWithRootCauseMessage(
                    org.postgresql.util.PSQLException.class,
                    "[403]");
            // ... and the subject still exists
            assertThat(rbacSubjectRepository.findByName(givenSubject.getName())).isNotNull();
        }

        @Test
        @Transactional(propagation = Propagation.NEVER)
        public void directlyDeletingTheSubjectRowAlsoDeletesItsReferenceAndGrants() {
            // given a freshly created subject which has been granted a role, so it holds a grant
            final var givenSubject = givenANewSubject();
            grantAnArbitraryRoleTo(givenSubject.getUuid());
            final var grantsBefore = countGrantsReferencing(givenSubject.getUuid());
            assertThat(grantsBefore)
                    .as("precondition: the subject must hold at least one grant")
                    .isGreaterThan(0);

            // when the subject row itself is deleted (the naive physical delete)
            final var deleteAttempt = jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser");
                em.createNativeQuery("delete from rbac.subject where uuid = :uuid")
                        .setParameter("uuid", givenSubject.getUuid())
                        .executeUpdate();
            });

            // then the delete succeeds; the BEFORE DELETE trigger removes its grants and the AFTER
            // DELETE trigger removes the then-orphaned rbac.reference row (pg_trigger_depth() = 1)
            deleteAttempt.assertSuccessful();
            assertThat(rbacSubjectRepository.findByName(givenSubject.getName()))
                    .as("the subject row itself is gone")
                    .isNull();
            assertThat(referenceExists(givenSubject.getUuid()))
                    .as("the rbac.reference row is removed together with the subject")
                    .isFalse();
            assertThat(countGrantsReferencing(givenSubject.getUuid()))
                    .as("all grants referencing the deleted subject are removed")
                    .isZero();
        }

        private void grantAnArbitraryRoleTo(final UUID subjectUuid) {
            jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser");
                final var roleUuid = (UUID) em.createNativeQuery("""
                                select r.uuid
                                  from rbac.role r
                                  join rbac.object o on o.uuid = r.objectuuid
                                 where o.objecttable = 'rbactest.package' and r.roletype = 'TENANT'
                                 limit 1
                                """, UUID.class)
                        .getSingleResult();
                em.createNativeQuery("call rbac.grantRoleToSubjectUnchecked(:grantedByRole, :grantedRole, :subject)")
                        .setParameter("grantedByRole", roleUuid)
                        .setParameter("grantedRole", roleUuid)
                        .setParameter("subject", subjectUuid)
                        .executeUpdate();
            }).assertSuccessful();
        }

        private long countGrantsReferencing(final UUID subjectUuid) {
            return jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser");
                return ((Number) em.createNativeQuery(
                                "select count(*) from rbac.grant where ascendantuuid = :uuid or descendantuuid = :uuid")
                        .setParameter("uuid", subjectUuid)
                        .getSingleResult()).longValue();
            }).assumeSuccessful().returnedValue();
        }

        private boolean referenceExists(final UUID subjectUuid) {
            return jpaAttempt.transacted(() -> {
                context("hsh-alex_superuser");
                return ((Number) em.createNativeQuery(
                                "select count(*) from rbac.reference where uuid = :uuid")
                        .setParameter("uuid", subjectUuid)
                        .getSingleResult()).longValue() > 0;
            }).assumeSuccessful().returnedValue();
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
            context("hsh-alex_superuser");

            // when
            final var result = rbacSubjectRepository.findPermissionsOfUserByUuid(subjectUuid("hsh-fran_superuser"))
                    .stream().filter(p -> p.getObjectTable().contains("rbactest."))
                    .sorted(comparing(RbacSubjectPermission::toString)).toList();

            // then
            allTheseRbacPermissionsAreReturned(result, ALL_USER_PERMISSIONS);
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewTheirOwnPermissions() {
            // given
            context("tst-customer_admin_xxx");

            // when
            final var result = rbacSubjectRepository.findPermissionsOfUserByUuid(subjectUuid("tst-customer_admin_xxx"));

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
            context("tst-customer_admin_xxx");
            final UUID subjectUuid = subjectUuid("hsh-alex_superuser");

            // when
            final var result = attempt(em, () ->
                    rbacSubjectRepository.findPermissionsOfUserByUuid(subjectUuid)
            );

            // then
            result.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "[403] permissions of user \"" + subjectUuid
                            + "\" are not accessible to user \"tst-customer_admin_xxx\"");
        }

        @Test
        public void customerAdmin_withoutAssumedRole_canViewAllPermissionsWithinThePacketsRealm() {
            // given
            context("tst-customer_admin_xxx");

            // when
            final var result = rbacSubjectRepository.findPermissionsOfUserByUuid(subjectUuid("tst-pac_admin_xxx00"));

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
            context("tst-customer_admin_xxx");

            // when
            final var result = rbacSubjectRepository.findPermissionsOfUserByUuid(subjectUuid("tst-pac_admin_yyy00"));

            // then
            noRbacPermissionsAreReturned(result);
        }

        @Test
        public void packetAdmin_withoutAssumedRole_canViewAllPermissionsWithinThePacketsRealm() {
            // given
            context("tst-pac_admin_xxx00");

            // when
            final var result = rbacSubjectRepository.findPermissionsOfUserByUuid(subjectUuid("tst-pac_admin_xxx00"));

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
        final var givenUserName = "tst-user_" + System.currentTimeMillis();
        final var givenUser = jpaAttempt.transacted(() -> {
            context(null);
            return rbacSubjectRepository.create(
                    RbacSubjectEntity.builder().uuid(UUID.randomUUID()).name(givenUserName).build());
        }).assumeSuccessful().returnedValue();
        assertThat(rbacSubjectRepository.findByName(givenUser.getName())).isNotNull();
        return givenUser;
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
