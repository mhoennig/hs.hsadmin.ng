package net.hostsharing.hsadminng.hs.accounts.scenarios;

import io.restassured.http.ContentType;
import lombok.val;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Fail.fail;
import static org.springframework.http.HttpStatus.OK;

public class UpdateProfile extends BaseProfileUseCase<UpdateProfile> {

    public UpdateProfile(final ScenarioTest testSuite, final FakeLoginUser asLoginUser) {
        super(testSuite, asLoginUser);

        introduction("A set of profile contains the login data for an RBAC subject.");
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {

        given("resolvedScopes",
                fetchScopeResourcesByDescriptorPairs("scopes")
        );

        return withTitle("Patch the Changes to the existing Profile", () -> {
                    val response = httpPatch(
                            asLoginUser, "/api/hs/accounts/profiles/%{profileUuid}", usingJsonBody("""
                                    {
                                         "active": %{active},
                                         "totpSecrets": @{totpSecrets},
                                         "emailAddress": ${emailAddress},
                                         "phonePassword": ${phonePassword},
                                         "smsNumber": ${smsNumber},
                                         "scopes": @{resolvedScopes}
                                    }
                                    """))
                            .reportWithResponse().expecting(expectedStatus);

                    return switch (expectedStatus) {
                        case OK -> response.expecting(ContentType.JSON)
                                .extractValue("nickname", "nickname")
                                .extractValue("totpSecrets", "totpSecrets");
                        case FORBIDDEN -> response.expecting(ContentType.JSON);
                        default -> fail("unexpected response: " + response);
                    };
                }
        );
    }

    @Override
    protected void verify(final UseCase<UpdateProfile>.HttpResponse response) {
        verify(
                "Verify the Patched Profile",
                () -> httpGet(asLoginUser, "/api/hs/accounts/profiles/%{profileUuid}")
                        .expecting(OK).expecting(JSON),
                path("uuid").contains("%{newProfile}"),
                path("nickname").contains("%{nickname}"),
                path("totpSecrets").contains("%{totpSecrets}")
        );
    }
}
