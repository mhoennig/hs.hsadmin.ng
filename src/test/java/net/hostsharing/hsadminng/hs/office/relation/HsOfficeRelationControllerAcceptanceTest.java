package net.hostsharing.hsadminng.hs.office.relation;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.office.test.ContextBasedTestWithCleanup;
import net.hostsharing.test.Accepts;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRepository;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeRelationTypeResource;
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
class HsOfficeRelationControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    public static final UUID GIVEN_NON_EXISTING_HOLDER_PERSON_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    Context contextMock;

    @Autowired
    HsOfficeRelationRepository relationRepo;

    @Autowired
    HsOfficePersonRepository personRepo;

    @Autowired
    HsOfficeContactRepository contactRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @Nested
    @Accepts({ "Relation:F(Find)" })
    class ListRelations {

        @Test
        void globalAdmin_withoutAssumedRoles_canViewAllRelationsOfGivenPersonAndType_ifNoCriteriaGiven() throws JSONException {

            // given
            context.define("superuser-alex@hostsharing.net");
            final var givenPerson = personRepo.findPersonByOptionalNameLike("Hostsharing eG").get(0);

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/relations?personUuid=%s&relationType=%s"
                            .formatted(givenPerson.getUuid(), HsOfficeRelationTypeResource.PARTNER))
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    [
                        {
                            "anchor": { "personType": "LEGAL_PERSON", "tradeName": "Hostsharing eG" },
                            "holder": { "personType": "LEGAL_PERSON", "tradeName": "First GmbH" },
                            "type": "PARTNER",
                            "mark": null,
                            "contact": { "label": "first contact" }
                        },
                        {
                            "anchor": { "personType": "LEGAL_PERSON", "tradeName": "Hostsharing eG" },
                            "holder": { "personType": "INCORPORATED_FIRM", "tradeName": "Fourth eG" },
                            "type": "PARTNER",
                            "contact": { "label": "fourth contact" }
                        },
                        {
                            "anchor": { "personType": "LEGAL_PERSON", "tradeName": "Hostsharing eG" },
                            "holder": { "personType": "LEGAL_PERSON", "tradeName": "Second e.K.", "givenName": "Peter", "familyName": "Smith" },
                            "type": "PARTNER",
                            "mark": null,
                            "contact": { "label": "second contact" }
                        },
                        {
                            "anchor": { "personType": "LEGAL_PERSON", "tradeName": "Hostsharing eG" },
                            "holder": { "personType": "NATURAL_PERSON", "givenName": "Peter", "familyName": "Smith" },
                            "type": "PARTNER",
                            "mark": null,
                            "contact": { "label": "sixth contact" }
                        },
                        {
                            "anchor": { "personType": "LEGAL_PERSON", "tradeName": "Hostsharing eG" },
                            "holder": { "personType": "INCORPORATED_FIRM", "tradeName": "Third OHG" },
                            "type": "PARTNER",
                            "mark": null,
                            "contact": { "label": "third contact" }
                        }
                    ]
                    """));
                // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Relation:C(Create)" })
    class AddRelation {

        @Test
        void globalAdmin_withoutAssumedRole_canAddRelation() {

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
                                   "type": "%s",
                                   "mark": "%s",
                                   "anchorUuid": "%s",
                                   "holderUuid": "%s",
                                   "contactUuid": "%s"
                                 }
                            """.formatted(
                                HsOfficeRelationTypeResource.SUBSCRIBER,
                                "operations-discuss",
                                givenAnchorPerson.getUuid(),
                                givenHolderPerson.getUuid(),
                                givenContact.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/relations")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("type", is("SUBSCRIBER"))
                        .body("mark", is("operations-discuss"))
                        .body("anchor.tradeName", is("Third OHG"))
                        .body("holder.givenName", is("Paul"))
                        .body("contact.label", is("second contact"))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new relation can be accessed under the generated UUID
            final var newUserUuid = toCleanup(HsOfficeRelationEntity.class, UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1)));
            assertThat(newUserUuid).isNotNull();
        }

        @Test
        void globalAdmin_canNotAddRelation_ifAnchorPersonDoesNotExist() {

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
                                   "type": "%s",
                                   "anchorUuid": "%s",
                                   "holderUuid": "%s",
                                   "contactUuid": "%s"
                                 }
                            """.formatted(
                            HsOfficeRelationTypeResource.DEBITOR,
                            givenAnchorPersonUuid,
                            givenHolderPerson.getUuid(),
                            givenContact.getUuid()))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/relations")
                .then().log().all().assertThat()
                    .statusCode(404)
                    .body("message", is("cannot find anchorUuid " + GIVEN_NON_EXISTING_HOLDER_PERSON_UUID));
            // @formatter:on
        }

        @Test
        void globalAdmin_canNotAddRelation_ifHolderPersonDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Third").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("fourth").get(0);

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "type": "%s",
                                   "anchorUuid": "%s",
                                   "holderUuid": "%s",
                                   "contactUuid": "%s"
                                 }
                            """.formatted(
                            HsOfficeRelationTypeResource.DEBITOR,
                            givenAnchorPerson.getUuid(),
                            GIVEN_NON_EXISTING_HOLDER_PERSON_UUID,
                            givenContact.getUuid()))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/relations")
                .then().log().all().assertThat()
                    .statusCode(404)
                    .body("message", is("cannot find holderUuid " + GIVEN_NON_EXISTING_HOLDER_PERSON_UUID));
            // @formatter:on
        }

        @Test
        void globalAdmin_canNotAddRelation_ifContactDoesNotExist() {

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
                               "type": "%s",
                               "anchorUuid": "%s",
                               "holderUuid": "%s",
                               "contactUuid": "%s"
                             }
                            """.formatted(
                                    HsOfficeRelationTypeResource.DEBITOR,
                                    givenAnchorPerson.getUuid(),
                                    givenHolderPerson.getUuid(),
                                    givenContactUuid))
                    .port(port)
                .when()
                    .post("http://localhost/api/hs/office/relations")
                .then().log().all().assertThat()
                    .statusCode(404)
                    .body("message", is("cannot find contactUuid 00000000-0000-0000-0000-000000000000"));
            // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Relation:R(Read)" })
    class GetRelation {

        @Test
        void globalAdmin_withoutAssumedRole_canGetArbitraryRelation() {
            context.define("superuser-alex@hostsharing.net");
            final UUID givenRelationUuid = findRelation("First", "Firby").getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/relations/" + givenRelationUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                        "anchor": { "tradeName": "First GmbH" },
                        "holder": { "familyName": "Firby" },
                        "contact": { "label": "first contact" }
                    }
                    """)); // @formatter:on
        }

        @Test
        @Accepts({ "Relation:X(Access Control)" })
        void normalUser_canNotGetUnrelatedRelation() {
            context.define("superuser-alex@hostsharing.net");
            final UUID givenRelationUuid = findRelation("First", "Firby").getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/relations/" + givenRelationUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        @Accepts({ "Relation:X(Access Control)" })
        void contactAdminUser_canGetRelatedRelation() {
            context.define("superuser-alex@hostsharing.net");
            final var givenRelation = findRelation("First", "Firby");
            assertThat(givenRelation.getContact().getLabel()).isEqualTo("first contact");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "contact-admin@firstcontact.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/relations/" + givenRelation.getUuid())
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                        "anchor": { "tradeName": "First GmbH" },
                        "holder": { "familyName": "Firby" },
                        "contact": { "label": "first contact" }
                    }
                    """)); // @formatter:on
        }
    }

    private HsOfficeRelationEntity findRelation(
            final String anchorPersonName,
            final String holderPersoneName) {
        final var anchorPersonUuid = personRepo.findPersonByOptionalNameLike(anchorPersonName).get(0).getUuid();
        final var holderPersonUuid = personRepo.findPersonByOptionalNameLike(holderPersoneName).get(0).getUuid();
        final var givenRelation = relationRepo
                .findRelationRelatedToPersonUuid(anchorPersonUuid)
                .stream()
                .filter(r -> r.getHolder().getUuid().equals(holderPersonUuid))
                .findFirst().orElseThrow();
        return givenRelation;
    }

    @Nested
    @Accepts({ "Relation:U(Update)" })
    class PatchRelation {

        @Test
        void globalAdmin_withoutAssumedRole_canPatchContactOfArbitraryRelation() {

            context.define("superuser-alex@hostsharing.net");
            final var givenRelation = givenSomeTemporaryRelationBessler();
            assertThat(givenRelation.getContact().getLabel()).isEqualTo("seventh contact");
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
                    .patch("http://localhost/api/hs/office/relations/" + givenRelation.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("type", is("REPRESENTATIVE"))
                    .body("anchor.tradeName", is("Erben Bessler"))
                    .body("holder.familyName", is("Winkler"))
                    .body("contact.label", is("fourth contact"));
                // @formatter:on

            // finally, the relation is actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(relationRepo.findByUuid(givenRelation.getUuid())).isPresent().get()
                    .matches(rel -> {
                        assertThat(rel.getAnchor().getTradeName()).contains("Bessler");
                        assertThat(rel.getHolder().getFamilyName()).contains("Winkler");
                        assertThat(rel.getContact().getLabel()).isEqualTo("fourth contact");
                        assertThat(rel.getType()).isEqualTo(HsOfficeRelationType.REPRESENTATIVE);
                        return true;
                    });
        }
    }

    @Nested
    @Accepts({ "Relation:D(Delete)" })
    class DeleteRelation {

        @Test
        void globalAdmin_withoutAssumedRole_canDeleteArbitraryRelation() {
            context.define("superuser-alex@hostsharing.net");
            final var givenRelation = givenSomeTemporaryRelationBessler();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/relations/" + givenRelation.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given relation is gone
            assertThat(relationRepo.findByUuid(givenRelation.getUuid())).isEmpty();
        }

        @Test
        @Accepts({ "Relation:X(Access Control)" })
        void contactAdminUser_canNotDeleteRelatedRelation() {
            context.define("superuser-alex@hostsharing.net");
            final var givenRelation = givenSomeTemporaryRelationBessler();
            assertThat(givenRelation.getContact().getLabel()).isEqualTo("seventh contact");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "contact-admin@seventhcontact.example.com")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/relations/" + givenRelation.getUuid())
                .then().log().body().assertThat()
                    .statusCode(403); // @formatter:on

            // then the given relation is still there
            assertThat(relationRepo.findByUuid(givenRelation.getUuid())).isNotEmpty();
        }

        @Test
        @Accepts({ "Relation:X(Access Control)" })
        void normalUser_canNotDeleteUnrelatedRelation() {
            context.define("superuser-alex@hostsharing.net");
            final var givenRelation = givenSomeTemporaryRelationBessler();
            assertThat(givenRelation.getContact().getLabel()).isEqualTo("seventh contact");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/relations/" + givenRelation.getUuid())
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on

            // then the given relation is still there
            assertThat(relationRepo.findByUuid(givenRelation.getUuid())).isNotEmpty();
        }
    }

    private HsOfficeRelationEntity givenSomeTemporaryRelationBessler() {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var givenAnchorPerson = personRepo.findPersonByOptionalNameLike("Erben Bessler").get(0);
            final var givenHolderPerson = personRepo.findPersonByOptionalNameLike("Winkler").get(0);
            final var givenContact = contactRepo.findContactByOptionalLabelLike("seventh contact").get(0);
            final var newRelation = HsOfficeRelationEntity.builder()
                    .type(HsOfficeRelationType.REPRESENTATIVE)
                    .anchor(givenAnchorPerson)
                    .holder(givenHolderPerson)
                    .contact(givenContact)
                    .build();

            assertThat(toCleanup(relationRepo.save(newRelation))).isEqualTo(newRelation);

            return newRelation;
        }).assertSuccessful().returnedValue();
    }

}
