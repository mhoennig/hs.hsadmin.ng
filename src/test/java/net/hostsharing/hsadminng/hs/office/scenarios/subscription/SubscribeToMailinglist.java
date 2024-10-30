package net.hostsharing.hsadminng.hs.office.scenarios.subscription;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.office.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class SubscribeToMailinglist extends UseCase<SubscribeToMailinglist> {

    public SubscribeToMailinglist(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {

        obtain("Person: %{partnerPersonTradeName}", () ->
                        httpGet("/api/hs/office/persons?name=" + uriEncoded("%{partnerPersonTradeName}"))
                                .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In production data this query could result in multiple outputs. In that case, you have to find out which is the right one."
        );

        obtain("Person: %{subscriberGivenName} %{subscriberFamilyName}", () ->
            httpPost("/api/hs/office/persons", usingJsonBody("""
                        {
                            "personType": "NATURAL_PERSON",
                            "familyName": ${subscriberFamilyName},
                            "givenName": ${subscriberGivenName}
                        }
                        """))
                    .expecting(HttpStatus.CREATED).expecting(ContentType.JSON)
        );

        obtain("Contact: %{subscriberGivenName} %{subscriberFamilyName}", () ->
            httpPost("/api/hs/office/contacts", usingJsonBody("""
                    {
                        "caption": "%{subscriberGivenName} %{subscriberFamilyName}",
                        "emailAddresses": {
                            "main": ${subscriberEMailAddress}
                        }
                    }
                    """))
                    .expecting(CREATED).expecting(JSON)
        );

        return httpPost("/api/hs/office/relations", usingJsonBody("""
                {
                   "type": "SUBSCRIBER",
                   "mark": ${mailingList},
                   "anchorUuid": ${Person: %{partnerPersonTradeName}},
                   "holderUuid": ${Person: %{subscriberGivenName} %{subscriberFamilyName}},
                   "contactUuid": ${Contact: %{subscriberGivenName} %{subscriberFamilyName}}
                }
                """))
                .expecting(CREATED).expecting(JSON);
    }
}
