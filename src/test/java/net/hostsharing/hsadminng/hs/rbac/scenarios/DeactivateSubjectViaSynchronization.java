package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

public class DeactivateSubjectViaSynchronization extends UseCase<DeactivateSubjectViaSynchronization> {

    private final FakeLoginUser loginUser;

    public DeactivateSubjectViaSynchronization(final ScenarioTest testSuite, final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;

        introduction("""
                When a user or group is disabled or removed in Keycloak, the sync program
                synchronizes the desired state with `deactivated: true` through the same UUID-keyed
                create-or-update `PUT /api/rbac/subjects/{subjectUuid}` it uses for creating and
                renaming subjects. The subject is then soft-deleted: its row and its grants are
                retained, but it is excluded from all read paths; re-synchronizing with
                `deactivated: false` (or without that property) reactivates the subject.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        return withTitle(
                "Synchronize the subject as deactivated via PUT",
                () -> httpPut(
                        loginUser,
                        "/api/rbac/subjects/%{subjectUuid}",
                        usingJsonBody("""
                                {
                                    "name": ${subjectName},
                                    "type": ${subjectType},
                                    "deactivated": true
                                }
                                """))
                        .expecting(expectedStatus));
    }

    @Override
    protected void verify(final HttpResponse response) {
        withTitle(
                "The deactivated subject is no longer visible",
                () -> httpGet(loginUser, "/api/rbac/subjects/%{subjectUuid}")
                        .expecting(NOT_FOUND));
    }
}
