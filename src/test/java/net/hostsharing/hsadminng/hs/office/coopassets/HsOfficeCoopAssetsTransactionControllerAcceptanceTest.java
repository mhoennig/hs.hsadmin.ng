package net.hostsharing.hsadminng.hs.office.coopassets;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipRepository;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static net.hostsharing.hsadminng.hs.office.coopassets.HsOfficeCoopAssetsTransactionType.DEPOSIT;
import static net.hostsharing.hsadminng.rbac.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, DisableSecurityConfig.class, JpaAttempt.class,
                    MessageTranslator.class}
)
@ActiveProfiles("test")
@Transactional
@Tag("officeIntegrationTest")
class HsOfficeCoopAssetsTransactionControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    @LocalServerPort
    Integer port;

    @Autowired
    Context context;

    @Autowired
    HsOfficeCoopAssetsTransactionRepository coopAssetsTransactionRepo;

    @Autowired
    HsOfficeMembershipRepository membershipRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @Nested
    class GetListOfCoopAssetsTransactions {

        @Test
        void globalAdmin_canViewAllCoopAssetsTransactions() {

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/coopassetstransactions")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", hasSize(3*6)); // @formatter:on
        }

        @Test
        void globalAdmin_canFindCoopAssetsTransactionsByMemberNumber() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipByMemberNumber(1000202).orElseThrow();

            RestAssured // @formatter:off
                    .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
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
                                "reference": "ref 1000202-1",
                                "comment": "initial deposit",
                                "adoptionAssetTx": null,
                                "transferAssetTx": null,
                                "revertedAssetTx": null,
                                "reversalAssetTx": null
                            },
                            {
                                "transactionType": "DISBURSAL",
                                "assetValue": -128.00,
                                "valueDate": "2021-09-01",
                                "reference": "ref 1000202-2",
                                "comment": "partial disbursal",
                                "adoptionAssetTx": null,
                                "transferAssetTx": null,
                                "revertedAssetTx": null,
                                "reversalAssetTx": null
                            },
                            {
                                "transactionType": "DEPOSIT",
                                "assetValue": 128.00,
                                "valueDate": "2022-10-20",
                                "reference": "ref 1000202-3",
                                "comment": "some loss",
                                "adoptionAssetTx": null,
                                "transferAssetTx": null,
                                "revertedAssetTx": null,
                                "reversalAssetTx": {
                                    "transactionType": "REVERSAL",
                                    "assetValue": -128.00,
                                    "valueDate": "2022-10-21",
                                    "reference": "ref 1000202-3",
                                    "comment": "some reversal",
                                    "adoptionAssetTx.uuid": null,
                                    "transferAssetTx.uuid": null,
                                    "reversalAssetTx.uuid": null
                                }
                            },
                            {
                                "transactionType": "REVERSAL",
                                "assetValue": -128.00,
                                "valueDate": "2022-10-21",
                                "reference": "ref 1000202-3",
                                "comment": "some reversal",
                                "adoptionAssetTx": null,
                                "transferAssetTx": null,
                                "revertedAssetTx": {
                                    "transactionType": "DEPOSIT",
                                    "assetValue": 128.00,
                                    "valueDate": "2022-10-20",
                                    "reference": "ref 1000202-3",
                                    "comment": "some loss",
                                    "adoptionAssetTx.uuid": null,
                                    "transferAssetTx.uuid": null,
                                    "revertedAssetTx.uuid": null
                                },
                                "reversalAssetTx": null
                            },
                            {
                                "transactionType": "TRANSFER",
                                "assetValue": -192.00,
                                "valueDate": "2023-12-31",
                                "reference": "ref 1000202-3",
                                "comment": "some reversal",
                                "adoptionAssetTx": {
                                    "transactionType": "ADOPTION",
                                    "assetValue": 192.00,
                                    "valueDate": "2023-12-31",
                                    "reference": "ref 1000202-3",
                                    "comment": "some reversal",
                                    "adoptionAssetTx.uuid": null,
                                    "revertedAssetTx.uuid": null,
                                    "reversalAssetTx.uuid": null
                                },
                                "transferAssetTx": null,
                                "revertedAssetTx": null,
                                "reversalAssetTx": null
                            },
                            {
                                "transactionType": "ADOPTION",
                                "assetValue": 192.00,
                                "valueDate": "2023-12-31",
                                "reference": "ref 1000202-3",
                                "comment": "some reversal",
                                "adoptionAssetTx": null,
                                "transferAssetTx": {
                                    "transactionType": "TRANSFER",
                                    "assetValue": -192.00,
                                    "valueDate": "2023-12-31",
                                    "reference": "ref 1000202-3",
                                    "comment": "some reversal",
                                    "transferAssetTx.uuid": null,
                                    "revertedAssetTx.uuid": null,
                                    "reversalAssetTx.uuid": null
                                },
                                "revertedAssetTx": null,
                                "reversalAssetTx": null
                            }
                        ]
                        """)); // @formatter:on
        }

        @Test
        void globalAdmin_canFindCoopAssetsTransactionsByMembershipUuidAndDateRange() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipByMemberNumber(1000202).orElseThrow();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
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
                                "reference": "ref 1000202-2",
                                "comment": "partial disbursal"
                            }
                        ]
                        """)); // @formatter:on
        }
    }

    @Nested
    class PostNewCoopAssetTransaction {

        @Test
        void globalAdmin_canPostNewCoopAssetTransaction() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipByMemberNumber(1000101).orElseThrow();

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                               {
                                   "membership.uuid": "%s",
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
            final var newAssetTxUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newAssetTxUuid).isNotNull();
        }

        @Test
        void globalAdmin_canAddCoopAssetsReversalTransaction() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipByMemberNumber(1000101).orElseThrow();
            final var givenTransaction = jpaAttempt.transacted(() -> {
                // TODO.impl: introduce something like transactedAsSuperuser / transactedAs("...", ...)
                context.define("superuser-alex@hostsharing.net");
                return coopAssetsTransactionRepo.save(HsOfficeCoopAssetsTransactionEntity.builder()
                                .transactionType(DEPOSIT)
                                .valueDate(LocalDate.of(2022, 10, 20))
                                .membership(givenMembership)
                                .assetValue(new BigDecimal("256.00"))
                                .reference("test ref")
                        .build());
                    }).assertSuccessful().assertNotNull().returnedValue();
            toCleanup(HsOfficeCoopAssetsTransactionEntity.class, givenTransaction.getUuid());

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                                   {
                                       "membership.uuid": "%s",
                                       "transactionType": "REVERSAL",
                                       "assetValue": %s,
                                       "valueDate": "2022-10-30",
                                       "reference": "test ref reversal",
                                       "comment": "some coop assets reversal transaction",
                                       "revertedAssetTx.uuid": "%s"
                                     }
                                """.formatted(
                                    givenMembership.getUuid(),
                                    givenTransaction.getAssetValue().negate().toString(),
                                    givenTransaction.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/coopassetstransactions")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("", lenientlyEquals("""
                            {
                                "transactionType": "REVERSAL",
                                "assetValue": -256.00,
                                "valueDate": "2022-10-30",
                                "reference": "test ref reversal",
                                "comment": "some coop assets reversal transaction",
                                "revertedAssetTx": {
                                    "transactionType": "DEPOSIT",
                                    "assetValue": 256.00,
                                    "valueDate": "2022-10-20",
                                    "reference": "test ref"
                                }
                            }
                            """))
                    .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new coopAssetsTransaction can be accessed under the generated UUID
            final var newAssetTxUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newAssetTxUuid).isNotNull();
            toCleanup(HsOfficeCoopAssetsTransactionEntity.class, newAssetTxUuid);
        }

        @Test
        void globalAdmin_canNotCancelMoreAssetsThanCurrentlySubscribed() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipByMemberNumber(1000101).orElseThrow();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                    .header("Accept-Language", "de")
                    .contentType(ContentType.JSON)
                    .body("""
                           {
                               "membership.uuid": "%s",
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
                                 "statusCode": 400,
                                 "statusPhrase": "Bad Request",
                                 "message": "ERROR: [400] Geschäftsguthaben-Transaktion würde zu einem negativen Geschäftsguthaben-Saldo führen"
                             }
                        """));  // @formatter:on
        }
    }

    @Nested
    class GetCoopAssetTransaction {

        @Test
        void globalAdmin_withoutAssumedRole_canGetArbitraryCoopAssetTransaction() {
            context.define("superuser-alex@hostsharing.net");
            final var givenCoopAssetTransactionUuid = coopAssetsTransactionRepo.findCoopAssetsTransactionByOptionalMembershipUuidAndDateRange(
                    null,
                    LocalDate.of(2010, 3, 15),
                    LocalDate.of(2010, 3, 15)).get(0).getUuid();

            RestAssured // @formatter:off
                .given().header("Authorization", "Bearer superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/coopassetstransactions/" + givenCoopAssetTransactionUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                        "transactionType": "DEPOSIT"
                    }
                    """)); // @formatter:on
        }

        @Test
        void normalUser_canNotGetUnrelatedCoopAssetTransaction() {
            context.define("superuser-alex@hostsharing.net");
            final var givenCoopAssetTransactionUuid = coopAssetsTransactionRepo.findCoopAssetsTransactionByOptionalMembershipUuidAndDateRange(
                    null,
                    LocalDate.of(2010, 3, 15),
                    LocalDate.of(2010, 3, 15)).get(0).getUuid();

            RestAssured // @formatter:off
                .given().header("Authorization", "Bearer selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/coopassetstransactions/" + givenCoopAssetTransactionUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        void partnerPersonUser_canGetRelatedCoopAssetTransaction() {
            context.define("superuser-alex@hostsharing.net");
            final var givenCoopAssetTransactionUuid = coopAssetsTransactionRepo.findCoopAssetsTransactionByOptionalMembershipUuidAndDateRange(
                    null,
                    LocalDate.of(2010, 3, 15),
                    LocalDate.of(2010, 3, 15)).get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer person-FirstGmbH@example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/coopassetstransactions/" + givenCoopAssetTransactionUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                         "transactionType": "DEPOSIT",
                         "assetValue": 320
                     }
                    """)); // @formatter:on
        }
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net", null);
            // HsOfficeCoopAssetsTransactionEntity respectively hs_office.coopassettx_rv
            // cannot be deleted at all, but the underlying table record can be deleted.
            em.createNativeQuery("delete from hs_office.coopassettx where reference like 'temp %'")
                    .executeUpdate();
        }).assertSuccessful();
    }
}
