package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

public class DeactivateSubject extends UseCase<DeactivateSubject> {

    private final FakeLoginUser loginUser;

    public DeactivateSubject(final ScenarioTest testSuite, final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;

        introduction("""
                When a user or group is removed from Keycloak, the sync program deletes the
                corresponding subject. This is a soft-delete (deactivation): the subject record is
                retained but no longer visible or assignable, so `DELETE` returns `204 No Content`
                and the subject can no longer be fetched.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        return withTitle(
                "Deactivate the removed subject via DELETE",
                () -> httpDelete(loginUser, "/api/rbac/subjects/%{subjectUuid}")
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
