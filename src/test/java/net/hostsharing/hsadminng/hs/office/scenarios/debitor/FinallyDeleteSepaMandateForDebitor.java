package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public class FinallyDeleteSepaMandateForDebitor extends UseCase<FinallyDeleteSepaMandateForDebitor> {

    public FinallyDeleteSepaMandateForDebitor(final ScenarioTest testSuite) {
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

        // TODO.spec: When to allow actual deletion of SEPA-mandates? Add constraint accordingly.
        return withTitle("Delete the SEPA-Mandate by its UUID", () -> httpDelete("/api/hs/office/sepamandates/&{SEPA-Mandate: %{bankAccountIBAN}}")
                .expecting(HttpStatus.NO_CONTENT)
        );
    }
}
