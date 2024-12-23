package net.hostsharing.hsadminng.hs.office.coopshares;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipRepository;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {HsadminNgApplication.class, DisableSecurityConfig.class, JpaAttempt.class})
@ActiveProfiles("test")
@Transactional
class HsOfficeCoopSharesTransactionControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    @Autowired
    Context context;

    @Autowired
    HsOfficeCoopSharesTransactionRepository coopSharesTransactionRepo;

    @Autowired
    HsOfficeMembershipRepository membershipRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @LocalServerPort
    private Integer port;

    @BeforeEach
    @AfterEach
    void cleanup() {
        jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net", null);
            // HsOfficeCoopSharesTransactionEntity respectively hs_office.coopsharetx_rv
            // cannot be deleted at all, but the underlying table record can be deleted.
            em.createNativeQuery("delete from hs_office.coopsharetx where reference like 'temp %'").executeUpdate();
        }).assertSuccessful();
    }

    @Nested
    class getListOfCoopSharesTransactions {

        @Test
        void globalAdmin_canViewAllCoopSharesTransactions() {

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/coopsharestransactions")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasSize(12));  // @formatter:on
        }

        @Test
        void globalAdmin_canFindCoopSharesTransactionsByMemberNumber() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipByMemberNumber(1000202).orElseThrow();

            RestAssured // @formatter:off
                .given().header("current-subject", "superuser-alex@hostsharing.net").port(port).when().get("http://localhost/api/hs/office/coopsharestransactions?membershipUuid=" + givenMembership.getUuid()).then().log().all().assertThat().statusCode(200).contentType("application/json").body("", lenientlyEquals("""
                    [
                        {
                            "transactionType": "SUBSCRIPTION",
                            "shareCount": 4,
                            "valueDate": "2010-03-15",
                            "reference": "ref 1000202-1",
                            "comment": "initial subscription"
                        },
                        {
                            "transactionType": "CANCELLATION",
                            "shareCount": -2,
                            "valueDate": "2021-09-01",
                            "reference": "ref 1000202-2",
                            "comment": "cancelling some"
                        },
                        {
                                "transactionType": "SUBSCRIPTION",
                                "shareCount": 2,
                                "valueDate": "2022-10-20",
                                "reference": "ref 1000202-3",
                                "comment": "some subscription",
                                "reversalShareTx": {
                                    "transactionType": "REVERSAL",
                                    "shareCount": -2,
                                    "valueDate": "2022-10-21",
                                    "reference": "ref 1000202-4",
                                    "comment": "some reversal"
                                }
                            },
                        {
                            "transactionType": "REVERSAL",
                            "shareCount": -2,
                            "valueDate": "2022-10-21",
                            "reference": "ref 1000202-4",
                            "comment": "some reversal",
                            "revertedShareTx": {
                                    "transactionType": "SUBSCRIPTION",
                                    "shareCount": 2,
                                    "valueDate": "2022-10-20",
                                    "reference": "ref 1000202-3",
                                    "comment": "some subscription"
                            }
                        }
                    ]
                    """)); // @formatter:on
        }

        @Test
        void globalAdmin_canFindCoopSharesTransactionsByMembershipUuidAndDateRange() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipByMemberNumber(1000202).orElseThrow();

            RestAssured // @formatter:off
                .given().header("current-subject", "superuser-alex@hostsharing.net").port(port).when()
                    .get("http://localhost/api/hs/office/coopsharestransactions?membershipUuid=" + givenMembership.getUuid() + "&fromValueDate=2020-01-01&toValueDate=2021-12-31").then().log().all().assertThat().statusCode(200).contentType("application/json").body("", lenientlyEquals("""
                    [
                        {
                            "transactionType": "CANCELLATION",
                            "shareCount": -2,
                            "valueDate": "2021-09-01",
                            "reference": "ref 1000202-2",
                            "comment": "cancelling some"
                        }
                    ]
                    """)); // @formatter:on
        }
    }

    @Nested
    class AddCoopSharesTransaction {

        @Test
        void globalAdmin_canAddCoopSharesTransaction() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipByMemberNumber(1000101).orElseThrow();

            final var location = RestAssured // @formatter:off
                .given().header("current-subject", "superuser-alex@hostsharing.net").contentType(ContentType.JSON).body("""
                       {
                           "membership.uuid": "%s",
                           "transactionType": "SUBSCRIPTION",
                           "shareCount": 8,
                           "valueDate": "2022-10-13",
                           "reference": "temp ref A",
                           "comment": "just some test coop shares transaction"
                         }
                    """.formatted(givenMembership.getUuid())).port(port).when().post("http://localhost/api/hs/office/coopsharestransactions").then().log().all().assertThat().statusCode(201).contentType(ContentType.JSON).body("uuid", isUuidValid()).body("", lenientlyEquals("""
                        {
                            "transactionType": "SUBSCRIPTION",
                            "shareCount": 8,
                            "valueDate": "2022-10-13",
                            "reference": "temp ref A",
                            "comment": "just some test coop shares transaction"
                        }
                    """)).header("Location", startsWith("http://localhost")).extract().header("Location");  // @formatter:on

            // finally, the new coopSharesTransaction can be accessed under the generated UUID
            final var newShareTxUuid = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
            assertThat(newShareTxUuid).isNotNull();
        }

        @Test
        void globalAdmin_canAddCoopSharesReversalTransaction() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipByMemberNumber(1000101).orElseThrow();
            final var givenTransaction = jpaAttempt.transacted(() -> {
                // TODO.impl: introduce something like transactedAsSuperuser / transactedAs("...", ...)
                context.define("superuser-alex@hostsharing.net");
                return coopSharesTransactionRepo.save(HsOfficeCoopSharesTransactionEntity.builder()
                    .transactionType(HsOfficeCoopSharesTransactionType.SUBSCRIPTION)
                    .valueDate(LocalDate.of(2022, 10, 20))
                    .membership(givenMembership)
                    .shareCount(13)
                    .reference("test ref")
                    .build());
            }).assertSuccessful().assertNotNull().returnedValue();
            toCleanup(HsOfficeCoopSharesTransactionEntity.class, givenTransaction.getUuid());

            final var location = RestAssured // @formatter:off
                .given()
                .header("current-subject", "superuser-alex@hostsharing.net")
                .contentType(ContentType.JSON)
                .body("""
                           {
                               "membership.uuid": "%s",
                               "transactionType": "REVERSAL",
                               "shareCount": %s,
                               "valueDate": "2022-10-30",
                               "reference": "test reversal ref",
                               "comment": "some coop shares reversal transaction",
                               "revertedShareTx.uuid": "%s"
                           }
                           """.formatted(
                    givenMembership.getUuid(),
                    -givenTransaction.getShareCount(),
                    givenTransaction.getUuid()))
                .port(port)
                .when()
                .post("http://localhost/api/hs/office/coopsharestransactions")
                .then().log().all().assertThat()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("uuid", isUuidValid())
                .body("", lenientlyEquals("""
                            {
                                "transactionType": "REVERSAL",
                                "shareCount": -13,
                                "valueDate": "2022-10-30",
                                "reference": "test reversal ref",
                                "comment": "some coop shares reversal transaction",
                                "revertedShareTx": {
                                    "transactionType": "SUBSCRIPTION",
                                    "shareCount": 13,
                                    "valueDate": "2022-10-20",
                                    "reference": "test ref"
                                }
                            }
                            """))
                .header("Location", startsWith("http://localhost"))
                .extract().header("Location");  // @formatter:on

            // finally, the new coopAssetsTransaction can be accessed under the generated UUID
            final var newShareTxUuid = UUID.fromString(
                location.substring(location.lastIndexOf('/') + 1));
            assertThat(newShareTxUuid).isNotNull();
            toCleanup(HsOfficeCoopSharesTransactionEntity.class, newShareTxUuid);
        }

        @Test
        void globalAdmin_canNotCancelMoreSharesThanCurrentlySubscribed() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipByMemberNumber(1000101).orElseThrow();

            RestAssured // @formatter:off
                .given().header("current-subject", "superuser-alex@hostsharing.net").contentType(ContentType.JSON).body("""
                    {
                        "membership.uuid": "%s",
                        "transactionType": "CANCELLATION",
                        "shareCount": -80,
                        "valueDate": "2022-10-13",
                        "reference": "temp ref X",
                        "comment": "just some test coop shares transaction"
                      }
                     """.formatted(givenMembership.getUuid())).port(port).when().post("http://localhost/api/hs/office/coopsharestransactions").then().log().all().assertThat().statusCode(400).contentType(ContentType.JSON).body("", lenientlyEquals("""
                        {
                             "statusCode": 400,
                             "statusPhrase": "Bad Request",
                             "message": "ERROR: [400] coop shares transaction would result in a negative number of shares"
                         }
                    """));  // @formatter:on
        }
    }

    @Nested
    class GetCoopShareTransaction {

        @Test
        void globalAdmin_withoutAssumedRole_canGetArbitraryCoopShareTransaction() {
            context.define("superuser-alex@hostsharing.net");
            final var givenCoopShareTransactionUuid = coopSharesTransactionRepo.findCoopSharesTransactionByOptionalMembershipUuidAndDateRange(null, LocalDate.of(2010, 3, 15), LocalDate.of(2010, 3, 15)).get(0).getUuid();

            RestAssured // @formatter:off
                .given().header("current-subject", "superuser-alex@hostsharing.net").port(port).when().get("http://localhost/api/hs/office/coopsharestransactions/" + givenCoopShareTransactionUuid).then().log().body().assertThat().statusCode(200).contentType("application/json").body("", lenientlyEquals("""
                    {
                        "transactionType": "SUBSCRIPTION"
                    }
                    """)); // @formatter:on
        }

        @Test
        void normalUser_canNotGetUnrelatedCoopShareTransaction() {
            context.define("superuser-alex@hostsharing.net");
            final var givenCoopShareTransactionUuid = coopSharesTransactionRepo.findCoopSharesTransactionByOptionalMembershipUuidAndDateRange(null, LocalDate.of(2010, 3, 15), LocalDate.of(2010, 3, 15)).get(0).getUuid();

            RestAssured // @formatter:off
                .given().header("current-subject", "selfregistered-user-drew@hostsharing.org").port(port).when().get("http://localhost/api/hs/office/coopsharestransactions/" + givenCoopShareTransactionUuid).then().log().body().assertThat().statusCode(404); // @formatter:on
        }

        @Test
        void partnerPersonUser_canGetRelatedCoopShareTransaction() {
            context.define("superuser-alex@hostsharing.net");
            final var givenCoopShareTransactionUuid = coopSharesTransactionRepo.findCoopSharesTransactionByOptionalMembershipUuidAndDateRange(null, LocalDate.of(2010, 3, 15), LocalDate.of(2010, 3, 15)).get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "person-FirstGmbH@example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/coopsharestransactions/" + givenCoopShareTransactionUuid)
                .then()
                    .log().body()
                    .assertThat()
                        .statusCode(200)
                        .contentType("application/json")
                        .body("", lenientlyEquals("""
                            {
                                 "transactionType": "SUBSCRIPTION",
                                 "shareCount": 4
                             }
                            """)); // @formatter:on
        }
    }
}
