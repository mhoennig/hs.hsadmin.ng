package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class CreateSepaMandateForDebitor extends UseCase<CreateSepaMandateForDebitor> {

    public CreateSepaMandateForDebitor(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {

        obtain("Debitor: Test AG - main debitor", () ->
            httpGet("/api/hs/office/debitors/%{debitorNumber}")
                    .expecting(OK).expecting(JSON),
            response -> response.getFromBody("uuid")
        );

        obtain("BankAccount: Test AG - debit bank account", () ->
            httpPost("/api/hs/office/bankaccounts", usingJsonBody("""
                     {
                         "holder": ${bankAccountHolder},
                         "iban": ${bankAccountIBAN},
                         "bic": ${bankAccountBIC}
                    }
                    """))
                    .expecting(CREATED).expecting(JSON)
        );

        return httpPost("/api/hs/office/sepamandates", usingJsonBody("""
                {
                   "debitor.uuid": ${Debitor: Test AG - main debitor},
                   "bankAccount.uuid": ${BankAccount: Test AG - debit bank account},
                   "reference": ${mandateReference},
                   "agreement": ${mandateAgreement},
                   "validFrom": ${mandateValidFrom}
                }
                """))
                .expecting(CREATED).expecting(JSON);
    }
}
