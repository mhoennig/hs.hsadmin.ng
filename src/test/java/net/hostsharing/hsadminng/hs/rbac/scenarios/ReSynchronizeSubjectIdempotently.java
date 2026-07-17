package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.springframework.http.HttpStatus.OK;

public class ReSynchronizeSubjectIdempotently extends UseCase<ReSynchronizeSubjectIdempotently> {

    private final FakeLoginUser loginUser;

    public ReSynchronizeSubjectIdempotently(final ScenarioTest testSuite, final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;

        introduction("""
                The sync program re-synchronizes an already existing subject with the same UUID but a
                new name (a Keycloak rename). Because the sync program cannot know whether this backend
                already has the subject, adding and renaming are the same idempotent PUT: it updates
                the name in place, creates no second subject, and replaying the identical request is a
                no-op — all returning `200 OK`.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        return withTitle(
                "Re-synchronize the subject with a new name (rename) via PUT",
                () -> putNewName().expecting(expectedStatus));
    }

    private HttpResponse putNewName() {
        return httpPut(
                loginUser,
                "/api/rbac/subjects/%{subjectUuid}",
                usingJsonBody("""
                        {
                            "name": ${newSubjectName},
                            "type": ${subjectType}
                        }
                        """));
    }

    @Override
    protected void verify(final HttpResponse response) {
        // the renamed subject carries the new name and keeps its UUID
        response.path("uuid").isEqualTo(ScenarioTest.resolve("%{subjectUuid}", DROP_COMMENTS));
        response.path("name").isEqualTo(ScenarioTest.resolve("%{newSubjectName}", DROP_COMMENTS));

        // replaying the identical PUT leaves the subject unchanged and still returns 200 OK
        withTitle(
                "Replaying the identical PUT is a no-op (idempotent)",
                () -> {
                    final var replay = putNewName().expecting(OK);
                    replay.path("name").isEqualTo(ScenarioTest.resolve("%{newSubjectName}", DROP_COMMENTS));
                    return replay;
                });

        // the rename did not create a second subject
        withTitle(
                "The rename did not create a second subject",
                () -> httpGet(loginUser, "/api/rbac/subjects?name=%{newSubjectName}")
                        .expecting(OK)
                        .expectArrayElements(1));
    }
}
