package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

public class SynchronizeNewSubjectReusingDeactivatedName extends UseCase<SynchronizeNewSubjectReusingDeactivatedName> {

    private final FakeLoginUser loginUser;

    public SynchronizeNewSubjectReusingDeactivatedName(final ScenarioTest testSuite, final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;

        introduction("""
                Keycloak enforces unique usernames only among existing users, so a name freed by a
                removed (here: deactivated) subject can be claimed by a completely new user with a
                new UUID. Synchronizing that new user creates a new subject (`201 Created`) despite
                the deactivated subject still carrying the same name, because name-uniqueness only
                applies to active subjects. The deactivated subject remains deactivated and untouched.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        return withTitle(
                "Synchronize the new subject reusing the deactivated subject's name via PUT",
                () -> httpPut(
                        loginUser,
                        "/api/rbac/subjects/%{newSubjectUuid}",
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
        // a new subject was created, carrying the new UUID and the reused name
        response.path("uuid").isEqualTo(ScenarioTest.resolve("%{newSubjectUuid}", DROP_COMMENTS));
        response.path("name").isEqualTo(ScenarioTest.resolve("%{subjectName}", DROP_COMMENTS));

        // the reused name refers to exactly one (active) subject
        withTitle(
                "The reused name refers to exactly one active subject",
                () -> httpGet(loginUser, "/api/rbac/subjects?name=%{subjectName}")
                        .expecting(OK)
                        .expectArrayElements(1));

        // the deactivated subject with the same name remains deactivated
        withTitle(
                "The deactivated subject with the same name remains deactivated",
                () -> httpGet(loginUser, "/api/rbac/subjects/%{deactivatedSubjectUuid}")
                        .expecting(NOT_FOUND));
    }
}
