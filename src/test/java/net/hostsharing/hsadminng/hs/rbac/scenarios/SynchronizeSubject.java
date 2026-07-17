package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import net.hostsharing.hsadminng.rbac.subject.Subject;
import org.springframework.http.HttpStatus;

import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.springframework.http.HttpStatus.OK;

public class SynchronizeSubject extends UseCase<SynchronizeSubject> {

    private final FakeLoginUser loginUser;

    public SynchronizeSubject(final ScenarioTest testSuite, final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;

        introduction("""
                The external Keycloak sync program synchronizes a single subject through the
                UUID-keyed idempotent `PUT /api/rbac/subjects/{subjectUuid}`. The UUID in the
                path is the same UUID as in Keycloak. Creating a new subject returns `201 Created`,
                updating an existing subject's name returns `200 OK`. Only a global-admin may
                synchronize subjects (others are rejected with `403`). Without an explicit
                organization, only realm-prefixed names are accepted (others are rejected with `400`)
                and the organization is derived from the name prefix. With an explicit organization,
                USER names are free except that they must not start with `/`; GROUP names must start
                with `/` directly followed by the organization, because JWTs reference groups just
                by name and thus the organization must stay derivable from it.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        return withTitle(
                "Synchronize the subject via HTTP PUT",
                () -> httpPut(
                        loginUser,
                        "/api/rbac/subjects/%{subjectUuid}",
                        usingJsonBody("""
                                {
                                    "name": ${subjectName},
                                    "organization": ${organization???},
                                    "type": ${subjectType}
                                }
                                """))
                        .expecting(expectedStatus));
    }

    @Override
    protected void verify(final HttpResponse response) {
        response.path("uuid").isEqualTo(ScenarioTest.resolve("%{subjectUuid}", DROP_COMMENTS));
        response.path("name").isEqualTo(ScenarioTest.resolve("%{subjectName}", DROP_COMMENTS));
        response.path("organization").isEqualTo(expectedOrganization());
        response.path("type").isEqualTo(ScenarioTest.resolve("%{subjectType}", DROP_COMMENTS));

        // the organization is actually stored (and the subject visible), not just echoed in the PUT response
        withTitle(
                "The stored subject carries the expected organization",
                () -> {
                    final var fetched = httpGet(loginUser, "/api/rbac/subjects/%{subjectUuid}")
                            .expecting(OK);
                    fetched.path("name").isEqualTo(ScenarioTest.resolve("%{subjectName}", DROP_COMMENTS));
                    fetched.path("organization").isEqualTo(expectedOrganization());
                    return fetched;
                });
    }

    private static String expectedOrganization() {
        // an `expected("expectedOrganization", ...)` property pins the literal value in the scenario;
        // otherwise the explicitly given organization resp. the name-prefix derivation applies
        final var pinnedOrganization = ScenarioTest.resolve("%{expectedOrganization???}", DROP_COMMENTS);
        if (!pinnedOrganization.isEmpty()) {
            return pinnedOrganization;
        }
        final var explicitOrganization = ScenarioTest.resolve("%{organization???}", DROP_COMMENTS);
        if (!explicitOrganization.isEmpty()) {
            return explicitOrganization;
        }
        return Subject.organizationFromName(ScenarioTest.resolve("%{subjectName}", DROP_COMMENTS));
    }
}
