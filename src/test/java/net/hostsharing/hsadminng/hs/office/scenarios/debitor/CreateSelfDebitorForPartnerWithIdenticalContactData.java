package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.CREATED;

public class CreateSelfDebitorForPartnerWithIdenticalContactData
        extends UseCase<CreateSelfDebitorForPartnerWithIdenticalContactData> {

    public CreateSelfDebitorForPartnerWithIdenticalContactData(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {
        withTitle("Determine Partner-Person UUID", () ->
                httpGet("/api/hs/office/partners/" + uriEncoded("%{partnerNumber}"))
                        .reportWithResponse().expecting(HttpStatus.OK).expecting(JSON)
                        .extractUuidAlias("partnerRel.holder.uuid", "partnerPersonUuid")
                        .extractUuidAlias("partnerRel.contact.uuid", "partnerContactUuid")
        );

        return httpPost("/api/hs/office/debitors", usingJsonBody("""
                {
                    "debitorRel": {
                        "anchor.uuid": ${partnerPersonUuid},
                        "holder.uuid": ${partnerPersonUuid},
                        "contact.uuid": ${partnerContactUuid}
                     },
                    "debitorNumberSuffix": ${debitorNumberSuffix},
                    "billable": ${billable},
                    "vatId": ${vatId???},
                    "vatCountryCode": ${vatCountryCode???},
                    "vatBusiness": ${vatBusiness},
                    "vatReverseCharge": ${vatReverseCharge},
                    "defaultPrefix": ${defaultPrefix}
                }
                """))
                .expecting(CREATED).expecting(JSON);
    }

}
