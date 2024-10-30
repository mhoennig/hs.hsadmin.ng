package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.office.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.office.scenarios.person.CreatePerson;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class CreateExternalDebitorForPartner extends UseCase<CreateExternalDebitorForPartner> {

    public CreateExternalDebitorForPartner(final ScenarioTest testSuite) {
        super(testSuite);

        requires("Person: Billing GmbH", alias -> new CreatePerson(testSuite, alias)
                .given("personType", "LEGAL_PERSON")
                .given("tradeName", "Billing GmbH")
        );
    }

    @Override
    protected HttpResponse run() {

        obtain("Person: %{partnerPersonTradeName}", () ->
                        httpGet("/api/hs/office/persons?name=" + uriEncoded("%{partnerPersonTradeName}"))
                                .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In production data this query could result in multiple outputs. In that case, you have to find out which is the right one."
        );

        obtain("BankAccount: Billing GmbH - refund bank account", () ->
            httpPost("/api/hs/office/bankaccounts", usingJsonBody("""
                     {
                         "holder": "Billing GmbH - refund bank account",
                         "iban": "DE02120300000000202051",
                         "bic": "BYLADEM1001"
                    }
                    """))
                    .expecting(CREATED).expecting(JSON)
        );

        obtain("Contact: Billing GmbH - Test AG billing", () ->
            httpPost("/api/hs/office/contacts", usingJsonBody("""
                    {
                        "caption": "Billing GmbH, billing for Test AG",
                        "emailAddresses": {
                            "main": "test-ag@billing-GmbH.example.com"
                        }
                    }
                    """))
                    .expecting(CREATED).expecting(JSON)
        );

        return httpPost("/api/hs/office/debitors", usingJsonBody("""
                {
                    "debitorRel": {
                        "anchorUuid": ${Person: %{partnerPersonTradeName}},
                        "holderUuid": ${Person: Billing GmbH},
                        "contactUuid": ${Contact: Billing GmbH - Test AG billing}
                     },
                    "debitorNumberSuffix": ${debitorNumberSuffix},
                    "billable": ${billable},
                    "vatId": ${vatId},
                    "vatCountryCode": ${vatCountryCode},
                    "vatBusiness": ${vatBusiness},
                    "vatReverseCharge": ${vatReverseCharge},
                    "refundBankAccountUuid": ${BankAccount: Billing GmbH - refund bank account},
                    "defaultPrefix": ${defaultPrefix}
                }
                """))
                .expecting(CREATED).expecting(JSON);
    }
}
