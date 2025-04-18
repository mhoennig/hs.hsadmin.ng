package net.hostsharing.hsadminng.hs.office.contact;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONException;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.rbac.test.IsValidUuidMatcher.isUuidValid;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, DisableSecurityConfig.class, JpaAttempt.class }
)
@ActiveProfiles("test")
@Transactional
@Tag("officeIntegrationTest")
class HsOfficeContactControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    Context contextMock;

    @Autowired
    HsOfficeContactRbacRepository contactRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @Nested
    class ListContacts {

        @Test
        void globalAdmin_withoutAssumedRoles_canViewAllContacts_ifNoCriteriaGiven() throws JSONException {

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/contacts")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        [
                            { "caption": "first contact" },
                            { "caption": "second contact" },
                            { "caption": "third contact" },
                            { "caption": "fourth contact" },
                            { "caption": "fifth contact" },
                            { "caption": "sixth contact" },
                            { "caption": "seventh contact" },
                            { "caption": "eighth contact" },
                            { "caption": "ninth contact" },
                            { "caption": "tenth contact" },
                            { "caption": "eleventh contact" },
                            { "caption": "twelfth contact" }
                        ]
                        """
                            ));
                // @formatter:on
        }
    }

    @Nested
    class AddContact {

        @Test
        void globalAdmin_withoutAssumedRole_canAddContact() {

            context.define("superuser-alex@hostsharing.net");

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                               {
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
                            """)
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/contacts")
                    .then().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("caption", is("Temp Contact"))
                        .body("emailAddresses", is(Map.of("main", "test@example.org")))
                        .body("postalAddress", hasEntry("name", "Herr Test Contact"))
                        .body("postalAddress", hasEntry("street", "Am Schieferbruch 3"))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new contact can be accessed under the generated UUID
            final var newSubjectUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newSubjectUuid).isNotNull();
        }
    }

    @Nested
    class GetContact {

        @Test
        void globalAdmin_withoutAssumedRole_canGetArbitraryContact() {
            context.define("superuser-alex@hostsharing.net");
            final var givenContactUuid = contactRepo.findContactByOptionalCaptionLike("first").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/contacts/" + givenContactUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                        "caption": "first contact"
                    }
                    """)); // @formatter:on
        }

        @Test
        void normalUser_canNotGetUnrelatedContact() {
            context.define("superuser-alex@hostsharing.net");
            final var givenContactUuid = contactRepo.findContactByOptionalCaptionLike("first").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/contacts/" + givenContactUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        void contactAdminUser_canGetRelatedContact() {
            context.define("superuser-alex@hostsharing.net");
            final var givenContactUuid = contactRepo.findContactByOptionalCaptionLike("first").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer contact-admin@firstcontact.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/contacts/" + givenContactUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                        "caption": "first contact",
                        "emailAddresses": {
                            "main": "contact-admin@firstcontact.example.com"
                        },
                        "phoneNumbers": {
                            "phone_office": "+49 123 1234567"
                        }
                     }
                    """)); // @formatter:on
        }
    }

    @Nested
    class PatchContact {

        @Test
        void globalAdmin_withoutAssumedRole_canPatchAllPropertiesOfArbitraryContact() {

            context.define("superuser-alex@hostsharing.net");
            final var givenContact = givenSomeTemporaryContactCreatedBy("selfregistered-test-user@hostsharing.org");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                       {
                           "caption": "Temp patched contact",
                           "emailAddresses": {
                                "main": "patched@example.org"
                           },
                           "postalAddress": {
                                "extra": "Extra Property",
                                "co": "P. Patcher",
                                "street": "Patchstraße 5"
                           },
                           "phoneNumbers": {
                                "phone_office": "+01 100 123456"
                            }
                       }
                       """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/contacts/" + givenContact.getUuid())
                .then().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("caption", is("Temp patched contact"))
                    .body("emailAddresses", is(Map.of("main", "patched@example.org")))
                    .body("postalAddress", hasEntry("name", givenContact.getPostalAddress().get("name"))) // unchanged
                    .body("postalAddress", hasEntry("extra", "Extra Property")) // unchanged
                    .body("postalAddress", hasEntry("co", "P. Patcher")) // patched
                    .body("postalAddress", hasEntry("street", "Patchstraße 5")) // patched
                    .body("phoneNumbers", is(Map.of("phone_office", "+01 100 123456")));
                // @formatter:on

            // finally, the contact is actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(contactRepo.findByUuid(givenContact.getUuid())).isPresent().get()
                    .matches(person -> {
                        assertThat(person.getCaption()).isEqualTo("Temp patched contact");
                        assertThat(person.getEmailAddresses()).containsExactlyEntriesOf(Map.of("main", "patched@example.org"));
                        assertThat(person.getPostalAddress()).containsAllEntriesOf(Map.ofEntries(
                                entry("name", givenContact.getPostalAddress().get("name")),
                                entry("co", "P. Patcher"),
                                entry("street", "Patchstraße 5")
                        ));
                        assertThat(person.getPhoneNumbers()).containsExactlyEntriesOf(Map.of("phone_office", "+01 100 123456"));
                        return true;
                    });
        }

        @Test
        void globalAdmin_withoutAssumedRole_canPatchPartialPropertiesOfArbitraryContact() {

            context.define("superuser-alex@hostsharing.net");
            final var givenContact = givenSomeTemporaryContactCreatedBy("selfregistered-test-user@hostsharing.org");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                       {
                           "emailAddresses": {
                                "main": "patched@example.org"
                           },
                           "phoneNumbers": {
                                "phone_office": "+01 100 123456"
                            }
                       }
                       """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/contacts/" + givenContact.getUuid())
                .then().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("caption", is(givenContact.getCaption()))
                    .body("emailAddresses", is(Map.of("main", "patched@example.org")))
                    .body("phoneNumbers", is(Map.of("phone_office", "+01 100 123456")));
            // @formatter:on

            // finally, the contact is actually updated
            assertThat(contactRepo.findByUuid(givenContact.getUuid())).isPresent().get()
                    .matches(person -> {
                        assertThat(person.getCaption()).isEqualTo(givenContact.getCaption());
                        assertThat(person.getEmailAddresses()).containsExactlyEntriesOf(Map.of("main", "patched@example.org"));
                        assertThat(person.getPostalAddress()).containsExactlyInAnyOrderEntriesOf(givenContact.getPostalAddress());
                        assertThat(person.getPhoneNumbers()).containsExactlyEntriesOf(Map.of("phone_office", "+01 100 123456"));
                        return true;
                    });
        }
    }

    @Nested
    class DeleteContact {

        @Test
        void globalAdmin_withoutAssumedRole_canDeleteArbitraryContact() {
            context.define("superuser-alex@hostsharing.net");
            final var givenContact = givenSomeTemporaryContactCreatedBy("selfregistered-test-user@hostsharing.org");

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/contacts/" + givenContact.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given contact is gone
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", null);
                assertThat(contactRepo.findByUuid(givenContact.getUuid())).isEmpty();
            }).assertSuccessful();
        }

        @Test
        void contactOwner_canDeleteRelatedContact() {
            final var givenContact = givenSomeTemporaryContactCreatedBy("selfregistered-test-user@hostsharing.org");

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer selfregistered-test-user@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/contacts/" + givenContact.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given contact is still there
            jpaAttempt.transacted(() -> {
                context("superuser-alex@hostsharing.net", null);
                assertThat(contactRepo.findByUuid(givenContact.getUuid())).isEmpty();
            }).assertSuccessful();
        }

        @Test
        void normalUser_canNotDeleteUnrelatedContact() {
            context.define("superuser-alex@hostsharing.net");
            final var givenContact = givenSomeTemporaryContactCreatedBy("selfregistered-test-user@hostsharing.org");

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", "Bearer selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/contacts/" + givenContact.getUuid())
                .then().log().body().assertThat()
                    .statusCode(404); // unrelated user cannot even view the contact
            // @formatter:on

            // then the given contact is still there
            assertThat(contactRepo.findByUuid(givenContact.getUuid())).isNotEmpty();
        }
    }

    private HsOfficeContactRbacEntity givenSomeTemporaryContactCreatedBy(final String creatingUser) {
        return jpaAttempt.transacted(() -> {
            context.define(creatingUser);
            final var newContact = HsOfficeContactRbacEntity.builder()
                    .caption("Temp from " + Context.getCallerMethodNameFromStackFrame(1) )
                    .postalAddress(Map.ofEntries(
                            entry("name", RandomStringUtils.randomAlphabetic(6) + " " + RandomStringUtils.randomAlphabetic(10)),
                            entry("street", RandomStringUtils.randomAlphabetic(10) + randomInt(1, 99)),
                            entry("zipcode", "D-" + randomInt(10000, 99999)),
                            entry("city", RandomStringUtils.randomAlphabetic(10))
                    ))
                    .emailAddresses(Map.of("main", RandomStringUtils.randomAlphabetic(10) + "@example.org"))
                    .phoneNumbers(Map.of("phone_office", "+01 200 " + RandomStringUtils.randomNumeric(8)))
                    .build();

            return contactRepo.save(newContact);
        }).assertSuccessful().returnedValue();
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net", null);
            em.createQuery("DELETE FROM HsOfficeContactRbacEntity c WHERE c.caption LIKE 'Temp %'").executeUpdate();
        }).assertSuccessful();
    }

    private int randomInt(final int min, final int max) {
        return ThreadLocalRandom.current().nextInt(min, max);
    }
}
