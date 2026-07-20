package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

public class DeleteSubjectPermanently extends UseCase<DeleteSubjectPermanently> {

    private final FakeLoginUser loginUser;

    public DeleteSubjectPermanently(final ScenarioTest testSuite, final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;

        introduction("""
                A subject which is gone for good can be permanently deleted, together with all of
                its grants. This is destructive and irreversible; to just deactivate a USER or
                GROUP subject, synchronize it with `deactivated: true` instead.
                As a safeguard against deleting the wrong subject, the `DELETE` request has to
                repeat the subject's name and type as query parameters, which are verified
                against the subject identified by the UUID in the path.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        return withTitle(
                "Permanently delete the subject via DELETE",
                () -> httpDelete(loginUser, "/api/rbac/subjects/%{subjectUuid}?name=%{subjectName}&type=%{subjectType}")
                        .expecting(expectedStatus));
    }

    @Override
    protected void verify(final HttpResponse response) {
        withTitle(
                "The deleted subject is gone",
                () -> httpGet(loginUser, "/api/rbac/subjects/%{subjectUuid}")
                        .expecting(NOT_FOUND));
    }
}
