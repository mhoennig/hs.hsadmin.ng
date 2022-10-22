package net.hostsharing.hsadminng.hs.office.membership;

import com.vladmihalcea.hibernate.type.range.Range;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRepository;
import net.hostsharing.test.Accepts;
import net.hostsharing.test.JpaAttempt;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.util.UUID;

import static net.hostsharing.hsadminng.hs.office.membership.HsOfficeReasonForTermination.NONE;
import static net.hostsharing.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class }
)
@Transactional
class HsOfficeMembershipControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    Context contextMock;

    @Autowired
    HsOfficeMembershipRepository membershipRepo;

    @Autowired
    HsOfficeDebitorRepository debitorRepo;

    @Autowired
    HsOfficePartnerRepository partnerRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @Autowired
    EntityManager em;

    private static int tempMemberNumber = 20010;

    @Nested
    @Accepts({ "Membership:F(Find)" })
    class ListMemberships {

        @Test
        void globalAdmin_canViewAllMemberships_ifNoCriteriaGiven() throws JSONException {

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/memberships")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                      [
                          {
                              "partner": { "person": { "tradeName": "First GmbH" } },
                              "mainDebitor": { "debitorNumber": 10001 },
                              "memberNumber": 10001,
                              "validFrom": "2022-10-01",
                              "validTo": null,
                              "reasonForTermination": "NONE"
                          },
                          {
                              "partner": { "person": { "tradeName": "Second e.K." } },
                              "mainDebitor": { "debitorNumber": 10002 },
                              "memberNumber": 10002,
                              "validFrom": "2022-10-01",
                              "validTo": null,
                              "reasonForTermination": "NONE"
                          },
                          {
                              "partner": { "person": { "tradeName": "Third OHG" } },
                              "mainDebitor": { "debitorNumber": 10003 },
                              "memberNumber": 10003,
                              "validFrom": "2022-10-01",
                              "validTo": null,
                              "reasonForTermination": "NONE"
                          }
                      ]
                    """));
                // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Membership:C(Create)" })
    class AddMembership {

        @Test
        void globalAdmin_canAddMembership() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("Third").get(0);
            final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike("Third").get(0);

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                               {
                                   "partnerUuid": "%s",
                                   "mainDebitorUuid": "%s",
                                   "memberNumber": 20001,
                                   "validFrom": "2022-10-13"
                                 }
                            """.formatted(givenPartner.getUuid(), givenDebitor.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/memberships")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("mainDebitor.debitorNumber", is(givenDebitor.getDebitorNumber()))
                        .body("partner.person.tradeName", is("Third OHG"))
                        .body("memberNumber", is(20001))
                        .body("validFrom", is("2022-10-13"))
                        .body("validTo", equalTo(null))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new membership can be accessed under the generated UUID
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newUserUuid).isNotNull();
            assertThat(membershipRepo.findByUuid(newUserUuid)).isPresent();
        }
    }

    @Nested
    @Accepts({ "Membership:R(Read)" })
    class GetMembership {

        @Test
        void globalAdmin_canGetArbitraryMembership() {
            context.define("superuser-alex@hostsharing.net");
            final var givenMembershipUuid = membershipRepo.findMembershipsByOptionalPartnerUuidAndOptionalMemberNumber(
                            null,
                            10001)
                    .get(0)
                    .getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/memberships/" + givenMembershipUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                         "partner": { "person": { "tradeName": "First GmbH" } },
                         "mainDebitor": { "debitorNumber": 10001 },
                         "memberNumber": 10001,
                         "validFrom": "2022-10-01",
                         "validTo": null,
                         "reasonForTermination": "NONE"
                     }
                    """)); // @formatter:on
        }

        @Test
        @Accepts({ "Membership:X(Access Control)" })
        void normalUser_canNotGetUnrelatedMembership() {
            context.define("superuser-alex@hostsharing.net");
            final var givenMembershipUuid = membershipRepo.findMembershipsByOptionalPartnerUuidAndOptionalMemberNumber(
                            null,
                            10001)
                    .get(0)
                    .getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/memberships/" + givenMembershipUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        @Accepts({ "Membership:X(Access Control)" })
        void debitorAgentUser_canGetRelatedMembership() {
            context.define("superuser-alex@hostsharing.net");
            final var givenMembershipUuid = membershipRepo.findMembershipsByOptionalPartnerUuidAndOptionalMemberNumber(
                            null,
                            10003)
                    .get(0)
                    .getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "hs_office_debitor#10003ThirdOHG-thirdcontact.agent")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/memberships/" + givenMembershipUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                         "partner": { "person": { "tradeName": "Third OHG" } },
                         "mainDebitor": {
                             "debitorNumber": 10003,
                             "billingContact": { "label": "third contact" }
                         },
                         "memberNumber": 10003,
                         "validFrom": "2022-10-01",
                         "validTo": null,
                         "reasonForTermination": "NONE"
                    }
                    """)); // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Membership:U(Update)" })
    class PatchMembership {

        @Test
        void globalAdmin_canPatchValidToOfArbitraryMembership() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = givenSomeTemporaryMembershipBessler();

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                           {
                               "validTo": "2023-12-31",
                               "reasonForTermination": "CANCELLATION"
                           }
                          """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/memberships/" + givenMembership.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("partner.person.tradeName", is(givenMembership.getPartner().getPerson().getTradeName()))
                    .body("mainDebitor.debitorNumber", is(givenMembership.getMainDebitor().getDebitorNumber()))
                    .body("memberNumber", is(givenMembership.getMemberNumber()))
                    .body("validFrom", is("2022-11-01"))
                    .body("validTo", is("2023-12-31"))
                    .body("reasonForTermination", is("CANCELLATION"));
            // @formatter:on

            // finally, the Membership is actually updated
            assertThat(membershipRepo.findByUuid(givenMembership.getUuid())).isPresent().get()
                    .matches(mandate -> {
                        assertThat(mandate.getPartner().toShortString()).isEqualTo("First GmbH");
                        assertThat(mandate.getMainDebitor().toString()).isEqualTo(givenMembership.getMainDebitor().toString());
                        assertThat(mandate.getMemberNumber()).isEqualTo(givenMembership.getMemberNumber());
                        assertThat(mandate.getValidity().asString()).isEqualTo("[2022-11-01,2024-01-01)");
                        assertThat(mandate.getReasonForTermination()).isEqualTo(HsOfficeReasonForTermination.CANCELLATION);
                        return true;
                    });
        }

        @Test
        void globalAdmin_canPatchMainDebitorOfArbitraryMembership() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = givenSomeTemporaryMembershipBessler();
            final var givenNewMainDebitor = debitorRepo.findDebitorByDebitorNumber(10003).get(0);

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                           {
                               "mainDebitorUuid": "%s"
                           }
                          """.formatted(givenNewMainDebitor.getUuid()))
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/memberships/" + givenMembership.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("partner.person.tradeName", is(givenMembership.getPartner().getPerson().getTradeName()))
                    .body("mainDebitor.debitorNumber", is(10003))
                    .body("memberNumber", is(givenMembership.getMemberNumber()))
                    .body("validFrom", is("2022-11-01"))
                    .body("validTo", nullValue())
                    .body("reasonForTermination", is("NONE"));
            // @formatter:on

            // finally, the Membership is actually updated
            assertThat(membershipRepo.findByUuid(givenMembership.getUuid())).isPresent().get()
                    .matches(mandate -> {
                        assertThat(mandate.getPartner().toShortString()).isEqualTo("First GmbH");
                        assertThat(mandate.getMainDebitor().toString()).isEqualTo(givenMembership.getMainDebitor().toString());
                        assertThat(mandate.getMemberNumber()).isEqualTo(givenMembership.getMemberNumber());
                        assertThat(mandate.getValidity().asString()).isEqualTo("[2022-11-01,)");
                        assertThat(mandate.getReasonForTermination()).isEqualTo(NONE);
                        return true;
                    });
        }

        @Test
        void partnerAgent_canViewButNotPatchValidityOfRelatedMembership() {

            context.define("superuser-alex@hostsharing.net", "hs_office_partner#FirstGmbH-firstcontact.agent");
            final var givenMembership = givenSomeTemporaryMembershipBessler();

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "hs_office_partner#FirstGmbH-firstcontact.agent")
                    .contentType(ContentType.JSON)
                    .body("""
                           {
                               "validTo": "2023-12-31",
                               "reasonForTermination": "CANCELLATION"
                           }
                           """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/memberships/" + givenMembership.getUuid())
                .then().assertThat()
                    .statusCode(403); // @formatter:on

            // finally, the Membership is actually updated
            assertThat(membershipRepo.findByUuid(givenMembership.getUuid())).isPresent().get()
                    .matches(mandate -> {
                        assertThat(mandate.getValidity().asString()).isEqualTo("[2022-11-01,)");
                        assertThat(mandate.getReasonForTermination()).isEqualTo(NONE);
                        return true;
                    });
        }
    }

    @Nested
    @Accepts({ "Membership:D(Delete)" })
    class DeleteMembership {

        @Test
        void globalAdmin_canDeleteArbitraryMembership() {
            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = givenSomeTemporaryMembershipBessler();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/memberships/" + givenMembership.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given Membership is gone
            assertThat(membershipRepo.findByUuid(givenMembership.getUuid())).isEmpty();
        }

        @Test
        @Accepts({ "Membership:X(Access Control)" })
        void partnerAgentUser_canNotDeleteRelatedMembership() {
            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = givenSomeTemporaryMembershipBessler();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "hs_office_partner#FirstGmbH-firstcontact.agent")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/memberships/" + givenMembership.getUuid())
                .then().log().body().assertThat()
                    .statusCode(403); // @formatter:on

            // then the given Membership is still there
            assertThat(membershipRepo.findByUuid(givenMembership.getUuid())).isNotEmpty();
        }

        @Test
        @Accepts({ "Membership:X(Access Control)" })
        void normalUser_canNotDeleteUnrelatedMembership() {
            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = givenSomeTemporaryMembershipBessler();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/memberships/" + givenMembership.getUuid())
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on

            // then the given Membership is still there
            assertThat(membershipRepo.findByUuid(givenMembership.getUuid())).isNotEmpty();
        }
    }

    private HsOfficeMembershipEntity givenSomeTemporaryMembershipBessler() {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findDebitorByOptionalNameLike("First").get(0);
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("First").get(0);
            final var newMembership = HsOfficeMembershipEntity.builder()
                    .uuid(UUID.randomUUID())
                    .partner(givenPartner)
                    .mainDebitor(givenDebitor)
                    .memberNumber(++tempMemberNumber)
                    .validity(Range.closedInfinite(LocalDate.parse("2022-11-01")))
                    .reasonForTermination(NONE)
                    .build();

            return membershipRepo.save(newMembership);
        }).assertSuccessful().returnedValue();
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net", null);
            final var query = em.createQuery("DELETE FROM HsOfficeMembershipEntity m WHERE m.memberNumber >= 20000");
            query.executeUpdate();
        });
    }
}
