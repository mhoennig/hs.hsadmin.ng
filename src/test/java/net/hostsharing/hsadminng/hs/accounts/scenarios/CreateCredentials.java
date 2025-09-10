package net.hostsharing.hsadminng.hs.accounts.scenarios;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public class CreateCredentials extends BaseCredentialsUseCase<CreateCredentials> {

    public CreateCredentials(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("A set of credentials contains the login data for an RBAC subject.");
    }

    @Override
    protected HttpResponse run() {

        obtain("Person: %{personGivenName} %{personFamilyName}", () ->
                httpGet("/api/hs/office/persons?name=%{personFamilyName}")
                        .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In real situations we have more precise measures to find the related person."
        );

        given("resolvedContexts",
            fetchContextResourcesByDescriptorPairs("contexts")
        );

        return obtain("newCredentials", () ->
            httpPost("/api/hs/accounts/credentials", usingJsonBody("""
                {
                     "person.uuid": ${Person: %{personGivenName} %{personFamilyName}},
                     "nickname": ${nickname},
                     "active": %{active},
                     "totpSecrets": @{totpSecrets},
                     "emailAddress": ${emailAddress},
                     "phonePassword": ${phonePassword},
                     "smsNumber": ${smsNumber},
                     "onboardingToken": ${onboardingToken},
                     "globalUid": %{globalUid},
                     "globalGid": %{globalGid},
                     "contexts": @{resolvedContexts}
                }
                """))
                .expecting(HttpStatus.CREATED).expecting(ContentType.JSON)
        );
    }

    @Override
    protected void verify(final UseCase<CreateCredentials>.HttpResponse response) {
        verify(
                "Verify the New Credentials",
                () -> httpGet("/api/hs/accounts/credentials/%{newCredentials}")
                        .expecting(OK).expecting(JSON),
                path("uuid").contains("%{newCredentials}"),
                path("nickname").contains("%{nickname}"),
                path("person.uuid").contains("%{Person: %{personGivenName} %{personFamilyName}}"),
                path("totpSecrets").contains("@{totpSecrets}"),
                path("onboardingToken").contains("%{onboardingToken}")
        );
    }
}
