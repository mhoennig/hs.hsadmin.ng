package net.hostsharing.hsadminng.rbac.rbacrole;

import io.restassured.RestAssured;
import net.hostsharing.hsadminng.Accepts;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.rbacuser.RbacUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import javax.persistence.EntityManager;

import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = HsadminNgApplication.class
)
@Accepts({ "ROL:*:S:Schema" })
class RbacRoleControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    EntityManager em;

    @Autowired
    Context context;

    @Autowired
    RbacUserRepository rbacUserRepository;

    @Autowired
    RbacRoleRepository rbacRoleRepository;

    @Test
    @Accepts({ "ROL:L(List)" })
    void testGlobalAdmin_withoutAssumedRole_canViewAllRoles() {

        // @formatter:off
        RestAssured
            .given()
                .header("current-user", "mike@example.org")
                .port(port)
            .when()
                .get("http://localhost/api/rbac-roles")
            .then().assertThat()
                .statusCode(200)
                .contentType("application/json")
                .body("", hasItem(hasEntry("roleName", "test_customer#xxx.admin")))
                .body("", hasItem(hasEntry("roleName", "test_customer#xxx.owner")))
                .body("", hasItem(hasEntry("roleName", "test_customer#xxx.tenant")))
                // ...
                .body("", hasItem(hasEntry("roleName", "global#test-global.admin")))
                .body("", hasItem(hasEntry("roleName", "test_customer#yyy.admin")))
                .body("", hasItem(hasEntry("roleName", "test_package#yyy00.admin")))
                .body("", hasItem(hasEntry("roleName", "test_unixuser#yyy00-aaaa.owner")))
                .body( "size()", greaterThanOrEqualTo(73)); // increases with new test data
        // @formatter:on
    }

    @Test
    @Accepts({ "ROL:L(List)", "ROL:X(Access Control)" })
    void testGlobalAdmin_withAssumedPackageAdminRole_canViewPackageAdminRoles() {

        // @formatter:off
        RestAssured
            .given()
                .header("current-user", "mike@example.org")
                .header("assumed-roles", "test_package#yyy00.admin")
                .port(port)
            .when()
                .get("http://localhost/api/rbac-roles")
            .then()
                .log().body()
            .assertThat()
                .statusCode(200)
                .contentType("application/json")
                .body("[0].roleName", is("test_customer#yyy.tenant"))
                .body("[1].roleName", is("test_package#yyy00.admin"))
                .body("[2].roleName", is("test_package#yyy00.tenant"))
                .body("[3].roleName", is("test_unixuser#yyy00-aaaa.admin"))
                .body("size()", is(7)); // increases with new test data
        // @formatter:on
    }

    @Test
    @Accepts({ "ROL:L(List)", "ROL:X(Access Control)" })
    void packageAdmin_withoutAssumedRole_canViewPackageAdminRoles() {

        // @formatter:off
        RestAssured
            .given()
                .header("current-user", "pac-admin-zzz00@zzz.example.com")
                .port(port)
            .when()
                .get("http://localhost/api/rbac-roles")
            .then().assertThat()
                .statusCode(200)
                .contentType("application/json")
                .body("[0].roleName", is("test_customer#zzz.tenant"))
                .body("[1].roleName", is("test_package#zzz00.admin"))
                .body("[2].roleName", is("test_package#zzz00.tenant"))
                .body("[3].roleName", is("test_unixuser#zzz00-aaaa.admin"))
                .body("size()", is(7)); // increases with new test data
        // @formatter:on
    }
}
