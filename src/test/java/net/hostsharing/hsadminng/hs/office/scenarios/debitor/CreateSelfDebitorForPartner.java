package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.office.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class CreateSelfDebitorForPartner extends UseCase<CreateSelfDebitorForPartner> {

    public CreateSelfDebitorForPartner(final ScenarioTest testSuite, final String resultAlias) {
        super(testSuite, resultAlias);
    }

    @Override
    protected HttpResponse run() {
        obtain("partnerPersonUuid", () ->
                httpGet("/api/hs/office/relations?relationType=PARTNER&personData=" + uriEncoded("%{partnerPersonTradeName}"))
                        .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].holder.uuid"),
                "In production, data this query could result in multiple outputs. In that case, you have to find out which is the right one."
        );

        obtain("BankAccount: Test AG - refund bank account", () ->
            httpPost("/api/hs/office/bankaccounts", usingJsonBody("""
                     {
                         "holder": "Test AG - refund bank account",
                         "iban": "DE88100900001234567892",
                         "bic": "BEVODEBB"
                    }
                    """))
                    .expecting(CREATED).expecting(JSON)
        );

        obtain("Contact: Test AG - billing department", () ->
            httpPost("/api/hs/office/contacts", usingJsonBody("""
                    {
                        "caption": ${billingContactCaption},
                        "emailAddresses": {
                            "main": ${billingContactEmailAddress}
                        }
                    }
                    """))
                    .expecting(CREATED).expecting(JSON)
        );

        return httpPost("/api/hs/office/debitors", usingJsonBody("""
                {
                    "debitorRel": {
                        "anchorUuid": ${partnerPersonUuid},
                        "holderUuid": ${partnerPersonUuid},
                        "contactUuid": ${Contact: Test AG - billing department}
                     },
                    "debitorNumberSuffix": ${debitorNumberSuffix},
                    "billable": ${billable},
                    "vatId": ${vatId},
                    "vatCountryCode": ${vatCountryCode},
                    "vatBusiness": ${vatBusiness},
                    "vatReverseCharge": ${vatReverseCharge},
                    "refundBankAccountUuid": ${BankAccount: Test AG - refund bank account},
                    "defaultPrefix": ${defaultPrefix}
                }
                """))
                .expecting(CREATED).expecting(JSON);
    }

}
