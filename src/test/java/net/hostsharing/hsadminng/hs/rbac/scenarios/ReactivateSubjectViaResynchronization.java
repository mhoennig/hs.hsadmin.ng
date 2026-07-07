package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.springframework.http.HttpStatus.OK;

public class ReactivateSubjectViaResynchronization extends UseCase<ReactivateSubjectViaResynchronization> {

    private final FakeLoginUser loginUser;

    public ReactivateSubjectViaResynchronization(final ScenarioTest testSuite, final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;

        introduction("""
                When a subject reappears in Keycloak, e.g. via a replayed event or a re-created
                user with the same UUID, the sync program simply synchronizes it again. Because the
                earlier removal was just a deactivation, the UUID-keyed upsert reactivates the
                retained subject: it keeps its UUID, becomes visible again, and the upsert reports
                an update (`200 OK`), not a creation.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        return withTitle(
                "Re-synchronize the deactivated subject via PUT with the same UUID",
                () -> httpPut(
                        loginUser,
                        "/api/rbac/subjects/%{subjectUuid}",
                        usingJsonBody("""
                                {
                                    "name": ${subjectName},
                                    "type": ${subjectType}
                                }
                                """))
                        .expecting(expectedStatus));
    }

    @Override
    protected void verify(final HttpResponse response) {
        // the reactivated subject keeps its UUID and carries the synchronized name
        response.path("uuid").isEqualTo(ScenarioTest.resolve("%{subjectUuid}", DROP_COMMENTS));
        response.path("name").isEqualTo(ScenarioTest.resolve("%{subjectName}", DROP_COMMENTS));

        // the counterpart of the deactivation check: the subject can be fetched again
        withTitle(
                "The reactivated subject is visible again",
                () -> {
                    final var fetched = httpGet(loginUser, "/api/rbac/subjects/%{subjectUuid}")
                            .expecting(OK);
                    fetched.path("name").isEqualTo(ScenarioTest.resolve("%{subjectName}", DROP_COMMENTS));
                    return fetched;
                });
    }
}
