package net.hostsharing.hsadminng.hs.office.coopassets;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.coopassets.HsOfficeCoopAssetsTransactionRepository;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipRepository;
import net.hostsharing.test.Accepts;
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
class HsOfficeCoopAssetsTransactionControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    HsOfficeMembershipRepository membershipRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @Autowired
    EntityManager em;

    @Nested
    @Accepts({ "CoopAssetsTransaction:F(Find)" })
    class ListCoopAssetsTransactions {

        @Test
        void globalAdmin_canViewAllCoopAssetsTransactions() {

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/coopassetstransactions")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasSize(9));  // @formatter:on
        }

        @Test
        void globalAdmin_canFindCoopAssetsTransactionsByMemberNumber() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipsByOptionalPartnerUuidAndOptionalMemberNumber(null, 10002)
                    .get(0);

            RestAssured // @formatter:off
                    .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/coopassetstransactions?membershipUuid="+givenMembership.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        [
                            {
                                "transactionType": "DEPOSIT",
                                "assetValue": 320.00,
                                "valueDate": "2010-03-15",
                                "reference": "ref 10002-1",
                                "comment": "initial deposit"
                            },
                            {
                                "transactionType": "DISBURSAL",
                                "assetValue": -128.00,
                                "valueDate": "2021-09-01",
                                "reference": "ref 10002-2",
                                "comment": "partial disbursal"
                            },
                            {
                                "transactionType": "ADJUSTMENT",
                                "assetValue": 128.00,
                                "valueDate": "2022-10-20",
                                "reference": "ref 10002-3",
                                "comment": "some adjustment"
                            }
                        ]
                        """)); // @formatter:on
        }

        @Test
        void globalAdmin_canFindCoopAssetsTransactionsByMemberNumberAndDateRange() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipsByOptionalPartnerUuidAndOptionalMemberNumber(null, 10002)
                    .get(0);

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/coopassetstransactions?membershipUuid="
                            + givenMembership.getUuid() + "&fromValueDate=2020-01-01&toValueDate=2021-12-31")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        [
                            {
                                "transactionType": "DISBURSAL",
                                "assetValue": -128.00,
                                "valueDate": "2021-09-01",
                                "reference": "ref 10002-2",
                                "comment": "partial disbursal"
                            }
                        ]
                        """)); // @formatter:on
        }
    }

    @Nested
    @Accepts({ "CoopAssetsTransaction:C(Create)" })
    class AddCoopAssetsTransaction {

        @Test
        void globalAdmin_canAddCoopAssetsTransaction() {

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
                                   "transactionType": "DEPOSIT",
                                   "assetValue": 1024.00,
                                   "valueDate": "2022-10-13",
                                   "reference": "temp ref A",
                                   "comment": "just some test coop assets transaction"
                                 }
                            """.formatted(givenMembership.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/coopassetstransactions")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("", lenientlyEquals("""
                            {
                                "transactionType": "DEPOSIT",
                                "assetValue": 1024.00,
                                "valueDate": "2022-10-13",
                                "reference": "temp ref A",
                                "comment": "just some test coop assets transaction"
                            }
                        """))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new coopAssetsTransaction can be accessed under the generated UUID
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newUserUuid).isNotNull();
        }

        @Test
        void globalAdmin_canNotCancelMoreAssetsThanCurrentlySubscribed() {

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
                               "transactionType": "DISBURSAL",
                               "assetValue": -10240.00,
                               "valueDate": "2022-10-13",
                               "reference": "temp ref X",
                               "comment": "just some test coop assets transaction"
                             }
                            """.formatted(givenMembership.getUuid()))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/coopassetstransactions")
                .then().log().all().assertThat()
                    .statusCode(400)
                    .contentType(ContentType.JSON)
                    .body("", lenientlyEquals("""
                            {
                                 "status": 400,
                                 "error": "Bad Request",
                                 "message": "ERROR: [400] coop assets transaction would result in a negative balance of assets"
                             }
                        """));  // @formatter:on
        }
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net", null);
            // HsOfficeCoopAssetsTransactionEntity respectively hs_office_coopassetstransaction_rv
            // cannot be deleted at all, but the underlying table record can be deleted.
            em.createNativeQuery("delete from hs_office_coopassetstransaction where reference like 'temp %'")
                    .executeUpdate();
        }).assertSuccessful();
    }
}
