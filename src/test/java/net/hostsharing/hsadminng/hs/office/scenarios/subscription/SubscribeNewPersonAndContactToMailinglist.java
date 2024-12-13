package net.hostsharing.hsadminng.hs.office.scenarios.subscription;

import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class SubscribeNewPersonAndContactToMailinglist extends UseCase<SubscribeNewPersonAndContactToMailinglist> {

    public SubscribeNewPersonAndContactToMailinglist(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {

        obtain("Person: %{partnerPersonTradeName}", () ->
                httpGet("/api/hs/office/persons?name=" + uriEncoded("%{partnerPersonTradeName}"))
                        .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In production, data this query could result in multiple outputs. In that case, you have to find out which is the right one."
        );

        return httpPost("/api/hs/office/relations", usingJsonBody("""
                {
                   "type": "SUBSCRIBER",
                   "mark": ${mailingList},
                   "anchor.uuid": ${Person: %{partnerPersonTradeName}},
                   "holder": {
                        "personType": "NATURAL_PERSON",
                        "familyName": ${subscriberFamilyName},
                        "givenName": ${subscriberGivenName}
                    },
                   "contact": {
                        "caption": "%{subscriberGivenName} %{subscriberFamilyName}",
                        "emailAddresses": {
                            "main": ${subscriberEMailAddress}
                        }
                    }
                }
                """))
                .expecting(CREATED).expecting(JSON);
    }
}
