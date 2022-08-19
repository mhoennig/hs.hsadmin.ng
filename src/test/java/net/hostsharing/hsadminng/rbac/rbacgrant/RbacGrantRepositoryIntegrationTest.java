package net.hostsharing.hsadminng.rbac.rbacgrant;

import net.hostsharing.hsadminng.Accepts;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.rbacrole.RbacRoleRepository;
import net.hostsharing.hsadminng.rbac.rbacuser.RbacUserEntity;
import net.hostsharing.hsadminng.rbac.rbacuser.RbacUserRepository;
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
import static org.assertj.core.api.Assumptions.assumeThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { RbacGrantRepository.class, Context.class, JpaAttempt.class })
@DirtiesContext
class RbacGrantRepositoryIntegrationTest {

    @Autowired
    Context context;

    @Autowired
    RbacGrantRepository rbacGrantRepository;

    @Autowired
    RbacUserRepository rbacUserRepository;

    @Autowired
    RbacRoleRepository rbacRoleRepository;

    @Autowired
    EntityManager em;

    @Autowired
    JpaAttempt jpaAttempt;

    @Nested
    class FindAllGrantsOfUser {

        @Test
        @Accepts({ "GRT:L(List)" })
        public void packageAdmin_canViewItsRbacGrants() {
            // given
            currentUser("aaa00@aaa.example.com");

            // when
            final var result = rbacGrantRepository.findAll();

            // then
            exactlyTheseRbacGrantsAreReturned(
                    result,
                    "{ grant assumed role package#aaa00.admin to user aaa00@aaa.example.com by role customer#aaa.admin }");
        }

        @Test
        @Accepts({ "GRT:L(List)" })
        public void customerAdmin_canViewItsRbacGrants() {
            // given
            currentUser("admin@aaa.example.com");

            // when
            final var result = rbacGrantRepository.findAll();

            // then
            exactlyTheseRbacGrantsAreReturned(
                    result,
                    "{ grant assumed role customer#aaa.admin to user admin@aaa.example.com by role global#hostsharing.admin }",
                    "{ grant assumed role package#aaa00.admin to user aaa00@aaa.example.com by role customer#aaa.admin }",
                    "{ grant assumed role package#aaa01.admin to user aaa01@aaa.example.com by role customer#aaa.admin }",
                    "{ grant assumed role package#aaa02.admin to user aaa02@aaa.example.com by role customer#aaa.admin }");
        }

        @Test
        @Accepts({ "GRT:L(List)" })
        public void customerAdmin_withAssumedRole_canOnlyViewRbacGrantsVisibleByAssumedRole() {
            // given:
            currentUser("admin@aaa.example.com");
            assumedRoles("package#aaa00.admin");

            // when
            final var result = rbacGrantRepository.findAll();

            // then
            exactlyTheseRbacGrantsAreReturned(
                    result,
                    "{ grant assumed role package#aaa00.admin to user aaa00@aaa.example.com by role customer#aaa.admin }");
        }
    }

    @Nested
    class GrantRoleToUser {

        @Test
        public void customerAdmin_canGrantOwnPackageAdminRole_toArbitraryUser() {
            // given
            currentUser("admin@aaa.example.com");
            assumedRoles("customer#aaa.admin");
            final var givenArbitraryUserUuid = rbacUserRepository.findByName("aac00@aac.example.com").getUuid();
            final var givenOwnPackageRoleUuid = rbacRoleRepository.findByRoleName("package#aaa00.admin").getUuid();

            // when
            final var grant = RbacGrantEntity.builder()
                    .granteeUserUuid(givenArbitraryUserUuid).grantedRoleUuid(givenOwnPackageRoleUuid)
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
                            "{ grant assumed role package#aaa00.admin to user aac00@aac.example.com by role customer#aaa.admin }");
        }

