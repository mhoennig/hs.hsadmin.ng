package net.hostsharing.hsadminng.hs.office.membership;

import io.hypersistence.utils.hibernate.type.range.Range;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRealRepository;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
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

import static net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipStatus.ACTIVE;
import static net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipStatus.CANCELLED;
import static net.hostsharing.hsadminng.rbac.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, DisableSecurityConfig.class, JpaAttempt.class }
)
@ActiveProfiles("test")
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
    HsOfficePartnerRealRepository partnerRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @Nested
    class ListMemberships {

        @Test
        void globalAdmin_canViewAllMemberships_ifNoCriteriaGiven() throws JSONException {

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/memberships")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                      [
                          {
                              "partner": { "partnerNumber": "P-10001" },
                              "memberNumber": "M-1000101",
                              "memberNumberSuffix": "01",
                              "validFrom": "2022-10-01",
                              "validTo": null,
                              "status": "ACTIVE"
                          },
                          {
                              "partner": { "partnerNumber": "P-10002" },
                              "memberNumber": "M-1000202",
                              "memberNumberSuffix": "02",
                              "validFrom": "2022-10-01",
                              "validTo": null,
                              "status": "ACTIVE"
                          },
                          {
                              "partner": { "partnerNumber": "P-10003" },
                              "memberNumber": "M-1000303",
                              "memberNumberSuffix": "03",
                              "validFrom": "2022-10-01",
                              "validTo": null,
                              "status": "ACTIVE"
                          }
                      ]
                    """));
                // @formatter:on
        }

        @Test
        void globalAdmin_canViewMembershipsByPartnerUuid() {

            context.define("superuser-alex@hostsharing.net");
            final var partner = partnerRepo.findPartnerByPartnerNumber(10001).orElseThrow();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
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
                              "partner": { "partnerNumber": "P-10001" },
                              "memberNumber": "M-1000101",
                              "memberNumberSuffix": "01",
                              "validFrom": "2022-10-01",
                              "validTo": null,
                              "status": "ACTIVE"
                          }
                      ]
                    """));
            // @formatter:on
        }

        @Test
        void globalAdmin_canViewMembershipsByPartnerNumber() {

            RestAssured // @formatter:off
                    .given()
                        .header("current-subject", "superuser-alex@hostsharing.net")
                        .port(port)
                    .when()
                        .queryParam("partnerNumber", "P-10002" )
                        .get("http://localhost/api/hs/office/memberships")
                    .then().log().all().assertThat()
                        .statusCode(200)
                        .contentType("application/json")
                        .body("", lenientlyEquals("""
                          [
                              {
                                  "partner": { "partnerNumber": "P-10002" },
                                  "memberNumber": "M-1000202",
                                  "memberNumberSuffix": "02",
                                  "validFrom": "2022-10-01",
                                  "validTo": null,
                                  "status": "ACTIVE"
                              }
                          ]
                        """));
            // @formatter:on
        }
    }

    @Nested
    class AddMembership {

        @Test
        void globalAdmin_canAddMembership() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike("Third").get(0);
            final var givenMemberSuffix = TEMP_MEMBER_NUMBER_SUFFIX;
            final var expectedMemberNumber = Integer.parseInt(givenPartner.getPartnerNumber() + TEMP_MEMBER_NUMBER_SUFFIX);

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-subject", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                               {
                                   "partner.uuid": "%s",
                                   "memberNumberSuffix": "%s",
                                   "validFrom": "2022-10-13",
                                   "membershipFeeBillable": "true"
                                 }
                            """.formatted(givenPartner.getUuid(), givenMemberSuffix))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/memberships")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("partner.partnerNumber", is("P-10003"))
                        .body("memberNumber", is("M-" + expectedMemberNumber))
                        .body("memberNumberSuffix", is(givenMemberSuffix))
                        .body("validFrom", is("2022-10-13"))
                        .body("validTo", equalTo(null))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new membership can be accessed under the generated UUID
            final var newSubjectUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newSubjectUuid).isNotNull();
            assertThat(membershipRepo.findByUuid(newSubjectUuid)).isPresent();
        }
    }

    @Nested
    class GetMembership {

        @Test
        void globalAdmin_canGetArbitraryMembership() {
            context.define("superuser-alex@hostsharing.net");
            final var givenMembershipUuid = membershipRepo.findMembershipByMemberNumber(1000101).orElseThrow().getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/memberships/" + givenMembershipUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                         "partner": { "partnerNumber": "P-10001" },
                         "memberNumber": "M-1000101",
                         "memberNumberSuffix": "01",
                         "validFrom": "2022-10-01",
                         "validTo": null,
                         "status": "ACTIVE"
                     }
                    """)); // @formatter:on
        }

        @Test
        void normalUser_canNotGetUnrelatedMembership() {
            context.define("superuser-alex@hostsharing.net");
            final var givenMembershipUuid = membershipRepo.findMembershipByMemberNumber(1000101).orElseThrow().getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/memberships/" + givenMembershipUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        void partnerRelAgent_canGetRelatedMembership() {
            context.define("superuser-alex@hostsharing.net");
            final var givenMembershipUuid = membershipRepo.findMembershipByMemberNumber(1000303).orElseThrow().getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "hs_office.relation#HostsharingeG-with-PARTNER-ThirdOHG:AGENT")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/memberships/" + givenMembershipUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                         "partner": { "partnerNumber": "P-10003" },
                         "memberNumber": "M-1000303",
                         "memberNumberSuffix": "03",
                         "validFrom": "2022-10-01",
                         "validTo": null,
                         "status": "ACTIVE"
                    }
                    """)); // @formatter:on
        }
    }

    @Nested
    class PatchMembership {

        @Test
        void globalAdmin_canPatchValidToOfArbitraryMembership() {

            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = givenSomeTemporaryMembershipBessler("First");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                           {
                               "validTo": "2023-12-31",
                               "status": "CANCELLED"
                           }
                          """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/memberships/" + givenMembership.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("partner.partnerNumber", is("P-" + givenMembership.getPartner().getPartnerNumber()))
                    .body("memberNumberSuffix", is(givenMembership.getMemberNumberSuffix()))
                    .body("validFrom", is("2022-11-01"))
                    .body("validTo", is("2023-12-31"))
                    .body("status", is("CANCELLED"));
            // @formatter:on

            // finally, the Membership is actually updated
            assertThat(membershipRepo.findByUuid(givenMembership.getUuid())).isPresent().get()
                    .matches(mandate -> {
                        assertThat(mandate.getPartner().toShortString()).isEqualTo("P-10001");
                        assertThat(mandate.getMemberNumberSuffix()).isEqualTo(givenMembership.getMemberNumberSuffix());
                        assertThat(mandate.getValidity().asString()).isEqualTo("[2022-11-01,2024-01-01)");
                        assertThat(mandate.getStatus()).isEqualTo(CANCELLED);
                        return true;
                    });
        }

        @Test
        void partnerRelAdmin_canPatchValidityOfRelatedMembership() {

            // given
            final var givenPartnerAdmin = "hs_office.relation#HostsharingeG-with-PARTNER-FirstGmbH:ADMIN";
            context.define("superuser-alex@hostsharing.net", givenPartnerAdmin);
            final var givenMembership = givenSomeTemporaryMembershipBessler("First");

            // when
            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", givenPartnerAdmin)
                    .contentType(ContentType.JSON)
                    .body("""
                           {
                               "validTo": "2024-01-01",
                               "status": "CANCELLED"
                           }
                           """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/memberships/" + givenMembership.getUuid())
                .then().assertThat()
                    .statusCode(200); // @formatter:on

            // finally, the Membership is actually updated
            assertThat(membershipRepo.findByUuid(givenMembership.getUuid())).isPresent().get()
                    .matches(mandate -> {
                        assertThat(mandate.getValidity().asString()).isEqualTo("[2022-11-01,2024-01-02)");
                        assertThat(mandate.getStatus()).isEqualTo(CANCELLED);
                        return true;
                    });
        }
    }

    @Nested
    class DeleteMembership {

        @Test
        void globalAdmin_canDeleteArbitraryMembership() {
            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = givenSomeTemporaryMembershipBessler("First");

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/memberships/" + givenMembership.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given Membership is gone
            assertThat(membershipRepo.findByUuid(givenMembership.getUuid())).isEmpty();
        }

        @Test
        void partnerAgentUser_canNotDeleteRelatedMembership() {
            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = givenSomeTemporaryMembershipBessler("First");

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "hs_office.relation#HostsharingeG-with-PARTNER-FirstGmbH:AGENT")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/memberships/" + givenMembership.getUuid())
                .then().log().body().assertThat()
                    .statusCode(403); // @formatter:on

            // then the given Membership is still there
            assertThat(membershipRepo.findByUuid(givenMembership.getUuid())).isNotEmpty();
        }

        @Test
        void normalUser_canNotDeleteUnrelatedMembership() {
            context.define("superuser-alex@hostsharing.net");
            final var givenMembership = givenSomeTemporaryMembershipBessler("First");

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/memberships/" + givenMembership.getUuid())
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on

            // then the given Membership is still there
            assertThat(membershipRepo.findByUuid(givenMembership.getUuid())).isNotEmpty();
        }
    }

    private HsOfficeMembershipEntity givenSomeTemporaryMembershipBessler(final String partnerName) {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var givenPartner = partnerRepo.findPartnerByOptionalNameLike(partnerName).get(0);
            final var newMembership = HsOfficeMembershipEntity.builder()
                    .partner(givenPartner)
                    .memberNumberSuffix(TEMP_MEMBER_NUMBER_SUFFIX)
                    .validity(Range.closedInfinite(LocalDate.parse("2022-11-01")))
                    .status(ACTIVE)
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
