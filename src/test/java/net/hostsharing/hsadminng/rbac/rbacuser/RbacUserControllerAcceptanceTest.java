package net.hostsharing.hsadminng.rbac.rbacuser;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = HsadminNgApplication.class
)
@Transactional
class RbacUserControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    EntityManager em;

    @Autowired
    Context context;

    @Autowired
    RbacUserRepository rbacUserRepository;

    @Nested
    class ApiRbacUsersGet {

        @Test
        void hostsharingAdmin_withoutAssumedRole_canViewAllUsers() {

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "mike@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac-users")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].name", is("aaa00@aaa.example.com"))
                    .body("[1].name", is("aaa01@aaa.example.com"))
                    .body("[2].name", is("aaa02@aaa.example.com"))
                    .body("[3].name", is("aab00@aab.example.com"))
                    // ...
                    .body("[11].name", is("admin@aac.example.com"))
                    .body("[12].name", is("mike@hostsharing.net"))
                    .body("[13].name", is("sven@hostsharing.net"))
                    .body("size()", greaterThanOrEqualTo(14));
            // @formatter:on
        }

        @Test
        void hostsharingAdmin_withoutAssumedRole_canViewAllUsersByName() {

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "mike@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac-users?name=aac")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].name", is("aac00@aac.example.com"))
                    .body("[1].name", is("aac01@aac.example.com"))
                    .body("[2].name", is("aac02@aac.example.com"))
                    .body("size()", is(3));
            // @formatter:on
        }

        @Test
        void customerAdmin_withoutAssumedRole_canViewUsersInItsRealm() {

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "admin@aab.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac-users")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].name", is("aab00@aab.example.com"))
                    .body("[1].name", is("aab01@aab.example.com"))
                    .body("[2].name", is("aab02@aab.example.com"))
                    .body("[3].name", is("admin@aab.example.com"))
                    .body("size()", is(4));
            // @formatter:on
        }

        @Test
        void packetAdmin_withoutAssumedRole_canViewAllUsersOfItsPackage() {

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "aaa01@aaa.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/rbac-users")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].name", is("aaa01@aaa.example.com"))
                    .body("size()", is(1));
            // @formatter:on
        }
    }

    @Nested
    class ApiRbacUsersPost {

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
                    .post("http://localhost/api/rbac-users")
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
            context.setCurrentUser("new-user@example.com");
            assertThat(rbacUserRepository.findByUuid(newUserUuid))
                .extracting(RbacUserEntity::getName).isEqualTo("new-user@example.com");
        }
    }
}
