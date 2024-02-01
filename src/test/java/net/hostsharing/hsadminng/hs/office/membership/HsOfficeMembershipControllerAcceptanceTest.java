package net.hostsharing.hsadminng.hs.office.membership;

import com.vladmihalcea.hibernate.type.range.Range;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorRepository;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRepository;
import net.hostsharing.hsadminng.hs.office.test.ContextBasedTestWithCleanup;
import net.hostsharing.test.Accepts;
import net.hostsharing.test.JpaAttempt;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
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
class HsOfficeMembershipControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    private static final String TEMP_MEMBER_NUMBER_SUFFIX = "90";

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

    @PersistenceContext
    EntityManager em;

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
                              "mainDebitor": { "debitorNumber": 1000111 },
                              "memberNumber": 1000101,
                              "memberNumberSuffix": "01",
                              "validFrom": "2022-10-01",
                              "validTo": null,
                              "reasonForTermination": "NONE"
                          },
                          {
                              "partner": { "person": { "tradeName": "Second e.K." } },
                              "mainDebitor": { "debitorNumber": 1000212 },
                              "memberNumber": 1000202,
                              "memberNumberSuffix": "02",
                              "validFrom": "2022-10-01",
                              "validTo": null,
                              "reasonForTermination": "NONE"
                          },
                          {
                              "partner": { "person": { "tradeName": "Third OHG" } },
                              "mainDebitor": { "debitorNumber": 1000313 },
                              "memberNumber": 1000303,
                              "memberNumberSuffix": "03",
                              "validFrom": "2022-10-01",
                              "validTo": null,
                              "reasonForTermination": "NONE"
                          }
                      ]
                    """));
                // @formatter:on
        }

        @Test
        void globalAdmin_canViewMembershipsByPartnerUuid() {

            context.define("superuser-alex@hostsharing.net");
            final var partner = partnerRepo.findPartnerByPartnerNumber(10001);

            RestAssured // @formatter:off
                    .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                    .when()
                    .queryParam("partnerUuid", partner.getUuid() )
                    .get("http://localhost/api/hs/office/memberships")
                    .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                      [
                          {
                              "partner": { "person": { "tradeName": "First GmbH" } },
                              "mainDebitor": { "debitorNumber": 1000111 },
                              "memberNumber": 1000101,
                              "memberNumberSuffix": "01",
                              "validFrom": "2022-10-01",
                              "validTo": null,
                              "reasonForTermination": "NONE"
                          }
                      ]
                    """));
            // @formatter:on
        }

        @Test
        void globalAdmin_canViewMembershipsByMemberNumber() {

            RestAssured // @formatter:off
                    .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                    .when()
                    .queryParam("memberNumber", 1000202 )
                    .get("http://localhost/api/hs/office/memberships")
                    .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                      [
                          {
                              "partner": { "person": { "tradeName": "Second e.K." } },
                              "mainDebitor": { "debitorNumber": 1000212 },
                              "memberNumber": 1000202,
                              "memberNumberSuffix": "02",
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
            final var givenMemberSuffix = TEMP_MEMBER_NUMBER_SUFFIX;
            final var expectedMemberNumber = Integer.parseInt(givenPartner.getPartnerNumber() + TEMP_MEMBER_NUMBER_SUFFIX);

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                               {
                                   "partnerUuid": "%s",
                                   "mainDebitorUuid": "%s",
                                   "memberNumberSuffix": "%s",
                                   "validFrom": "2022-10-13",
                                   "membershipFeeBillable": "true"
                                 }
                            """.formatted(givenPartner.getUuid(), givenDebitor.getUuid(), givenMemberSuffix))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/memberships")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("mainDebitor.debitorNumber", is(givenDebitor.getDebitorNumber()))
                        .body("mainDebitor.debitorNumberSuffix", is((int) givenDebitor.getDebitorNumberSuffix()))
                        .body("partner.person.tradeName", is("Third OHG"))
                        .body("memberNumber", is(expectedMemberNumber))
                        .body("memberNumberSuffix", is(givenMemberSuffix))
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
            final var givenMembershipUuid = membershipRepo.findMembershipByMemberNumber(1000101).getUuid();

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
                         "mainDebitor": { "debitorNumber": 1000111 },
                         "memberNumber": 1000101,
                         "memberNumberSuffix": "01",
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
            final var givenMembershipUuid = membershipRepo.findMembershipByMemberNumber(1000101).getUuid();

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
            final var givenMembershipUuid = membershipRepo.findMembershipByMemberNumber(1000303).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "hs_office_debitor#1000313:ThirdOHG-thirdcontact.agent")
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
                             "debitorNumber": 1000313,
                             "billingContact": { "label": "third contact" }
                         },
                         "memberNumber": 1000303,
                         "memberNumberSuffix": "03",
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
                    .body("mainDebitor.debitorNumberSuffix", is((int) givenMembership.getMainDebitor().getDebitorNumberSuffix()))
                    .body("mainDebitor.debitorNumberSuffix", is((int) givenMembership.getMainDebitor().getDebitorNumberSuffix()))
                    .body("memberNumberSuffix", is(givenMembership.getMemberNumberSuffix()))
                    .body("validFrom", is("2022-11-01"))
                    .body("validTo", is("2023-12-31"))
                    .body("reasonForTermination", is("CANCELLATION"));
            // @formatter:on

            // finally, the Membership is actually updated
            assertThat(membershipRepo.findByUuid(givenMembership.getUuid())).isPresent().get()
                    .matches(mandate -> {
                        assertThat(mandate.getPartner().toShortString()).isEqualTo("LP First GmbH");
                        assertThat(mandate.getMainDebitor().toString()).isEqualTo(givenMembership.getMainDebitor().toString());
                        assertThat(mandate.getMemberNumberSuffix()).isEqualTo(givenMembership.getMemberNumberSuffix());
                        assertThat(mandate.getValidity().asString()).isEqualTo("[2022-11-01,2024-01-01)");
                        assertThat(mandate.getReasonForTermination()).isEqualTo(HsOfficeReasonForTermination.CANCELLATION);
                        return true;
                    });
        }

        @Test
        void globalAdmin_canPatchMainDebitorOfArbitraryMembership() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = givenSomeTemporaryMembershipBessler();
            final var givenNewMainDebitor = debitorRepo.findDebitorByDebitorNumber(1000313).get(0);

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
                    .body("mainDebitor.debitorNumber", is(1000313))
                    .body("memberNumberSuffix", is(givenMembership.getMemberNumberSuffix()))
                    .body("validFrom", is("2022-11-01"))
                    .body("validTo", nullValue())
                    .body("reasonForTermination", is("NONE"));
            // @formatter:on

            // finally, the Membership is actually updated
            assertThat(membershipRepo.findByUuid(givenMembership.getUuid())).isPresent().get()
                    .matches(mandate -> {
                        assertThat(mandate.getPartner().toShortString()).isEqualTo("LP First GmbH");
                        assertThat(mandate.getMainDebitor().toString()).isEqualTo(givenMembership.getMainDebitor().toString());
                        assertThat(mandate.getMemberNumberSuffix()).isEqualTo(givenMembership.getMemberNumberSuffix());
                        assertThat(mandate.getValidity().asString()).isEqualTo("[2022-11-01,)");
                        assertThat(mandate.getReasonForTermination()).isEqualTo(NONE);
                        return true;
                    });
        }

        @Test
        void partnerAgent_canViewButNotPatchValidityOfRelatedMembership() {

            context.define("superuser-alex@hostsharing.net", "hs_office_partner#10001:FirstGmbH-firstcontact.agent");
            final var givenMembership = givenSomeTemporaryMembershipBessler();

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "hs_office_partner#10001:FirstGmbH-firstcontact.agent")
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
                    .memberNumberSuffix(TEMP_MEMBER_NUMBER_SUFFIX)
                    .validity(Range.closedInfinite(LocalDate.parse("2022-11-01")))
                    .reasonForTermination(NONE)
                    .membershipFeeBillable(true)
                    .build();

            return membershipRepo.save(newMembership);
        }).assertSuccessful().returnedValue();
    }

    @AfterEach
    void cleanup() {
        jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net", null);
            final var query = em.createQuery(
                    "DELETE FROM HsOfficeMembershipEntity m WHERE m.memberNumberSuffix >= '%s'"
                            .formatted(TEMP_MEMBER_NUMBER_SUFFIX)
            );
            query.executeUpdate();
        }).assertSuccessful();
    }
}
