package net.hostsharing.hsadminng.hs.accounts.scenarios;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.CREATED;

public class CreateAccountForPreexistingSubject extends CreateAccountForExistingPerson {

    public CreateAccountForPreexistingSubject(final ScenarioTest testSuite, final FakeLoginUser asLoginUser) {
        super(testSuite, asLoginUser);

        introduction("""
                An account combines an RBAC subject with a natural person and thus grant's access to data in hsadmin-NG.
                Here, the USER subject already exists, e.g. previously synchronized from Keycloak,
                and is referenced just by its UUID when the account gets created.
                """);
    }

    @Override
    protected HttpResponse run() {

        withTitle("Synchronize the USER Subject from Keycloak", () ->
                httpPut(asLoginUser, "/api/rbac/subjects/%{subjectUuid}", usingJsonBody("""
                        {
                            "name": ${subjectName},
                            "type": "USER"
                        }
                        """))
                        .expecting(CREATED).expecting(JSON),
                "This is what the Keycloak sync program does for each new Keycloak user."
        );

        return super.run();
    }

    @Override
    protected HttpResponse postNewAccount() {
        return obtain("theAccount", () ->
                httpPost(asLoginUser, "/api/hs/accounts/accounts", usingJsonBody("""
                        {
                             "person.uuid": ${Person: %{personGivenName} %{personFamilyName}},
                             "subject.uuid": ${subjectUuid},
                             "globalUid": %{globalUid},
                             "globalGid": %{globalGid}
                        }
                        """))
                        .expecting(HttpStatus.CREATED).expecting(ContentType.JSON)
        );
    }
}
