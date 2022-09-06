package net.hostsharing.hsadminng.hs.admin.partner;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.Accepts;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static net.hostsharing.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.test.JsonBuilder.jsonObject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = HsadminNgApplication.class
)
@Transactional
class HsAdminPartnerControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    Context contextMock;
    @Autowired
    HsAdminPartnerRepository partnerRepository;

    @Nested
    @Accepts({ "Partner:F(Find)" })
    class ListPartners {

        @Test
        void testHostsharingAdmin_withoutAssumedRoles_canViewAllPartners_ifNoCriteriaGiven() {
            RestAssured // @formatter:off
                .given()
                    .header("current-user", "mike@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/admin/partners")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].contact.label", is("Ixx AG"))
                    .body("[0].person.tradeName", is("Ixx AG"))
                    .body("[1].contact.label", is("Ypsilon GmbH"))
                    .body("[1].person.tradeName", is("Ypsilon GmbH"))
                    .body("[2].contact.label", is("Zett OHG"))
                    .body("[2].person.tradeName", is("Zett OHG"))
                    .body("size()", greaterThanOrEqualTo(3));
                // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Partner:C(Create)" })
    class AddPartner {

        private final static String NEW_PARTNER_JSON_WITHOUT_UUID =
                """
                                {
                                   "person": {
                                     "personType": "LEGAL",
                                     "tradeName": "Test Corp.",
                                     "givenName": null,
                                     "familyName": null
                                   },
                                   "contact": {
                                     "label": "Test Corp.",
                                     "postalAddress": "Test Corp.\\nTestweg 50\\n20001 Hamburg",
                                     "emailAddresses": "office@example.com",
                                     "phoneNumbers": "040 12345"
                                   },
                                   "registrationOffice": "Registergericht Hamburg",
                                   "registrationNumber": "123456",
                                   "birthName": null,
                                   "birthday": null,
                                   "dateOfDeath": null
                                 }
                        """;

        @Test
        void hostsharingAdmin_withoutAssumedRole_canAddPartner_withExplicitUuid() {

            final var givenUUID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "mike@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body(jsonObject(NEW_PARTNER_JSON_WITHOUT_UUID)
                                .with("uuid", givenUUID.toString()).toString())
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/admin/partners")
                    .then().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", is("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
                        .body("registrationNumber", is("123456"))
                        .body("person.tradeName", is("Test Corp."))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new partner can be viewed by its own admin
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newUserUuid).isEqualTo(givenUUID);
            // TODO: context.define("partner-admin@ttt.example.com");
            // assertThat(partnerRepository.findByUuid(newUserUuid))
            //        .hasValueSatisfying(c -> assertThat(c.getPerson().getTradeName()).isEqualTo("Test Corp."));
        }

        @Test
        void hostsharingAdmin_withoutAssumedRole_canAddPartner_withGeneratedUuid() {

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "mike@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body(NEW_PARTNER_JSON_WITHOUT_UUID)
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/admin/partners")
                    .then().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("registrationNumber", is("123456"))
                        .body("person.tradeName", is("Test Corp."))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new partner can be viewed by its own admin
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newUserUuid).isNotNull();
            // TODO: context.define("partner-admin@ttt.example.com");
            // assertThat(partnerRepository.findByUuid(newUserUuid))
            //        .hasValueSatisfying(c -> assertThat(c.getPerson().getTradeName()).isEqualTo("Test Corp."));
        }
    }

    @Nested
    @Accepts({ "Partner:R(Read)" })
    class GetPartner {

        @Test
        void hostsharingAdmin_withoutAssumedRole_canGetArbitraryPartner() {
            // TODO: final var givenPartnerUuid = partnerRepository.findPartnerByOptionalNameLike("Ixx").get(0).getUuid();
            final var givenPartnerUuid = UUID.randomUUID();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "mike@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/admin/partners/" + givenPartnerUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("person.tradeName", is("Ixx AG"))
                    .body("contact.label", is("Ixx AG"));
                // @formatter:on
        }

        @Test
        @Accepts({ "Partner:X(Access Control)" })
        void normalUser_canNotGetUnrelatedPartner() {
            // TODO: final var givenPartnerUuid = partnerRepository.findPartnerByOptionalNameLike("Ixx").get(0).getUuid();
            final UUID givenPartnerUuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "somebody@example.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/admin/partners/" + givenPartnerUuid)
                .then().log().body().assertThat()
                    .statusCode(404);
            // @formatter:on
        }
    }
}
