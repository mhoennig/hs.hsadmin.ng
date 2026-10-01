package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import lombok.val;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class CreateSepaMandateForDebitor extends UseCase<CreateSepaMandateForDebitor> {

    public CreateSepaMandateForDebitor(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {

        obtain("Debitor: %{debitorNumber}", () ->
            httpGet(asGlobalAgent(), "/api/hs/office/debitors/%{debitorNumber}")
                    .expecting(OK).expecting(JSON),
            response -> response.getFromBody("uuid")
        );

        obtain("BankAccount: %{bankAccountHolder}", () ->
            httpPost(asGlobalAgent(), "/api/hs/office/bankaccounts", usingJsonBody("""
                     {
                         "holder": ${bankAccountHolder},
                         "iban": ${bankAccountIBAN},
                         "bic": ${bankAccountBIC},
                         "overrideBankDirectory": ${bankAccountOverrideBankDirectory???}
                    }
                    """))
                    .expecting(CREATED).expecting(JSON)
        );

        return httpPost(asGlobalAgent(), "/api/hs/office/sepamandates", usingJsonBody("""
                {
                   "debitor.uuid": ${Debitor: %{debitorNumber}},
                   "bankAccount.uuid": ${BankAccount: %{bankAccountHolder}},
                   "reference": ${mandateReference},
                   "agreement": ${mandateAgreement},
                   "validFrom": ${mandateValidFrom},
                   "validTo": ${mandateValidTo???}
                }
                """))
                .expecting(CREATED).expecting(JSON);
    }

    @Override
    protected void verify(final HttpResponse response) {
        // an empty expectation means the bank is not in the directory, thus the response carries no bankName
        val expectedBankName = ScenarioTest.resolve("%{expectedBankName???}", DROP_COMMENTS);
        if (expectedBankName == null || expectedBankName.isEmpty()) {
            response.path("bankAccount.bankName").isNull();
        } else {
            response.path("bankAccount.bankName").isEqualTo(expectedBankName);
        }
    }
}
