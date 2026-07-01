package net.hostsharing.hsadminng.rbac.subject;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@Transactional
@Tag("generalIntegrationTest")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = HsadminNgApplication.class)
@ActiveProfiles("fake-jwt")
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
                                "name": "tst-new_user"
                              }
                              """)
                        .port(port)
                    .when()
                        .post("http://localhost/api/rbac/subjects")
                    .then().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("name", is("tst-new_user"))
                        .header("Location", startsWith("http://localhost"))
                        .extract().header("Location");
            // @formatter:on

            // finally, the user can view its own record
            final var newSubjectUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            context.define("tst-new_user");
            assertThat(rbacSubjectRepository.findByUuid(newSubjectUuid))
                    .extracting(RbacSubjectEntity::getName).isEqualTo("tst-new_user");
        }

        @Test
        void anybody_cannotCreateAUserWithInvalidName() {

            // @formatter:off
            RestAssured
                    .given()
                        .contentType(ContentType.JSON)
                        .body("""
                              {
                                "name": "invalid-username@example.com"
                              }
                              """)
                        .port(port)
                    .when()
                        .post("http://localhost/api/rbac/subjects")
                    .then().assertThat()
                        .statusCode(400)
                        .body("message", containsString(
                                "USER subject name 'invalid-username@example.com' does not match required pattern"
                        ));
            // @formatter:on
        }

        @Test
        void anybody_canCreateANewGroup() {
            final var newGroupName = "/xyz-Team-" + System.currentTimeMillis();

            // @formatter:off
            RestAssured
                    .given()
                        .contentType(ContentType.JSON)
                        .body("""
                              {
                                "name": "%s",
                                "type": "GROUP"
                              }
                              """.formatted(newGroupName))
                        .port(port)
                    .when()
                        .post("http://localhost/api/rbac/subjects")
                    .then().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("name", is(newGroupName))
                        .body("type", is("GROUP"));
            // @formatter:on
        }

        @Test
        void anybody_cannotCreateAGroupWithUserName() {

            // @formatter:off
            RestAssured
                    .given()
                        .contentType(ContentType.JSON)
                        .body("""
                              {
                                "name": "xyz-Team",
                                "type": "GROUP"
                              }
                              """)
                        .port(port)
                    .when()
                        .post("http://localhost/api/rbac/subjects")
                    .then().assertThat()
                        .statusCode(400);
            // @formatter:on
        }
    }

    @Nested
    class GetRbacSubject {

        @Test
        void globalAdmin_withoutAssumedRole_canGetArbitraryUser() {
            final var givenUser = findRbacSubjectByName("tst-pac_admin_xxx00");

            // @formatter:off
            RestAssured
                .given()
                    .header("Authorization", bearer("hsh-alex_superuser"))
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects/" + givenUser.getUuid())
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("name", is("tst-pac_admin_xxx00"));
            // @formatter:on
        }

        @Test
        void globalAdmin_withAssumedCustomerAdminRole_canGetUserWithinInItsRealm() {
            final var givenUser = findRbacSubjectByName("tst-pac_admin_yyy00");

            // @formatter:off
            RestAssured
                .given()
                    .header("Authorization", bearer("hsh-alex_superuser"))
                    .header("Hostsharing-Assumed-Roles", "rbactest.customer#yyy:ADMIN")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects/" + givenUser.getUuid())
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("name", is("tst-pac_admin_yyy00"));
            // @formatter:on
        }

        @Test
        void customerAdmin_withoutAssumedRole_canGetUserWithinInItsRealm() {
            final var givenUser = findRbacSubjectByName("tst-pac_admin_yyy00");

            // @formatter:off
            RestAssured
                .given()
                    .header("Authorization", bearer("tst-customer_admin_yyy"))
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects/" + givenUser.getUuid())
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("name", is("tst-pac_admin_yyy00"));
            // @formatter:on
        }

        @Test
        void customerAdmin_withoutAssumedRole_canNotGetUserOutsideOfItsRealm() {
            final var givenUser = findRbacSubjectByName("tst-pac_admin_yyy00");

            // @formatter:off
            RestAssured
                .given()
                    .header("Authorization", bearer("tst-customer_admin_xxx"))
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
                    .header("Authorization", bearer("hsh-alex_superuser"))
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects")
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasItem(hasEntry("name", "tst-customer_admin_xxx")))
                    .body("", hasItem(hasEntry("name", "tst-customer_admin_yyy")))
                    .body("", hasItem(hasEntry("name", "tst-customer_admin_zzz")))
                    .body("", hasItem(hasEntry("name", "hsh-alex_superuser")))
                    // ...
                    .body("", hasItem(hasEntry("name", "tst-pac_admin_zzz01")))
                    .body("", hasItem(hasEntry("name", "tst-pac_admin_zzz02")))
                    .body("", hasItem(hasEntry("name", "hsh-fran_superuser")))
                    .body("size()", greaterThanOrEqualTo(14));
            // @formatter:on
        }

        @Test
        void globalAdmin_withoutAssumedRole_canViewAllUsersByName() {

            // @formatter:off
            RestAssured
                .given()
                    .header("Authorization", bearer("hsh-alex_superuser"))
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects?name=tst-pac_admin_zzz0")
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].name", is("tst-pac_admin_zzz00"))
                    .body("[0].type", is("USER"))
                    .body("[1].name", is("tst-pac_admin_zzz01"))
                    .body("[2].name", is("tst-pac_admin_zzz02"))
                    .body("size()", is(3));
            // @formatter:on
        }

        @Test
        void globalAdmin_withoutAssumedRole_canViewSubjectsByType() {

            // @formatter:off
            RestAssured
                .given()
                    .header("Authorization", bearer("hsh-alex_superuser"))
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects?type=GROUP")
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasItem(hasEntry("name", "/hsh-Hostmasters")))
                    .body("", hasItem(hasEntry("type", "GROUP")))
                    .body("findAll { it.type != 'GROUP' }.size()", is(0))
                    .body("size()", greaterThanOrEqualTo(3));
            // @formatter:on
        }

        @Test
        void user_withJwtGroupMembership_canViewThatGroupSubject() {

            // @formatter:off
            RestAssured
                .given()
                    .header("Authorization", bearer(
                            "tst-person_firbysusan",
                            List.of("/xyz-Service")))
                    .queryParam("name", "/xyz-Service")
                    .queryParam("type", "GROUP")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].name", is("/xyz-Service"))
                    .body("[0].type", is("GROUP"))
                    .body("size()", is(1));
            // @formatter:on
        }

        @Test
        void globalAdmin_withAssumedCustomerAdminRole_canViewUsersInItsRealm() {

            // @formatter:off
            RestAssured
                .given()
                    .header("Authorization", bearer("hsh-alex_superuser"))
                    .header("Hostsharing-Assumed-Roles", "rbactest.customer#yyy:ADMIN")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].name", is("tst-customer_admin_yyy"))
                    .body("[1].name", is("tst-pac_admin_yyy00"))
                    .body("[2].name", is("tst-pac_admin_yyy01"))
                    .body("[3].name", is("tst-pac_admin_yyy02"))
                    .body("size()", is(4));
            // @formatter:on
        }

        @Test
        void customerAdmin_withoutAssumedRole_canViewUsersInItsRealm() {

            // @formatter:off
            RestAssured
                .given()
                    .header("Authorization", bearer("tst-customer_admin_yyy"))
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].name", is("tst-customer_admin_yyy"))
                    .body("[1].name", is("tst-pac_admin_yyy00"))
                    .body("[2].name", is("tst-pac_admin_yyy01"))
                    .body("[3].name", is("tst-pac_admin_yyy02"))
                    .body("size()", is(4));
            // @formatter:on
        }

        @Test
        void packetAdmin_withoutAssumedRole_canViewAllUsersOfItsPackage() {

            // @formatter:off
            RestAssured
                .given()
                    .header("Authorization", bearer("tst-pac_admin_xxx01"))
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac/subjects")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].name", is("tst-pac_admin_xxx01"))
                    .body("size()", is(1));
            // @formatter:on
        }
    }

    @Nested
    class ListRbacSubjectPermissions {

        @Test
        void globalAdmin_withoutAssumedRole_canViewArbitraryUsersPermissions() {
            final var givenUser = findRbacSubjectByName("tst-pac_admin_yyy00");

            // @formatter:off
            RestAssured
                .given()
                    .header("Authorization", bearer("hsh-alex_superuser"))
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
            final var givenUser = findRbacSubjectByName("tst-pac_admin_yyy00");

            // @formatter:off
            RestAssured
                .given()
                    .header("Authorization", bearer("hsh-alex_superuser"))
                    .header("Hostsharing-Assumed-Roles", "rbactest.customer#yyy:ADMIN")
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
            final var givenUser = findRbacSubjectByName("tst-pac_admin_yyy00");

            // @formatter:off
            RestAssured
                .given()
                    .header("Authorization", bearer("tst-pac_admin_yyy00"))
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
            final var givenUser = findRbacSubjectByName("tst-pac_admin_xxx00");

            // @formatter:off
            RestAssured
                .given()
                    .header("Authorization", bearer("tst-pac_admin_yyy00"))
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
            RestAssured
                    .given()
                        .header("Authorization", bearer(givenUser.getName()))
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
            RestAssured
                    .given()
                        .header("Authorization", bearer("tst-customer_admin_xxx"))
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
            RestAssured
                    .given()
                        .header("Authorization", bearer("hsh-alex_superuser"))
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
            context.define("hsh-alex_superuser");
            return rbacSubjectRepository.findByName(userName);
        }).returnedValue();
    }

    RbacSubjectEntity givenANewUser() {
        final var givenUserName = "tst-user_" + System.currentTimeMillis();
        final var givenUser = jpaAttempt.transacted(() -> {
            context.define("hsh-alex_superuser");
            return rbacSubjectRepository.create(
                    RbacSubjectEntity.builder().uuid(randomUUID()).name(givenUserName).build());
        }).assumeSuccessful().returnedValue();
        assertThat(rbacSubjectRepository.findByName(givenUser.getName())).isNotNull();
        return givenUser;
    }

}
