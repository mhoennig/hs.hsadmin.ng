package net.hostsharing.hsadminng.rbac.rbacgrant;

import net.hostsharing.hsadminng.Accepts;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.rbacrole.RbacRoleRepository;
import net.hostsharing.hsadminng.rbac.rbacuser.RbacUserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.annotation.DirtiesContext;

import javax.persistence.EntityManager;
import java.util.List;

import static net.hostsharing.test.JpaAttempt.attempt;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ComponentScan(basePackageClasses = { Context.class, RbacGrantRepository.class })
@DirtiesContext
@Accepts({ "GRT:S(Schema)" })
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

    @Nested
    class FindAllRbacGrants {

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
                "grant( aaa00@aaa.example.com -> package#aaa00.admin: managed assumed empowered )");
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
                "grant( admin@aaa.example.com -> customer#aaa.admin: managed assumed empowered )",
                "grant( aaa00@aaa.example.com -> package#aaa00.admin: managed assumed empowered )",
                "grant( aaa01@aaa.example.com -> package#aaa01.admin: managed assumed empowered )",
                "grant( aaa02@aaa.example.com -> package#aaa02.admin: managed assumed empowered )");
        }

        @Test
        @Accepts({ "GRT:L(List)" })
        public void customerAdmin_withAssumedRole_cannotViewRbacGrants() {
            // given:
            currentUser("admin@aaa.example.com");
            assumedRoles("package#aab00.admin");

            // when
            final var result = attempt(
                em,
                () -> rbacGrantRepository.findAll());

            // then
            result.assertExceptionWithRootCauseMessage(
                JpaSystemException.class,
                "[403] user admin@aaa.example.com", "has no permission to assume role package#aab00#admin");
        }
    }

    @Nested
    class CreateRbacGrant {

        @Test
        @Accepts({ "GRT:C(Create)" })
        public void customerAdmin_canGrantOwnPackageAdminRole_toArbitraryUser() {
            // given
            currentUser("admin@aaa.example.com");
            final var userUuid = rbacUserRepository.findUuidByName("aac00@aac.example.com");
            final var roleUuid = rbacRoleRepository.findByRoleName("package#aaa00.admin").getUuid();

            // when
            final var grant = RbacGrantEntity.builder()
                .userUuid(userUuid).roleUuid(roleUuid)
                .assumed(true).empowered(false)
                .build();
            final var attempt = attempt(em, () ->
                rbacGrantRepository.save(grant)
            );

            // then
            assertThat(attempt.wasSuccessful()).isTrue();
            assertThat(rbacGrantRepository.findAll())
                .extracting(RbacGrantEntity::toDisplay)
                .contains("grant( aac00@aac.example.com -> package#aaa00.admin: assumed )");
        }
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
            .filteredOn(g -> !g.getUserName().startsWith("test-user-")) // ignore test-users created by other tests
            .extracting(RbacGrantEntity::toDisplay)
            .containsExactlyInAnyOrder(expectedGrant);
    }

}
