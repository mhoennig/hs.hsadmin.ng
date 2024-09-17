package net.hostsharing.hsadminng.rbac.grant;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.role.RbacRoleEntity;
import net.hostsharing.hsadminng.rbac.role.RbacRoleRepository;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectEntity;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectRepository;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class }
)
@Transactional(readOnly = true, propagation = Propagation.NEVER)
class RbacGrantControllerAcceptanceTest extends ContextBasedTest {

    @LocalServerPort
    Integer port;

    @PersistenceContext
    EntityManager em;

    @Autowired
    RbacSubjectRepository rbacSubjectRepository;

    @Autowired
    RbacRoleRepository rbacRoleRepository;

    @Autowired
    RbacGrantRepository rbacGrantRepository;

    @Autowired
    JpaAttempt jpaAttempt;

    @Nested
    class ListGrants {

        @Test
        void globalAdmin_withoutAssumedRole_canViewAllGrants() {
            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/grants")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasItem(
                            allOf(
                                // TODO: should there be a grantedByRole or just a grantedByTrigger?
                                hasEntry("grantedByRoleIdName", "rbactest.customer#xxx:OWNER"),
                                hasEntry("grantedRoleIdName", "rbactest.customer#xxx:ADMIN"),
                                hasEntry("granteeSubjectName", "customer-admin@xxx.example.com")
                            )
                    ))
                    .body("", hasItem(
                            allOf(
                                    // TODO: should there be a grantedByRole or just a grantedByTrigger?
                                    hasEntry("grantedByRoleIdName", "rbactest.customer#yyy:OWNER"),
                                    hasEntry("grantedRoleIdName", "rbactest.customer#yyy:ADMIN"),
                                    hasEntry("granteeSubjectName", "customer-admin@yyy.example.com")
                            )
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("grantedByRoleIdName", "rbac.global#global:ADMIN"),
                                    hasEntry("grantedRoleIdName", "rbac.global#global:ADMIN"),
                                    hasEntry("granteeSubjectName", "superuser-fran@hostsharing.net")
                            )
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("grantedByRoleIdName", "rbactest.customer#xxx:ADMIN"),
                                    hasEntry("grantedRoleIdName", "rbactest.package#xxx00:ADMIN"),
                                    hasEntry("granteeSubjectName", "pac-admin-xxx00@xxx.example.com")
                            )
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("grantedByRoleIdName", "rbactest.customer#zzz:ADMIN"),
                                    hasEntry("grantedRoleIdName", "rbactest.package#zzz02:ADMIN"),
                                    hasEntry("granteeSubjectName", "pac-admin-zzz02@zzz.example.com")
                            )
                    ))
                    .body("size()", greaterThanOrEqualTo(14));
                // @formatter:on
        }

        @Test
        void globalAdmin_withAssumedPackageAdminRole_canViewPacketRelatedGrants() {
            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "rbactest.package#yyy00:ADMIN")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/grants")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasItem(
                            allOf(
                                    hasEntry("grantedByRoleIdName", "rbactest.customer#yyy:ADMIN"),
                                    hasEntry("grantedRoleIdName", "rbactest.package#yyy00:ADMIN"),
                                    hasEntry("granteeSubjectName", "pac-admin-yyy00@yyy.example.com")
                            )
                    ))
                    .body("size()", is(1));
                // @formatter:on
        }

        @Test
        void packageAdmin_withoutAssumedRole_canViewPacketRelatedGrants() {
            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "pac-admin-yyy00@yyy.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/grants")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasItem(
                            allOf(
                                    hasEntry("grantedByRoleIdName", "rbactest.customer#yyy:ADMIN"),
                                    hasEntry("grantedRoleIdName", "rbactest.package#yyy00:ADMIN"),
                                    hasEntry("granteeSubjectName", "pac-admin-yyy00@yyy.example.com")
                            )
                    ))
                    .body("[0].grantedByRoleIdName", is("rbactest.customer#yyy:ADMIN"))
                    .body("[0].grantedRoleIdName", is("rbactest.package#yyy00:ADMIN"))
                    .body("[0].granteeSubjectName", is("pac-admin-yyy00@yyy.example.com"));
            // @formatter:on
        }
    }

    @Nested
    class GetGrantById {

        @Test
        void customerAdmin_withAssumedPacketAdminRole_canReadPacketAdminsGrantById() {
            // given
            final var givencurrentSubjectAsPackageAdmin = new Subject("customer-admin@xxx.example.com");
            final var givenGranteeUser = findRbacSubjectByName("pac-admin-xxx00@xxx.example.com");
            final var givenGrantedRole = getRbacRoleByName("rbactest.package#xxx00:ADMIN");

            // when
            final var grant = givencurrentSubjectAsPackageAdmin.getGrantById()
                    .forGrantedRole(givenGrantedRole).toGranteeUser(givenGranteeUser);

            // then
            grant.assertThat()
                    .statusCode(200)
                    .body("grantedByRoleIdName", is("rbactest.customer#xxx:ADMIN"))
                    .body("grantedRoleIdName", is("rbactest.package#xxx00:ADMIN"))
                    .body("granteeSubjectName", is("pac-admin-xxx00@xxx.example.com"));
        }

        @Test
        void packageAdmin_withoutAssumedRole_canReadItsOwnGrantById() {
            // given
            final var givencurrentSubjectAsPackageAdmin = new Subject("pac-admin-xxx00@xxx.example.com");
            final var givenGranteeUser = findRbacSubjectByName("pac-admin-xxx00@xxx.example.com");
            final var givenGrantedRole = getRbacRoleByName("rbactest.package#xxx00:ADMIN");

            // when
            final var grant = givencurrentSubjectAsPackageAdmin.getGrantById()
                    .forGrantedRole(givenGrantedRole).toGranteeUser(givenGranteeUser);

            // then
            grant.assertThat()
                    .statusCode(200)
                    .body("grantedByRoleIdName", is("rbactest.customer#xxx:ADMIN"))
                    .body("grantedRoleIdName", is("rbactest.package#xxx00:ADMIN"))
                    .body("granteeSubjectName", is("pac-admin-xxx00@xxx.example.com"));
        }

        @Test
        void packageAdmin_withAssumedPackageAdmin_canStillReadItsOwnGrantById() {
            // given
            final var givencurrentSubjectAsPackageAdmin = new Subject(
                    "pac-admin-xxx00@xxx.example.com",
                    "rbactest.package#xxx00:ADMIN");
            final var givenGranteeUser = findRbacSubjectByName("pac-admin-xxx00@xxx.example.com");
            final var givenGrantedRole = getRbacRoleByName("rbactest.package#xxx00:ADMIN");

            // when
            final var grant = givencurrentSubjectAsPackageAdmin.getGrantById()
                    .forGrantedRole(givenGrantedRole).toGranteeUser(givenGranteeUser);

            // then
            grant.assertThat()
                    .statusCode(200)
                    .body("grantedByRoleIdName", is("rbactest.customer#xxx:ADMIN"))
                    .body("grantedRoleIdName", is("rbactest.package#xxx00:ADMIN"))
                    .body("granteeSubjectName", is("pac-admin-xxx00@xxx.example.com"));
        }

        @Test
        void packageAdmin_withAssumedPackageTenantRole_canNotReadItsOwnGrantByIdAnymore() {

            // given
            final var givencurrentSubjectAsPackageAdmin = new Subject(
                    "pac-admin-xxx00@xxx.example.com",
                    "rbactest.package#xxx00:TENANT");
            final var givenGranteeUser = findRbacSubjectByName("pac-admin-xxx00@xxx.example.com");
            final var givenGrantedRole = getRbacRoleByName("rbactest.package#xxx00:ADMIN");
            final var grant = givencurrentSubjectAsPackageAdmin.getGrantById()
                    .forGrantedRole(givenGrantedRole).toGranteeUser(givenGranteeUser);

            // then
            grant.assertThat()
                    .statusCode(404);
        }
    }

    @Nested
    class GrantRoleToSubject {

        @Test
        void packageAdmin_canGrantOwnPackageAdminRole_toArbitraryUser() {

            // given
            final var givenNewUser = createRbacSubject();
            final var givenRoleToGrant = "rbactest.package#xxx00:ADMIN";
            final var givencurrentSubjectAsPackageAdmin = new Subject("pac-admin-xxx00@xxx.example.com", givenRoleToGrant);
            final var givenOwnPackageAdminRole =
                    getRbacRoleByName(givencurrentSubjectAsPackageAdmin.assumedRole);

            // when
            final var response = givencurrentSubjectAsPackageAdmin
                    .grantsRole(givenOwnPackageAdminRole).assumed()
                    .toUser(givenNewUser);

            // then
            response.assertThat()
                    .statusCode(201)
                    .body("grantedByRoleIdName", is("rbactest.package#xxx00:ADMIN"))
                    .body("assumed", is(true))
                    .body("grantedRoleIdName", is("rbactest.package#xxx00:ADMIN"))
                    .body("granteeSubjectName", is(givenNewUser.getName()));
            assertThat(findAllGrantsOf(givencurrentSubjectAsPackageAdmin))
                    .extracting(RbacGrantEntity::toDisplay)
                    .contains("{ grant role:" + givenOwnPackageAdminRole.getRoleName() +
                            " to user:" + givenNewUser.getName() +
                            " by role:" + givenRoleToGrant + " and assume }");
        }

        @Test
        void packageAdmin_canNotGrantAlienPackageAdminRole_toArbitraryUser() {

            // given
            final var givenNewUser = createRbacSubject();
            final var givenRoleToGrant = "rbactest.package#xxx00:ADMIN";
            final var givencurrentSubjectAsPackageAdmin = new Subject("pac-admin-xxx00@xxx.example.com", givenRoleToGrant);
            final var givenAlienPackageAdminRole = getRbacRoleByName("rbactest.package#yyy00:ADMIN");

            // when
            final var result = givencurrentSubjectAsPackageAdmin
                    .grantsRole(givenAlienPackageAdminRole).assumed()
                    .toUser(givenNewUser);

            // then
            result.assertThat()
                    .statusCode(403)
                    .body("message", containsString("Access to granted role"))
                    .body("message", containsString("forbidden for rbactest.package#xxx00:ADMIN"));
            assertThat(findAllGrantsOf(givencurrentSubjectAsPackageAdmin))
                    .extracting(RbacGrantEntity::getGranteeSubjectName)
                    .doesNotContain(givenNewUser.getName());
        }
    }

    @Nested
    class RevokeRoleFromSubject {

        @Test
        @Transactional(propagation = Propagation.NEVER)
        void packageAdmin_canRevokePackageAdminRole_grantedByPackageAdmin_fromArbitraryUser() {

            // given
            final var givenArbitraryUser = createRbacSubject();
            final var givenRoleToGrant = "rbactest.package#xxx00:ADMIN";
            final var givenCurrentSubjectAsPackageAdmin = new Subject("pac-admin-xxx00@xxx.example.com", givenRoleToGrant);
            final var givenOwnPackageAdminRole = getRbacRoleByName("rbactest.package#xxx00:ADMIN");

            // and given an existing grant
            assumeCreated(givenCurrentSubjectAsPackageAdmin
                    .grantsRole(givenOwnPackageAdminRole).assumed()
                    .toUser(givenArbitraryUser));
            assumeGrantExists(
                    givenCurrentSubjectAsPackageAdmin,
                    "{ grant role:%s to user:%s by role:%s and assume }".formatted(
                            givenOwnPackageAdminRole.getRoleName(),
                            givenArbitraryUser.getName(),
                            givenCurrentSubjectAsPackageAdmin.assumedRole));

            // when
            final var revokeResponse = givenCurrentSubjectAsPackageAdmin
                    .revokesRole(givenOwnPackageAdminRole)
                    .fromUser(givenArbitraryUser);

            // then
            revokeResponse.assertThat().statusCode(204);
            assertThat(findAllGrantsOf(givenCurrentSubjectAsPackageAdmin))
                    .extracting(RbacGrantEntity::getGranteeSubjectName)
                    .doesNotContain(givenArbitraryUser.getName());
        }
    }

    private void assumeCreated(final ValidatableResponse response) {
        assertThat(response.extract().response().statusCode()).isEqualTo(201);
    }

    class Subject {

        final String currentSubject;
        final String assumedRole;

        public Subject(final String currentSubject, final String assumedRole) {
            this.currentSubject = currentSubject;
            this.assumedRole = assumedRole;
        }

        public Subject(final String currentSubject) {
            this(currentSubject, "");
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
            private RbacSubjectEntity granteeUser;

            public GrantFixture(final RbacRoleEntity roleToGrant) {
                this.grantedRole = roleToGrant;
            }

            GrantFixture assumed() {
                this.assumed = true;
                return this;
            }

            ValidatableResponse toUser(final RbacSubjectEntity granteeUser) {
                this.granteeUser = granteeUser;

                return RestAssured // @formatter:ff
                        .given()
                        .header("current-subject", grantingSubject.currentSubject)
                        .header("assumed-roles", grantingSubject.assumedRole)
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                  "assumed": true,
                                  "grantedRoleUuid": "%s",
                                  "granteeSubjectUuid": "%s"
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
            private RbacSubjectEntity granteeUser;

            public RevokeFixture(final RbacRoleEntity roleToGrant) {
                this.grantedRole = roleToGrant;
            }

            ValidatableResponse fromUser(final RbacSubjectEntity granteeUser) {
                this.granteeUser = granteeUser;

                return RestAssured // @formatter:ff
                        .given()
                        .header("current-subject", currentSubject.currentSubject)
                        .header("assumed-roles", currentSubject.assumedRole)
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                  "assumed": true,
                                  "grantedRoleUuid": "%s",
                                  "granteeSubjectUuid": "%s"
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

            ValidatableResponse toGranteeUser(final RbacSubjectEntity granteeUser) {

                return RestAssured // @formatter:ff
                        .given()
                        .header("current-subject", currentSubject.currentSubject)
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
        assertThat(findAllGrantsOf(grantingSubject))
                .extracting(RbacGrantEntity::toDisplay)
                .contains(expectedGrant);
    }

    List<RbacGrantEntity> findAllGrantsOf(final Subject grantingSubject) {
        return jpaAttempt.transacted(() -> {
            context(grantingSubject.currentSubject, null);
            return rbacGrantRepository.findAll();
        }).returnedValue();
    }

    RbacSubjectEntity createRbacSubject() {
        return jpaAttempt.transacted(() -> {
            final String newUserName = "test-user-" + RandomStringUtils.randomAlphabetic(8) + "@example.com";
            context(null);
            return rbacSubjectRepository.create(new RbacSubjectEntity(UUID.randomUUID(), newUserName));
        }).returnedValue();
    }

    RbacSubjectEntity findRbacSubjectByName(final String userName) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net", null);
            return rbacSubjectRepository.findByName(userName);
        }).assertNotNull().returnedValue();
    }

    RbacRoleEntity getRbacRoleByName(final String roleName) {
        return jpaAttempt.transacted(() -> {
            context("superuser-alex@hostsharing.net", null);
            return rbacRoleRepository.findByRoleName(roleName);
        }).assertNotNull().returnedValue();
    }
}
