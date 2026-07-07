package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;

public class SynchronizeSubject extends UseCase<SynchronizeSubject> {

    private final FakeLoginUser loginUser;

    public SynchronizeSubject(final ScenarioTest testSuite, final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;

        introduction("""
                The external Keycloak sync program synchronizes a single subject through the
                UUID-keyed idempotent upsert `PUT /api/rbac/subjects/{subjectUuid}`. The UUID in the
                path is the same UUID as in Keycloak. Creating a new subject returns `201 Created`,
                updating an existing subject's name returns `200 OK`. Only a global-admin may
                synchronize subjects (others are rejected with `403`), and only realm-prefixed names
                are accepted (others are rejected with `400`).
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        return withTitle(
                "Synchronize (upsert) the subject via PUT",
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
        response.path("uuid").isEqualTo(ScenarioTest.resolve("%{subjectUuid}", DROP_COMMENTS));
        response.path("name").isEqualTo(ScenarioTest.resolve("%{subjectName}", DROP_COMMENTS));
        response.path("type").isEqualTo(ScenarioTest.resolve("%{subjectType}", DROP_COMMENTS));
    }
}
