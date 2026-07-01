package net.hostsharing.hsadminng.rbac.role;

import io.restassured.RestAssured;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.rbac.subject.RbacSubjectRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;

@Tag("generalIntegrationTest")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = HsadminNgApplication.class)
@ActiveProfiles("fake-jwt")
class RbacRoleControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    RbacSubjectRepository rbacSubjectRepository;

    @Autowired
    RbacRoleRepository rbacRoleRepository;

    @Test
    void globalAdmin_withoutAssumedRole_canViewAllRoles() {

        // @formatter:off
        RestAssured
            .given()
                .header("Authorization", bearer("hsh-alex_superuser"))
                .port(port)
            .when()
                .get("http://localhost/api/rbac/roles")
            .then().assertThat()
                .statusCode(200)
                .contentType("application/json")
                .body("", hasItem(hasEntry("roleIdName", "rbactest.customer#xxx:ADMIN")))
                .body("", hasItem(hasEntry("roleIdName", "rbactest.customer#xxx:OWNER")))
                .body("", hasItem(hasEntry("roleIdName", "rbactest.customer#xxx:TENANT")))
                // ...
                .body("", hasItem(hasEntry("roleIdName", "rbac.global#global:ADMIN")))
                .body("", hasItem(hasEntry("roleIdName", "rbactest.customer#yyy:ADMIN")))
                .body("", hasItem(hasEntry("roleIdName", "rbactest.package#yyy00:ADMIN")))
                .body("", hasItem(hasEntry("roleIdName", "rbactest.domain#yyy00-aaaa:OWNER")))
                .body( "size()", greaterThanOrEqualTo(73)); // increases with new test data
        // @formatter:on
    }

    @Test
    void globalAdmin_withoutAssumedRole_canViewRoleByName() {

        // @formatter:off
        RestAssured
            .given()
                .header("Authorization", bearer("hsh-alex_superuser"))
                .queryParam("name", "rbactest.customer#yyy:ADMIN")
                .port(port)
            .when()
                .get("http://localhost/api/rbac/roles")
            .then().assertThat()
                .statusCode(200)
                .contentType("application/json")
                .body("[0].roleName", matchesRegex("rbactest\\.customer#[0-9a-f-]{36}:ADMIN"))
                .body("[0].roleIdName", is("rbactest.customer#yyy:ADMIN"))
                .body("size()", is(1));
        // @formatter:on
    }

    @Test
    void globalAdmin_withAssumedPackageAdminRole_canViewPackageAdminRoles() {

        // @formatter:off
        RestAssured
            .given()
                .header("Authorization", bearer("hsh-alex_superuser"))
                .header("Hostsharing-Assumed-Roles", "rbactest.package#yyy00:ADMIN")
                .port(port)
            .when()
                .get("http://localhost/api/rbac/roles")
            .then()
                .log().body()
            .assertThat()
                .statusCode(200)
                .contentType("application/json")

                .body("", hasItem(hasEntry("roleIdName", "rbactest.customer#yyy:TENANT")))
                .body("", hasItem(hasEntry("roleIdName", "rbactest.domain#yyy00-aaaa:OWNER")))
                .body("", hasItem(hasEntry("roleIdName", "rbactest.domain#yyy00-aaaa:ADMIN")))
                .body("", hasItem(hasEntry("roleIdName", "rbactest.domain#yyy00-aaab:OWNER")))
                .body("", hasItem(hasEntry("roleIdName", "rbactest.domain#yyy00-aaab:ADMIN")))
                .body("", hasItem(hasEntry("roleIdName", "rbactest.package#yyy00:ADMIN")))
                .body("", hasItem(hasEntry("roleIdName", "rbactest.package#yyy00:TENANT")))

                .body("", not(hasItem(hasEntry("roleIdName", "rbactest.customer#xxx:TENANT"))))
                .body("", not(hasItem(hasEntry("roleIdName", "rbactest.domain#xxx00-aaaa:ADMIN"))))
                .body("", not(hasItem(hasEntry("roleIdName", "rbactest.package#xxx00:ADMIN"))))
                .body("", not(hasItem(hasEntry("roleIdName", "rbactest.package#xxx00:TENANT"))))
        ;
        // @formatter:on
    }

    @Test
    void packageAdmin_withoutAssumedRole_canViewPackageAdminRoles() {

        // @formatter:off
        RestAssured
            .given()
                .header("Authorization", bearer("tst-pac_admin_zzz00"))
                .port(port)
            .when()
                .get("http://localhost/api/rbac/roles")
            .then().assertThat()
                .statusCode(200)
                .contentType("application/json")

                .body("", hasItem(hasEntry("roleIdName", "rbactest.customer#zzz:TENANT")))
                .body("", hasItem(hasEntry("roleIdName", "rbactest.domain#zzz00-aaaa:ADMIN")))
                .body("", hasItem(hasEntry("roleIdName", "rbactest.package#zzz00:ADMIN")))
                .body("", hasItem(hasEntry("roleIdName", "rbactest.package#zzz00:TENANT")))

                .body("", not(hasItem(hasEntry("roleIdName", "rbactest.customer#yyy:TENANT"))))
                .body("", not(hasItem(hasEntry("roleIdName", "rbactest.domain#yyy00-aaaa:ADMIN"))))
                .body("", not(hasItem(hasEntry("roleIdName", "rbactest.package#yyy00:ADMIN"))))
                .body("", not(hasItem(hasEntry("roleIdName", "rbactest.package#yyy00:TENANT"))));
        // @formatter:on
    }
}
