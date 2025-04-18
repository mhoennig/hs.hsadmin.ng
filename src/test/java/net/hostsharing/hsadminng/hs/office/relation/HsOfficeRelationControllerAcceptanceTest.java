package net.hostsharing.hsadminng.hs.office.relation;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealRepository;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonRealRepository;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeRelationTypeResource;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static net.hostsharing.hsadminng.rbac.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.hasEntry;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, DisableSecurityConfig.class, JpaAttempt.class }
)
@ActiveProfiles("test")
@Transactional
@Tag("officeIntegrationTest")
class HsOfficeRelationControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    public static final UUID GIVEN_NON_EXISTING_HOLDER_PERSON_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    HsOfficeRelationRealRepository realRelationRepo;

    @Autowired
    HsOfficePersonRealRepository realPersonRepo;

    @Autowired
    HsOfficeContactRealRepository realContactRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @Nested
    class GetListOfRelations {

        @Test
        void globalAdmin_withoutAssumedRoles_canViewAllRelationsOfGivenPersonAndType() {

            // given
            context.define("superuser-alex@hostsharing.net");
            final var givenPerson = realPersonRepo.findPersonByOptionalNameLike("Hostsharing eG").getFirst();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
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
                            "contact": { "caption": "first contact" }
                        },
                        {
                            "anchor": { "personType": "LEGAL_PERSON", "tradeName": "Hostsharing eG" },
                            "holder": { "personType": "LEGAL_PERSON", "tradeName": "Fourth eG" },
                            "type": "PARTNER",
                            "contact": { "caption": "fourth contact" }
                        },
                        {
                            "anchor": { "personType": "LEGAL_PERSON", "tradeName": "Hostsharing eG" },
                            "holder": { "personType": "LEGAL_PERSON", "tradeName": "Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K.", "givenName": "Peter", "familyName": "Smith" },
                            "type": "PARTNER",
                            "mark": null,
                            "contact": { "caption": "second contact" }
                        },
                        {
                            "anchor": { "personType": "LEGAL_PERSON", "tradeName": "Hostsharing eG" },
                            "holder": { "personType": "NATURAL_PERSON", "givenName": "Peter", "familyName": "Smith" },
                            "type": "PARTNER",
                            "mark": null,
                            "contact": { "caption": "sixth contact" }
                        },
                        {
                            "anchor": { "personType": "LEGAL_PERSON", "tradeName": "Hostsharing eG" },
                            "holder": { "personType": "INCORPORATED_FIRM", "tradeName": "Third OHG" },
                            "type": "PARTNER",
                            "mark": null,
                            "contact": { "caption": "third contact" }
                        }
                    ]
                    """));
                // @formatter:on
        }

        @Test
        void personAdmin_canViewAllRelationsOfGivenRelatedPersonAndAnyType() {

            // given
            context.define("contact-admin@firstcontact.example.com");
            final var givenPerson = realPersonRepo.findPersonByOptionalNameLike("First GmbH").getFirst();

            RestAssured // @formatter:off
                    .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                    .port(port)
                    .when()
                    .get("http://localhost/api/hs/office/relations?personUuid=%s"
                            .formatted(givenPerson.getUuid()))
                    .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    [
                         {
                             "anchor": {
                                 "tradeName": "First GmbH"
                             },
                             "holder": {
                                 "givenName": "Susan",
                                 "familyName": "Firby"
                             },
                             "type": "REPRESENTATIVE",
                             "mark": null,
                             "contact": { "caption": "first contact" }
                         },
                         {
                             "anchor": {
                                 "tradeName": "Hostsharing eG"
                             },
                             "holder": {
                                 "tradeName": "First GmbH"
                             },
                             "type": "PARTNER",
                             "mark": null,
                             "contact": { "caption": "first contact" }
                         },
                         {
                             "anchor": {
                                 "tradeName": "First GmbH"
                             },
                             "holder": {
                                 "tradeName": "First GmbH"
                             },
                             "type": "DEBITOR",
                             "mark": null,
                             "contact": { "caption": "first contact" }
                         }
                     ]
                    """));
            // @formatter:on
        }

        @Test
        void globalAdmin_canViewAllRelationsWithGivenContactData() {

            // given
            context.define("superuser-alex@hostsharing.net");

            RestAssured // @formatter:off
                    .given()
                        .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                        .port(port)
                    .when()
                        .get("http://localhost/api/hs/office/relations?personData=firby&contactData=Contact-Admin@FirstContact.Example.COM")
                    .then().log().all().assertThat()
                        .statusCode(200)
                        .contentType("application/json")
                        .body("", lenientlyEquals("""
                        [
                            {
                                "anchor": {
                                    "personType": "LEGAL_PERSON",
                                    "tradeName": "First GmbH"
                                },
                                "holder": {
                                    "personType": "NATURAL_PERSON",
                                    "givenName": "Susan",
                                    "familyName": "Firby"
                                },
                                "type": "REPRESENTATIVE",
                                "contact": {
                                    "caption": "first contact",
                                    "postalAddress": {
                                        "country": "Germany"
                                    },
                                    "emailAddresses": {
                                        "main": "contact-admin@firstcontact.example.com"
                                    },
                                    "phoneNumbers": {
                                        "phone_office": "+49 123 1234567"
                                    }
                                }
                            }
                        ]
                        """));
            // @formatter:on
        }
    }

    @Nested
    class AddRelation {

        @Test
        void globalAdmin_withoutAssumedRole_canAddRelationWithHolderUuidAndContactUuid() {

            context.define("superuser-alex@hostsharing.net");
            final var givenAnchorPerson = realPersonRepo.findPersonByOptionalNameLike("Third").getFirst();
            final var givenHolderPerson = realPersonRepo.findPersonByOptionalNameLike("Paul").getFirst();
            final var givenContact = realContactRepo.findContactByOptionalCaptionLike("second").getFirst();

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                               {
                                   "type": "%s",
                                   "mark": "%s",
                                   "anchor.uuid": "%s",
                                   "holder.uuid": "%s",
                                   "contact.uuid": "%s"
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
                        .body("contact.caption", is("second contact"))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location"); // @formatter:on

            // finally, the new relation can be accessed under the generated UUID
            final var newSubjectUuid = toCleanup(HsOfficeRelationRealEntity.class, UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1)));
            assertThat(newSubjectUuid).isNotNull();
        }

        @Test
        void globalAdmin_withoutAssumedRole_canAddRelationWithHolderAndContactData() {

            context.define("superuser-alex@hostsharing.net");
            final var givenAnchorPerson = realPersonRepo.findPersonByOptionalNameLike("Third").getFirst();

            final var location = RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                                "type": "%s",
                                "mark": "%s",
                                "anchor.uuid": "%s",
                                "holder":   {
                                   "personType": "NATURAL_PERSON",
                                   "familyName": "Person",
                                   "givenName": "Temp"
                                 },
                                "contact": {
                                   "caption": "Temp Contact",
                                   "postalAddress": {
                                        "name": "Herr Test Contact",
                                        "firm": "Test Contact GmbH",
                                        "street": "Am Schieferbruch 3",
                                        "zipcode": "12345",
                                        "city": "Dachstadt",
                                        "country": "Germany"
                                    },
                                   "emailAddresses": {
                                        "main": "test@example.org"
                                   }
                                 }
                            }
                            """.formatted(
                                    HsOfficeRelationTypeResource.SUBSCRIBER,
                                    "operations-discuss",
                                    givenAnchorPerson.getUuid()
                            )
                    )
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
                    .body("holder.givenName", is("Temp"))
                    .body("holder.familyName", is("Person"))
                    .body("contact.caption", is("Temp Contact"))
                    .body("contact.emailAddresses", is(Map.of("main", "test@example.org")))
                    .body("contact.postalAddress", hasEntry("name", "Herr Test Contact"))
                    .body("contact.postalAddress", hasEntry("street", "Am Schieferbruch 3"))
                    .header("Location", startsWith("http://localhost"))
                    .extract().header("Location"); // @formatter:on

            // finally, the new relation can be accessed under the generated UUID
            final var newSubjectUuid = toCleanup(HsOfficeRelationRealEntity.class, UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1)));
            assertThat(newSubjectUuid).isNotNull();
        }

        @Test
        void globalAdmin_canNotAddRelation_ifAnchorPersonDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenAnchorPersonUuid = GIVEN_NON_EXISTING_HOLDER_PERSON_UUID;
            final var givenHolderPerson = realPersonRepo.findPersonByOptionalNameLike("Smith").getFirst();
            final var givenContact = realContactRepo.findContactByOptionalCaptionLike("fourth").getFirst();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "type": "%s",
                                   "anchor.uuid": "%s",
                                   "holder.uuid": "%s",
                                   "contact.uuid": "%s"
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
                    .body("message", is("ERROR: [404] cannot find Person by anchorUuid: " + GIVEN_NON_EXISTING_HOLDER_PERSON_UUID));
            // @formatter:on
        }

        @Test
        void globalAdmin_canNotAddRelation_ifHolderPersonDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenAnchorPerson = realPersonRepo.findPersonByOptionalNameLike("Third").getFirst();
            final var givenContact = realContactRepo.findContactByOptionalCaptionLike("fourth").getFirst();

            final var location = RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                               {
                                   "type": "%s",
                                   "anchor.uuid": "%s",
                                   "holder.uuid": "%s",
                                   "contact.uuid": "%s"
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
                    .body("message", is("ERROR: [404] cannot find Person by holderUuid: " + GIVEN_NON_EXISTING_HOLDER_PERSON_UUID));
            // @formatter:on
        }

        @Test
        void globalAdmin_canNotAddRelation_ifContactDoesNotExist() {

            context.define("superuser-alex@hostsharing.net");
            final var givenAnchorPerson = realPersonRepo.findPersonByOptionalNameLike("Third").getFirst();
            final var givenHolderPerson = realPersonRepo.findPersonByOptionalNameLike("Paul").getFirst();
            final var givenContactUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                           {
                               "type": "%s",
                               "anchor.uuid": "%s",
                               "holder.uuid": "%s",
                               "contact.uuid": "%s"
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
                    .body("message", is("ERROR: [404] cannot find Contact by contactUuid: 00000000-0000-0000-0000-000000000000"));
            // @formatter:on
        }
    }

    @Nested
    class GetRelation {

        @Test
        void globalAdmin_withoutAssumedRole_canGetArbitraryRelation() {
            context.define("superuser-alex@hostsharing.net");
            final UUID givenRelationUuid = findRelation("First", "Firby").getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
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
                        "contact": { "caption": "first contact" }
                    }
                    """)); // @formatter:on
        }

        @Test
        void normalUser_canNotGetUnrelatedRelation() {
            context.define("superuser-alex@hostsharing.net");
            final UUID givenRelationUuid = findRelation("First", "Firby").getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/relations/" + givenRelationUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        void contactAdminUser_canGetRelatedRelation() {
            context.define("superuser-alex@hostsharing.net");
            final var givenRelation = findRelation("First", "Firby");
            assertThat(givenRelation.getContact().getCaption()).isEqualTo("first contact");

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer contact-admin@firstcontact.example.com")
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
                        "contact": { "caption": "first contact" }
                    }
                    """)); // @formatter:on
        }
    }

    private HsOfficeRelation findRelation(
            final String anchorPersonName,
            final String holderPersoneName) {
        final var anchorPersonUuid = realPersonRepo.findPersonByOptionalNameLike(anchorPersonName).getFirst().getUuid();
        final var holderPersonUuid = realPersonRepo.findPersonByOptionalNameLike(holderPersoneName).getFirst().getUuid();
        final var givenRelation = realRelationRepo
                .findRelationRelatedToPersonUuid(anchorPersonUuid)
                .stream()
                .filter(r -> r.getHolder().getUuid().equals(holderPersonUuid))
                .findFirst().orElseThrow();
        return givenRelation;
    }

    @Nested
    class PatchRelation {

        @Test
        void globalAdmin_withoutAssumedRole_canPatchContactOfArbitraryRelation() {

            context.define("superuser-alex@hostsharing.net");
            final var givenRelation = givenSomeTemporaryRelationBessler();
            assertThat(givenRelation.getContact().getCaption()).isEqualTo("seventh contact");
            final var givenContact = realContactRepo.findContactByOptionalCaptionLike("fourth").getFirst();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                           {
                              "contact.uuid": "%s"
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
                    .body("contact.caption", is("fourth contact"));
                // @formatter:on

            // finally, the relation is actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(realRelationRepo.findByUuid(givenRelation.getUuid())).isPresent().get()
                    .matches(rel -> {
                        assertThat(rel.getAnchor().getTradeName()).contains("Bessler");
                        assertThat(rel.getHolder().getFamilyName()).contains("Winkler");
                        assertThat(rel.getContact().getCaption()).isEqualTo("fourth contact");
                        assertThat(rel.getType()).isEqualTo(HsOfficeRelationType.REPRESENTATIVE);
                        return true;
                    });
        }
    }

    @Nested
    class DeleteRelation {

        @Test
        void globalAdmin_withoutAssumedRole_canDeleteArbitraryRelation() {
            context.define("superuser-alex@hostsharing.net");
            final var givenRelation = givenSomeTemporaryRelationBessler();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/relations/" + givenRelation.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given relation is gone
            assertThat(realRelationRepo.findByUuid(givenRelation.getUuid())).isEmpty();
        }

        @Test
        void contactAdminUser_canNotDeleteRelatedRelation() {
            context.define("superuser-alex@hostsharing.net");
            final var givenRelation = givenSomeTemporaryRelationBessler();
            assertThat(givenRelation.getContact().getCaption()).isEqualTo("seventh contact");

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer contact-admin@seventhcontact.example.com")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/relations/" + givenRelation.getUuid())
                .then().log().body().assertThat()
                    .statusCode(403); // @formatter:on

            // then the given relation is still there
            assertThat(realRelationRepo.findByUuid(givenRelation.getUuid())).isNotEmpty();
        }

        @Test
        void normalUser_canNotDeleteUnrelatedRelation() {
            context.define("superuser-alex@hostsharing.net");
            final var givenRelation = givenSomeTemporaryRelationBessler();
            assertThat(givenRelation.getContact().getCaption()).isEqualTo("seventh contact");

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/relations/" + givenRelation.getUuid())
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on

            // then the given relation is still there
            assertThat(realRelationRepo.findByUuid(givenRelation.getUuid())).isNotEmpty();
        }
    }

    private HsOfficeRelation givenSomeTemporaryRelationBessler() {
        return jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net");
            final var givenAnchorPerson = realPersonRepo.findPersonByOptionalNameLike("Erben Bessler").getFirst();
            final var givenHolderPerson = realPersonRepo.findPersonByOptionalNameLike("Winkler").getFirst();
            final var givenContact = realContactRepo.findContactByOptionalCaptionLike("seventh contact").getFirst();
            final var newRelation = HsOfficeRelationRealEntity.builder()
                    .type(HsOfficeRelationType.REPRESENTATIVE)
                    .anchor(givenAnchorPerson)
                    .holder(givenHolderPerson)
                    .contact(givenContact)
                    .build();

            assertThat(toCleanup(realRelationRepo.save(newRelation))).isEqualTo(newRelation);

            return newRelation;
        }).assertSuccessful().returnedValue();
    }

}
