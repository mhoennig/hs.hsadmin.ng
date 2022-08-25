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
    void hostsharingAdmin_withoutAssumedRole_canViewAllRoles() {

        // @formatter:off
        RestAssured
            .given()
                .header("current-user", "mike@hostsharing.net")
                .port(port)
            .when()
                .get("http://localhost/api/rbac-roles")
            .then().assertThat()
                .statusCode(200)
                .contentType("application/json")
                .body("", hasItem(hasEntry("roleName", "customer#xxx.admin")))
                .body("", hasItem(hasEntry("roleName", "customer#xxx.owner")))
                .body("", hasItem(hasEntry("roleName", "customer#xxx.tenant")))
                // ...
                .body("", hasItem(hasEntry("roleName", "global#hostsharing.admin")))
                .body("", hasItem(hasEntry("roleName", "customer#yyy.admin")))
                .body("", hasItem(hasEntry("roleName", "package#yyy00.admin")))
                .body("", hasItem(hasEntry("roleName", "unixuser#yyy00-aaaa.owner")))
                .body( "size()", greaterThanOrEqualTo(73)); // increases with new test data
        // @formatter:on
    }

    @Test
    @Accepts({ "ROL:L(List)", "ROL:X(Access Control)" })
    void hostsharingAdmin_withAssumedPackageAdminRole_canViewPackageAdminRoles() {

        // @formatter:off
        RestAssured
            .given()
                .header("current-user", "mike@hostsharing.net")
                .header("assumed-roles", "package#yyy00.admin")
                .port(port)
            .when()
                .get("http://localhost/api/rbac-roles")
            .then()
                .log().body()
            .assertThat()
                .statusCode(200)
                .contentType("application/json")
                .body("[0].roleName", is("customer#yyy.tenant"))
                .body("[1].roleName", is("package#yyy00.admin"))
                .body("[2].roleName", is("package#yyy00.tenant"))
                .body("[3].roleName", is("unixuser#yyy00-aaaa.admin"))
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
                .body("[0].roleName", is("customer#zzz.tenant"))
                .body("[1].roleName", is("package#zzz00.admin"))
                .body("[2].roleName", is("package#zzz00.tenant"))
                .body("[3].roleName", is("unixuser#zzz00-aaaa.admin"))
                .body("size()", is(7)); // increases with new test data
        // @formatter:on
    }

}
