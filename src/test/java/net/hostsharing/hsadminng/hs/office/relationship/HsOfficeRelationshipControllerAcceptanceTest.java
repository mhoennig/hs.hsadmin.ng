package net.hostsharing.hsadminng.hs.office.relationship;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.office.test.ContextBasedTestWithCleanup;
import net.hostsharing.test.Accepts;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRepository;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeRelationshipTypeResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRepository;
import net.hostsharing.test.JpaAttempt;
import org.json.JSONException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

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
class HsOfficeRelationshipControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    public static final UUID GIVEN_NON_EXISTING_HOLDER_PERSON_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    Context contextMock;

    @Autowired
    HsOfficeRelationshipRepository relationshipRepo;

    @Autowired
    HsOfficePersonRepository personRepo;

    @Autowired
    HsOfficeContactRepository contactRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @Nested
    @Accepts({ "Relationship:F(Find)" })
    class ListRelationships {

        @Test
        void globalAdmin_withoutAssumedRoles_canViewAllRelationshipsOfGivenPersonAndType_ifNoCriteriaGiven() throws JSONException {

            // given
            context.define("superuser-alex@hostsharing.net");
            final var givenPerson = personRepo.findPersonByOptionalNameLike("Hostsharing eG").get(0);

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/relationships?personUuid=%s&relationshipType=%s"
                            .formatted(givenPerson.getUuid(), HsOfficeRelationshipTypeResource.PARTNER))
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    [
                        {
                            "relAnchor": { "personType": "LEGAL_PERSON", "tradeName": "Hostsharing eG" },
                            "relHolder": { "personType": "LEGAL_PERSON", "tradeName": "First GmbH" },
                            "relType": "PARTNER",
                            "relMark": null,
                            "contact": { "label": "first contact" }
                        },
                        {
                            "relAnchor": { "personType": "LEGAL_PERSON", "tradeName": "Hostsharing eG" },
                            "relHolder": { "personType": "INCORPORATED_FIRM", "tradeName": "Fourth eG" },
                            "relType": "PARTNER",
                            "contact": { "label": "fourth contact" }
                        },
                        {
                            "relAnchor": { "personType": "LEGAL_PERSON", "tradeName": "Hostsharing eG" },
                            "relHolder": { "personType": "LEGAL_PERSON", "tradeName": "Second e.K.", "givenName": "Peter", "familyName": "Smith" },
                            "relType": "PARTNER",
                            "relMark": null,
                            "contact": { "label": "second contact" }
                        },
                        {
                            "relAnchor": { "personType": "LEGAL_PERSON", "tradeName": "Hostsharing eG" },
                            "relHolder": { "personType": "NATURAL_PERSON", "givenName": "Peter", "familyName": "Smith" },
                            "relType": "PARTNER",
                            "relMark": null,
                            "contact": { "label": "sixth contact" }
                        },
                        {
                            "relAnchor": { "personType": "LEGAL_PERSON", "tradeName": "Hostsharing eG" },
                            "relHolder": { "personType": "INCORPORATED_FIRM", "tradeName": "Third OHG" },
                            "relType": "PARTNER",
                            "relMark": null,
                            "contact": { "label": "third contact" }
                        }
                    ]
                    """));
                // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Relationship:C(Create)" })
    class AddRelationship {

        @Test
        void globalAdmin_withoutAssumedRole_canAddRelationship() {

            context.define("superuser-alex@hostsharing.net");
            final var givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Third").get(0);
            final var givenHolderPerson = personRepo.findPersonByOptionalNameLike("Paul").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("second").get(0);

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                               {
                                   "relType": "%s",
                                   "relAnchorUuid": "%s",
                                   "relHolderUuid": "%s",
                                   "contactUuid": "%s"
                                 }
                            """.formatted(
                                HsOfficeRelationshipTypeResource.ACCOUNTING,
                                givenAnchorPerson.getUuid(),
                                givenHolderPerson.getUuid(),
                                givenContact.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/relationships")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("relType", is("ACCOUNTING"))
                        .body("relAnchor.tradeName", is("Third OHG"))
                        .body("relHolder.givenName", is("Paul"))
                        .body("contact.label", is("second contact"))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new relationship can be accessed under the generated UUID
            final var newUserUuid = toCleanup(HsOfficeRelationshipEntity.class, UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1)));
            assertThat(newUserUuid).isNotNull();
        }

        @Test
        void globalAdmin_canNotAddRelationship_ifAnchorPersonDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenAnchorPersonUuid = GIVEN_NON_EXISTING_HOLDER_PERSON_UUID;
            final var givenHolderPerson = personRepo.findPersonByOptionalNameLike("Smith").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("fourth").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "relType": "%s",
                                   "relAnchorUuid": "%s",
                                   "relHolderUuid": "%s",
                                   "contactUuid": "%s"
                                 }
                            """.formatted(
                            HsOfficeRelationshipTypeResource.ACCOUNTING,
                            givenAnchorPersonUuid,
                            givenHolderPerson.getUuid(),
                            givenContact.getUuid()))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/relationships")
                .then().log().all().assertThat()
                    .statusCode(404)
                    .body("message", is("cannot find relAnchorUuid " + GIVEN_NON_EXISTING_HOLDER_PERSON_UUID));
            // @formatter:on
        }

        @Test
        void globalAdmin_canNotAddRelationship_ifHolderPersonDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Third").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("fourth").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "relType": "%s",
                                   "relAnchorUuid": "%s",
                                   "relHolderUuid": "%s",
                                   "contactUuid": "%s"
                                 }
                            """.formatted(
                            HsOfficeRelationshipTypeResource.ACCOUNTING,
                            givenAnchorPerson.getUuid(),
                            GIVEN_NON_EXISTING_HOLDER_PERSON_UUID,
                            givenContact.getUuid()))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/relationships")
                .then().log().all().assertThat()
                    .statusCode(404)
                    .body("message", is("cannot find relHolderUuid " + GIVEN_NON_EXISTING_HOLDER_PERSON_UUID));
            // @formatter:on
        }

        @Test
        void globalAdmin_canNotAddRelationship_ifContactDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Third").get(0);
            final var givenHolderPerson = personRepo.findPersonByOptionalNameLike("Paul").get(0);
            final var givenContactUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                           {
                               "relType": "%s",
                               "relAnchorUuid": "%s",
                               "relHolderUuid": "%s",
                               "contactUuid": "%s"
                             }
                            """.formatted(
                                    HsOfficeRelationshipTypeResource.ACCOUNTING,
                                    givenAnchorPerson.getUuid(),
                                    givenHolderPerson.getUuid(),
                                    givenContactUuid))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/relationships")
                .then().log().all().assertThat()
                    .statusCode(404)
                    .body("message", is("cannot find contactUuid 00000000-0000-0000-0000-000000000000"));
            // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Relationship:R(Read)" })
    class GetRelationship {

        @Test
        void globalAdmin_withoutAssumedRole_canGetArbitraryRelationship() {
            context.define("superuser-alex@hostsharing.net");
            final UUID givenRelationshipUuid = findRelationship("First", "Firby").getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/relationships/" + givenRelationshipUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                        "relAnchor": { "tradeName": "First GmbH" },
                        "relHolder": { "familyName": "Firby" },
                        "contact": { "label": "first contact" }
                    }
                    """)); // @formatter:on
        }

        @Test
        @Accepts({ "Relationship:X(Access Control)" })
        void normalUser_canNotGetUnrelatedRelationship() {
            context.define("superuser-alex@hostsharing.net");
            final UUID givenRelationshipUuid = findRelationship("First", "Firby").getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/relationships/" + givenRelationshipUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        @Accepts({ "Relationship:X(Access Control)" })
        void contactAdminUser_canGetRelatedRelationship() {
            context.define("superuser-alex@hostsharing.net");
            final var givenRelationship = findRelationship("First", "Firby");
            assertThat(givenRelationship.getContact().getLabel()).isEqualTo("first contact");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "contact-admin@firstcontact.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/relationships/" + givenRelationship.getUuid())
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                        "relAnchor": { "tradeName": "First GmbH" },
                        "relHolder": { "familyName": "Firby" },
                        "contact": { "label": "first contact" }
                    }
                    """)); // @formatter:on
        }
    }

    private HsOfficeRelationshipEntity findRelationship(
            final String anchorPersonName,
            final String holderPersoneName) {
        final var anchorPersonUuid = personRepo.findPersonByOptionalNameLike(anchorPersonName).get(0).getUuid();
        final var holderPersonUuid = personRepo.findPersonByOptionalNameLike(holderPersoneName).get(0).getUuid();
        final var givenRelationship = relationshipRepo
                .findRelationshipRelatedToPersonUuid(anchorPersonUuid)
                .stream()
                .filter(r -> r.getRelHolder().getUuid().equals(holderPersonUuid))
                .findFirst().orElseThrow();
        return givenRelationship;
    }

    @Nested
    @Accepts({ "Relationship:U(Update)" })
    class PatchRelationship {

        @Test
        void globalAdmin_withoutAssumedRole_canPatchContactOfArbitraryRelationship() {

            context.define("superuser-alex@hostsharing.net");
            final var givenRelationship = givenSomeTemporaryRelationshipBessler();
            assertThat(givenRelationship.getContact().getLabel()).isEqualTo("seventh contact");
            final var givenContact = contactRepo.findContactByOptionalLabelLike("fourth").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                           {
                              "contactUuid": "%s"
                           }
                            """.formatted(givenContact.getUuid()))
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/relationships/" + givenRelationship.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("relType", is("REPRESENTATIVE"))
                    .body("relAnchor.tradeName", is("Erben Bessler"))
                    .body("relHolder.familyName", is("Winkler"))
                    .body("contact.label", is("fourth contact"));
                // @formatter:on

            // finally, the relationship is actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(relationshipRepo.findByUuid(givenRelationship.getUuid())).isPresent().get()
                    .matches(rel -> {
                        assertThat(rel.getRelAnchor().getTradeName()).contains("Bessler");
                        assertThat(rel.getRelHolder().getFamilyName()).contains("Winkler");
                        assertThat(rel.getContact().getLabel()).isEqualTo("fourth contact");
                        assertThat(rel.getRelType()).isEqualTo(HsOfficeRelationshipType.REPRESENTATIVE);
                        return true;
                    });
        }
    }

    @Nested
    @Accepts({ "Relationship:D(Delete)" })
    class DeleteRelationship {

        @Test
        void globalAdmin_withoutAssumedRole_canDeleteArbitraryRelationship() {
            context.define("superuser-alex@hostsharing.net");
            final var givenRelationship = givenSomeTemporaryRelationshipBessler();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/relationships/" + givenRelationship.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given relationship is gone
            assertThat(relationshipRepo.findByUuid(givenRelationship.getUuid())).isEmpty();
        }

        @Test
        @Accepts({ "Relationship:X(Access Control)" })
        void contactAdminUser_canNotDeleteRelatedRelationship() {
            context.define("superuser-alex@hostsharing.net");
            final var givenRelationship = givenSomeTemporaryRelationshipBessler();
            assertThat(givenRelationship.getContact().getLabel()).isEqualTo("seventh contact");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "contact-admin@seventhcontact.example.com")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/relationships/" + givenRelationship.getUuid())
                .then().log().body().assertThat()
                    .statusCode(403); // @formatter:on

            // then the given relationship is still there
            assertThat(relationshipRepo.findByUuid(givenRelationship.getUuid())).isNotEmpty();
        }

        @Test
        @Accepts({ "Relationship:X(Access Control)" })
        void normalUser_canNotDeleteUnrelatedRelationship() {
            context.define("superuser-alex@hostsharing.net");
            final var givenRelationship = givenSomeTemporaryRelationshipBessler();
            assertThat(givenRelationship.getContact().getLabel()).isEqualTo("seventh contact");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/relationships/" + givenRelationship.getUuid())
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on

            // then the given relationship is still there
            assertThat(relationshipRepo.findByUuid(givenRelationship.getUuid())).isNotEmpty();
        }
    }

    private HsOfficeRelationshipEntity givenSomeTemporaryRelationshipBessler() {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Erben Bessler").get(0);
            final var givenHolderPerson = personRepo.findPersonByOptionalNameLike("Winkler").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("seventh contact").get(0);
            final var newRelationship = HsOfficeRelationshipEntity.builder()
                    .relType(HsOfficeRelationshipType.REPRESENTATIVE)
                    .relAnchor(givenAnchorPerson)
                    .relHolder(givenHolderPerson)
                    .contact(givenContact)
                    .build();

            assertThat(toCleanup(relationshipRepo.save(newRelationship))).isEqualTo(newRelationship);

            return newRelationship;
        }).assertSuccessful().returnedValue();
    }

}
