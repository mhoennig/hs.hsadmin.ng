package net.hostsharing.hsadminng.rbac.rbacrole;

import io.restassured.RestAssured;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.rbacuser.RbacUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = HsadminNgApplication.class
)
class RbacRoleControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    RbacUserRepository rbacUserRepository;

    @Autowired
    RbacRoleRepository rbacRoleRepository;

    @Test
    void globalAdmin_withoutAssumedRole_canViewAllRoles() {

        // @formatter:off
        RestAssured
            .given()
                .header("current-user", "superuser-alex@hostsharing.net")
                .port(port)
            .when()
                .get("http://localhost/api/rbac/roles")
            .then().assertThat()
                .statusCode(200)
                .contentType("application/json")
                .body("", hasItem(hasEntry("roleName", "test_customer#xxx:ADMIN")))
                .body("", hasItem(hasEntry("roleName", "test_customer#xxx:OWNER")))
                .body("", hasItem(hasEntry("roleName", "test_customer#xxx:TENANT")))
                // ...
                .body("", hasItem(hasEntry("roleName", "global#global:ADMIN")))
                .body("", hasItem(hasEntry("roleName", "test_customer#yyy:ADMIN")))
                .body("", hasItem(hasEntry("roleName", "test_package#yyy00:ADMIN")))
                .body("", hasItem(hasEntry("roleName", "test_domain#yyy00-aaaa:OWNER")))
                .body( "size()", greaterThanOrEqualTo(73)); // increases with new test data
        // @formatter:on
    }

    @Test
    void globalAdmin_withAssumedPackageAdminRole_canViewPackageAdminRoles() {

        // @formatter:off
        RestAssured
            .given()
                .header("current-user", "superuser-alex@hostsharing.net")
                .header("assumed-roles", "test_package#yyy00:ADMIN")
                .port(port)
            .when()
                .get("http://localhost/api/rbac/roles")
            .then()
                .log().body()
            .assertThat()
                .statusCode(200)
                .contentType("application/json")

                .body("", hasItem(hasEntry("roleName", "test_customer#yyy:TENANT")))
                .body("", hasItem(hasEntry("roleName", "test_domain#yyy00-aaaa:OWNER")))
                .body("", hasItem(hasEntry("roleName", "test_domain#yyy00-aaaa:ADMIN")))
                .body("", hasItem(hasEntry("roleName", "test_domain#yyy00-aaab:OWNER")))
                .body("", hasItem(hasEntry("roleName", "test_domain#yyy00-aaab:ADMIN")))
                .body("", hasItem(hasEntry("roleName", "test_package#yyy00:ADMIN")))
                .body("", hasItem(hasEntry("roleName", "test_package#yyy00:TENANT")))

                .body("", not(hasItem(hasEntry("roleName", "test_customer#xxx:TENANT"))))
                .body("", not(hasItem(hasEntry("roleName", "test_domain#xxx00-aaaa:ADMIN"))))
                .body("", not(hasItem(hasEntry("roleName", "test_package#xxx00:ADMIN"))))
                .body("", not(hasItem(hasEntry("roleName", "test_package#xxx00:TENANT"))))
        ;
        // @formatter:on
    }

    @Test
    void packageAdmin_withoutAssumedRole_canViewPackageAdminRoles() {

        // @formatter:off
        RestAssured
            .given()
                .header("current-user", "pac-admin-zzz00@zzz.example.com")
                .port(port)
            .when()
                .get("http://localhost/api/rbac/roles")
            .then().assertThat()
                .statusCode(200)
                .contentType("application/json")

                .body("", hasItem(hasEntry("roleName", "test_customer#zzz:TENANT")))
                .body("", hasItem(hasEntry("roleName", "test_domain#zzz00-aaaa:ADMIN")))
                .body("", hasItem(hasEntry("roleName", "test_package#zzz00:ADMIN")))
                .body("", hasItem(hasEntry("roleName", "test_package#zzz00:TENANT")))

                .body("", not(hasItem(hasEntry("roleName", "test_customer#yyy:TENANT"))))
                .body("", not(hasItem(hasEntry("roleName", "test_domain#yyy00-aaaa:ADMIN"))))
                .body("", not(hasItem(hasEntry("roleName", "test_package#yyy00:ADMIN"))))
                .body("", not(hasItem(hasEntry("roleName", "test_package#yyy00:TENANT"))));
        // @formatter:on
    }
}
