package net.hostsharing.hsadminng.hs.rbac.scenarios;

import lombok.SneakyThrows;
import net.hostsharing.hsadminng.hs.scenarios.Produces;
import net.hostsharing.hsadminng.hs.scenarios.Requires;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;

import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asSubject;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SubjectSyncScenarioTests extends ScenarioTest {

    // stable, Keycloak-assigned UUIDs shared with this backend (the subject's identity never changes)
    private static final String ALICE_UUID = "238a0001-0000-0000-0000-000000000001";
    private static final String TEAM_UUID = "238a0002-0000-0000-0000-000000000002";
    private static final String EVE_UUID = "238a0004-0000-0000-0000-000000000004";
    private static final String NEW_ALICIA_UUID = "238a0005-0000-0000-0000-000000000005";
    private static final String INVALID_UUID = "238a0007-0000-0000-0000-000000000007";
    private static final String BOB_UUID = "239a0001-0000-0000-0000-000000000001";
    private static final String OPERATORS_UUID = "239a0002-0000-0000-0000-000000000002";
    private static final String CAROL_UUID = "239a0003-0000-0000-0000-000000000003";
    private static final String INVALID_USER_UUID = "239a0004-0000-0000-0000-000000000004";
    private static final String INVALID_GROUP_UUID = "239a0005-0000-0000-0000-000000000005";
    private static final String INVALID_GROUP_ORG_UUID = "239a0006-0000-0000-0000-000000000006";
    private static final String JANE_UUID = "239a0007-0000-0000-0000-000000000007";
    private static final String SMITH_OPERATORS_UUID = "239a0008-0000-0000-0000-000000000008";

    @SneakyThrows
    @org.junit.jupiter.api.BeforeEach
    protected void beforeScenario(final TestInfo testInfo) {
        super.beforeScenario(testInfo);
    }

    /**
     * Verifies Scenario#238.01: A global-admin can synchronize a new USER subject.
     */
    @Test
    @Order(9031)
    @Produces("SubjectSync: sync-alice")
    void aGlobalAdminCanSynchronizeANewUserSubject() {
        new SynchronizeSubject(scenarioTest, asGlobalAgent())
                .given("subjectUuid", ALICE_UUID)
                .given("subjectName", "sync-alice")
                .given("subjectType", "USER")
                .thenExpect(HttpStatus.CREATED)
                .keep();
    }

    /**
     * Verifies Scenario#238.02: A global-admin can synchronize a new GROUP subject.
     */
    @Test
    @Order(9032)
    @Produces("SubjectSync: /sync-Team")
    void aGlobalAdminCanSynchronizeANewGroupSubject() {
        new SynchronizeSubject(scenarioTest, asGlobalAgent())
                .given("subjectUuid", TEAM_UUID)
                .given("subjectName", "/sync-Team")
                .given("subjectType", "GROUP")
                .thenExpect(HttpStatus.CREATED)
                .keep();
    }

    /**
     * Verifies Scenario#238.03: Re-synchronizing an existing subject updates its name idempotently.
     */
    @Test
    @Order(9033)
    @Requires("SubjectSync: sync-alice")
    @Produces("SubjectSync: sync-alice - renamed to sync-alicia")
    void reSynchronizingAnExistingSubjectUpdatesItsNameIdempotently() {
        new ReSynchronizeSubjectIdempotently(scenarioTest, asGlobalAgent())
                .given("subjectUuid", "%{SubjectSync: sync-alice}")
                .given("subjectType", "USER")
                .given("newSubjectName", "sync-alicia")
                .thenExpect(HttpStatus.OK);
    }

    /**
     * Verifies Scenario#238.04: A non-global-admin cannot synchronize subjects.
     */
    @Test
    @Order(9034)
    void aNonGlobalAdminCannotSynchronizeSubjects() {
        new SynchronizeSubject(scenarioTest,
                asSubject("tst-customer_admin_xxx").whichIs("an authenticated user without the global-admin role"))
                .given("subjectUuid", EVE_UUID)
                .given("subjectName", "sync-eve")
                .given("subjectType", "USER")
                .thenExpect(HttpStatus.FORBIDDEN);
    }

    /**
     * Verifies Scenario#238.05: A global-admin can deactivate a subject removed from Keycloak.
     */
    @Test
    @Order(9035)
    @Requires("SubjectSync: sync-alice - renamed to sync-alicia")
    @Produces("SubjectSync: sync-alice - deactivated")
    void aGlobalAdminCanDeactivateARemovedSubject() {
        new DeactivateSubjectViaSynchronization(scenarioTest, asGlobalAgent())
                .given("subjectUuid", "%{SubjectSync: sync-alice}")
                .given("subjectName", "sync-alicia") // the current name, from the rename in Scenario#238.03
                .given("subjectType", "USER")
                .thenExpect(HttpStatus.OK);
    }

    /**
     * Verifies Scenario#238.06: A deactivated subject no longer appears in queries.
     */
    @Test
    @Order(9036)
    @Requires("SubjectSync: sync-alice - deactivated")
    void aDeactivatedSubjectNoLongerAppearsInQueries() {
        new ViewRbacSubjects(scenarioTest, asGlobalAgent())
                .introduction("""
                        A deactivated subject is excluded from all read paths, so it neither appears in the
                        list of subjects (`GET /api/rbac/subjects`) nor can it be assigned any longer.
                        """)
                .expected("unexpectedSubjectNames", """
                        [
                          { "name": "sync-alice" },
                          { "name": "sync-alicia" }
                        ]
                        """)
                .thenExpect(HttpStatus.OK);
    }

    /**
     * Verifies Scenario#238.07: Invalid subject names are rejected.
     */
    @Test
    @Order(9037)
    void invalidSubjectNamesAreRejected() {
        new SynchronizeSubject(scenarioTest, asGlobalAgent())
                .given("subjectUuid", INVALID_UUID)
                .given("subjectName", "invalidusername@example.com")
                .given("subjectType", "USER")
                .thenExpect(HttpStatus.BAD_REQUEST);
    }

    /**
     * Verifies Scenario#238.08: A new subject with a new UUID is created despite a deactivated subject with the same name.
     */
    @Test
    @Order(9038)
    @Requires("SubjectSync: sync-alice - deactivated")
    @Produces("SubjectSync: sync-alicia (successor)")
    void aNewSubjectWithANewUuidIsCreatedDespiteADeactivatedSubjectWithTheSameName() {
        new SynchronizeNewSubjectReusingDeactivatedName(scenarioTest, asGlobalAgent())
                .given("newSubjectUuid", NEW_ALICIA_UUID)
                .given("subjectName", "sync-alicia")
                .given("subjectType", "USER")
                .given("deactivatedSubjectUuid", "%{SubjectSync: sync-alice}")
                .thenExpect(HttpStatus.CREATED)
                .keep();
    }

    /**
     * Verifies Scenario#239.01: A global-admin can synchronize a USER subject with an explicit organization.
     */
    @Test
    @Order(9040)
    @Produces("SubjectSync: bob@example.com")
    void aGlobalAdminCanSynchronizeAUserSubjectWithAnExplicitOrganization() {
        new SynchronizeSubject(scenarioTest, asGlobalAgent())
                .given("subjectUuid", BOB_UUID)
                .given("subjectName", "bob@example.com")
                .given("organization", "example")
                .given("subjectType", "USER")
                .thenExpect(HttpStatus.CREATED)
                .keep();
    }

    /**
     * Verifies Scenario#239.02: A global-admin can synchronize a GROUP subject with an explicit organization.
     */
    @Test
    @Order(9041)
    @Produces("SubjectSync: /example-Operators")
    void aGlobalAdminCanSynchronizeAGroupSubjectWithAnExplicitOrganization() {
        new SynchronizeSubject(scenarioTest, asGlobalAgent())
                .given("subjectUuid", OPERATORS_UUID)
                .given("subjectName", "/example-Operators")
                .given("organization", "example")
                .given("subjectType", "GROUP")
                .thenExpect(HttpStatus.CREATED);
    }

    /**
     * Verifies Scenario#239.03: Without an explicit organization, the organization is derived from the name prefix.
     */
    @Test
    @Order(9042)
    @Produces("SubjectSync: xyz-carol")
    void aSubjectSynchronizedWithoutExplicitOrganizationGetsItDerivedFromTheNamePrefix() {
        new SynchronizeSubject(scenarioTest, asGlobalAgent())
                .introduction("""
                        The sync program synchronizes a subject with a realm-prefixed name and **no** explicit
                        organization through the UUID-keyed idempotent `PUT /api/rbac/subjects/{subjectUuid}`,
                        just like all PR#238 sync requests. The organization is derived from the name prefix
                        (the part before the first `-`, without the leading `/` of GROUP names) and stored
                        with the subject.
                        """)
                .given("subjectUuid", CAROL_UUID)
                .given("subjectName", "xyz-carol")
                .given("subjectType", "USER")
                .expected("expectedOrganization", "xyz")
                .thenExpect(HttpStatus.CREATED);
    }

    /**
     * Verifies Scenario#239.04: A USER subject name starting with a slash is rejected despite an explicit organization.
     */
    @Test
    @Order(9043)
    void aUserSubjectNameStartingWithASlashIsRejectedDespiteAnExplicitOrganization() {
        new SynchronizeSubject(scenarioTest, asGlobalAgent())
                .given("subjectUuid", INVALID_USER_UUID)
                .given("subjectName", "/bob")
                .given("organization", "example")
                .given("subjectType", "USER")
                .thenExpect(HttpStatus.BAD_REQUEST);
    }

    /**
     * Verifies Scenario#239.05: A GROUP subject name without a leading slash is rejected despite an explicit organization.
     */
    @Test
    @Order(9044)
    void aGroupSubjectNameWithoutALeadingSlashIsRejectedDespiteAnExplicitOrganization() {
        new SynchronizeSubject(scenarioTest, asGlobalAgent())
                .given("subjectUuid", INVALID_GROUP_UUID)
                .given("subjectName", "example-Operators")
                .given("organization", "example")
                .given("subjectType", "GROUP")
                .thenExpect(HttpStatus.BAD_REQUEST);
    }

    /**
     * Verifies Scenario#239.06: A GROUP organization which does not match the group-name prefix is rejected.
     */
    @Test
    @Order(9045)
    void aGroupOrganizationWhichDoesNotMatchTheGroupNamePrefixIsRejected() {
        new SynchronizeSubject(scenarioTest, asGlobalAgent())
                .given("subjectUuid", INVALID_GROUP_ORG_UUID)
                .given("subjectName", "/example-Operators")
                .given("organization", "xyz")
                .given("subjectType", "GROUP")
                .thenExpect(HttpStatus.BAD_REQUEST);
    }

    /**
     * Verifies Scenario#238.09: A deactivated subject is reactivated by the next synchronization with the same UUID.
     *
     * <p>It reactivates with its original name `sync-alice`, because a new subject took over the name
     * `sync-alicia` in Scenario#238.08 — just like a new Keycloak user can claim the freed name.</p>
     */
    @Test
    @Order(9039)
    @Requires("SubjectSync: sync-alice - deactivated")
    void aDeactivatedSubjectIsReactivatedByTheNextSynchronizationWithTheSameUuid() {
        new SynchronizeSubject(scenarioTest, asGlobalAgent())
                .introduction("""
                        When a subject reappears in Keycloak, e.g. because the external sync presumed
                        it was deleted because of some sort of system failure or because it was simply
                        disabled in Keycloak and got enabled again, the sync program simply synchronizes it again.
                        And because the earlier removal was just a deactivation, the UUID-keyed PUT reactivates the
                        retained subject: it keeps its UUID, becomes visible again, and the response reports
                        an update (`200 OK`), not a creation.
                        
                        This is necessary so that explicit RBAC-grant's don't get lost too soon.
                        """)
                .given("subjectUuid", "%{SubjectSync: sync-alice}")
                .given("subjectName", "sync-alice")
                .given("subjectType", "USER")
                .thenExpect(HttpStatus.OK);
    }

    /**
     * Verifies Scenario#239.07: Subjects can be listed filtered by their organization.
     */
    @Test
    @Order(9046)
    @Requires("SubjectSync: xyz-carol")
    void subjectsCanBeListedFilteredByTheirOrganization() {
        new ListSubjectsFilteredByOrganization(scenarioTest, asGlobalAgent())
                .given("userUuid", JANE_UUID)
                .given("userName", "jane@example.com")
                .given("organization", "smith") // used for both the user+group
                .given("groupUuid", SMITH_OPERATORS_UUID)
                .given("groupName", "/smith-Operators")
                .expected("unexpectedSubjectNames", """
                        [
                          { "name": "xyz-carol" },
                          { "name": "hsh-alex_superuser" }
                        ]
                        """)
                .thenExpect(HttpStatus.OK);
    }

    /**
     * Verifies Scenario#239.08: Re-synchronizing a USER subject with a different organization moves it to that
     * organization.
     */
    @Test
    @Order(9047)
    @Requires("SubjectSync: bob@example.com")
    void reSynchronizingAUserSubjectWithADifferentOrganizationMovesItToThatOrganization() {
        new SynchronizeSubject(scenarioTest, asGlobalAgent())
                .introduction("""
                        The sync program re-synchronizes an already existing USER subject with the same UUID
                        and name, but a different explicit organization, e.g. after the user moved to another
                        organization in Keycloak. The idempotent PUT updates the organization in place
                        and returns `200 OK`; the UUID and the name remain unchanged.
                        For GROUP subjects the organization is bound to the group-name prefix,
                        so it can only change together with a matching rename.
                        """)
                .given("subjectUuid", "%{SubjectSync: bob@example.com}")
                .given("subjectName", "bob@example.com")
                .given("organization", "acme")
                .given("subjectType", "USER")
                .thenExpect(HttpStatus.OK);
    }

    /**
     * Verifies Scenario#238.10: A subject which is gone for good can be permanently deleted;
     * as a safeguard, the DELETE request has to repeat the subject's name and type.
     */
    @Test
    @Order(9048)
    @Requires("SubjectSync: sync-alicia (successor)")
    void aGlobalAdminCanPermanentlyDeleteASubject() {
        new DeleteSubjectPermanently(scenarioTest, asGlobalAgent())
                .given("subjectUuid", "%{SubjectSync: sync-alicia (successor)}")
                .given("subjectName", "sync-alicia")
                .given("subjectType", "USER")
                .thenExpect(HttpStatus.NO_CONTENT);
    }
}
