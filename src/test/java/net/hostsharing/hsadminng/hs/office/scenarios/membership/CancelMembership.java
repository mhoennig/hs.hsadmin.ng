package net.hostsharing.hsadminng.hs.office.scenarios.membership;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public class CancelMembership extends UseCase<CancelMembership> {

    public CancelMembership(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {

        obtain("Membership: %{memberNumber}", () ->
                httpGet("/api/hs/office/memberships?memberNumber=%{memberNumber}")
                        .expectArrayElements(1),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid")
        );

        return withTitle("Patch the New Status Into the Membership", () ->
                httpPatch("/api/hs/office/memberships/%{Membership: %{memberNumber}}", usingJsonBody("""
                {
                   "validTo": ${validTo},
                   "status": ${newStatus}
                }
                """))
                .expecting(HttpStatus.OK).expecting(ContentType.JSON)
        );
    }

    @Override
    protected void verify(final UseCase<CancelMembership>.HttpResponse response) {
        verify(
                "Verify That the Membership Got Cancelled",
                () -> httpGet("/api/hs/office/memberships/%{Membership: %{memberNumber}}")
                        .expecting(OK).expecting(JSON),
                path("validTo").contains("%{validTo}"),
                path("status").contains("CANCELLED")
        );
    }
}
