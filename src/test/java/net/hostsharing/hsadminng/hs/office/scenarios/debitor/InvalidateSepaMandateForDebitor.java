package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public class InvalidateSepaMandateForDebitor extends UseCase<InvalidateSepaMandateForDebitor> {

    public InvalidateSepaMandateForDebitor(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {

        obtain("SEPA-Mandate: %{bankAccountIBAN}", () ->
                httpGet("/api/hs/office/sepamandates?iban=&{bankAccountIBAN}")
                        .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "With production data, the bank-account could be used in multiple SEPA-mandates, make sure to use the right one!"
        );

        return withTitle("Patch the End of the Mandate into the SEPA-Mandate", () ->
                httpPatch("/api/hs/office/sepamandates/&{SEPA-Mandate: %{bankAccountIBAN}}", usingJsonBody("""
                {
                   "validUntil": ${mandateValidUntil}
                }
                """))
                .expecting(OK).expecting(JSON)
        );
    }
}
