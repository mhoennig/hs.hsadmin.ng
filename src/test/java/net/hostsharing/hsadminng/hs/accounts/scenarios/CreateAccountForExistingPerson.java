package net.hostsharing.hsadminng.hs.accounts.scenarios;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public class CreateAccountForExistingPerson extends BaseAccountUseCase<CreateAccountForExistingPerson> {

    public CreateAccountForExistingPerson(final ScenarioTest testSuite, final FakeLoginUser asLoginUser) {
        super(testSuite, asLoginUser);

        introduction("An account combines an RBAC subject with a natural person and thus grant's access to data in hsadmin-NG.");
    }

    @Override
    protected HttpResponse run() {

        obtain("Person: %{personGivenName} %{personFamilyName}", () ->
                httpGet(asLoginUser, "/api/hs/office/persons?name=%{personFamilyName}&type=%{personGivenType}")
                        .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In real situations we have more precise measures to find the related person."
        );

        return obtain("newAccount", () ->
            httpPost(asLoginUser, "/api/hs/accounts/accounts", usingJsonBody("""
                {
                     "person.uuid": ${Person: %{personGivenName} %{personFamilyName}},
                     "subjectName": ${subjectName},
                     "globalUid": %{globalUid},
                     "globalGid": %{globalGid}
                }
                """))
                .expecting(HttpStatus.CREATED).expecting(ContentType.JSON)
        );
    }

    @Override
    protected void verify(final UseCase<CreateAccountForExistingPerson>.HttpResponse response) {
        verify(
                "Verify the new Account",
                () -> httpGet(asLoginUser, "/api/hs/accounts/accounts/%{newAccount}")
                        .expecting(OK).expecting(JSON),
                path("uuid").contains("%{newAccount}"),
                path("subjectName").contains("%{subjectName}"),
                path("person.uuid").contains("%{Person: %{personGivenName} %{personFamilyName}}")
        );
    }
}
