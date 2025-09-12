package net.hostsharing.hsadminng.hs.accounts.scenarios;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public class UpdateProfile extends BaseProfileUseCase<UpdateProfile> {

    public UpdateProfile(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("A set of profile contains the login data for an RBAC subject.");
    }

    @Override
    protected HttpResponse run() {

        given("resolvedScopes",
                fetchScopeResourcesByDescriptorPairs("scopes")
        );

        withTitle("Patch the Changes to the existing Profile", () ->
            httpPatch("/api/hs/accounts/profiles/%{profileUuid}", usingJsonBody("""
                {
                     "active": %{active},
                     "totpSecrets": @{totpSecrets},
                     "emailAddress": ${emailAddress},
                     "phonePassword": ${phonePassword},
                     "smsNumber": ${smsNumber},
                     "scopes": @{resolvedScopes}
                }
                """))
                .reportWithResponse().expecting(HttpStatus.OK).expecting(ContentType.JSON)
                .extractValue("nickname", "nickname")
                .extractValue("totpSecrets", "totpSecrets")
        );

        return null;
    }

    @Override
    protected void verify(final UseCase<UpdateProfile>.HttpResponse response) {
        verify(
                "Verify the Patched Profile",
                () -> httpGet("/api/hs/accounts/profiles/%{profileUuid}")
                        .expecting(OK).expecting(JSON),
                path("uuid").contains("%{newProfile}"),
                path("nickname").contains("%{nickname}"),
                path("totpSecrets").contains("%{totpSecrets}")
        );
    }
}
