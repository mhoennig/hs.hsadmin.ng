package net.hostsharing.hsadminng.hs.office.scenarios.membership;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.office.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;
import org.springframework.http.HttpStatus;

public class CreateMembership extends UseCase<CreateMembership> {

    public CreateMembership(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {
        obtain("Membership: %{partnerName} 00", () ->
            httpPost("/api/hs/office/memberships", usingJsonBody("""
                    {
                       "partnerUuid": ${Partner: Test AG},
                       "memberNumberSuffix": ${memberNumberSuffix},
                       "validFrom": ${validFrom},
                       "membershipFeeBillable": ${membershipFeeBillable}
                    }
                    """))
                    .expecting(HttpStatus.CREATED).expecting(ContentType.JSON)
        );
        return null;
    }
}
