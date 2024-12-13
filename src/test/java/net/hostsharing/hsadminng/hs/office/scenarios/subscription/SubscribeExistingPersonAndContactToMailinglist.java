package net.hostsharing.hsadminng.hs.office.scenarios.subscription;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class SubscribeExistingPersonAndContactToMailinglist extends UseCase<SubscribeExistingPersonAndContactToMailinglist> {

    public SubscribeExistingPersonAndContactToMailinglist(final ScenarioTest testSuite) {
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

        obtain(
                "Person: %{subscriberGivenName} %{subscriberFamilyName}", () ->
                httpGet("/api/hs/office/persons?name=%{subscriberFamilyName}")
                        .expecting(HttpStatus.OK).expecting(ContentType.JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In real scenarios there are most likely multiple results and you have to choose the right one."
        );

        obtain("Contact: %{subscriberEMailAddress}", () ->
                httpGet("/api/hs/office/contacts?emailAddress=%{subscriberEMailAddress}")
                        .expecting(HttpStatus.OK).expecting(ContentType.JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In real scenarios there are most likely multiple results and you have to choose the right one."
        );

        return httpPost("/api/hs/office/relations", usingJsonBody("""
                {
                   "type": "SUBSCRIBER",
                   "mark": ${mailingList},
                   "anchor.uuid": ${Person: %{partnerPersonTradeName}},
                   "holder.uuid": ${Person: %{subscriberGivenName} %{subscriberFamilyName}},
                   "contact.uuid": ${Contact: %{subscriberEMailAddress}}
                }
                """))
                .expecting(CREATED).expecting(JSON);
    }
}
