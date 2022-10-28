package net.hostsharing.hsadminng.hs.office.contact;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.test.Accepts;
import net.hostsharing.test.JpaAttempt;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONException;
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
class HsOfficeContactControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    Context context;

    @Autowired
    Context contextMock;

    @Autowired
    HsOfficeContactRepository contactRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @Nested
    @Accepts({ "Contact:F(Find)" })
    class ListContacts {

        @Test
        void globalAdmin_withoutAssumedRoles_canViewAllContacts_ifNoCriteriaGiven() throws JSONException {

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/contacts")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        [
                            { "label": "first contact" },
                            { "label": "second contact" },
                            { "label": "third contact" },
                            { "label": "forth contact" },
                            { "label": "fifth contact" },
                            { "label": "sixth contact" },
                            { "label": "seventh contact" },
                            { "label": "eighth contact" },
                            { "label": "ninth contact" },
                            { "label": "tenth contact" },
                            { "label": "eleventh contact" },
                            { "label": "twelfth contact" }
                        ]
                        """
                            ));
                // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Contact:C(Create)" })
    class AddContact {

        @Test
        void globalAdmin_withoutAssumedRole_canAddContact() {

            context.define("superuser-alex@hostsharing.net");

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-user", "superuser-alex@hostsharing.net")
                        .contentType(ContentType.JSON)
                        .body("""
                               {
                                   "label": "Temp Contact",
                                   "emailAddresses": "test@example.org"
                                 }
                            """)
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/office/contacts")
                    .then().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("uuid", isUuidValid())
                        .body("label", is("Temp Contact"))
                        .body("emailAddresses", is("test@example.org"))
                        .header("Location", startsWith("http://localhost"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new contact can be accessed under the generated UUID
            final var newUserUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newUserUuid).isNotNull();
        }
    }

    @Nested
    @Accepts({ "Contact:R(Read)" })
    class GetContact {

        @Test
        void globalAdmin_withoutAssumedRole_canGetArbitraryContact() {
            context.define("superuser-alex@hostsharing.net");
            final var givenContactUuid = contactRepo.findContactByOptionalLabelLike("first").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/contacts/" + givenContactUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                        "label": "first contact"
                    }
                    """)); // @formatter:on
        }

        @Test
        @Accepts({ "Contact:X(Access Control)" })
        void normalUser_canNotGetUnrelatedContact() {
            context.define("superuser-alex@hostsharing.net");
            final var givenContactUuid = contactRepo.findContactByOptionalLabelLike("first").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/contacts/" + givenContactUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        @Accepts({ "Contact:X(Access Control)" })
        void contactAdminUser_canGetRelatedContact() {
            context.define("superuser-alex@hostsharing.net");
            final var givenContactUuid = contactRepo.findContactByOptionalLabelLike("first").get(0).getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "contact-admin@firstcontact.example.com")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/office/contacts/" + givenContactUuid)
                .then().log().body().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    {
                         "label": "first contact",
                         "emailAddresses": "contact-admin@firstcontact.example.com",
                         "phoneNumbers": "+49 123 1234567"
                     }
                    """)); // @formatter:on
        }
    }

    @Nested
    @Accepts({ "Contact:U(Update)" })
    class PatchContact {

        @Test
        void globalAdmin_withoutAssumedRole_canPatchAllPropertiesOfArbitraryContact() {

            context.define("superuser-alex@hostsharing.net");
            final var givenContact = givenSomeTemporaryContactCreatedBy("selfregistered-test-user@hostsharing.org");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                       {
                           "label": "Temp patched contact",
                           "emailAddresses": "patched@example.org",
                           "postalAddress": "Patched Address",
                           "phoneNumbers": "+01 100 123456"
                       }
                       """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/contacts/" + givenContact.getUuid())
                .then().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("label", is("Temp patched contact"))
                    .body("emailAddresses", is("patched@example.org"))
                    .body("postalAddress", is("Patched Address"))
                    .body("phoneNumbers", is("+01 100 123456"));
                // @formatter:on

            // finally, the contact is actually updated
            context.define("superuser-alex@hostsharing.net");
            assertThat(contactRepo.findByUuid(givenContact.getUuid())).isPresent().get()
                    .matches(person -> {
                        assertThat(person.getLabel()).isEqualTo("Temp patched contact");
                        assertThat(person.getEmailAddresses()).isEqualTo("patched@example.org");
                        assertThat(person.getPostalAddress()).isEqualTo("Patched Address");
                        assertThat(person.getPhoneNumbers()).isEqualTo("+01 100 123456");
                        return true;
                    });
        }

        @Test
        void globalAdmin_withoutAssumedRole_canPatchPartialPropertiesOfArbitraryContact() {

            context.define("superuser-alex@hostsharing.net");
            final var givenContact = givenSomeTemporaryContactCreatedBy("selfregistered-test-user@hostsharing.org");

            final var location = RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .contentType(ContentType.JSON)
                    .body("""
                       {
                           "emailAddresses": "patched@example.org",
                           "phoneNumbers": "+01 100 123456"
                       }
                            """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/office/contacts/" + givenContact.getUuid())
                .then().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("uuid", isUuidValid())
                    .body("label", is(givenContact.getLabel()))
                    .body("emailAddresses", is("patched@example.org"))
                    .body("postalAddress", is(givenContact.getPostalAddress()))
                    .body("phoneNumbers", is("+01 100 123456"));
            // @formatter:on

            // finally, the contact is actually updated
            assertThat(contactRepo.findByUuid(givenContact.getUuid())).isPresent().get()
                    .matches(person -> {
                        assertThat(person.getLabel()).isEqualTo(givenContact.getLabel());
                        assertThat(person.getEmailAddresses()).isEqualTo("patched@example.org");
                        assertThat(person.getPostalAddress()).isEqualTo(givenContact.getPostalAddress());
                        assertThat(person.getPhoneNumbers()).isEqualTo("+01 100 123456");
                        return true;
                    });
        }

    }

    @Nested
    @Accepts({ "Contact:D(Delete)" })
    class DeleteContact {

        @Test
        void globalAdmin_withoutAssumedRole_canDeleteArbitraryContact() {
            context.define("superuser-alex@hostsharing.net");
            final var givenContact = givenSomeTemporaryContactCreatedBy("selfregistered-test-user@hostsharing.org");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "superuser-alex@hostsharing.net")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/contacts/" + givenContact.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given contact is gone
            assertThat(contactRepo.findByUuid(givenContact.getUuid())).isEmpty();
        }

        @Test
        @Accepts({ "Contact:X(Access Control)" })
        void contactOwner_canDeleteRelatedContact() {
            final var givenContact = givenSomeTemporaryContactCreatedBy("selfregistered-test-user@hostsharing.org");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-test-user@hostsharing.org")
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/office/contacts/" + givenContact.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given contact is still there
            assertThat(contactRepo.findByUuid(givenContact.getUuid())).isEmpty();
        }

        @Test
        @Accepts({ "Contact:X(Access Control)" })
        void normalUser_canNotDeleteUnrelatedContact() {
            context.define("superuser-alex@hostsharing.net");
            final var givenContact = givenSomeTemporaryContactCreatedBy("selfregistered-test-user@hostsharing.org");

            RestAssured // @formatter:off
                .given()
                    .header("current-user", "selfregistered-user-drew@hostsharing.org")
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

    private HsOfficeContactEntity givenSomeTemporaryContactCreatedBy(final String creatingUser) {
        return jpaAttempt.transacted(() -> {
            context.define(creatingUser);
            final var newContact = HsOfficeContactEntity.builder()
                    .uuid(UUID.randomUUID())
                    .label("Temp from " + Context.getCallerMethodNameFromStackFrame(1) )
                    .emailAddresses(RandomStringUtils.randomAlphabetic(10) + "@example.org")
                    .postalAddress("Postal Address " + RandomStringUtils.randomAlphabetic(10))
                    .phoneNumbers("+01 200 " + RandomStringUtils.randomNumeric(8))
                    .build();

            return contactRepo.save(newContact);
        }).assertSuccessful().returnedValue();
    }

    @BeforeEach
    @AfterEach
    void cleanup() {
        jpaAttempt.transacted(() -> {
            context.define("superuser-alex@hostsharing.net", null);
            em.createQuery("DELETE FROM HsOfficeContactEntity c WHERE c.label LIKE 'Temp %'").executeUpdate();
        }).assertSuccessful();
    }
}
