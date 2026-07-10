package net.hostsharing.hsadminng.hs.accounts.scenarios;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asSubject;
import static org.springframework.http.HttpStatus.OK;

public class CreateAccountForNewPerson extends BaseAccountUseCase<CreateAccountForNewPerson> {

    public CreateAccountForNewPerson(final ScenarioTest testSuite, final FakeLoginUser asLoginUser) {
        super(testSuite, asLoginUser);

        introduction("An account combines an RBAC subject with a natural person and thus grant's access to data in hsadmin-NG.");
    }

    @Override
    protected HttpResponse run() {
        return postNewAccount();
    }

    protected HttpResponse postNewAccount() {
        return obtain("newAccount", () ->
            httpPost(asLoginUser, "/api/hs/accounts/accounts", usingJsonBody("""
                {
                     "person": {
                         "personType": "NATURAL_PERSON",
                         "salutation": "Hallo",
                         "title": null,
                         "givenName": ${personGivenName},
                         "familyName": ${personFamilyName}
                     },
                     "subject": {
                         "uuid": ${subjectUuid},
                         "name": ${subjectName}
                     },
                     "globalUid": %{globalUid},
                     "globalGid": %{globalGid}
                }
                """))
                .expecting(HttpStatus.CREATED).expecting(ContentType.JSON)
        );
    }

    @Override
    protected void verify(final UseCase<CreateAccountForNewPerson>.HttpResponse response) {
        obtain("Person: %{personGivenName} %{personFamilyName}", () ->
                        httpGet(asLoginUser, "/api/hs/office/persons?name=%{personFamilyName}")
                                .expecting(OK).expecting(JSON),
                personResponse -> personResponse.expectArrayElements(1).getFromBody("[0].uuid"),
                "In real situations we have more precise measures to find the related person."
        );

        verify(
                "Verify the new Account as its own Subject",
                () -> httpGet(asSubject("%{subjectName}"), "/api/hs/accounts/accounts/%{newAccount}")
                        .expecting(OK).expecting(JSON),
                path("uuid").contains("%{newAccount}"),
                path("subject.uuid").contains("%{newAccount}"),
                path("subject.name").contains("%{subjectName}"),
                path("person.uuid").contains("%{Person: %{personGivenName} %{personFamilyName}}")
        );
    }
}
