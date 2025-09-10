package net.hostsharing.hsadminng.hs.accounts.scenarios;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public class UpdateCredentials extends BaseCredentialsUseCase<UpdateCredentials> {

    public UpdateCredentials(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("A set of credentials contains the login data for an RBAC subject.");
    }

    @Override
    protected HttpResponse run() {

        given("resolvedContexts",
                fetchContextResourcesByDescriptorPairs("contexts")
        );

        withTitle("Patch the Changes to the existing Credentials", () ->
            httpPatch("/api/hs/accounts/credentials/%{credentialsUuid}", usingJsonBody("""
                {
                     "active": %{active},
                     "totpSecrets": @{totpSecrets},
                     "emailAddress": ${emailAddress},
                     "phonePassword": ${phonePassword},
                     "smsNumber": ${smsNumber},
                     "contexts": @{resolvedContexts}
                }
                """))
                .reportWithResponse().expecting(HttpStatus.OK).expecting(ContentType.JSON)
                .extractValue("nickname", "nickname")
                .extractValue("totpSecrets", "totpSecrets")
        );

        return null;
    }

    @Override
    protected void verify(final UseCase<UpdateCredentials>.HttpResponse response) {
        verify(
                "Verify the Patched Credentials",
                () -> httpGet("/api/hs/accounts/credentials/%{credentialsUuid}")
                        .expecting(OK).expecting(JSON),
                path("uuid").contains("%{newCredentials}"),
                path("nickname").contains("%{nickname}"),
                path("totpSecrets").contains("%{totpSecrets}")
        );
    }
}
