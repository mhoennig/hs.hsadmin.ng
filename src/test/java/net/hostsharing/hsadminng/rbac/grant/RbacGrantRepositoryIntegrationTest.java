package net.hostsharing.hsadminng.rbac.grant;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.role.RbacRoleRepository;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectRepository;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import( { Context.class, JpaAttempt.class })
class RbacGrantRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    Context context;

    @MockBean
    HttpServletRequest request;

    @Autowired
    RbacGrantRepository rbacGrantRepository;

    @Autowired
    RawRbacGrantRepository rawRbacGrantRepository;

    @Autowired
    RbacSubjectRepository rbacSubjectRepository;

    @Autowired
    RbacRoleRepository rbacRoleRepository;

    @PersistenceContext
    EntityManager em;

    @Autowired
    JpaAttempt jpaAttempt;

    @Nested
    class FindAllGrantsOfUser {

        @Test
        public void packageAdmin_canViewItsRbacGrants() {
            // given
            context("pac-admin-xxx00@xxx.example.com", null);

            // when
            final var result = rbacGrantRepository.findAll();

            // then
            exactlyTheseRbacGrantsAreReturned(
                    result,
                    "{ grant role:rbactest.package#xxx00:ADMIN to user:pac-admin-xxx00@xxx.example.com by role:rbactest.customer#xxx:ADMIN and assume }");
        }

        @Test
        public void customerAdmin_canViewItsRbacGrants() {
            // given
            context("customer-admin@xxx.example.com", null);

            // when
            final var result = rbacGrantRepository.findAll();

            // then
            exactlyTheseRbacGrantsAreReturned(
                    result,
                    "{ grant role:rbactest.customer#xxx:ADMIN to user:customer-admin@xxx.example.com by role:rbactest.customer#xxx:OWNER and assume }",
                    "{ grant role:rbactest.package#xxx00:ADMIN to user:pac-admin-xxx00@xxx.example.com by role:rbactest.customer#xxx:ADMIN and assume }",
                    "{ grant role:rbactest.package#xxx01:ADMIN to user:pac-admin-xxx01@xxx.example.com by role:rbactest.customer#xxx:ADMIN and assume }",
                    "{ grant role:rbactest.package#xxx02:ADMIN to user:pac-admin-xxx02@xxx.example.com by role:rbactest.customer#xxx:ADMIN and assume }");
        }

        @Test
        public void customerAdmin_withAssumedRole_canOnlyViewRbacGrantsVisibleByAssumedRole() {
            // given:
            context("customer-admin@xxx.example.com", "rbactest.package#xxx00:ADMIN");

            // when
            final var result = rbacGrantRepository.findAll();

            // then
            exactlyTheseRbacGrantsAreReturned(
                    result,
                    "{ grant role:rbactest.package#xxx00:ADMIN to user:pac-admin-xxx00@xxx.example.com by role:rbactest.customer#xxx:ADMIN and assume }");
        }
    }

    @Nested
    class GrantRoleToSubject {

        @Test
        public void customerAdmin_canGrantOwnPackageAdminRole_toArbitraryUser() {
            // given
            context("customer-admin@xxx.example.com", "rbactest.customer#xxx:ADMIN");
            final var givenArbitrarySubjectUuid = rbacSubjectRepository.findByName("pac-admin-zzz00@zzz.example.com").getUuid();
            final var givenOwnPackageRoleUuid = rbacRoleRepository.findByRoleName("rbactest.package#xxx00:ADMIN").getUuid();

            // when
            final var grant = RbacGrantEntity.builder()
                    .granteeSubjectUuid(givenArbitrarySubjectUuid).grantedRoleUuid(givenOwnPackageRoleUuid)
                    .assumed(true)
                    .build();
            final var attempt = attempt(em, () ->
                    rbacGrantRepository.save(grant)
            );

            // then
            assertThat(attempt.caughtException()).isNull();
            assertThat(rbacGrantRepository.findAll())
                    .extracting(RbacGrantEntity::toDisplay)
                    .contains(
                            "{ grant role:rbactest.package#xxx00:ADMIN to user:pac-admin-zzz00@zzz.example.com by role:rbactest.customer#xxx:ADMIN and assume }");
        }

        @Test
        @Transactional(propagation = Propagation.NEVER)
        public void packageAdmin_canNotGrantPackageOwnerRole() {
            // given
            record Given(RbacSubjectEntity arbitraryUser, UUID packageOwnerRoleUuid) {}
            final var given = jpaAttempt.transacted(() -> {
                // to find the uuids of we need to have access rights to these
                context("customer-admin@xxx.example.com", null);
                return new Given(
                        createNewUser(),
                        rbacRoleRepository.findByRoleName("rbactest.package#xxx00:OWNER").getUuid()
                );
            }).assumeSuccessful().returnedValue();

            // when
            final var attempt = jpaAttempt.transacted(() -> {
                // now we try to use these uuids as a less privileged user
                context("pac-admin-xxx00@xxx.example.com", "rbactest.package#xxx00:ADMIN");
                final var grant = RbacGrantEntity.builder()
                        .granteeSubjectUuid(given.arbitraryUser.getUuid())
                        .grantedRoleUuid(given.packageOwnerRoleUuid)
                        .assumed(true)
                        .build();
                rbacGrantRepository.save(grant);
            });

            // then
            attempt.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "ERROR: [403] Access to granted role rbactest.package#xxx00:OWNER",
                    "forbidden for rbactest.package#xxx00:ADMIN");
            jpaAttempt.transacted(() -> {
                // finally, we use the new user to make sure, no roles were granted
                context(given.arbitraryUser.getName(), null);
                assertThat(rbacGrantRepository.findAll())
                        .hasSize(0);
            });
        }
    }

    @Nested
    class revokeRoleFromSubject {

        @Test
        public void customerAdmin_canRevokeSelfGrantedPackageAdminRole() {
            // given
            final var grant = create(grant()
                    .byUser("customer-admin@xxx.example.com").withAssumedRole("rbactest.customer#xxx:ADMIN")
                    .grantingRole("rbactest.package#xxx00:ADMIN").toUser("pac-admin-zzz00@zzz.example.com"));

            // when
            context("customer-admin@xxx.example.com", "rbactest.customer#xxx:ADMIN");
            final var revokeAttempt = attempt(em, () ->
                    rbacGrantRepository.deleteByRbacGrantId(grant.getRbacGrantId()));

            // then
            context("customer-admin@xxx.example.com", "rbactest.customer#xxx:ADMIN");
            assertThat(revokeAttempt.caughtExceptionsRootCause()).isNull();
            assertThat(rbacGrantRepository.findAll())
                    .extracting(RbacGrantEntity::getGranteeSubjectName)
                    .doesNotContain("pac-admin-zzz00@zzz.example.com");
        }

        @Test
        public void packageAdmin_canRevokeOwnPackageAdminRoleGrantedByAnotherAdminOfThatPackage() {
            // given
            final var newUser = createNewUserTransacted();
            final var grant = create(grant()
                    .byUser("customer-admin@xxx.example.com").withAssumedRole("rbactest.package#xxx00:ADMIN")
                    .grantingRole("rbactest.package#xxx00:ADMIN").toUser(newUser.getName()));

            // when
            context("pac-admin-xxx00@xxx.example.com", "rbactest.package#xxx00:ADMIN");
            final var revokeAttempt = attempt(em, () ->
                    rbacGrantRepository.deleteByRbacGrantId(grant.getRbacGrantId()));

            // then
            assertThat(revokeAttempt.caughtExceptionsRootCause()).isNull();
            context("customer-admin@xxx.example.com", "rbactest.customer#xxx:ADMIN");
            assertThat(rbacGrantRepository.findAll())
                    .extracting(RbacGrantEntity::getGranteeSubjectName)
                    .doesNotContain("pac-admin-zzz00@zzz.example.com");
        }

        @Test
        public void packageAdmin_canNotRevokeOwnPackageAdminRoleGrantedByOwnerRoleOfThatPackage() {
            // given
            final var grant = create(grant()
                    .byUser("customer-admin@xxx.example.com").withAssumedRole("rbactest.package#xxx00:OWNER")
                    .grantingRole("rbactest.package#xxx00:ADMIN").toUser("pac-admin-zzz00@zzz.example.com"));
            final var grantedByRole = rbacRoleRepository.findByRoleName("rbactest.package#xxx00:OWNER");

            // when
            context("pac-admin-xxx00@xxx.example.com", "rbactest.package#xxx00:ADMIN");
            final var revokeAttempt = attempt(em, () ->
                    rbacGrantRepository.deleteByRbacGrantId(grant.getRbacGrantId()));

            // then
            revokeAttempt.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "ERROR: [403] Revoking role created by %s is forbidden for {rbactest.package#xxx00:ADMIN}.".formatted(
                            grantedByRole.getUuid()
                    ));
        }

        private RbacGrantEntity create(GrantBuilder with) {
            context(with.byUserName, with.assumedRole);
            final var givenArbitrarySubjectUuid = rbacSubjectRepository.findByName(with.granteeSubjectName).getUuid();
            final var givenOwnPackageRoleUuid = rbacRoleRepository.findByRoleName(with.grantedRole).getUuid();

            final var grant = RbacGrantEntity.builder()
                    .granteeSubjectUuid(givenArbitrarySubjectUuid).grantedRoleUuid(givenOwnPackageRoleUuid)
                    .assumed(true)
                    .build();
            final var grantAttempt = attempt(em, () ->
                    rbacGrantRepository.save(grant)
            );

            assertThat(grantAttempt.caughtException()).isNull();
            assertThat(rawRbacGrantRepository.findAll())
                    .extracting(RawRbacGrantEntity::toDisplay)
                    .contains("{ grant role:%s to user:%s by %s and assume }".formatted(
                            with.grantedRole, with.granteeSubjectName, with.assumedRole
                    ));

            return grant;
        }

        private GrantBuilder grant() {
            return new GrantBuilder();
        }

        static class GrantBuilder {

            String byUserName;
            String assumedRole = "";
            String grantedRole;
            String granteeSubjectName;

            GrantBuilder byUser(final String userName) {
                byUserName = userName;
                return this;
            }

            GrantBuilder withAssumedRole(final String assumedRole) {
                this.assumedRole = assumedRole != null ? assumedRole : "";
                return this;
            }

            GrantBuilder grantingRole(final String grantingRole) {
                this.grantedRole = grantingRole;
                return this;
            }

            GrantBuilder toUser(final String toUser) {
                this.granteeSubjectName = toUser;
                return this;
            }
        }
    }

    @Nested
    class RebuildRbacSystem {

        @Test
        public void rebuildingTheRbacSystemWitSameRbacSpecDoesNotChangeGrantNorRoleCount() {
            final var grantCountBefore = sql("SELECT COUNT(*) FROM rbac.grants");
            final var roleCountBefore = sql("SELECT COUNT(*) FROM rbac.role");

            jpaAttempt.transacted(() ->
                    em.createNativeQuery("CALL rbactest.package_rebuild_rbac_system()")
            );

            final var grantCountAfter = sql("SELECT COUNT(*) FROM rbac.grants");
            assertThat(grantCountBefore).as("grant count must not change").isEqualTo(grantCountAfter);

            final var roleCountAfter = sql("SELECT COUNT(*) FROM rbac.role");
            assertThat(roleCountBefore).as("role count must not change").isEqualTo(roleCountAfter);
        }

        private Object sql(final String query) {
            return jpaAttempt.transacted(() ->
                    em.createNativeQuery(query).getSingleResult()
            ).assumeSuccessful().returnedValue();
        }
    }

    private RbacSubjectEntity createNewUserTransacted() {
        return jpaAttempt.transacted(() -> {
            final var newUserName = "test-user-" + System.currentTimeMillis() + "@example.com";
            context(null);
            return rbacSubjectRepository.create(new RbacSubjectEntity(null, newUserName));
        }).assumeSuccessful().returnedValue();
    }

    private RbacSubjectEntity createNewUser() {
        return rbacSubjectRepository.create(
                new RbacSubjectEntity(null, "test-user-" + System.currentTimeMillis() + "@example.com"));
    }

    void exactlyTheseRbacGrantsAreReturned(final List<RbacGrantEntity> actualResult, final String... expectedGrant) {
        assertThat(actualResult)
                .filteredOn(g -> !g.getGranteeSubjectName().startsWith("test-user-")) // ignore test-users created by other tests
                .extracting(RbacGrantEntity::toDisplay)
                .containsExactlyInAnyOrder(expectedGrant);
    }

}
