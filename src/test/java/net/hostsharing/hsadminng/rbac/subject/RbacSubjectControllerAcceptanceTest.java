package net.hostsharing.hsadminng.rbac.subject;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, DisableSecurityConfig.class, JpaAttempt.class }
)
@ActiveProfiles("test")
@Transactional
class RbacSubjectControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    JpaAttempt jpaAttempt;

    @Autowired
    Context context;

    @Autowired
    RbacSubjectRepository rbacSubjectRepository;

    @Nested
    class CreateRbacSubject {

        @Test
        void anybody_canCreateANewUser() {

            // @formatter:off
            final var location = RestAssured
                    .given()
                        .contentType(ContentType.JSON)
                        .body("""
                              {
                                "name": "new-user@example.com"
                              }
                              """)
                        .port(port)
                    .when()
                        .post("http://localhost/api/rbac/subjects")
                    .then().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("name", is("new-user@example.com"))
                        .header("Location", startsWith("http://localhost"))
                        .extract().header("Location");
            // @formatter:on

            // finally, the user can view its own record
            final var newSubjectUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            context.define("new-user@example.com");
            assertThat(rbacSubjectRepository.findByUuid(newSubjectUuid))
                    .extracting(RbacSubjectEntity::getName).isEqualTo("new-user@example.com");
        }
    }

    @Nested
    class GetRbacSubject {

        @Test
        void globalAdmin_withoutAssumedRole_canGetArbitraryUser() {
            final var givenUser = findRbacSubjectByName("pac-admin-xxx00@xxx.example.com");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects/" + givenUser.getUuid())
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("name", is("pac-admin-xxx00@xxx.example.com"));
            // @formatter:on
        }

        @Test
        void globalAdmin_withAssumedCustomerAdminRole_canGetUserWithinInItsRealm() {
            final var givenUser = findRbacSubjectByName("pac-admin-yyy00@yyy.example.com");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "rbactest.customer#yyy:ADMIN")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects/" + givenUser.getUuid())
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("name", is("pac-admin-yyy00@yyy.example.com"));
            // @formatter:on
        }

        @Test
        void customerAdmin_withoutAssumedRole_canGetUserWithinInItsRealm() {
            final var givenUser = findRbacSubjectByName("pac-admin-yyy00@yyy.example.com");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-subject", "customer-admin@yyy.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects/" + givenUser.getUuid())
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("name", is("pac-admin-yyy00@yyy.example.com"));
            // @formatter:on
        }

        @Test
        void customerAdmin_withoutAssumedRole_canNotGetUserOutsideOfItsRealm() {
            final var givenUser = findRbacSubjectByName("pac-admin-yyy00@yyy.example.com");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-subject", "customer-admin@xxx.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects/" + givenUser.getUuid())
                .then().log().body().assertThat()
                    .statusCode(404);
            // @formatter:on
        }
    }

    @Nested
    class ListRbacSubjects {

        @Test
        void globalAdmin_withoutAssumedRole_canViewAllUsers() {

            // @formatter:off
            RestAssured
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects")
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasItem(hasEntry("name", "customer-admin@xxx.example.com")))
                    .body("", hasItem(hasEntry("name", "customer-admin@yyy.example.com")))
                    .body("", hasItem(hasEntry("name", "customer-admin@zzz.example.com")))
                    .body("", hasItem(hasEntry("name", "superuser-alex@hostsharing.net")))
                    // ...
                    .body("", hasItem(hasEntry("name", "pac-admin-zzz01@zzz.example.com")))
                    .body("", hasItem(hasEntry("name", "pac-admin-zzz02@zzz.example.com")))
                    .body("", hasItem(hasEntry("name", "superuser-fran@hostsharing.net")))
                    .body("size()", greaterThanOrEqualTo(14));
            // @formatter:on
        }

        @Test
        void globalAdmin_withoutAssumedRole_canViewAllUsersByName() {

            // @formatter:off
            RestAssured
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects?name=pac-admin-zzz0")
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].name", is("pac-admin-zzz00@zzz.example.com"))
                    .body("[1].name", is("pac-admin-zzz01@zzz.example.com"))
                    .body("[2].name", is("pac-admin-zzz02@zzz.example.com"))
                    .body("size()", is(3));
            // @formatter:on
        }

        @Test
        void globalAdmin_withAssumedCustomerAdminRole_canViewUsersInItsRealm() {

            // @formatter:off
            RestAssured
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "rbactest.customer#yyy:ADMIN")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].name", is("customer-admin@yyy.example.com"))
                    .body("[1].name", is("pac-admin-yyy00@yyy.example.com"))
                    .body("[2].name", is("pac-admin-yyy01@yyy.example.com"))
                    .body("[3].name", is("pac-admin-yyy02@yyy.example.com"))
                    .body("size()", is(4));
            // @formatter:on
        }

        @Test
        void customerAdmin_withoutAssumedRole_canViewUsersInItsRealm() {

            // @formatter:off
            RestAssured
                .given()
                    .header("current-subject", "customer-admin@yyy.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].name", is("customer-admin@yyy.example.com"))
                    .body("[1].name", is("pac-admin-yyy00@yyy.example.com"))
                    .body("[2].name", is("pac-admin-yyy01@yyy.example.com"))
                    .body("[3].name", is("pac-admin-yyy02@yyy.example.com"))
                    .body("size()", is(4));
            // @formatter:on
        }

        @Test
        void packetAdmin_withoutAssumedRole_canViewAllUsersOfItsPackage() {

            // @formatter:off
            RestAssured
                .given()
                    .header("current-subject", "pac-admin-xxx01@xxx.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].name", is("pac-admin-xxx01@xxx.example.com"))
                    .body("size()", is(1));
            // @formatter:on
        }
    }

    @Nested
    class ListRbacSubjectPermissions {

        @Test
        void globalAdmin_withoutAssumedRole_canViewArbitraryUsersPermissions() {
            final var givenUser = findRbacSubjectByName("pac-admin-yyy00@yyy.example.com");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects/" + givenUser.getUuid() + "/permissions")
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasItem(
                            allOf(
                                    hasEntry("roleName", "rbactest.customer#yyy:TENANT"),
                                    hasEntry("op", "SELECT"))
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("roleName", "rbactest.domain#yyy00-aaaa:OWNER"),
                                    hasEntry("op", "DELETE"))
                    ))
                    // actual content tested in integration test, so this is enough for here:
                    .body("size()", greaterThanOrEqualTo(6));
            // @formatter:on
        }

        @Test
        void globalAdmin_withAssumedCustomerAdminRole_canViewArbitraryUsersPermissions() {
            final var givenUser = findRbacSubjectByName("pac-admin-yyy00@yyy.example.com");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "rbactest.customer#yyy:ADMIN")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects/" + givenUser.getUuid() + "/permissions")
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasItem(
                            allOf(
                                    hasEntry("roleName", "rbactest.customer#yyy:TENANT"),
                                    hasEntry("op", "SELECT"))
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("roleName", "rbactest.domain#yyy00-aaaa:OWNER"),
                                    hasEntry("op", "DELETE"))
                    ))
                    // actual content tested in integration test, so this is enough for here:
                    .body("size()", greaterThanOrEqualTo(6));
            // @formatter:on
        }

        @Test
        void packageAdmin_withoutAssumedRole_canViewPermissionsOfUsersInItsRealm() {
            final var givenUser = findRbacSubjectByName("pac-admin-yyy00@yyy.example.com");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-subject", "pac-admin-yyy00@yyy.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects/" + givenUser.getUuid() + "/permissions")
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasItem(
                            allOf(
                                    hasEntry("roleName", "rbactest.customer#yyy:TENANT"),
                                    hasEntry("op", "SELECT"))
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("roleName", "rbactest.domain#yyy00-aaaa:OWNER"),
                                    hasEntry("op", "DELETE"))
                    ))
                    // actual content tested in integration test, so this is enough for here:
                    .body("size()", greaterThanOrEqualTo(6));
            // @formatter:on
        }

        @Test
        void packageAdmin_canViewPermissionsOfUsersOutsideOfItsRealm() {
            final var givenUser = findRbacSubjectByName("pac-admin-xxx00@xxx.example.com");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-subject", "pac-admin-yyy00@yyy.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects/" + givenUser.getUuid() + "/permissions")
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("size()", is(0));
            // @formatter:on
        }
    }

    @Nested
    class DeleteRbacSubject {

        @Test
        void anybody_canDeleteTheirOwnUser() {

            // given
            final var givenUser = givenANewUser();

            // @formatter:off
            final var location = RestAssured
                    .given()
                        .header("current-subject", givenUser.getName())
                        .port(port)
                    .when()
                        .delete("http://localhost/api/rbac/subjects/" + givenUser.getUuid())
                    .then().log().all().assertThat()
                        .statusCode(204);
            // @formatter:on

            // finally, the user is actually deleted
            assertThat(rbacSubjectRepository.findByName(givenUser.getName())).isNull();
        }

        @Test
        void customerAdmin_canNotDeleteOtherUser() {

            // given
            final var givenUser = givenANewUser();

            // @formatter:off
            final var location = RestAssured
                    .given()
                        .header("current-subject", "customer-admin@xxx.example.com")
                        .port(port)
                    .when()
                        .delete("http://localhost/api/rbac/subjects/" + givenUser.getUuid())
                    .then().log().all().assertThat()
                        // that user cannot even see other users, thus the system won't even try to delete
                        .statusCode(204);
            // @formatter:on

            // finally, the user is still there
            assertThat(rbacSubjectRepository.findByName(givenUser.getName())).isNotNull();
        }

        @Test
        void globalAdmin_canDeleteArbitraryUser() {

            // given
            final var givenUser = givenANewUser();

            // @formatter:off
            final var location = RestAssured
                    .given()
                        .header("current-subject", "superuser-alex@hostsharing.net")
                        .port(port)
                    .when()
                        .delete("http://localhost/api/rbac/subjects/" + givenUser.getUuid())
                    .then().log().all().assertThat()
                        .statusCode(204);
            // @formatter:on

            // finally, the user is actually deleted
            assertThat(rbacSubjectRepository.findByName(givenUser.getName())).isNull();
        }
    }

    RbacSubjectEntity findRbacSubjectByName(final String userName) {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            return rbacSubjectRepository.findByName(userName);
        }).returnedValue();
    }

    RbacSubjectEntity givenANewUser() {
        final var givenUserName = "test-user-" + System.currentTimeMillis() + "@example.com";
        final var givenUser = jpaAttempt.transacted(() -> {
            context.define(null);
            return rbacSubjectRepository.create(new RbacSubjectEntity(UUID.randomUUID(), givenUserName));
        }).assumeSuccessful().returnedValue();
        assertThat(rbacSubjectRepository.findByName(givenUser.getName())).isNotNull();
        return givenUser;
    }

}
