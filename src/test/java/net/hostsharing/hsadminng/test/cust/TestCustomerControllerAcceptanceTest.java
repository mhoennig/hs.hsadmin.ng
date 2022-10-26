package net.hostsharing.hsadminng.test.cust;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.test.JpaAttempt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class }
)
@Transactional
class TestCustomerControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    Context contextMock;

    @Autowired
    TestCustomerRepository testCustomerRepository;

    @Autowired
    JpaAttempt jpaAttempt;

    @Autowired
    EntityManager em;

    @Nested
    class ListCustomers {

        @Test
        void globalAdmin_withoutAssumedRoles_canViewAllCustomers_ifNoCriteriaGiven() {
            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/test/customers")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].prefix", is("xxx"))
                    .body("[1].prefix", is("yyy"))
                    .body("[2].prefix", is("zzz"))
                    .body("size()", greaterThanOrEqualTo(3));
                // @formatter:on
        }

        @Test
        void globalAdmin_withoutAssumedRoles_canViewMatchingCustomers_ifCriteriaGiven() {
            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/test/customers?prefix=y")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].prefix", is("yyy"))
                    .body("size()", is(1));
            // @formatter:on
        }

        @Test
        void globalAdmin_withoutAssumedCustomerAdminRole_canOnlyViewOwnCustomer() {
            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "test_customer#yyy.admin")
                    .port(port)
                .when()
                    .get("http://localhost/api/test/customers")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].prefix", is("yyy"))
                    .body("size()", is(1));
            // @formatter:on
        }

        @Test
        void customerAdmin_withoutAssumedRole_canOnlyViewOwnCustomer() {
            RestAssured // @formatter:off
                .given()
                    .header("current-user", "customer-admin@yyy.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/test/customers")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].prefix", is("yyy"))
                    .body("size()", is(1));
            // @formatter:on
        }
    }

    @Nested
    class AddCustomer {

        @Test
        void globalAdmin_withoutAssumedRole_canAddCustomer() {

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                              {
                                "reference": 90020,
                                "prefix": "uuu",
                                "adminUserName": "customer-admin@uuu.example.com"
                              }
                              """)
                        .port(port)
                    .when()
                        .post("http://localhost/api/test/customers")
                    .then().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("prefix", is("uuu"))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new customer can be viewed by its own admin
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            context.define("customer-admin@uuu.example.com");
            assertThat(testCustomerRepository.findByUuid(newUserUuid))
                    .hasValueSatisfying(c -> assertThat(c.getPrefix()).isEqualTo("uuu"));
        }

        @Test
        void globalAdmin_withAssumedCustomerAdminRole_canNotAddCustomer() {

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "test_customer#xxx.admin")
                    .contentType(ContentType.JSON)
                    .body("""
                              {
                                "reference": 90010,
                                "prefix": "uuu",
                                "adminUserName": "customer-admin@uuu.example.com"
                              }
                              """)
                    .port(port)
                .when()
                    .post("http://localhost/api/test/customers")
                .then().assertThat()
                    .statusCode(403)
                    .contentType(ContentType.JSON)
                    .statusCode(403)
                    .body("message", containsString("add-customer not permitted for test_customer#xxx.admin"));
            // @formatter:on

            // finally, the new customer was not created
            context.define("superuser-fran@hostsharing.net");
            assertThat(testCustomerRepository.findCustomerByOptionalPrefixLike("uuu")).hasSize(0);
        }

        @Test
        void customerAdmin_withoutAssumedRole_canNotAddCustomer() {

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "customer-admin@yyy.example.com")
                    .contentType(ContentType.JSON)
                    .body("""
                              {
                                "reference": 90010,
                                "prefix": "uuu",
                                "adminUserName": "customer-admin@uuu.example.com"
                              }
                              """)
                    .port(port)
                .when()
                    .post("http://localhost/api/test/customers")
                .then().assertThat()
                    .statusCode(403)
                    .contentType(ContentType.JSON)
                    .statusCode(403)
                    .body("message", containsString("add-customer not permitted for customer-admin@yyy.example.com"));
                // @formatter:on

            // finally, the new customer was not created
            context.define("superuser-fran@hostsharing.net");
            assertThat(testCustomerRepository.findCustomerByOptionalPrefixLike("uuu")).hasSize(0);
        }

        @Test
        void invalidRequestBodyJson_raisesClientError() {

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("{]") // deliberately invalid JSON
                    .port(port)
                .when()
                    .post("http://localhost/api/test/customers")
                .then().assertThat()
                    .statusCode(400)
                    .contentType(ContentType.JSON)
                    .body("message", containsString("JSON parse error: Unexpected close marker ']': expected '}'"))
                    .body("message", containsString("line: 1, column: 1"));
            // @formatter:on
        }
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net", null);
            em.createQuery("DELETE FROM TestCustomerEntity c WHERE c.reference < 99900").executeUpdate();
        }).assertSuccessful();
    }
}
