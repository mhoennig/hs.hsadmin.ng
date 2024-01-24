package net.hostsharing.hsadminng.hs.office.coopshares;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.UUID;

import static net.hostsharing.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {HsadminNgApplication.class, JpaAttempt.class})
@Transactional
class HsOfficeCoopSharesTransactionControllerAcceptanceTest {

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
            // HsOfficeCoopSharesTransactionEntity respectively hs_office_coopsharestransaction_rv
            // cannot be deleted at all, but the underlying table record can be deleted.
            em.createNativeQuery("delete from hs_office_coopsharestransaction where reference like 'temp %'").executeUpdate();
        }).assertSuccessful();
    }

    @Nested
    @Accepts({"CoopSharesTransaction:F(Find)"})
    class ListCoopSharesTransactions {

        @Test
        void globalAdmin_canViewAllCoopSharesTransactions() {

            RestAssured // @formatter:off
                .given().header("current-user", "superuser-alex@hostsharing.net").port(port).when().get("http://localhost/api/hs/office/coopsharestransactions").then().log().all().assertThat().statusCode(200).contentType("application/json").body("", hasSize(9));  // @formatter:on
        }

        @Test
        void globalAdmin_canFindCoopSharesTransactionsByMemberNumber() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipByMemberNumber(1000202);

            RestAssured // @formatter:off
                .given().header("current-user", "superuser-alex@hostsharing.net").port(port).when().get("http://localhost/api/hs/office/coopsharestransactions?membershipUuid=" + givenMembership.getUuid()).then().log().all().assertThat().statusCode(200).contentType("application/json").body("", lenientlyEquals("""
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
                            "transactionType": "ADJUSTMENT",
                            "shareCount": 2,
                            "valueDate": "2022-10-20",
                            "reference": "ref 1000202-3",
                            "comment": "some adjustment"
                        }
                    ]
                    """)); // @formatter:on
        }

        @Test
        void globalAdmin_canFindCoopSharesTransactionsByMembershipUuidAndDateRange() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipByMemberNumber(1000202);

            RestAssured // @formatter:off
                .given().header("current-user", "superuser-alex@hostsharing.net").port(port).when()
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
    @Accepts({"CoopSharesTransaction:C(Create)"})
    class AddCoopSharesTransaction {

        @Test
        void globalAdmin_canAddCoopSharesTransaction() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipByMemberNumber(1000101);

            final var location = RestAssured // @formatter:off
                .given().header("current-user", "superuser-alex@hostsharing.net").contentType(ContentType.JSON).body("""
                       {
                           "membershipUuid": "%s",
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
            final var newUserUuid = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
            assertThat(newUserUuid).isNotNull();
        }

        @Test
        void globalAdmin_canNotCancelMoreSharesThanCurrentlySubscribed() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = membershipRepo.findMembershipByMemberNumber(1000101);

            final var location = RestAssured // @formatter:off
                .given().header("current-user", "superuser-alex@hostsharing.net").contentType(ContentType.JSON).body("""
                    {
                        "membershipUuid": "%s",
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
    @Accepts({"CoopShareTransaction:R(Read)"})
    class GetCoopShareTransaction {

        @Test
        void globalAdmin_withoutAssumedRole_canGetArbitraryCoopShareTransaction() {
            context.define("superuser-alex@hostsharing.net");
            final var givenCoopShareTransactionUuid = coopSharesTransactionRepo.findCoopSharesTransactionByOptionalMembershipUuidAndDateRange(null, LocalDate.of(2010, 3, 15), LocalDate.of(2010, 3, 15)).get(0).getUuid();

            RestAssured // @formatter:off
                .given().header("current-user", "superuser-alex@hostsharing.net").port(port).when().get("http://localhost/api/hs/office/coopsharestransactions/" + givenCoopShareTransactionUuid).then().log().body().assertThat().statusCode(200).contentType("application/json").body("", lenientlyEquals("""
                    {
                        "transactionType": "SUBSCRIPTION"
                    }
                    """)); // @formatter:on
        }

        @Test
        @Accepts({"CoopShareTransaction:X(Access Control)"})
        void normalUser_canNotGetUnrelatedCoopShareTransaction() {
            context.define("superuser-alex@hostsharing.net");
            final var givenCoopShareTransactionUuid = coopSharesTransactionRepo.findCoopSharesTransactionByOptionalMembershipUuidAndDateRange(null, LocalDate.of(2010, 3, 15), LocalDate.of(2010, 3, 15)).get(0).getUuid();

            RestAssured // @formatter:off
                .given().header("current-user", "selfregistered-user-drew@hostsharing.org").port(port).when().get("http://localhost/api/hs/office/coopsharestransactions/" + givenCoopShareTransactionUuid).then().log().body().assertThat().statusCode(404); // @formatter:on
        }

        @Test
        @Accepts({"CoopShareTransaction:X(Access Control)"})
        void contactAdminUser_canGetRelatedCoopShareTransaction() {
            context.define("superuser-alex@hostsharing.net");
            final var givenCoopShareTransactionUuid = coopSharesTransactionRepo.findCoopSharesTransactionByOptionalMembershipUuidAndDateRange(null, LocalDate.of(2010, 3, 15), LocalDate.of(2010, 3, 15)).get(0).getUuid();

            RestAssured // @formatter:off
                .given().header("current-user", "contact-admin@firstcontact.example.com").port(port).when().get("http://localhost/api/hs/office/coopsharestransactions/" + givenCoopShareTransactionUuid).then().log().body().assertThat().statusCode(200).contentType("application/json").body("", lenientlyEquals("""
                    {
                         "transactionType": "SUBSCRIPTION",
                         "shareCount": 4
                     }
                    """)); // @formatter:on
        }
    }
}
