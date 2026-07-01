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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

import static java.util.UUID.randomUUID;
import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;

@Transactional(readOnly = true, propagation = Propagation.NEVER)
@Tag("generalIntegrationTest")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = HsadminNgApplication.class)
@ActiveProfiles("fake-jwt")
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
                    .header("Authorization", bearer("hsh-alex_superuser"))
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
                                hasEntry("granteeSubjectName", "tst-customer_admin_xxx")
                            )
                    ))
                    .body("", hasItem(
                            allOf(
                                    // TODO: should there be a grantedByRole or just a grantedByTrigger?
                                    hasEntry("grantedByRoleIdName", "rbactest.customer#yyy:OWNER"),
                                    hasEntry("grantedRoleIdName", "rbactest.customer#yyy:ADMIN"),
                                    hasEntry("granteeSubjectName", "tst-customer_admin_yyy")
                            )
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("grantedByRoleIdName", "rbac.global#global:ADMIN"),
                                    hasEntry("grantedRoleIdName", "rbac.global#global:ADMIN"),
                                    hasEntry("granteeSubjectName", "hsh-fran_superuser")
                            )
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("grantedByRoleIdName", "rbactest.customer#xxx:ADMIN"),
                                    hasEntry("grantedRoleIdName", "rbactest.package#xxx00:ADMIN"),
                                    hasEntry("granteeSubjectName", "tst-pac_admin_xxx00")
                            )
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("grantedByRoleIdName", "rbactest.customer#zzz:ADMIN"),
                                    hasEntry("grantedRoleIdName", "rbactest.package#zzz02:ADMIN"),
                                    hasEntry("granteeSubjectName", "tst-pac_admin_zzz02")
                            )
                    ))
                    .body("size()", greaterThanOrEqualTo(14));
                // @formatter:on
        }

        @Test
        void globalAdmin_withAssumedPackageAdminRole_canViewPacketRelatedGrants() {
            RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("hsh-alex_superuser"))
                    .header("Hostsharing-Assumed-Roles", "rbactest.package#yyy00:ADMIN")
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
                                    hasEntry("granteeSubjectName", "tst-pac_admin_yyy00")
                            )
                    ))
                    .body("size()", is(1));
                // @formatter:on
        }

        @Test
        void packageAdmin_withoutAssumedRole_canViewPacketRelatedGrants() {
            RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("tst-pac_admin_yyy00"))
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
                                    hasEntry("granteeSubjectName", "tst-pac_admin_yyy00")
                            )
                    ))
                    .body("[0].grantedByRoleIdName", is("rbactest.customer#yyy:ADMIN"))
                    .body("[0].grantedRoleIdName", is("rbactest.package#yyy00:ADMIN"))
                    .body("[0].granteeSubjectName", is("tst-pac_admin_yyy00"));
            // @formatter:on
        }
    }

    @Nested
    class GetListOfGrantsByUuid {

        @Test
        void customerAdmin_withAssumedPacketAdminRole_canReadPacketAdminsGrantById() {
            // given
            final var givencurrentSubjectAsPackageAdmin = new Subject("tst-customer_admin_xxx");
            final var givenGranteeUser = findRbacSubjectByName("tst-pac_admin_xxx00");
            final var givenGrantedRole = getRbacRoleByIdName("rbactest.package#xxx00:ADMIN");

            // when
            final var grant = givencurrentSubjectAsPackageAdmin.getListOfGrantsByUuid()
                    .forGrantedRole(givenGrantedRole).toGranteeUser(givenGranteeUser);

            // then
            grant.assertThat()
                    .statusCode(200)
                    .body("grantedByRoleIdName", is("rbactest.customer#xxx:ADMIN"))
                    .body("grantedRoleIdName", is("rbactest.package#xxx00:ADMIN"))
                    .body("granteeSubjectName", is("tst-pac_admin_xxx00"));
        }

        @Test
        void packageAdmin_withoutAssumedRole_canReadItsOwnGrantById() {
            // given
            final var givencurrentSubjectAsPackageAdmin = new Subject("tst-pac_admin_xxx00");
            final var givenGranteeUser = findRbacSubjectByName("tst-pac_admin_xxx00");
            final var givenGrantedRole = getRbacRoleByIdName("rbactest.package#xxx00:ADMIN");

            // when
            final var grant = givencurrentSubjectAsPackageAdmin.getListOfGrantsByUuid()
                    .forGrantedRole(givenGrantedRole).toGranteeUser(givenGranteeUser);

            // then
            grant.assertThat()
                    .statusCode(200)
                    .body("grantedByRoleIdName", is("rbactest.customer#xxx:ADMIN"))
                    .body("grantedRoleIdName", is("rbactest.package#xxx00:ADMIN"))
                    .body("granteeSubjectName", is("tst-pac_admin_xxx00"));
        }

        @Test
        void packageAdmin_withAssumedPackageAdmin_canStillReadItsOwnGrantById() {
            // given
            final var givencurrentSubjectAsPackageAdmin = new Subject(
                    "tst-pac_admin_xxx00",
                    "rbactest.package#xxx00:ADMIN");
            final var givenGranteeUser = findRbacSubjectByName("tst-pac_admin_xxx00");
            final var givenGrantedRole = getRbacRoleByIdName("rbactest.package#xxx00:ADMIN");

            // when
            final var grant = givencurrentSubjectAsPackageAdmin.getListOfGrantsByUuid()
                    .forGrantedRole(givenGrantedRole).toGranteeUser(givenGranteeUser);

            // then
            grant.assertThat()
                    .statusCode(200)
                    .body("grantedByRoleIdName", is("rbactest.customer#xxx:ADMIN"))
                    .body("grantedRoleIdName", is("rbactest.package#xxx00:ADMIN"))
                    .body("granteeSubjectName", is("tst-pac_admin_xxx00"));
        }

        @Test
        void packageAdmin_withAssumedPackageTenantRole_canNotReadItsOwnGrantByIdAnymore() {

            // given
            final var givencurrentSubjectAsPackageAdmin = new Subject(
                    "tst-pac_admin_xxx00",
                    "rbactest.package#xxx00:TENANT");
            final var givenGranteeUser = findRbacSubjectByName("tst-pac_admin_xxx00");
            final var givenGrantedRole = getRbacRoleByIdName("rbactest.package#xxx00:ADMIN");
            final var grant = givencurrentSubjectAsPackageAdmin.getListOfGrantsByUuid()
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
            final var givencurrentSubjectAsPackageAdmin = new Subject("tst-pac_admin_xxx00", givenRoleToGrant);
            final var givenOwnPackageAdminRole =
                    getRbacRoleByIdName(givencurrentSubjectAsPackageAdmin.assumedRole);

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
                    .contains("{ grant role:" + givenOwnPackageAdminRole.getRoleIdName() +
                            " to user:" + givenNewUser.getName() +
                            " by role:" + givenRoleToGrant + " and assume }");
        }

        @Test
        void packageAdmin_canNotGrantAlienPackageAdminRole_toArbitraryUser() {

            // given
            final var givenNewUser = createRbacSubject();
            final var givenRoleToGrant = "rbactest.package#xxx00:ADMIN";
            final var givencurrentSubjectAsPackageAdmin = new Subject("tst-pac_admin_xxx00", givenRoleToGrant);
            final var givenAlienPackageAdminRole = getRbacRoleByIdName("rbactest.package#yyy00:ADMIN");

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
            final var givenCurrentSubjectAsPackageAdmin = new Subject("tst-pac_admin_xxx00", givenRoleToGrant);
            final var givenOwnPackageAdminRole = getRbacRoleByIdName("rbactest.package#xxx00:ADMIN");

            // and given an existing grant
            assumeCreated(givenCurrentSubjectAsPackageAdmin
                    .grantsRole(givenOwnPackageAdminRole).assumed()
                    .toUser(givenArbitraryUser));
            assumeGrantExists(
                    givenCurrentSubjectAsPackageAdmin,
                    "{ grant role:%s to user:%s by role:%s and assume }".formatted(
                            givenOwnPackageAdminRole.getRoleIdName(),
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

        GetListOfGrantsByUuidFixture getListOfGrantsByUuid() {
            return new GetListOfGrantsByUuidFixture();
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
                            .header("Authorization", bearer(grantingSubject.currentSubject))
                            .header("Hostsharing-Assumed-Roles", grantingSubject.assumedRole)
                            .contentType(ContentType.JSON)
                            .body("""
                                    {
                                      "assumed": true,
                                      "grantedRole.uuid": "%s",
                                      "granteeSubject.uuid": "%s"
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
                        .header("Authorization", bearer(currentSubject.currentSubject))
                        .header("Hostsharing-Assumed-Roles", currentSubject.assumedRole)
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                  "assumed": true,
                                  "grantedRole.uuid": "%s",
                                  "granteeSubject.uuid": "%s"
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

        private class GetListOfGrantsByUuidFixture {

            private Subject currentSubject = Subject.this;
            private RbacRoleEntity grantedRole;

            GetListOfGrantsByUuidFixture forGrantedRole(final RbacRoleEntity grantedRole) {
                this.grantedRole = grantedRole;
                return this;
            }

            ValidatableResponse toGranteeUser(final RbacSubjectEntity granteeUser) {

                return RestAssured // @formatter:ff
                        .given()
                        .header("Authorization", bearer(currentSubject.currentSubject))
                        .header("Hostsharing-Assumed-Roles", currentSubject.assumedRole)
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
            final String newUserName = "tst-user_" + RandomStringUtils.randomAlphabetic(8).toLowerCase();
            context(null);
            return rbacSubjectRepository.create(
                    RbacSubjectEntity.builder().uuid(randomUUID()).name(newUserName).build());
        }).returnedValue();
    }

    RbacSubjectEntity findRbacSubjectByName(final String userName) {
        return jpaAttempt.transacted(() -> {
            context("hsh-alex_superuser", null);
            return rbacSubjectRepository.findByName(userName);
        }).assertNotNull().returnedValue();
    }

    RbacRoleEntity getRbacRoleByIdName(final String roleIdName) {
        return jpaAttempt.transacted(() -> {
            context("hsh-alex_superuser", null);
            return rbacRoleRepository.findByRoleIdName(roleIdName).getFirst();
        }).assertNotNull().returnedValue();
    }
}
