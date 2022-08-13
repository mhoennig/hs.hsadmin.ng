package net.hostsharing.hsadminng.rbac.rbacgrant;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.Accepts;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.rbacrole.RbacRoleEntity;
import net.hostsharing.hsadminng.rbac.rbacrole.RbacRoleRepository;
import net.hostsharing.hsadminng.rbac.rbacuser.RbacUserEntity;
import net.hostsharing.hsadminng.rbac.rbacuser.RbacUserRepository;
import net.hostsharing.test.JpaAttempt;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = { HsadminNgApplication.class, JpaAttempt.class }
)
@Accepts({ "ROL:S(Schema)" })
class RbacGrantControllerAcceptanceTest {

    @LocalServerPort
    Integer port;

    @Autowired
    EntityManager em;

    @Autowired
    Context context;

    @Autowired
    RbacUserRepository rbacUserRepository;

    @Autowired
    RbacRoleRepository rbacRoleRepository;

    @Autowired
    RbacGrantRepository rbacGrantRepository;

    @Autowired
    JpaAttempt jpaAttempt;

    @Test
    @Accepts({ "ROL:C(Create)" })
    void packageAdmin_canGrantOwnPackageAdminRole_toArbitraryUser() {

        // given
        final var givenNewUserName = "test-user-" + RandomStringUtils.randomAlphabetic(8) + "@example.com";
        final String givenPackageAdmin = "aaa00@aaa.example.com";
        final var givenOwnPackageAdminRole = "package#aaa00.admin";
        // when
        RestAssured // @formatter:off
            .given()
                .header("current-user", givenPackageAdmin)
                .contentType(ContentType.JSON)
                .body("""
                      {
                        "userUuid": "%s",
                        "roleUuid": "%s",
                        "assumed": true,
                        "empowered": false
                      }
                      """.formatted(
                        createRBacUser(givenNewUserName).getUuid().toString(),
                        findRbacRoleByName(givenOwnPackageAdminRole).getUuid().toString())
                )
                .port(port)
            .when()
                .post("http://localhost/api/rbac-grants")
            .then().assertThat()
                .statusCode(201);
            // @formatter:on

        // then
        assertThat(findAllGrantsOfUser(givenPackageAdmin))
            .extracting(RbacGrantEntity::toDisplay)
            .contains("grant( " + givenNewUserName + " -> " + givenOwnPackageAdminRole + ": assumed )");
    }

    @Test
    @Accepts({ "ROL:C(Create)", "ROL:X(Access Control)" })
    void packageAdmin_canNotGrantAlienPackageAdminRole_toArbitraryUser() {

        // given
        final var givenNewUserName = "test-user-" + RandomStringUtils.randomAlphabetic(8) + "@example.com";
        final String givenPackageAdmin = "aaa00@aaa.example.com";
        final var givenAlienPackageAdminRole = "package#aab00.admin";

        // when
        RestAssured // @formatter:off
            .given()
            .header("current-user", givenPackageAdmin)
            .contentType(ContentType.JSON)
            .body("""
                      {
                        "userUuid": "%s",
                        "roleUuid": "%s",
                        "assumed": true,
                        "empowered": false
                      }
                      """.formatted(
                createRBacUser(givenNewUserName).getUuid().toString(),
                findRbacRoleByName(givenAlienPackageAdminRole).getUuid().toString())
            )
            .port(port)
            .when()
            .post("http://localhost/api/rbac-grants")
            .then().assertThat()
            .statusCode(403);
            // @formatter:on

        // then
        assertThat(findAllGrantsOfUser(givenPackageAdmin))
            .extracting(RbacGrantEntity::getUserName)
            .doesNotContain(givenNewUserName);
    }

    List<RbacGrantEntity> findAllGrantsOfUser(final String userName) {
        return jpaAttempt.transacted(() -> {
            context.setCurrentUser(userName);
            return rbacGrantRepository.findAll();
        }).returnedValue();
    }

    RbacUserEntity createRBacUser(final String userName) {
        return jpaAttempt.transacted(() -> {
            return rbacUserRepository.create(new RbacUserEntity(UUID.randomUUID(), userName));
        }).returnedValue();
    }

    RbacRoleEntity findRbacRoleByName(final String roleName) {
        return jpaAttempt.transacted(() -> {
            context.setCurrentUser("mike@hostsharing.net");
            return rbacRoleRepository.findByRoleName(roleName);
        }).returnedValue();
    }

}
