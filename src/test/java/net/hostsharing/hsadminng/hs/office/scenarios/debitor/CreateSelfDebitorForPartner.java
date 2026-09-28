package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class CreateSelfDebitorForPartner extends UseCase<CreateSelfDebitorForPartner> {

    public CreateSelfDebitorForPartner(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {
        obtain("partnerPersonUuid", () ->
                httpGet(asGlobalAgent(), "/api/hs/office/relations?relationType=PARTNER&personData=" + uriEncoded("%{partnerPersonTradeName}"))
                        .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].holder.uuid"),
                "In production, data this query could result in multiple outputs. In that case, you have to find out which is the right one."
        );

        obtain("Contact: Test AG - billing department", () ->
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
                        "anchor.uuid": ${partnerPersonUuid},
                        "holder.uuid": ${partnerPersonUuid},
                        "contact.uuid": ${Contact: Test AG - billing department}
                     },
                    "debitorNumberSuffix": ${debitorNumberSuffix},
                    "billable": ${billable},
                    "vatId": ${vatId},
                    "vatCountryCode": ${vatCountryCode},
                    "vatBusiness": ${vatBusiness},
                    "vatReverseCharge": ${vatReverseCharge},
                    "defaultPrefix": ${defaultPrefix}
                }
                """))
                .reportWithResponse().expecting(CREATED).expecting(JSON);
    }

    @Override
    protected void verify(final HttpResponse response) {
        path("refundBankAccount").doesNotExist().accept(response);
    }
}
