package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.office.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.CREATED;

public class CreateSepaMandateForDebitor extends UseCase<CreateSepaMandateForDebitor> {

    public CreateSepaMandateForDebitor(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {
        obtain("BankAccount: Test AG - debit bank account", () ->
            httpPost("/api/hs/office/bankaccounts", usingJsonBody("""
                     {
                         "holder": "Test AG - debit bank account",
                         "iban": "DE02701500000000594937",
                         "bic": "SSKMDEMM"
                    }
                    """))
                    .expecting(CREATED).expecting(JSON)
        );

        return httpPost("/api/hs/office/sepamandates", usingJsonBody("""
                {
                   "debitorUuid": ${Debitor: Test AG - main debitor},
                   "bankAccountUuid": ${BankAccount: Test AG - debit bank account},
                   "reference": "Test AG - main debitor",
                   "agreement": "2022-10-12",
                   "validFrom": "2022-10-13"
                }
                """))
                .expecting(CREATED).expecting(JSON);
    }
}
