package net.hostsharing.hsadminng.hs.accounts.scenarios;

import lombok.SneakyThrows;
import net.hostsharing.hsadminng.hs.scenarios.Produces;
import net.hostsharing.hsadminng.hs.scenarios.Requires;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;

import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asSubject;

class AccountScenarioTests extends ScenarioTest {

    @SneakyThrows
    @BeforeEach
    protected void beforeScenario(final TestInfo testInfo) {
        super.beforeScenario(testInfo);
    }

    @Nested
    @Order(90)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class RbacContextScenarios {

        @Test
        @Order(9010)
        @Produces("RBAC Context")
        void shouldFetchRbacContext() {
            new FetchRbacContext(scenarioTest)
                    .given("subjectName", "hsh-fran_superuser")
                    .given("assumedRoles", "rbactest.package#xxx00:ADMIN;rbactest.package#yyy00:ADMIN")
                    .given("expectedSubjectType", "USER")
                    .given("expectedToBeGlobalAdmin", true)
                    .thenExpect(HttpStatus.OK)
                    .keep();
        }
    }

    @Nested
    @Order(91)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CurrentLoginUserScenarios {

        @Test
        @Order(9110)
        @Produces("Current Login User")
        void shouldFetchCurrentLoginUser() {
            new CurrentLoginUser(scenarioTest)
                    .given("subjectName", "hsh-fran_superuser")
                    .given("personGivenName", "Fran")
                    .given("expectedToBeGlobalAdmin", true)
                    .thenExpect(HttpStatus.OK)
                    .keep();
        }
    }

    @Nested
    @Order(92)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ScenariosForExistingPersons {

        @Test
        @Order(9210)
        @Produces(
                explicitly = "Account: peter-smith",
                implicitly = { "Person: Peter Smith" })
        void shouldCreateInitialAccountForExistingNaturalPerson() {
            new CreateAccountForExistingPerson(scenarioTest, asGlobalAgent())
                    // to find a specific existing person
                    .given("personFamilyName", "Smith")
                    .given("personGivenName", "Peter")
                    .given("personGivenType", "NATURAL_PERSON")
                    // a login name, to be stored in the new RBAC subject
                    .given("subjectName", "xyz-peter.smith")
                    // initial account
                    .given("globalUid", 21011)
                    .given("globalGid", 21011)
                    .thenExpect(HttpStatus.OK)
                    .keep();
        }

        @Test
        @Order(9211)
        @Requires("Account: peter-smith")
        void newlyCreatedAccountForExistingNaturalPersonShouldBeAbleToViewThatPerson() {
            new AccountCanViewTheirOwnPerson(scenarioTest, asSubject("xyz-peter.smith"))
                    // to find a specific existing person
                    .given("subjectName", "xyz-peter.smith")
                    // some expected person data
                    .expected("personFamilyName", "Smith")
                    .expected("personGivenName", "Peter")
                    .thenExpect(HttpStatus.OK);
        }

        @Test
        @Order(9212)
        @Requires("Account: peter-smith")
        void newlyCreatedAccountForExistingNaturalPersonShouldBeAbleToViewExistingRelations() {
            new AccountCanViewTheirOwnRelations(scenarioTest, asSubject("xyz-peter.smith"))
                    // to find a specific existing person
                    .given("subjectName", "xyz-peter.smith")
                    // some expected person data ... which might change if test-data changes
                    .expected("expectedRelationsJson", """
                            [
                              {
                                "type": "REPRESENTATIVE",
                                "mark": null,
                                "anchor": { "tradeName": "Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K." },
                                "holder": { "givenName": "Peter", "familyName": "Smith" },
                                "contact": { "emailAddresses": { "main": "contact-admin@secondcontact.example.com" } }
                              },
                              {
                                "type": "PARTNER",
                                "mark": null,
                                "anchor": { "tradeName": "Hostsharing eG" },
                                "holder": { "givenName": "Peter", "familyName": "Smith" },
                                "contact": { "emailAddresses": { "main": "contact-admin@sixthcontact.example.com" } }
                              },
                              {
                                "type": "DEBITOR",
                                "mark": null,
                                "anchor": { "givenName": "Peter", "familyName": "Smith" },
                                "holder": { "givenName": "Peter", "familyName": "Smith" },
                                "contact": { "emailAddresses": { "main": "contact-admin@thirdcontact.example.com" } }
                              },
                              {
                                "type": "SUBSCRIBER",
                                "mark": "members-announce",
                                "anchor": { "tradeName": "Third OHG" },
                                "holder": { "givenName": "Peter", "familyName": "Smith" },
                                "contact": { "emailAddresses": { "main": "contact-admin@thirdcontact.example.com" } }
                              }
                            ]
                            """)
                    .thenExpect(HttpStatus.OK);
        }

        @Test
        @Order(9213)
        @Requires("Account: peter-smith")
        void newlyCreatedAccountForExistingNaturalPersonShouldBeAbleToViewExistingMemberships() {
            new AccountCanViewTheirOwnMemberships(scenarioTest, asSubject("xyz-peter.smith"))
                    // to find a specific existing person
                    .given("subjectName", "xyz-peter.smith")
                    // some expected membership data ... which might change if test-data changes
                    .expected("expectedMembershipsJson", """
                            [
                              {
                                "partner": {
                                  "partnerNumber": "P-10002",
                                  "partnerRel": {
                                    "type": "PARTNER",
                                    "anchor": { "tradeName": "Hostsharing eG" },
                                    "holder": { "tradeName": "Peter Smith - The Second Hand and Thrift Stores-n-Shipping e.K." },
                                    "contact": { "emailAddresses": { "main": "contact-admin@secondcontact.example.com" } }
                                  },
                                  "details": {
                                    "registrationOffice": "Hamburg",
                                    "registrationNumber": "RegNo123456789"
                                  }
                                },
                                "memberNumber": "M-1000202",
                                "memberNumberSuffix": "02",
                                "validFrom": "2022-10-01",
                                "validTo": "2025-12-31",
                                "status": "CANCELLED",
                                "membershipFeeBillable": true
                              }
                            ]
                            """)
                    .thenExpect(HttpStatus.OK);
        }
    }

