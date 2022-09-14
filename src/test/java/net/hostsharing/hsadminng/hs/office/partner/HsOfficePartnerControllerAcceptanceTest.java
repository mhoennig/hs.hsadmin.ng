package net.hostsharing.hsadminng.hs.office.partner;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.Accepts;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRepository;
import net.hostsharing.test.JpaAttempt;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static net.hostsharing.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class }
)
@Transactional
class HsOfficePartnerControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    Context contextMock;

    @Autowired
    HsOfficePartnerRepository partnerRepo;

    @Autowired
    HsOfficePersonRepository personRepo;

    @Autowired
    HsOfficeContactRepository contactRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    Set<UUID> tempPartnerUuids = new HashSet<>();

    @Nested
    @Accepts({ "Partner:F(Find)" })
    class ListPartners {

        @Test
        void globalAdmin_withoutAssumedRoles_canViewAllPartners_ifNoCriteriaGiven() throws JSONException {

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/partners")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    [
                        {
                            "person": { "tradeName": "First Impressions GmbH" },
                            "contact": { "label": "first contact" }
                        },
                        {
                            "person": { "tradeName": "Ostfriesische Kuhhandel OHG" },
                            "contact": { "label": "third contact" }
                        },
                        {
                            "person": { "tradeName": "Rockshop e.K." },
                            "contact": { "label": "second contact" }
                        }
                    ]
                    """));
                // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Partner:C(Create)" })
    class AddPartner {

        @Test
        void globalAdmin_withoutAssumedRole_canAddPartner_withGeneratedUuid() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPerson = personRepo.findPersonByOptionalNameLike("Ostfriesische").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("forth").get(0);

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                               {
                                   "contactUuid": "%s",
                                   "personUuid": "%s",
                                   "registrationOffice": "Registergericht Hamburg",
                                   "registrationNumber": "123456",
                                   "birthName": null,
                                   "birthday": null,
                                   "dateOfDeath": null
                                 }
                            """.formatted(givenContact.getUuid(), givenPerson.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/partners")
                    .then().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("registrationNumber", is("123456"))
                        .body("contact.label", is(givenContact.getLabel()))
                        .body("person.tradeName", is(givenPerson.getTradeName()))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new partner can be accessed under the generated UUID
            final var newUserUuid = toCleanup(UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1)));
            assertThat(newUserUuid).isNotNull();
        }

        @Test
        void globalAdmin_canNotAddPartner_ifContactDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPerson = personRepo.findPersonByOptionalNameLike("Ostfriesische").get(0);
            final var givenContactUuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "contactUuid": "%s",
                                   "personUuid": "%s",
                                   "registrationOffice": "Registergericht Hamburg",
                                   "registrationNumber": "123456",
                                   "birthName": null,
                                   "birthday": null,
                                   "dateOfDeath": null
                                 }
                            """.formatted(givenContactUuid, givenPerson.getUuid()))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/partners")
                .then().log().all().assertThat()
                    .statusCode(404)
                    .body("message", is("cannot find contact uuid 3fa85f64-5717-4562-b3fc-2c963f66afa6"));
            // @formatter:on
        }

        @Test
        void globalAdmin_canNotAddPartner_ifPersonDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenPersonUuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
            final var givenContact = contactRepo.findContactByOptionalLabelLike("forth").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "contactUuid": "%s",
                                   "personUuid": "%s",
                                   "registrationOffice": "Registergericht Hamburg",
                                   "registrationNumber": "123456",
                                   "birthName": null,
                                   "birthday": null,
                                   "dateOfDeath": null
                                 }
                            """.formatted(givenContact.getUuid(), givenPersonUuid))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/partners")
                .then().log().all().assertThat()
                    .statusCode(404)
                    .body("message", is("cannot find person uuid 3fa85f64-5717-4562-b3fc-2c963f66afa6"));
                // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Partner:R(Read)" })
    class GetPartner {

        @Test
        void globalAdmin_withoutAssumedRole_canGetArbitraryPartner() {
            context.define("superuser-alex@hostsharing.net");
            final var givenPartnerUuid = partnerRepo.findPartnerByOptionalNameLike("First").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/partners/" + givenPartnerUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                        "person": { "tradeName": "First Impressions GmbH" },
                        "contact": { "label": "first contact" }
                    }
                    """)); // @formatter:on
        }

        @Test
        @Accepts({ "Partner:X(Access Control)" })
        void normalUser_canNotGetUnrelatedPartner() {
            context.define("superuser-alex@hostsharing.net");
            final var givenPartnerUuid = partnerRepo.findPartnerByOptionalNameLike("First").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/partners/" + givenPartnerUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        @Accepts({ "Partner:X(Access Control)" })
        void contactAdminUser_canGetRelatedPartner() {
            context.define("superuser-alex@hostsharing.net");
            final var givenPartnerUuid = partnerRepo.findPartnerByOptionalNameLike("first contact").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "customer-admin@firstcontact.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/partners/" + givenPartnerUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                        "person": { "tradeName": "First Impressions GmbH" },
                        "contact": { "label": "first contact" }
                    }
                    """)); // @formatter:on
        }
    }

    private UUID toCleanup(final UUID tempPartnerUuid) {
        tempPartnerUuids.add(tempPartnerUuid);
        return tempPartnerUuid;
    }

    @AfterEach
    void cleanup() {
        tempPartnerUuids.forEach(uuid -> {
            jpaAttempt.transacted(() -> {
                context.define("superuser-alex@hostsharing.net", null);
                System.out.println("DELETING temporary partner: " + uuid);
                final var count = partnerRepo.deleteByUuid(uuid);
                assertThat(count).isGreaterThan(0);
            });
        });
    }

}
