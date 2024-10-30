package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.office.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public class InvalidateSepaMandateForDebitor extends UseCase<InvalidateSepaMandateForDebitor> {

    public InvalidateSepaMandateForDebitor(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {

        return httpPatch("/api/hs/office/sepamandates/" + uuid("SEPA-Mandate: Test AG"), usingJsonBody("""
                {
                   "validUntil": ${validUntil}
                }
                """))
                .expecting(OK).expecting(JSON);
    }
}