        @Test
        @Transactional(propagation = Propagation.NEVER)
        public void packageAdmin_canNotGrantPackageOwnerRole() {
            // given
            record Given(RbacUserEntity arbitraryUser, UUID packageOwnerRoleUuid) {}
            final var given = jpaAttempt.transacted(() -> {
                // to find the uuids of we need to have access rights to these
                currentUser("admin@aaa.example.com");
                return new Given(
                        createNewUser(),
                        rbacRoleRepository.findByRoleName("package#aaa00.owner").getUuid()
                );
            }).returnedValue();

            // when
            final var attempt = jpaAttempt.transacted(() -> {
                // now we try to use these uuids as a less privileged user
                currentUser("aaa00@aaa.example.com");
                assumedRoles("package#aaa00.admin");
                final var grant = RbacGrantEntity.builder()
                        .granteeUserUuid(given.arbitraryUser.getUuid())
                        .grantedRoleUuid(given.packageOwnerRoleUuid)
                        .assumed(true)
                        .build();
                rbacGrantRepository.save(grant);
            });

            // then
            attempt.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "ERROR: [403] Access to granted role " + given.packageOwnerRoleUuid
                            + " forbidden for {package#aaa00.admin}");
            jpaAttempt.transacted(() -> {
                // finally, we use the new user to make sure, no roles were granted
                currentUser(given.arbitraryUser.getName());
                assertThat(rbacGrantRepository.findAll())
                        .extracting(RbacGrantEntity::toDisplay)
                        .hasSize(0);
            });
        }
    }

    @Nested
    class RevokeRoleFromUser {

        @Test
        public void customerAdmin_canRevokeSelfGrantedPackageAdminRole() {
            // given
            final var grant = create(grant()
                    .byUser("admin@aaa.example.com").withAssumedRole("customer#aaa.admin")
                    .grantingRole("package#aaa00.admin").toUser("aac00@aac.example.com"));

            // when
            currentUser("admin@aaa.example.com");
            assumedRoles("customer#aaa.admin");
            final var revokeAttempt = attempt(em, () -> {
                rbacGrantRepository.deleteByRbacGrantId(grant.getRbacGrantId());
            });

            // then
            currentUser("admin@aaa.example.com");
            assumedRoles("customer#aaa.admin");
            assertThat(revokeAttempt.caughtExceptionsRootCause()).isNull();
            assertThat(rbacGrantRepository.findAll())
                    .extracting(RbacGrantEntity::getGranteeUserName)
                    .doesNotContain("aac00@aac.example.com");
        }

        @Test
        public void packageAdmin_canRevokeOwnPackageAdminRoleGrantedByAnotherAdminOfThatPackage() {
            // given
            final var grant = create(grant()
                    .byUser("admin@aaa.example.com").withAssumedRole("package#aaa00.admin")
                    .grantingRole("package#aaa00.admin").toUser(createNewUser().getName()));

            // when
            currentUser("aaa00@aaa.example.com");
            assumedRoles("package#aaa00.admin");
            final var revokeAttempt = attempt(em, () -> {
                rbacGrantRepository.deleteByRbacGrantId(grant.getRbacGrantId());
            });

            // then
            assertThat(revokeAttempt.caughtExceptionsRootCause()).isNull();
            currentUser("admin@aaa.example.com");
            assumedRoles("customer#aaa.admin");
            assertThat(rbacGrantRepository.findAll())
                    .extracting(RbacGrantEntity::getGranteeUserName)
                    .doesNotContain("aac00@aac.example.com");
        }

        @Test
        public void packageAdmin_canNotRevokeOwnPackageAdminRoleGrantedByOwnerRoleOfThatPackage() {
            // given
            final var grant = create(grant()
                    .byUser("admin@aaa.example.com").withAssumedRole("package#aaa00.owner")
                    .grantingRole("package#aaa00.admin").toUser("aac00@aac.example.com"));
            final var grantedByRole = rbacRoleRepository.findByRoleName("package#aaa00.owner");

            // when
            currentUser("aaa00@aaa.example.com");
            assumedRoles("package#aaa00.admin");
            final var revokeAttempt = attempt(em, () -> {
                rbacGrantRepository.deleteByRbacGrantId(grant.getRbacGrantId());
            });

            // then
            revokeAttempt.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "ERROR: [403] Revoking role created by %s is forbidden for {package#aaa00.admin}.".formatted(
                            grantedByRole.getUuid()
                    ));
        }

        private RbacGrantEntity create(GrantBuilder with) {
            currentUser(with.byUserName);
            assumedRoles(with.assumedRole);
            final var givenArbitraryUserUuid = rbacUserRepository.findByName(with.granteeUserName).getUuid();
            final var givenOwnPackageRoleUuid = rbacRoleRepository.findByRoleName(with.grantedRole).getUuid();

            final var grant = RbacGrantEntity.builder()
                    .granteeUserUuid(givenArbitraryUserUuid).grantedRoleUuid(givenOwnPackageRoleUuid)
                    .assumed(true)
                    .build();
            final var grantAttempt = attempt(em, () ->
                    rbacGrantRepository.save(grant)
            );

            assumeThat(grantAttempt.caughtException()).isNull();
            assumeThat(rbacGrantRepository.findAll())
                    .extracting(RbacGrantEntity::toDisplay)
                    .contains("{ grant assumed role %s to user %s by role %s }".formatted(
                            with.grantedRole, with.granteeUserName, with.assumedRole
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
            String granteeUserName;

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
                this.granteeUserName = toUser;
                return this;
            }
        }
    }

    private RbacUserEntity createNewUser() {
        return rbacUserRepository.create(
                new RbacUserEntity(null, "test-user-" + System.currentTimeMillis() + "@example.com"));
    }

    void currentUser(final String currentUser) {
        context.setCurrentUser(currentUser);
        assertThat(context.getCurrentUser()).as("precondition").isEqualTo(currentUser);
    }

    void assumedRoles(final String assumedRoles) {
        context.assumeRoles(assumedRoles);
        assertThat(context.getAssumedRoles()).as("precondition").containsExactly(assumedRoles.split(";"));
    }

    void exactlyTheseRbacGrantsAreReturned(final List<RbacGrantEntity> actualResult, final String... expectedGrant) {
        assertThat(actualResult)
                .filteredOn(g -> !g.getGranteeUserName().startsWith("test-user-")) // ignore test-users created by other tests
                .extracting(RbacGrantEntity::toDisplay)
                .containsExactlyInAnyOrder(expectedGrant);
    }

}