    @Nested
    @Order(93)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ScenariosForImplicitlyCreatedPersons {

        @Test
        @Order(9310)
        @Produces(
                explicitly = "Account: peter-newman",
                implicitly = { "Person: Peter Newman" })
        void shouldCreateInitialAccountForNewNaturalPerson() {
            new CreateAccountForNewPerson(scenarioTest, asGlobalAgent())
                    // to find a specific existing person
                    .given("personFamilyName", "Newman")
                    .given("personGivenName", "Peter")
                    // a login name, to be stored in the new RBAC subject
                    .given("subjectName", "xyz-peter.newman")
                    // initial account
                    .given("globalUid", 21012)
                    .given("globalGid", 21012)
                    .thenExpect(HttpStatus.OK)
                    .keep();
        }

        @Test
        @Order(9311)
        @Requires("Account: peter-newman")
        void newlyCreatedAccountForNewNaturalPersonShouldBeAbleToViewThatPerson() {
            new AccountCanViewTheirOwnPerson(scenarioTest, asSubject("xyz-peter.newman"))
                    // to find a specific existing person
                    .given("subjectName", "xyz-peter.newman")
                    // some expected person data
                    .expected("personFamilyName", "Newman")
                    .expected("personGivenName", "Peter")
                    .thenExpect(HttpStatus.OK);
        }
    }

    @Nested
    @Order(95)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ScenariosForGroupGrants {

        @Test
        @Order(9520)
        @Produces("GrantOfProjectAdminRoleToGroupSubject")
        void grantingAProjectAdminRoleToAGroup() {
            new GrantProjectAdminRoleToGroup(
                    scenarioTest,
                    asSubject("tst-person_firbysusan")
                            .whichIs("a Debitor-Admin, here concretely the Partner-Representative")
                            .withGroups("/xyz-Service"))
                    .given("roleIdNameToAssume", "hs_office.relation#FirstGmbH-with-DEBITOR-FirstGmbH:AGENT")
                    .given("projectCaption", "D-1000111 default project")
                    .given("projectIdName", "D-1000111-D-1000111defaultproject")
                    .given("nameOfGroupSubject", "/xyz-Service")
                    .thenExpect(HttpStatus.CREATED)
                    .keep();
        }

        @Test
        @Order(9521)
        @Requires("GrantOfProjectAdminRoleToGroupSubject")
        void usersOfAGroupCanAssumeARoleGrantedToThatGroup() {
            new AssumeBookingProjectAdminRoleAsGroupMember(
                    scenarioTest,
                    asSubject("tst-drew_selfregistered")
                            .whichIs("any user which does not even need to have any roles granted yet")
                            .withGroups("/xyz-Service"))
                    .expected("expectedAssumedRoleIdName", "hs_booking.project#D-1000111-D-1000111defaultproject:ADMIN")
                    .given("nameOfGroupSubject", "/xyz-Service")
                    .given("nameOfUserSubject", "tst-drew_selfregistered")
                    .given("projectCaption", "D-1000111 default project")
                    .thenExpect(HttpStatus.OK);
        }

        @Test
        @Order(9522)
        @Requires("GrantOfProjectAdminRoleToGroupSubject")
        void usersOfAGroupCanViewHostingAssetsBelowTheAssumedProject() {
            new ViewHostingAssetsAsGroupMember(
                    scenarioTest,
                    asSubject("tst-drew_selfregistered")
                            .whichIs("any user which does not even need to have any roles granted yet")
                            .withGroups("/xyz-Service"))
                    .given("nameOfGroupSubject", "/xyz-Service")
                    .given("nameOfUserSubject", "tst-drew_selfregistered")
                    .given("projectCaption", "D-1000111 default project")
                    .thenExpect(HttpStatus.OK);
        }
    }

}
