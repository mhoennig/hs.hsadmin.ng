package net.hostsharing.hsadminng.hs.hscustomer;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = HsadminNgApplication.class
)
@Transactional
class CustomerControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    Context contextMock;
    @Autowired
    CustomerRepository customerRepository;

    @Nested
    class ListCustomers {

        @Test
        void hostsharingAdmin_withoutAssumedRoles_canViewAllCustomers_ifNoCriteriaGiven() {
            RestAssured // @formatter:off
                .given()
                    .header("current-user", "mike@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/customers")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].prefix", is("xxx"))
                    .body("[1].prefix", is("yyy"))
                    .body("[2].prefix", is("zzz"))
                    .body("size()", greaterThanOrEqualTo(3));
                // @formatter:on
        }

        @Test
        void hostsharingAdmin_withoutAssumedRoles_canViewMatchingCustomers_ifCriteriaGiven() throws Exception {
            RestAssured // @formatter:off
                .given()
                    .header("current-user", "mike@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/customers?prefix=y")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].prefix", is("yyy"))
                    .body("size()", is(1));
            // @formatter:on
        }

        @Test
        void hostsharingAdmin_withoutAssumedCustomerAdminRole_canOnlyViewOwnCustomer() throws Exception {
            RestAssured // @formatter:off
                .given()
                    .header("current-user", "mike@hostsharing.net")
                    .header("assumed-roles", "customer#yyy.admin")
                    .port(port)
                .when()
                    .get("http://localhost/api/customers")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].prefix", is("yyy"))
                    .body("size()", is(1));
            // @formatter:on
        }

        @Test
        void customerAdmin_withoutAssumedRole_canOnlyViewOwnCustomer() throws Exception {
            RestAssured // @formatter:off
                    .given()
                    .header("current-user", "customer-admin@yyy.example.com")
                    .port(port)
                    .when()
                    .get("http://localhost/api/customers")
                    .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].prefix", is("yyy"))
                    .body("size()", is(1));
            // @formatter:on
        }
    }

    @Nested
    class CreateCustomer {

        @Test
        void hostsharingAdmin_withoutAssumedRole_canCreateCustomer() throws Exception {

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "mike@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                              {
                                "reference": 90020,
                                "prefix": "ttt",
                                "adminUserName": "customer-admin@ttt.example.com"
                              }
                              """)
                        .port(port)
                    .when()
                        .post("http://localhost/api/customers")
                    .then().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("prefix", is("ttt"))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new customer can be viewed by its own admin
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            context.setCurrentUser("customer-admin@ttt.example.com");
            assertThat(customerRepository.findByUuid(newUserUuid))
                    .hasValueSatisfying(c -> assertThat(c.getPrefix()).isEqualTo("ttt"));
        }

        @Test
        void hostsharingAdmin_withoutAssumedRole_canCreateCustomerWithGivenUuid() {

            final var givenUuid = UUID.randomUUID();

            final var location = RestAssured // @formatter:off
                    .given()
                    .header("current-user", "mike@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                              {
                                "uuid": "%s",
                                "reference": 90010,
                                "prefix": "vvv",
                                "adminUserName": "customer-admin@vvv.example.com"
                              }
                              """.formatted(givenUuid))
                    .port(port)
                    .when()
                    .post("http://localhost/api/customers")
                    .then().assertThat()
                    .statusCode(201)
                    .contentType(ContentType.JSON)
                    .body("prefix", is("vvv"))
                    .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new customer can be viewed by its own admin
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            context.setCurrentUser("customer-admin@vvv.example.com");
            assertThat(customerRepository.findByUuid(newUserUuid))
                    .hasValueSatisfying(c -> {
                        assertThat(c.getPrefix()).isEqualTo("vvv");
                        assertThat(c.getUuid()).isEqualTo(givenUuid);
                    });
        }

        @Test
        void hostsharingAdmin_withAssumedCustomerAdminRole_canNotCreateCustomer() throws Exception {

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "mike@hostsharing.net")
                    .header("assumed-roles", "customer#xxx.admin")
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
                    .post("http://localhost/api/customers")
                .then().assertThat()
                    .statusCode(403)
                    .contentType(ContentType.JSON)
                    .statusCode(403)
                    .body("message", containsString("add-customer not permitted for customer#xxx.admin"));
            // @formatter:on

            // finally, the new customer was not created
            context.setCurrentUser("sven@hostsharing.net");
            assertThat(customerRepository.findCustomerByOptionalPrefixLike("uuu")).hasSize(0);
        }

        @Test
        void customerAdmin_withoutAssumedRole_canNotCreateCustomer() throws Exception {

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
                    .post("http://localhost/api/customers")
                .then().assertThat()
                    .statusCode(403)
                    .contentType(ContentType.JSON)
                    .statusCode(403)
                    .body("message", containsString("add-customer not permitted for customer-admin@yyy.example.com"));
                // @formatter:on

            // finally, the new customer was not created
            context.setCurrentUser("sven@hostsharing.net");
            assertThat(customerRepository.findCustomerByOptionalPrefixLike("uuu")).hasSize(0);
        }
    }
}
