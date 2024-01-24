package net.hostsharing.hsadminng.rbac.rbacuser;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.test.Accepts;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class }
)
@Transactional
class RbacUserControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    JpaAttempt jpaAttempt;

    @Autowired
    Context context;

    @Autowired
    RbacUserRepository rbacUserRepository;

    @Nested
    class CreateRbacUser {

        @Test
        @Accepts({ "USR:C(Create)", "USR:X(Access Control)" })
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
                        .post("http://localhost/api/rbac/users")
                    .then().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("name", is("new-user@example.com"))
                        .header("Location", startsWith("http://localhost"))
                        .extract().header("Location");
            // @formatter:on

            // finally, the user can view its own record
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            context.define("new-user@example.com");
            assertThat(rbacUserRepository.findByUuid(newUserUuid))
                    .extracting(RbacUserEntity::getName).isEqualTo("new-user@example.com");
        }
    }

    @Nested
    class GetRbacUser {

        @Test
        @Accepts({ "USR:R(Read)" })
        void globalAdmin_withoutAssumedRole_canGetArbitraryUser() {
            final var givenUser = findRbacUserByName("pac-admin-xxx00@xxx.example.com");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/users/" + givenUser.getUuid())
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("name", is("pac-admin-xxx00@xxx.example.com"));
            // @formatter:on
        }

        @Test
        @Accepts({ "USR:R(Read)", "USR:X(Access Control)" })
        void globalAdmin_withAssumedCustomerAdminRole_canGetUserWithinInItsRealm() {
            final var givenUser = findRbacUserByName("pac-admin-yyy00@yyy.example.com");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "test_customer#yyy.admin")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/users/" + givenUser.getUuid())
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("name", is("pac-admin-yyy00@yyy.example.com"));
            // @formatter:on
        }

        @Test
        @Accepts({ "USR:R(Read)", "USR:X(Access Control)" })
        void customerAdmin_withoutAssumedRole_canGetUserWithinInItsRealm() {
            final var givenUser = findRbacUserByName("pac-admin-yyy00@yyy.example.com");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "customer-admin@yyy.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/users/" + givenUser.getUuid())
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("name", is("pac-admin-yyy00@yyy.example.com"));
            // @formatter:on
        }

        @Test
        @Accepts({ "USR:R(Read)", "USR:X(Access Control)" })
        void customerAdmin_withoutAssumedRole_canNotGetUserOutsideOfItsRealm() {
            final var givenUser = findRbacUserByName("pac-admin-yyy00@yyy.example.com");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "customer-admin@xxx.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/users/" + givenUser.getUuid())
                .then().log().body().assertThat()
                    .statusCode(404);
            // @formatter:on
        }
    }

    @Nested
    class ListRbacUsers {

        @Test
        @Accepts({ "USR:L(List)" })
        void globalAdmin_withoutAssumedRole_canViewAllUsers() {

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/users")
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
        @Accepts({ "USR:F(Filter)" })
        void globalAdmin_withoutAssumedRole_canViewAllUsersByName() {

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/users?name=pac-admin-zzz0")
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
        @Accepts({ "USR:L(List)", "USR:X(Access Control)" })
        void globalAdmin_withAssumedCustomerAdminRole_canViewUsersInItsRealm() {

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "test_customer#yyy.admin")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/users")
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
        @Accepts({ "USR:L(List)", "USR:X(Access Control)" })
        void customerAdmin_withoutAssumedRole_canViewUsersInItsRealm() {

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "customer-admin@yyy.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/users")
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
        @Accepts({ "USR:L(List)", "USR:X(Access Control)" })
        void packetAdmin_withoutAssumedRole_canViewAllUsersOfItsPackage() {

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "pac-admin-xxx01@xxx.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/users")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].name", is("pac-admin-xxx01@xxx.example.com"))
                    .body("size()", is(1));
            // @formatter:on
        }
    }

    @Nested
    class ListRbacUserPermissions {

        @Test
        @Accepts({ "PRM:L(List)" })
        void globalAdmin_withoutAssumedRole_canViewArbitraryUsersPermissions() {
            final var givenUser = findRbacUserByName("pac-admin-yyy00@yyy.example.com");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/users/" + givenUser.getUuid() + "/permissions")
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasItem(
                            allOf(
                                    hasEntry("roleName", "test_customer#yyy.tenant"),
                                    hasEntry("op", "view"))
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("roleName", "test_package#yyy00.admin"),
                                    hasEntry("op", "add-domain"))
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("roleName", "test_domain#yyy00-aaaa.owner"),
                                    hasEntry("op", "*"))
                    ))
                    .body("size()", is(7));
            // @formatter:on
        }

        @Test
        @Accepts({ "PRM:L(List)" })
        void globalAdmin_withAssumedCustomerAdminRole_canViewArbitraryUsersPermissions() {
            final var givenUser = findRbacUserByName("pac-admin-yyy00@yyy.example.com");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "test_package#yyy00.admin")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/users/" + givenUser.getUuid() + "/permissions")
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasItem(
                            allOf(
                                    hasEntry("roleName", "test_customer#yyy.tenant"),
                                    hasEntry("op", "view"))
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("roleName", "test_package#yyy00.admin"),
                                    hasEntry("op", "add-domain"))
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("roleName", "test_domain#yyy00-aaaa.owner"),
                                    hasEntry("op", "*"))
                    ))
                    .body("size()", is(7));
            // @formatter:on
        }

        @Test
        @Accepts({ "PRM:L(List)" })
        void packageAdmin_withoutAssumedRole_canViewPermissionsOfUsersInItsRealm() {
            final var givenUser = findRbacUserByName("pac-admin-yyy00@yyy.example.com");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "pac-admin-yyy00@yyy.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/users/" + givenUser.getUuid() + "/permissions")
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasItem(
                            allOf(
                                    hasEntry("roleName", "test_customer#yyy.tenant"),
                                    hasEntry("op", "view"))
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("roleName", "test_package#yyy00.admin"),
                                    hasEntry("op", "add-domain"))
                    ))
                    .body("", hasItem(
                            allOf(
                                    hasEntry("roleName", "test_domain#yyy00-aaaa.owner"),
                                    hasEntry("op", "*"))
                    ))
                    .body("size()", is(7));
            // @formatter:on
        }

        @Test
        @Accepts({ "PRM:L(List)" })
        void packageAdmin_canViewPermissionsOfUsersOutsideOfItsRealm() {
            final var givenUser = findRbacUserByName("pac-admin-xxx00@xxx.example.com");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "pac-admin-yyy00@yyy.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/users/" + givenUser.getUuid() + "/permissions")
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("size()", is(0));
            // @formatter:on
        }
    }

    @Nested
    class DeleteRbacUser {

        @Test
        @Accepts({ "USR:D(Create)" })
        void anybody_canDeleteTheirOwnUser() {

            // given
            final var givenUser = givenANewUser();

            // @formatter:off
            final var location = RestAssured
                    .given()
                        .header("current-user", givenUser.getName())
                        .port(port)
                    .when()
                        .delete("http://localhost/api/rbac/users/" + givenUser.getUuid())
                    .then().log().all().assertThat()
                        .statusCode(204);
            // @formatter:on

            // finally, the user is actually deleted
            assertThat(rbacUserRepository.findByName(givenUser.getName())).isNull();
        }

        @Test
        @Accepts({ "USR:D(Create)", "USR:X(Access Control)" })
        void customerAdmin_canNotDeleteOtherUser() {

            // given
            final var givenUser = givenANewUser();

            // @formatter:off
            final var location = RestAssured
                    .given()
                        .header("current-user", "customer-admin@xxx.example.com")
                        .port(port)
                    .when()
                        .delete("http://localhost/api/rbac/users/" + givenUser.getUuid())
                    .then().log().all().assertThat()
                        // that user cannot even see other users, thus the system won't even try to delete
                        .statusCode(204);
            // @formatter:on

            // finally, the user is still there
            assertThat(rbacUserRepository.findByName(givenUser.getName())).isNotNull();
        }

        @Test
        @Accepts({ "USR:D(Create)", "USR:X(Access Control)" })
        void globalAdmin_canDeleteArbitraryUser() {

            // given
            final var givenUser = givenANewUser();

            // @formatter:off
            final var location = RestAssured
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .port(port)
                    .when()
                        .delete("http://localhost/api/rbac/users/" + givenUser.getUuid())
                    .then().log().all().assertThat()
                        .statusCode(204);
            // @formatter:on

            // finally, the user is actually deleted
            assertThat(rbacUserRepository.findByName(givenUser.getName())).isNull();
        }
    }

    RbacUserEntity findRbacUserByName(final String userName) {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            return rbacUserRepository.findByName(userName);
        }).returnedValue();
    }

    RbacUserEntity givenANewUser() {
        final var givenUserName = "test-user-" + System.currentTimeMillis() + "@example.com";
        final var givenUser = jpaAttempt.transacted(() -> {
            context.define(null);
            return rbacUserRepository.create(new RbacUserEntity(UUID.randomUUID(), givenUserName));
        }).assumeSuccessful().returnedValue();
        assertThat(rbacUserRepository.findByName(givenUser.getName())).isNotNull();
        return givenUser;
    }

}
