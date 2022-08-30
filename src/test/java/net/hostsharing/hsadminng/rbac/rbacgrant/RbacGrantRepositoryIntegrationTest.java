package net.hostsharing.hsadminng.rbac.rbacgrant;

import net.hostsharing.hsadminng.Accepts;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.rbacrole.RbacRoleRepository;
import net.hostsharing.hsadminng.rbac.rbacuser.RbacUserEntity;
import net.hostsharing.hsadminng.rbac.rbacuser.RbacUserRepository;
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
import static org.assertj.core.api.Assumptions.assumeThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { RbacGrantRepository.class, Context.class, JpaAttempt.class })
@DirtiesContext
class RbacGrantRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    Context context;

    @MockBean
    HttpServletRequest request;

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
            context("pac-admin-xxx00@xxx.example.com", null);

            // when
            final var result = rbacGrantRepository.findAll();

            // then
            exactlyTheseRbacGrantsAreReturned(
                    result,
                    "{ grant assumed role package#xxx00.admin to user pac-admin-xxx00@xxx.example.com by role customer#xxx.admin }");
        }

        @Test
        @Accepts({ "GRT:L(List)" })
        public void customerAdmin_canViewItsRbacGrants() {
            // given
            context("customer-admin@xxx.example.com", null);

            // when
            final var result = rbacGrantRepository.findAll();

            // then
            exactlyTheseRbacGrantsAreReturned(
                    result,
                    "{ grant assumed role customer#xxx.admin to user customer-admin@xxx.example.com by role global#hostsharing.admin }",
                    "{ grant assumed role package#xxx00.admin to user pac-admin-xxx00@xxx.example.com by role customer#xxx.admin }",
                    "{ grant assumed role package#xxx01.admin to user pac-admin-xxx01@xxx.example.com by role customer#xxx.admin }",
                    "{ grant assumed role package#xxx02.admin to user pac-admin-xxx02@xxx.example.com by role customer#xxx.admin }");
        }

        @Test
        @Accepts({ "GRT:L(List)" })
        public void customerAdmin_withAssumedRole_canOnlyViewRbacGrantsVisibleByAssumedRole() {
            // given:
            context("customer-admin@xxx.example.com", "package#xxx00.admin");

            // when
            final var result = rbacGrantRepository.findAll();

            // then
            exactlyTheseRbacGrantsAreReturned(
                    result,
                    "{ grant assumed role package#xxx00.admin to user pac-admin-xxx00@xxx.example.com by role customer#xxx.admin }");
        }
    }

    @Nested
    class GrantRoleToUser {

        @Test
        public void customerAdmin_canGrantOwnPackageAdminRole_toArbitraryUser() {
            // given
            context("customer-admin@xxx.example.com", "customer#xxx.admin");
            final var givenArbitraryUserUuid = rbacUserRepository.findByName("pac-admin-zzz00@zzz.example.com").getUuid();
            final var givenOwnPackageRoleUuid = rbacRoleRepository.findByRoleName("package#xxx00.admin").getUuid();

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
                            "{ grant assumed role package#xxx00.admin to user pac-admin-zzz00@zzz.example.com by role customer#xxx.admin }");
        }

        @Test
        @Transactional(propagation = Propagation.NEVER)
        public void packageAdmin_canNotGrantPackageOwnerRole() {
            // given
            record Given(RbacUserEntity arbitraryUser, UUID packageOwnerRoleUuid) {}
            final var given = jpaAttempt.transacted(() -> {
                // to find the uuids of we need to have access rights to these
                context("customer-admin@xxx.example.com", null);
                return new Given(
                        createNewUser(),
                        rbacRoleRepository.findByRoleName("package#xxx00.owner").getUuid()
                );
            }).assumeSuccessful().returnedValue();

            // when
            final var attempt = jpaAttempt.transacted(() -> {
                // now we try to use these uuids as a less privileged user
                context("pac-admin-xxx00@xxx.example.com", "package#xxx00.admin");
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
                            + " forbidden for {package#xxx00.admin}");
            jpaAttempt.transacted(() -> {
                // finally, we use the new user to make sure, no roles were granted
                context(given.arbitraryUser.getName(), null);
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
                    .byUser("customer-admin@xxx.example.com").withAssumedRole("customer#xxx.admin")
                    .grantingRole("package#xxx00.admin").toUser("pac-admin-zzz00@zzz.example.com"));

            // when
            context("customer-admin@xxx.example.com", "customer#xxx.admin");
            final var revokeAttempt = attempt(em, () -> {
                rbacGrantRepository.deleteByRbacGrantId(grant.getRbacGrantId());
            });

            // then
            context("customer-admin@xxx.example.com", "customer#xxx.admin");
            assertThat(revokeAttempt.caughtExceptionsRootCause()).isNull();
            assertThat(rbacGrantRepository.findAll())
                    .extracting(RbacGrantEntity::getGranteeUserName)
                    .doesNotContain("pac-admin-zzz00@zzz.example.com");
        }

        @Test
        public void packageAdmin_canRevokeOwnPackageAdminRoleGrantedByAnotherAdminOfThatPackage() {
            // given
            final var newUser = createNewUserTransacted();
            final var grant = create(grant()
                    .byUser("customer-admin@xxx.example.com").withAssumedRole("package#xxx00.admin")
                    .grantingRole("package#xxx00.admin").toUser(newUser.getName()));

            // when
            context("pac-admin-xxx00@xxx.example.com", "package#xxx00.admin");
            final var revokeAttempt = attempt(em, () -> {
                rbacGrantRepository.deleteByRbacGrantId(grant.getRbacGrantId());
            });

            // then
            assertThat(revokeAttempt.caughtExceptionsRootCause()).isNull();
            context("customer-admin@xxx.example.com", "customer#xxx.admin");
            assertThat(rbacGrantRepository.findAll())
                    .extracting(RbacGrantEntity::getGranteeUserName)
                    .doesNotContain("pac-admin-zzz00@zzz.example.com");
        }

        @Test
        public void packageAdmin_canNotRevokeOwnPackageAdminRoleGrantedByOwnerRoleOfThatPackage() {
            // given
            final var grant = create(grant()
                    .byUser("customer-admin@xxx.example.com").withAssumedRole("package#xxx00.owner")
                    .grantingRole("package#xxx00.admin").toUser("pac-admin-zzz00@zzz.example.com"));
            final var grantedByRole = rbacRoleRepository.findByRoleName("package#xxx00.owner");

            // when
            context("pac-admin-xxx00@xxx.example.com", "package#xxx00.admin");
            final var revokeAttempt = attempt(em, () -> {
                rbacGrantRepository.deleteByRbacGrantId(grant.getRbacGrantId());
            });

            // then
            revokeAttempt.assertExceptionWithRootCauseMessage(
                    JpaSystemException.class,
                    "ERROR: [403] Revoking role created by %s is forbidden for {package#xxx00.admin}.".formatted(
                            grantedByRole.getUuid()
                    ));
        }

        private RbacGrantEntity create(GrantBuilder with) {
            context(with.byUserName, with.assumedRole);
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

    private RbacUserEntity createNewUserTransacted() {
        return jpaAttempt.transacted(() -> {
            final var newUserName = "test-user-" + System.currentTimeMillis() + "@example.com";
            context(newUserName);
            return rbacUserRepository.create(new RbacUserEntity(null, newUserName));
        }).assumeSuccessful().returnedValue();
    }

    private RbacUserEntity createNewUser() {
        return rbacUserRepository.create(
                new RbacUserEntity(null, "test-user-" + System.currentTimeMillis() + "@example.com"));
    }

    void exactlyTheseRbacGrantsAreReturned(final List<RbacGrantEntity> actualResult, final String... expectedGrant) {
        assertThat(actualResult)
                .filteredOn(g -> !g.getGranteeUserName().startsWith("test-user-")) // ignore test-users created by other tests
                .extracting(RbacGrantEntity::toDisplay)
                .containsExactlyInAnyOrder(expectedGrant);
    }

}
