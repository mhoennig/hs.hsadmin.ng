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
        new DeactivateSubject(scenarioTest, asGlobalAgent())
                .given("subjectUuid", "%{SubjectSync: sync-alice}")
                .thenExpect(HttpStatus.NO_CONTENT);
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
     * Verifies Scenario#238.09: A deactivated subject is reactivated by the next synchronization with the same UUID.
     *
     * <p>It reactivates with its original name `sync-alice`, because a new subject took over the name
     * `sync-alicia` in Scenario#238.08 — just like a new Keycloak user can claim the freed name.</p>
     */
    @Test
    @Order(9039)
    @Requires("SubjectSync: sync-alice - deactivated")
    void aDeactivatedSubjectIsReactivatedByTheNextSynchronizationWithTheSameUuid() {
        new ReactivateSubjectViaResynchronization(scenarioTest, asGlobalAgent())
                .given("subjectUuid", "%{SubjectSync: sync-alice}")
                .given("subjectName", "sync-alice")
                .given("subjectType", "USER")
                .thenExpect(HttpStatus.OK);
    }
}
