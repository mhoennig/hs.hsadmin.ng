package net.hostsharing.hsadminng.hs.accounts.scenarios;

import lombok.SneakyThrows;
import net.hostsharing.hsadminng.hs.rbac.scenarios.ViewRbacSubjects;
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
        void shouldFetchRbacContextWithEffectiveGroups() {
            new FetchRbacContext(scenarioTest,
                        asSubject("hsh-fran_superuser")
                                .withGroups("/hsh-Hostmasters", "/hsh-Team", "/not-synchronized"))
                    .given("subjectName", "hsh-fran_superuser")
                    .given("assumedRoles", "")
                    .given("expectedSubjectType", "USER")
                    .given("expectedToBeGlobalAdmin", true)
                    .expected("expectedClaimedGroups", """
                            [ "/hsh-Hostmasters", "/hsh-Team", "/not-synchronized" ]
                            """)
                    .expected("expectedEffectiveGroups", """
                            [
                              { "name": "/hsh-Hostmasters" },
                              { "name": "/hsh-Team" }
                            ]
                            """)
                    .thenExpect(HttpStatus.OK)
                    .keep();
        }

        @Test
        @Order(9020)
        void shouldFetchRbacContextWithClaimedButNoEffectiveGroupsAfterAssumingRoles() {
            new FetchRbacContext(scenarioTest, asSubject("hsh-fran_superuser").withGroups("/hsh-Hostmasters", "/not-synchronized"))
                    .given("subjectName", "hsh-fran_superuser")
                    .given("assumedRoles", "rbactest.package#xxx00:ADMIN;rbactest.package#yyy00:ADMIN")
                    .given("expectedSubjectType", "USER")
                    .given("expectedToBeGlobalAdmin", true)
                    .expected("expectedClaimedGroups", """
                            [ "/hsh-Hostmasters", "/not-synchronized" ]
                            """)
                    .expected("expectedEffectiveGroups", """
                            []
                            """)
                    .thenExpect(HttpStatus.OK);
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
    class AccountBootstrapScenarios {

        // stable, Keycloak-assigned UUIDs shared with this backend (the subject's identity never changes)
        private static final String KEYCLOAK_SYNC_SUBJECT_UUID = "242a0007-0000-0000-0000-000000000007";
        private static final String PETER_NEWMAN_SUBJECT_UUID = "242a0003-0000-0000-0000-000000000003";

        @Test
        @Order(9210)
        @Produces("Global-Admin Subject: hsh-keycloak_sync")
        void shouldCreateGlobalAdminSubjectWithoutAnOwnAccount() {
            new CreateGlobalAdminSubjectWithoutAccount(scenarioTest, asGlobalAgent())
                    // the preexisting USER subject, e.g. synchronized from Keycloak
                    .given("subjectUuid", KEYCLOAK_SYNC_SUBJECT_UUID)
                    .given("subjectName", "hsh-keycloak_sync")
                    .thenExpect(HttpStatus.CREATED)
                    .keep();
        }

        @Test
        @Order(9220)
        @Requires("Global-Admin Subject: hsh-keycloak_sync")
        @Produces(
                explicitly = "Account: xyz-peter.newman",
                implicitly = { "Person: Peter Newman" })
        void shouldCreatePersonAndSubjectAndAccountWithASingleRequest() {
            new CreateAccountForNewPerson(scenarioTest, asSubject("hsh-keycloak_sync"))
                    .introduction("""
                            An account combines an RBAC subject with a natural person and thus grant's access to data in hsadmin-NG.
                            Here, the natural person, the USER subject (with a UUID e.g. known from Keycloak),
                            and the account are all created with a single HTTP POST request —
                            executed by a global-admin subject which has no own account itself.
                            """)
                    // the person to be created along with the account
                    .given("personFamilyName", "Newman")
                    .given("personGivenName", "Peter")
                    // the subject UUID (e.g. known from Keycloak) and login name for the new USER subject
                    .given("subjectUuid", PETER_NEWMAN_SUBJECT_UUID)
                    .given("subjectName", "xyz-peter.newman")
                    // initial account
                    .given("globalUid", 21012)
                    .given("globalGid", 21012)
                    .thenExpect(HttpStatus.OK)
                    .keep();
        }

        @Test
        @Order(9221)
        @Requires("Account: xyz-peter.newman")
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
    @Order(93)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class OtherAccountCreatingScenarios {

        // stable, Keycloak-assigned UUIDs shared with this backend (the subject's identity never changes)
        private static final String JACK_TUCKER_SUBJECT_UUID = "242a0001-0000-0000-0000-000000000001";
        private static final String PETER_SMITH_SUBJECT_UUID = "242a0002-0000-0000-0000-000000000002";
        private static final String TOM_SAWYER_SUBJECT_UUID = "242a0004-0000-0000-0000-000000000004";

        @Test
        @Order(9310)
        @Produces(
                explicitly = "Account: xyz-peter.smith",
                implicitly = { "Person: Peter Smith" })
        void shouldCreateAccountAndUserSubjectFromGivenSubjectUuidAndName() {
            new CreateAccountForExistingPerson(scenarioTest, asGlobalAgent())
                    .introduction("""
                            An account combines an RBAC subject with a natural person and thus grant's access to data in hsadmin-NG.
                            Here, both the subject UUID (as assigned by Keycloak) and the subject name are given,
                            but no such subject exists yet; thus a new USER subject is created along with the account.
                            """)
                    // to find a specific existing person
                    .given("personFamilyName", "Smith")
                    .given("personGivenName", "Peter")
                    .given("personGivenType", "NATURAL_PERSON")
                    // the subject UUID (as assigned by Keycloak) and login name for the new USER subject
                    .given("subjectUuid", PETER_SMITH_SUBJECT_UUID)
                    .given("subjectName", "xyz-peter.smith")
                    // initial account
                    .given("globalUid", 21011)
                    .given("globalGid", 21011)
                    .thenExpect(HttpStatus.OK)
                    .keep();
        }

        @Test
        @Order(9311)
        @Requires("Account: xyz-peter.smith")
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
        @Order(9312)
        @Requires("Account: xyz-peter.smith")
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
        @Order(9313)
        @Requires("Account: xyz-peter.smith")
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

        @Test
        @Order(9320)
        @Produces(
                explicitly = "Account: xyz-tom.sawyer",
                implicitly = { "Person: Tom Sawyer" })
        void shouldCreateAccountWithNewPersonForPreexistingUserSubject() {
            new CreateAccountWithNewPersonForPreexistingSubject(scenarioTest, asGlobalAgent())
                    // the person to be created along with the account
                    .given("personFamilyName", "Sawyer")
                    .given("personGivenName", "Tom")
                    // the preexisting USER subject, e.g. synchronized from Keycloak
                    .given("subjectUuid", TOM_SAWYER_SUBJECT_UUID)
                    .given("subjectName", "xyz-tom.sawyer")
                    // initial account
                    .given("globalUid", 21016)
                    .given("globalGid", 21016)
                    .thenExpect(HttpStatus.OK)
                    .keep();
        }

        @Test
        @Order(9321)
        @Requires("Account: xyz-tom.sawyer")
        void newlyCreatedAccountForPreexistingSubjectAndNewPersonShouldBeAbleToViewThatPerson() {
            new AccountCanViewTheirOwnPerson(scenarioTest, asSubject("xyz-tom.sawyer"))
                    .given("subjectName", "xyz-tom.sawyer")
                    // some expected person data
                    .expected("personFamilyName", "Sawyer")
                    .expected("personGivenName", "Tom")
                    .thenExpect(HttpStatus.OK);
        }

        @Test
        @Order(9330)
        @Produces(
                explicitly = "Account: xyz-jack.tucker",
                implicitly = { "Person: Jack Tucker" })
        void shouldCreateAccountForPreexistingUserSubjectViaSubjectUuid() {
            new CreateAccountForPreexistingSubject(scenarioTest, asGlobalAgent())
                    // to find a specific existing person
                    .given("personFamilyName", "Tucker")
                    .given("personGivenName", "Jack")
                    .given("personGivenType", "NATURAL_PERSON")
                    // the preexisting USER subject, e.g. synchronized from Keycloak
                    .given("subjectUuid", JACK_TUCKER_SUBJECT_UUID)
                    .given("subjectName", "xyz-jack.tucker")
                    // initial account
                    .given("globalUid", 21015)
                    .given("globalGid", 21015)
                    .thenExpect(HttpStatus.OK)
                    .keep();
        }

        @Test
        @Order(9331)
        @Requires("Account: xyz-jack.tucker")
        void newlyCreatedAccountForPreexistingSubjectShouldBeAbleToViewThatPerson() {
            new AccountCanViewTheirOwnPerson(scenarioTest, asSubject("xyz-jack.tucker"))
                    .given("subjectName", "xyz-jack.tucker")
                    // some expected person data
                    .expected("personFamilyName", "Tucker")
                    .expected("personGivenName", "Jack")
                    .thenExpect(HttpStatus.OK);
        }

    }

    @Nested
    @Order(94)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class RbacSubjectVisibilityScenarios {

        // stable, Keycloak-assigned UUIDs shared with this backend (the subject's identity never changes)
        private static final String ABC_PETER_SMITH_SUBJECT_UUID = "242a0005-0000-0000-0000-000000000005";
        private static final String DEF_PETER_SMITH_SUBJECT_UUID = "242a0006-0000-0000-0000-000000000006";

        /**
         * Verifies Scenario#236.01: A user can see all subjects of the same organization,
         *      Scenario#236.02: Users can see all groups assigned to them,
         *      and Scenario#236.04: Users can NOT see arbitrary subjects.
         */
        @Test
        @Order(9410)
        @Requires("Account: xyz-peter.smith")
        void userCanViewSubjectsFromTheirOwnRealm() {
            new ViewRbacSubjects(scenarioTest, asSubject("xyz-peter.smith").withGroups("/xyz-Team"))
                    .introduction("""
                            This scenario verifies that subject visibility is based on the realm prefix
                            of the current login subject. Groups assigned to the user always belong to
                            the user's own realm and are thus visible via the realm prefix as well.
                            """)
                    .expected("expectedSubjectNames", """
                            [
                              { "name": "xyz-peter.smith" },
                              { "name": "/xyz-Service" },
                              { "name": "/xyz-Team" }
                            ]
                            """)
                    .expected("unexpectedSubjectNames", """
                            [
                              { "name": "hsh-alex_superuser" },
                              { "name": "/hsh-Hostmasters" },
                              { "name": "tst-drew_selfregistered" }
                            ]
                            """)
                    .thenExpect(HttpStatus.OK);
        }

        /**
         * Verifies Scenario#236.03: Users can see groups associated with the same person.
         */
        @Test
        @Order(9430)
        @Requires("Account: xyz-peter.smith")
        void userCanViewSubjectsAssociatedWithAnotherAccountOfTheSamePerson() {
            new ViewRbacSubjectsAssociatedWithSamePerson(scenarioTest,
                    asSubject("xyz-peter.smith"))
                    // to find the person by its name, we could also do this via account and uuid
                    .given("thePersonsFamilyName", "Smith")
                    .given("thePersonsGivenName", "Peter")
                    .given("nameOfAssociatedGroupSubjectFromAnotherOrg", "/abc-Team")
                    .given("theAccountSubjectName", "abc-peter.smith")
                    .using("theAccountSubjectUuid", ABC_PETER_SMITH_SUBJECT_UUID)
                    .using("theAccountGlobalUid", 21013)
                    .using("theAccountGlobalGid", 21013)
                    .thenExpect(HttpStatus.OK);
        }

        /**
         * Verifies Scenario#236.05: Assuming a non-global role drops subject visibility stemming from the user and its groups.
         */
        @Test
        @Order(9440)
        void assumingANonGlobalRoleDropsAllSubjectVisibility() {
            new ViewRbacSubjects(
                    scenarioTest,
                    asSubject("tst-person_firbysusan")
                            .whichIs("a regular user who is member of a group and can assume a non-global role")
                            .withGroups("/xyz-Service"))
                    .introduction("""
                            This scenario verifies that once a non-global ReBAC role is assumed,
                            the subject visibility otherwise stemming from the login subject and its groups
                            is dropped, keeping this endpoint consistent with purely ReBAC-based APIs in which
                            the concrete subject contributes no rights anymore once a role got assumed.
                            """)
                    .given("assumedRoleIdName", "hs_office.relation#FirstGmbH-with-DEBITOR-FirstGmbH:AGENT")
                    .using("assumedRoleDescription", "a non-global role")
                    .expected("expectedExactSubjectNames", """
                            []
                            """)
                    .thenExpect(HttpStatus.OK);
        }

        /**
         * Verifies Scenario#236.06: Assuming the global admin role keeps global subject visibility.
         */
        @Test
        @Order(9450)
        void assumingTheGlobalAdminRoleKeepsGlobalSubjectVisibility() {
            new ViewRbacSubjects(
                    scenarioTest,
                    asSubject("hsh-alex_superuser")
                            .whichIs("a global admin"))
                    .introduction("""
                            This scenario verifies that a global admin who assumed just the global admin role
                            still sees all subjects across all realms, keeping this endpoint consistent with
                            purely ReBAC-based APIs in which the global-admin role can see everything.
                            """)
                    .given("assumedRoleIdName", "rbac.global#global:ADMIN")
                    .using("assumedRoleDescription", "the global admin role")
                    .expected("expectedSubjectNames", """
                            [
                              { "name": "hsh-alex_superuser" },
                              { "name": "hsh-fran_superuser" },
                              { "name": "tst-customer_admin_xxx" },
                              { "name": "/hsh-Hostmasters" },
                              { "name": "/xyz-Service" }
                            ]
                            """)
                    .thenExpect(HttpStatus.OK);
        }

        /**
         * Verifies Scenario#236.07: Name and type filters narrow the visible subjects.
         */
        @Test
        @Order(9460)
        @Requires("Account: xyz-peter.smith")
        void nameAndTypeFiltersNarrowButNeverWidenVisibleSubjects() {
            new ViewRbacSubjectsNarrowedByFilters(
                    scenarioTest,
                    asSubject("xyz-peter.smith")
                            .whichIs("a user with same-realm subjects and a same-person account in another org"))
                    .given("thePersonsFamilyName", "Smith")
                    .given("thePersonsGivenName", "Peter")
                    .given("nameOfSamePersonGroupFromAnotherOrg", "/def-Team")
                    .given("theAccountSubjectName", "def-peter.smith")
                    .given("nameFilterPrefix", "/xyz")
                    .using("theAccountSubjectUuid", DEF_PETER_SMITH_SUBJECT_UUID)
                    .using("theAccountGlobalUid", 21014)
                    .using("theAccountGlobalGid", 21014)
                    .expected("expectedSubjectNamesWithTypeFilter", """
                            [
                              { "name": "/xyz-Service" },
                              { "name": "/xyz-Team" },
                              { "name": "/def-Team" }
                            ]
                            """)
                    .expected("unexpectedSubjectNamesWithTypeFilter", """
                            [
                              { "name": "xyz-peter.smith" }
                            ]
                            """)
                    .expected("expectedSubjectNamesWithNameFilter", """
                            [
                              { "name": "/xyz-Service" },
                              { "name": "/xyz-Team" }
                            ]
                            """)
                    .expected("unexpectedSubjectNamesWithNameFilter", """
                            [
                              { "name": "xyz-peter.smith" },
                              { "name": "/def-Team" }
                            ]
                            """)
                    .thenExpect(HttpStatus.OK);
        }

        /**
         * Verifies Scenario#236.08: A global admin without an assumed role sees all subjects.
         */
        @Test
        @Order(9470)
        void globalAdminWithoutAssumedRoleSeesAllSubjects() {
            new ViewRbacSubjects(
                    scenarioTest,
                    asSubject("hsh-alex_superuser").whichIs("a global admin without an assumed role"))
                    .introduction("""
                            This scenario verifies that a global admin who has not assumed any role
                            sees all subjects across all realms.
                            """)
                    .expected("expectedSubjectNames", """
                            [
                              { "name": "hsh-alex_superuser" },
                              { "name": "hsh-fran_superuser" },
                              { "name": "tst-customer_admin_xxx" },
                              { "name": "/hsh-Hostmasters" },
                              { "name": "/xyz-Service" }
                            ]
                            """)
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
