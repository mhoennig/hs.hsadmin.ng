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
    void hostsharingAdmin_withoutAssumedRole_canViewPackageAdminRoles() {

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
            .body("[0].roleName", is("customer#aaa.admin"))
            .body("[1].roleName", is("customer#aaa.owner"))
            .body("[2].roleName", is("customer#aaa.tenant"))
            // ...
            .body("", hasItem(hasEntry("roleName", "global#hostsharing.admin")))
            .body("", hasItem(hasEntry("roleName", "customer#aab.admin")))
            .body("", hasItem(hasEntry("roleName", "package#aab00.admin")))
            .body("", hasItem(hasEntry("roleName", "unixuser#aab00-aaaa.owner")))
            .body( "size()", is(73)); // increases with new test data
        // @formatter:on
    }

    @Test
    @Accepts({ "ROL:L(List)", "ROL:X(Access Control)" })
    void hostsharingAdmin_withAssumedPackageAdminRole_canViewPackageAdminRoles() {

        // @formatter:off
        RestAssured
            .given()
                .header("current-user", "mike@hostsharing.net")
                .header("assumed-roles", "package#aab00.admin")
                .port(port)
            .when()
                .get("http://localhost/api/rbac-roles")
            .then().assertThat()
                .statusCode(200)
                .contentType("application/json")
                .body("[0].roleName", is("customer#aab.tenant"))
                .body("[1].roleName", is("package#aab00.admin"))
                .body("[2].roleName", is("package#aab00.tenant"))
                .body("[3].roleName", is("unixuser#aab00-aaaa.admin"))
                .body("size()", is(7)); // increases with new test data
        // @formatter:on
    }

    @Test
    @Accepts({ "ROL:L(List)", "ROL:X(Access Control)" })
    void packageAdmin_withoutAssumedRole_canViewPackageAdminRoles() {

        // @formatter:off
        RestAssured
            .given()
            .header("current-user", "aac00@aac.example.com")
            .port(port)
            .when()
            .get("http://localhost/api/rbac-roles")
            .then().assertThat()
            .statusCode(200)
            .contentType("application/json")
            .body("[0].roleName", is("customer#aac.tenant"))
            .body("[1].roleName", is("package#aac00.admin"))
            .body("[2].roleName", is("package#aac00.tenant"))
            .body("[3].roleName", is("unixuser#aac00-aaaa.admin"))
            .body("size()", is(7)); // increases with new test data
        // @formatter:on
    }

}
