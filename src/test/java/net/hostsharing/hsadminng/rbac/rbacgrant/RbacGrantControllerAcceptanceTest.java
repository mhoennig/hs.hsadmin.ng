package net.hostsharing.hsadminng.rbac.rbacgrant;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import net.hostsharing.hsadminng.Accepts;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.rbacrole.RbacRoleEntity;
import net.hostsharing.hsadminng.rbac.rbacrole.RbacRoleRepository;
import net.hostsharing.hsadminng.rbac.rbacuser.RbacUserEntity;
import net.hostsharing.hsadminng.rbac.rbacuser.RbacUserRepository;
import net.hostsharing.test.JpaAttempt;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class }
)
@Accepts({ "GRT:S(Schema)" })
@Transactional(readOnly = true, propagation = Propagation.NEVER)
class RbacGrantControllerAcceptanceTest extends ContextBasedTest {

    @LocalServerPort
    Integer port;

    @Autowired
    EntityManager em;

    @Autowired
    RbacUserRepository rbacUserRepository;

    @Autowired
    RbacRoleRepository rbacRoleRepository;

    @Autowired
    RbacGrantRepository rbacGrantRepository;

    @Autowired
    JpaAttempt jpaAttempt;

    @Nested
    class ListGrants {

        @Test
        @Accepts("GRT:L(List)")
        void testGlobalAdmin_withoutAssumedRole_canViewAllGrants() {
            RestAssured // @formatter:off
                .given()
                    .header("current-user", "mike@example.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/grants")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasItem(
                            allOf(
                                hasEntry("grantedByRoleIdName", "global#test-global.admin"),
                                hasEntry("grantedRoleIdName", "test_customer#xxx.admin"),
                                hasEntry("granteeUserName", "customer-admin@xxx.example.com")
                            )
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("grantedByRoleIdName", "global#test-global.admin"),
                                    hasEntry("grantedRoleIdName", "test_customer#yyy.admin"),
                                    hasEntry("granteeUserName", "customer-admin@yyy.example.com")
                            )
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("grantedByRoleIdName", "global#test-global.admin"),
                                    hasEntry("grantedRoleIdName", "global#test-global.admin"),
                                    hasEntry("granteeUserName", "sven@example.org")
                            )
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("grantedByRoleIdName", "test_customer#xxx.admin"),
                                    hasEntry("grantedRoleIdName", "test_package#xxx00.admin"),
                                    hasEntry("granteeUserName", "pac-admin-xxx00@xxx.example.com")
                            )
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("grantedByRoleIdName", "test_customer#zzz.admin"),
                                    hasEntry("grantedRoleIdName", "test_package#zzz02.admin"),
                                    hasEntry("granteeUserName", "pac-admin-zzz02@zzz.example.com")
                            )
                    ))
                    .body("size()", greaterThanOrEqualTo(14));
                // @formatter:on
        }

        @Test
        @Accepts({ "GRT:L(List)", "GRT:X(Access Control)" })
        void testGlobalAdmin_withAssumedPackageAdminRole_canViewPacketRelatedGrants() {
            RestAssured // @formatter:off
                .given()
                    .header("current-user", "mike@example.org")
                    .header("assumed-roles", "test_package#yyy00.admin")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/grants")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasItem(
                            allOf(
                                    hasEntry("grantedByRoleIdName", "test_customer#yyy.admin"),
                                    hasEntry("grantedRoleIdName", "test_package#yyy00.admin"),
                                    hasEntry("granteeUserName", "pac-admin-yyy00@yyy.example.com")
                            )
                    ))
                    .body("size()", is(1));
                // @formatter:on
        }

        @Test
        @Accepts({ "GRT:L(List)", "GRT:X(Access Control)" })
        void packageAdmin_withoutAssumedRole_canViewPacketRelatedGrants() {
            RestAssured // @formatter:off
                .given()
                    .header("current-user", "pac-admin-yyy00@yyy.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/grants")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasItem(
                            allOf(
                                    hasEntry("grantedByRoleIdName", "test_customer#yyy.admin"),
                                    hasEntry("grantedRoleIdName", "test_package#yyy00.admin"),
                                    hasEntry("granteeUserName", "pac-admin-yyy00@yyy.example.com")
                            )
                    ))
                    .body("size()", is(1));
            // @formatter:on
        }
    }

    @Nested
    class GetGrantById {

        @Test
        @Accepts({ "GRT:R(Read)" })
        void customerAdmin_withAssumedPacketAdminRole_canReadPacketAdminsGrantById() {
            // given
            final var givenCurrentUserAsPackageAdmin = new Subject("customer-admin@xxx.example.com");
            final var givenGranteeUser = findRbacUserByName("pac-admin-xxx00@xxx.example.com");
            final var givenGrantedRole = findRbacRoleByName("test_package#xxx00.admin");

            // when
            final var grant = givenCurrentUserAsPackageAdmin.getGrantById()
                    .forGrantedRole(givenGrantedRole).toGranteeUser(givenGranteeUser);

            // then
            grant.assertThat()
                    .statusCode(200)
                    .body("grantedByRoleIdName", is("test_customer#xxx.admin"))
                    .body("grantedRoleIdName", is("test_package#xxx00.admin"))
                    .body("granteeUserName", is("pac-admin-xxx00@xxx.example.com"));
        }

        @Test
        @Accepts({ "GRT:R(Read)" })
        void packageAdmin_withoutAssumedRole_canReadItsOwnGrantById() {
            // given
            final var givenCurrentUserAsPackageAdmin = new Subject("pac-admin-xxx00@xxx.example.com");
            final var givenGranteeUser = findRbacUserByName("pac-admin-xxx00@xxx.example.com");
            final var givenGrantedRole = findRbacRoleByName("test_package#xxx00.admin");

            // when
            final var grant = givenCurrentUserAsPackageAdmin.getGrantById()
                    .forGrantedRole(givenGrantedRole).toGranteeUser(givenGranteeUser);

            // then
            grant.assertThat()
                    .statusCode(200)
                    .body("grantedByRoleIdName", is("test_customer#xxx.admin"))
                    .body("grantedRoleIdName", is("test_package#xxx00.admin"))
                    .body("granteeUserName", is("pac-admin-xxx00@xxx.example.com"));
        }

        @Test
        @Accepts({ "GRT:R(Read)", "GRT:X(Access Control)" })
        void packageAdmin_withAssumedPackageAdmin_canStillReadItsOwnGrantById() {
            // given
            final var givenCurrentUserAsPackageAdmin = new Subject(
                    "pac-admin-xxx00@xxx.example.com",
                    "test_package#xxx00.admin");
            final var givenGranteeUser = findRbacUserByName("pac-admin-xxx00@xxx.example.com");
            final var givenGrantedRole = findRbacRoleByName("test_package#xxx00.admin");

            // when
            final var grant = givenCurrentUserAsPackageAdmin.getGrantById()
                    .forGrantedRole(givenGrantedRole).toGranteeUser(givenGranteeUser);

            // then
            grant.assertThat()
                    .statusCode(200)
                    .body("grantedByRoleIdName", is("test_customer#xxx.admin"))
                    .body("grantedRoleIdName", is("test_package#xxx00.admin"))
                    .body("granteeUserName", is("pac-admin-xxx00@xxx.example.com"));
        }

        @Test
        @Accepts({ "GRT:R(Read)", "GRT:X(Access Control)" })
        void packageAdmin_withAssumedPackageTenantRole_canNotReadItsOwnGrantByIdAnymore() {

            // given
            final var givenCurrentUserAsPackageAdmin = new Subject(
                    "pac-admin-xxx00@xxx.example.com",
                    "test_package#xxx00.tenant");
            final var givenGranteeUser = findRbacUserByName("pac-admin-xxx00@xxx.example.com");
            final var givenGrantedRole = findRbacRoleByName("test_package#xxx00.admin");
            final var grant = givenCurrentUserAsPackageAdmin.getGrantById()
                    .forGrantedRole(givenGrantedRole).toGranteeUser(givenGranteeUser);

            // then
            grant.assertThat()
                    .statusCode(404);
        }
    }

    @Nested
    class GrantRoleToUser {

        @Test
        @Accepts({ "GRT:C(Create)" })
        void packageAdmin_canGrantOwnPackageAdminRole_toArbitraryUser() {

            // given
            final var givenNewUser = createRBacUser();
            final var givenRoleToGrant = "test_package#xxx00.admin";
            final var givenCurrentUserAsPackageAdmin = new Subject("pac-admin-xxx00@xxx.example.com", givenRoleToGrant);
            final var givenOwnPackageAdminRole =
                    findRbacRoleByName(givenCurrentUserAsPackageAdmin.assumedRole);

            // when
            final var response = givenCurrentUserAsPackageAdmin
                    .grantsRole(givenOwnPackageAdminRole).assumed()
                    .toUser(givenNewUser);

            // then
            response.assertThat()
                    .statusCode(201)
                    .body("grantedByRoleIdName", is("test_package#xxx00.admin"))
                    .body("assumed", is(true))
                    .body("grantedRoleIdName", is("test_package#xxx00.admin"))
                    .body("granteeUserName", is(givenNewUser.getName()));
            assertThat(findAllGrantsOf(givenCurrentUserAsPackageAdmin))
                    .extracting(RbacGrantEntity::toDisplay)
                    .contains("{ grant assumed role " + givenOwnPackageAdminRole.getRoleName() +
                            " to user " + givenNewUser.getName() +
                            " by role " + givenRoleToGrant + " }");
        }

        @Test
        @Accepts({ "GRT:C(Create)", "GRT:X(Access Control)" })
        void packageAdmin_canNotGrantAlienPackageAdminRole_toArbitraryUser() {

            // given
            final var givenNewUser = createRBacUser();
            final var givenRoleToGrant = "test_package#xxx00.admin";
            final var givenCurrentUserAsPackageAdmin = new Subject("pac-admin-xxx00@xxx.example.com", givenRoleToGrant);
            final var givenAlienPackageAdminRole = findRbacRoleByName("test_package#yyy00.admin");

            // when
            final var result = givenCurrentUserAsPackageAdmin
                    .grantsRole(givenAlienPackageAdminRole).assumed()
                    .toUser(givenNewUser);

            // then
            result.assertThat()
                    .statusCode(403)
                    .body("message", containsString("Access to granted role"))
                    .body("message", containsString("forbidden for {test_package#xxx00.admin}"));
            assertThat(findAllGrantsOf(givenCurrentUserAsPackageAdmin))
                    .extracting(RbacGrantEntity::getGranteeUserName)
                    .doesNotContain(givenNewUser.getName());
        }
    }

    @Nested
    class RevokeRoleFromUser {

        @Test
        @Accepts({ "GRT:D(Delete)" })
        @Transactional(propagation = Propagation.NEVER)
        void packageAdmin_canRevokePackageAdminRole_grantedByPackageAdmin_fromArbitraryUser() {

            // given
            final var givenArbitraryUser = createRBacUser();
            final var givenRoleToGrant = "test_package#xxx00.admin";
            final var givenCurrentUserAsPackageAdmin = new Subject("pac-admin-xxx00@xxx.example.com", givenRoleToGrant);
            final var givenOwnPackageAdminRole = findRbacRoleByName("test_package#xxx00.admin");

            // and given an existing grant
            assumeCreated(givenCurrentUserAsPackageAdmin
                    .grantsRole(givenOwnPackageAdminRole).assumed()
                    .toUser(givenArbitraryUser));
            assumeGrantExists(
                    givenCurrentUserAsPackageAdmin,
                    "{ grant assumed role %s to user %s by role %s }".formatted(
                            givenOwnPackageAdminRole.getRoleName(),
                            givenArbitraryUser.getName(),
                            givenCurrentUserAsPackageAdmin.assumedRole));

            // when
            final var revokeResponse = givenCurrentUserAsPackageAdmin
                    .revokesRole(givenOwnPackageAdminRole)
                    .fromUser(givenArbitraryUser);

            // then
            revokeResponse.assertThat().statusCode(204);
            assertThat(findAllGrantsOf(givenCurrentUserAsPackageAdmin))
                    .extracting(RbacGrantEntity::getGranteeUserName)
                    .doesNotContain(givenArbitraryUser.getName());
        }
    }

    private void assumeCreated(final ValidatableResponse response) {
        assumeThat(response.extract().response().statusCode()).isEqualTo(201);
    }

    class Subject {

        final String currentUser;
        final String assumedRole;

        public Subject(final String currentUser, final String assumedRole) {
            this.currentUser = currentUser;
            this.assumedRole = assumedRole;
        }

        public Subject(final String currentUser) {
            this(currentUser, "");
        }

        GrantFixture grantsRole(final RbacRoleEntity givenOwnPackageAdminRole) {
            return new GrantFixture(givenOwnPackageAdminRole);
        }

        RevokeFixture revokesRole(final RbacRoleEntity givenOwnPackageAdminRole) {
            return new RevokeFixture(givenOwnPackageAdminRole);
        }

        GetGrantByIdFixture getGrantById() {
            return new GetGrantByIdFixture();
        }

        class GrantFixture {

            private Subject grantingSubject = Subject.this;
            private final RbacRoleEntity grantedRole;
            private boolean assumed;
            private RbacUserEntity granteeUser;

            public GrantFixture(final RbacRoleEntity roleToGrant) {
                this.grantedRole = roleToGrant;
            }

            GrantFixture assumed() {
                this.assumed = true;
                return this;
            }

            ValidatableResponse toUser(final RbacUserEntity granteeUser) {
                this.granteeUser = granteeUser;

                return RestAssured // @formatter:ff
                        .given()
                        .header("current-user", grantingSubject.currentUser)
                        .header("assumed-roles", grantingSubject.assumedRole)
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                  "assumed": true,
                                  "grantedRoleUuid": "%s",
                                  "granteeUserUuid": "%s"
                                }
                                """.formatted(
                                grantedRole.getUuid(),
                                granteeUser.getUuid())
                        )
                        .port(port)
                        .when()
                        .post("http://localhost/api/rbac/grants")
                        .then().log().all(); // @formatter:on
            }
        }

        class RevokeFixture {

            private Subject currentSubject = Subject.this;
            private final RbacRoleEntity grantedRole;
            private boolean assumed;
            private RbacUserEntity granteeUser;

            public RevokeFixture(final RbacRoleEntity roleToGrant) {
                this.grantedRole = roleToGrant;
            }

            ValidatableResponse fromUser(final RbacUserEntity granteeUser) {
                this.granteeUser = granteeUser;

                return RestAssured // @formatter:ff
                        .given()
                        .header("current-user", currentSubject.currentUser)
                        .header("assumed-roles", currentSubject.assumedRole)
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                  "assumed": true,
                                  "grantedRoleUuid": "%s",
                                  "granteeUserUuid": "%s"
                                }
                                """.formatted(
                                grantedRole.getUuid(),
                                granteeUser.getUuid())
                        )
                        .port(port)
                        .when()
                        .delete("http://localhost/api/rbac/grants/%s/%s".formatted(
                                grantedRole.getUuid(), granteeUser.getUuid()
                        ))
                        .then().log().all(); // @formatter:on
            }
        }

        private class GetGrantByIdFixture {

            private Subject currentSubject = Subject.this;
            private RbacRoleEntity grantedRole;

            GetGrantByIdFixture forGrantedRole(final RbacRoleEntity grantedRole) {
                this.grantedRole = grantedRole;
                return this;
            }

            ValidatableResponse toGranteeUser(final RbacUserEntity granteeUser) {

                return RestAssured // @formatter:ff
                        .given()
                        .header("current-user", currentSubject.currentUser)
                        .header("assumed-roles", currentSubject.assumedRole)
                        .port(port)
                        .when()
                        .get("http://localhost/api/rbac/grants/%s/%s".formatted(
                                grantedRole.getUuid(), granteeUser.getUuid()
                        ))
                        .then().log().all();
                // @formatter:on
            }
        }
    }

    private void assumeGrantExists(final Subject grantingSubject, final String expectedGrant) {
        assumeThat(findAllGrantsOf(grantingSubject))
                .extracting(RbacGrantEntity::toDisplay)
                .contains(expectedGrant);
    }

    List<RbacGrantEntity> findAllGrantsOf(final Subject grantingSubject) {
        return jpaAttempt.transacted(() -> {
            context(grantingSubject.currentUser, null);
            return rbacGrantRepository.findAll();
        }).returnedValue();
    }

    RbacUserEntity createRBacUser() {
        return jpaAttempt.transacted(() -> {
            final String newUserName = "test-user-" + RandomStringUtils.randomAlphabetic(8) + "@example.com";
            context(null);
            return rbacUserRepository.create(new RbacUserEntity(UUID.randomUUID(), newUserName));
        }).returnedValue();
    }

    RbacUserEntity findRbacUserByName(final String userName) {
        return jpaAttempt.transacted(() -> {
            context("mike@example.org", null);
            return rbacUserRepository.findByName(userName);
        }).returnedValue();
    }

    RbacRoleEntity findRbacRoleByName(final String roleName) {
        return jpaAttempt.transacted(() -> {
            context("mike@example.org", null);
            return rbacRoleRepository.findByRoleName(roleName);
        }).returnedValue();
    }
}
