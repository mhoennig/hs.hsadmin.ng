package net.hostsharing.hsadminng.hs.accounts.scenarios;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public class CreateProfile extends BaseProfileUseCase<CreateProfile> {

    public CreateProfile(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("A set of profile contains the login data for an RBAC subject.");
    }

    @Override
    protected HttpResponse run() {

        obtain("Person: %{personGivenName} %{personFamilyName}", () ->
                httpGet("/api/hs/office/persons?name=%{personFamilyName}")
                        .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In real situations we have more precise measures to find the related person."
        );

        given("resolvedScopes",
            fetchScopeResourcesByDescriptorPairs("scopes")
        );

        return obtain("newProfile", () ->
            httpPost("/api/hs/accounts/profiles", usingJsonBody("""
                {
                     "person.uuid": ${Person: %{personGivenName} %{personFamilyName}},
                     "nickname": ${nickname},
                     "emailAddress": ${emailAddress},
                     "smsNumber": ${smsNumber},
                     "password": ${password},
                     "totpSecrets": @{totpSecrets},
                     "phonePassword": ${phonePassword},
                     "globalUid": %{globalUid},
                     "globalGid": %{globalGid},
                     "active": %{active},
                     "scopes": @{resolvedScopes}
                }
                """))
                .expecting(HttpStatus.CREATED).expecting(ContentType.JSON)
        );
    }

    @Override
    protected void verify(final UseCase<CreateProfile>.HttpResponse response) {
        verify(
                "Verify the new Profile",
                () -> httpGet("/api/hs/accounts/profiles/%{newProfile}")
                        .expecting(OK).expecting(JSON),
                path("uuid").contains("%{newProfile}"),
                path("nickname").contains("%{nickname}"),
                path("person.uuid").contains("%{Person: %{personGivenName} %{personFamilyName}}"),
                path("totpSecrets").contains("@{totpSecrets}")
        );
    }
}
