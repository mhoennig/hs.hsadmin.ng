package net.hostsharing.hsadminng.hs.office.scenarios.membership;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.office.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public class CreateMembership extends UseCase<CreateMembership> {

    public CreateMembership(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {
        return httpPost("/api/hs/office/memberships", usingJsonBody("""
                {
                   "partnerUuid": ${Partner: Test AG},
                   "memberNumberSuffix": ${memberNumberSuffix},
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
