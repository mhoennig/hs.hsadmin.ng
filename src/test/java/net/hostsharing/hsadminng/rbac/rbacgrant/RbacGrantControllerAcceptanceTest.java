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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = { HsadminNgApplication.class, JpaAttempt.class }
)
@Accepts({ "ROL:S(Schema)" })
@Transactional(propagation = Propagation.NEVER)
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

    @Nested
    class GrantRoleToUser {

        @Test
        @Accepts({ "GRT:C(Create)" })
        void packageAdmin_canGrantOwnPackageAdminRole_toArbitraryUser() {

            // given
            final var givenNewUserName = "test-user-" + RandomStringUtils.randomAlphabetic(8) + "@example.com";
            final String givenCurrentUserPackageAdmin = "aaa00@aaa.example.com";
            final String givenAssumedRole = "package#aaa00.admin";
            final var givenOwnPackageAdminRole = "package#aaa00.admin";

            // when
            RestAssured // @formatter:off
            .given()
                .header("current-user", givenCurrentUserPackageAdmin)
                .header("assumed-roles", givenAssumedRole)
                .contentType(ContentType.JSON)
                .body("""
                      {
                        "assumed": true,
                        "grantedRoleUuid": "%s",
                        "granteeUserUuid": "%s"
                      }
                      """.formatted(
                        findRbacRoleByName(givenOwnPackageAdminRole).getUuid().toString(),
                        createRBacUser(givenNewUserName).getUuid().toString())
                )
                .port(port)
            .when()
                .post("http://localhost/api/rbac-grants")
            .then().assertThat()
                .statusCode(201);
            // @formatter:on

            // then
            assertThat(findAllGrantsOfUser(givenCurrentUserPackageAdmin))
                .extracting(RbacGrantEntity::toDisplay)
                .contains("{ grant assumed role " + givenOwnPackageAdminRole +
                    " to user " + givenNewUserName +
                    " by role " + givenAssumedRole + " }");
        }

        @Test
        @Accepts({ "GRT:C(Create)", "GRT:X(Access Control)" })
        void packageAdmin_canNotGrantAlienPackageAdminRole_toArbitraryUser() {

            // given
            final var givenNewUserName = "test-user-" + RandomStringUtils.randomAlphabetic(8) + "@example.com";
            final String givenCurrentUserPackageAdmin = "aaa00@aaa.example.com";
            final String givenAssumedRole = "package#aaa00.admin";
            final var givenAlienPackageAdminRole = "package#aab00.admin";

            // when
            RestAssured // @formatter:off
            .given()
                .header("current-user", givenCurrentUserPackageAdmin)
                .header("assumed-roles", givenAssumedRole)
                .contentType(ContentType.JSON)
                .body("""
                          {
                            "assumed": true,
                            "grantedRoleUuid": "%s",
                            "granteeUserUuid": "%s"
                          }
                          """.formatted(
                    findRbacRoleByName(givenAlienPackageAdminRole).getUuid().toString(),
                    createRBacUser(givenNewUserName).getUuid().toString())
                )
                .port(port)
            .when()
                .post("http://localhost/api/rbac-grants")
            .then().assertThat()
                .body("message", containsString("Access to granted role"))
                .body("message", containsString("forbidden for {package#aaa00.admin}"))
                .statusCode(403);
            // @formatter:on

            // then
            assertThat(findAllGrantsOfUser(givenCurrentUserPackageAdmin))
                .extracting(RbacGrantEntity::getGranteeUserName)
                .doesNotContain(givenNewUserName);
        }
    }

    List<RbacGrantEntity> findAllGrantsOfUser(final String userName) {
        return jpaAttempt.transacted(() -> {
            context.setCurrentUser(userName);
            return rbacGrantRepository.findAll();
        }).returnedValue();
    }

    RbacUserEntity createRBacUser(final String userName) {
        return jpaAttempt.transacted(() ->
            rbacUserRepository.create(new RbacUserEntity(UUID.randomUUID(), userName))
        ).returnedValue();
    }

    RbacRoleEntity findRbacRoleByName(final String roleName) {
        return jpaAttempt.transacted(() -> {
            context.setCurrentUser("mike@hostsharing.net");
            return rbacRoleRepository.findByRoleName(roleName);
        }).returnedValue();
    }
}
