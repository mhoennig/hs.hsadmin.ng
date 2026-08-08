package net.hostsharing.hsadminng.hs.office.scenarios.demodata;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

/**
 * Creates a SEPA-mandate with its own bank account for a debitor.
 * CreateSepaMandateForDebitor cannot be reused, because it keeps its debitor and bank account
 * under aliases hard-wired to the Test AG scenario.
 */
public class CreateDemoSepaMandate extends UseCase<CreateDemoSepaMandate> {

    public CreateDemoSepaMandate(final ScenarioTest testSuite, final String resultAlias) {
        super(testSuite, resultAlias);
    }

    @Override
    protected HttpResponse run() {
        obtain("Debitor of %{mandateReference}", () ->
                httpGet(asGlobalAgent(), "/api/hs/office/debitors/%{debitorNumber}")
                        .expecting(OK).expecting(JSON),
                response -> response.getFromBody("uuid")
        );

        obtain("BankAccount: %{bankAccountHolder}", () ->
                httpPost(asGlobalAgent(), "/api/hs/office/bankaccounts", usingJsonBody("""
                        {
                            "holder": ${bankAccountHolder},
                            "iban": ${iban},
                            "bic": ${bic}
                        }
                        """))
                        .expecting(CREATED).expecting(JSON)
        );

        return httpPost(asGlobalAgent(), "/api/hs/office/sepamandates", usingJsonBody("""
                {
                   "debitor.uuid": ${Debitor of %{mandateReference}},
                   "bankAccount.uuid": ${BankAccount: %{bankAccountHolder}},
                   "reference": ${mandateReference},
                   "agreement": ${agreement},
                   "validFrom": ${validFrom},
                   "validTo": ${validTo???}
                }
                """))
                .expecting(CREATED).expecting(JSON);
    }

    @Override
    protected void verify(final UseCase<CreateDemoSepaMandate>.HttpResponse response) {
        verify(
                "Verify the SEPA-Mandate Got Created",
                () -> httpGet(asGlobalAgent(), "/api/hs/office/sepamandates/" + response.getLocationUuid())
                        .expecting(OK).expecting(JSON),
                path("reference").contains("%{mandateReference}")
        );
    }
}
