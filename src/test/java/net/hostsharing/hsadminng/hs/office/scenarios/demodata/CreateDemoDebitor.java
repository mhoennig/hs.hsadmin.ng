package net.hostsharing.hsadminng.hs.office.scenarios.demodata;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

/**
 * Creates a self-debitor with its own billing contact and bank account.
 * CreateSelfDebitorForPartner cannot be reused, because its bank account and billing contact
 * are hard-wired to the Test AG scenario, down to a fixed IBAN.
 */
public class CreateDemoDebitor extends UseCase<CreateDemoDebitor> {

    public CreateDemoDebitor(final ScenarioTest testSuite, final String resultAlias) {
        super(testSuite, resultAlias);
    }

    @Override
    protected HttpResponse run() {
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

        obtain("Contact: %{billingContactCaption}", () ->
                httpPost(asGlobalAgent(), "/api/hs/office/contacts", usingJsonBody("""
                        {
                            "caption": ${billingContactCaption},
                            "emailAddresses": {
                                "main": ${billingContactEmailAddress}
                            }
                        }
                        """))
                        .expecting(CREATED).expecting(JSON)
        );

        return httpPost(asGlobalAgent(), "/api/hs/office/debitors", usingJsonBody("""
                {
                    "debitorRel": {
                        "anchor.uuid": ${Person: %{partnerPersonName}},
                        "holder.uuid": ${Person: %{partnerPersonName}},
                        "contact.uuid": ${Contact: %{billingContactCaption}}
                     },
                    "debitorNumberSuffix": ${debitorNumberSuffix},
                    "billable": ${billable},
                    "vatBusiness": ${vatBusiness},
                    "vatReverseCharge": false,
                    "refundBankAccount.uuid": ${BankAccount: %{bankAccountHolder}},
                    "defaultPrefix": ${defaultPrefix}
                }
                """))
                .expecting(CREATED).expecting(JSON);
    }

    @Override
    protected void verify(final UseCase<CreateDemoDebitor>.HttpResponse response) {
        verify(
                "Verify the Debitor Got Created",
                () -> httpGet(asGlobalAgent(), "/api/hs/office/debitors/" + response.getLocationUuid())
                        .expecting(OK).expecting(JSON),
                path("debitorNumberSuffix").contains("%{debitorNumberSuffix}")
        );
    }
}
