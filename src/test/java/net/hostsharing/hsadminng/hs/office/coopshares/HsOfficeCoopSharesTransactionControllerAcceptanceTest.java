package net.hostsharing.hsadminng.hs.office.coopshares;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.Accepts;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipRepository;
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

import static net.hostsharing.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class }
)
@Transactional
class HsOfficeCoopSharesTransactionControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    Context contextMock;

    @Autowired
    HsOfficeCoopSharesTransactionRepository coopSharesTransactionRepo;

    @Autowired
    HsOfficeMembershipRepository membershipRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @Autowired
    EntityManager em;

    @Nested
    @Accepts({ "CoopSharesTransaction:F(Find)" })
    class ListCoopSharesTransactions {

        @Test
        void globalAdmin_canViewAllCoopSharesTransactions() {

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/coopsharestransactions")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasSize(9));  // @formatter:on
        }

        @Test
        void globalAdmin_canFindCoopSharesTransactionsByMemberNumber() {

            RestAssured // @formatter:off
                    .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/coopsharestransactions?memberNumber=10002")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        [
                            {
                                "transactionType": "SUBSCRIPTION",
                                "sharesCount": 2,
                                "valueDate": "2010-03-15",
                                "reference": "ref 10002-1",
                                "comment": "initial subscription"
                            },
                            {
                                "transactionType": "SUBSCRIPTION",
                                "sharesCount": 24,
                                "valueDate": "2021-09-01",
                                "reference": "ref 10002-2",
                                "comment": "subscibing more"
                            },
                            {
                                "transactionType": "CANCELLATION;",
                                "sharesCount": 12,
                                "valueDate": "2022-10-20",
                                "reference": "ref 10002-3",
                                "comment": "cancelling some"
                            }
                        ]
                        """)); // @formatter:on
        }

        @Test
        void globalAdmin_canFindCoopSharesTransactionsByMemberNumberAndDateRange() {

            RestAssured // @formatter:off
                    .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                    .when()
                    .get("http://localhost/api/hs/office/coopsharestransactions?memberNumber=10002&fromValueDate=2020-01-01&toValueDate=2021-12-31")
                    .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        [
                            {
                                "transactionType": "SUBSCRIPTION",
                                "sharesCount": 24,
                                "valueDate": "2021-09-01",
                                "reference": "ref 10002-2",
                                "comment": "subscibing more"
                            }
                        ]
                        """)); // @formatter:on
        }
    }

    @Nested
    @Accepts({ "CoopSharesTransaction:C(Create)" })
    class AddCoopSharesTransaction {

        @Test
        void globalAdmin_canAddCoopSharesTransaction() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipsByOptionalPartnerUuidAndOptionalMemberNumber(null, 10001)
                    .get(0);

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                               {
                                   "membershipUuid": "%s",
                                   "transactionType": "SUBSCRIPTION",
                                   "sharesCount": 8,
                                   "valueDate": "2022-10-13",
                                   "reference": "temp ref A",
                                   "comment": "just some test coop shares transaction" 
                                 }
                            """.formatted(givenMembership.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/coopsharestransactions")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("", lenientlyEquals("""
                            {
                                "transactionType": "SUBSCRIPTION",
                                "sharesCount": 0,
                                "valueDate": "2022-10-13",
                                "reference": "temp ref A",
                                "comment": "just some test coop shares transaction"
                            }
                        """))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new coopSharesTransaction can be accessed under the generated UUID
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newUserUuid).isNotNull();
        }

        // TODO.test: move validation tests to a ...WebMvcTest
        @Test
        void globalAdmin_canNotAddCoopSharesTransactionWhenMembershipUuidIsMissing() {

        }

    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net", null);
            // HsOfficeCoopSharesTransactionEntity respectively hs_office_coopsharestransaction_rv
            // cannot be deleted at all, but the underlying table record can be deleted.
            em.createNativeQuery("delete from hs_office_coopsharestransaction where reference like 'temp %'")
                    .executeUpdate();
        }).assertSuccessful();
    }
}
