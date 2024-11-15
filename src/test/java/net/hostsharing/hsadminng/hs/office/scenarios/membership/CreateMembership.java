package net.hostsharing.hsadminng.hs.office.scenarios.membership;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.office.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public class CreateMembership extends UseCase<CreateMembership> {

    public CreateMembership(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {

        obtain("Partner: %{partnerName}", () ->
                httpGet("/api/hs/office/partners?name=&{partnerName}")
                        .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In production, data this query could result in multiple outputs. In that case, you have to find out which is the right one."
        );

        return httpPost("/api/hs/office/memberships", usingJsonBody("""
                {
                   "partner.uuid": ${Partner: %{partnerName}},
                   "memberNumberSuffix": ${%{memberNumberSuffix???}???00},
                   "status": "ACTIVE",
                   "validFrom": ${validFrom},
                   "membershipFeeBillable": ${membershipFeeBillable}
                }
                """))
                .expecting(HttpStatus.CREATED).expecting(ContentType.JSON);
    }

    @Override
    protected void verify(final UseCase<CreateMembership>.HttpResponse response) {
        verify(
                "Verify That the Membership Got Created",
                () -> httpGet("/api/hs/office/memberships/" + response.getLocationUuid())
                        .expecting(OK).expecting(JSON),
                path("validFrom").contains("%{validFrom}"),
                path("status").contains("ACTIVE")
        );
    }
}
