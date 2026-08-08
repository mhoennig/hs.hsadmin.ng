package net.hostsharing.hsadminng.hs.office.scenarios.subscription;

import io.restassured.http.ContentType;
import lombok.val;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static org.springframework.http.HttpStatus.OK;

public class SubscribeExistingPersonAndContactToMailinglist extends UseCase<SubscribeExistingPersonAndContactToMailinglist> {

    private final FakeLoginUser loginUser;

    public SubscribeExistingPersonAndContactToMailinglist(final ScenarioTest testSuite) {
        this(testSuite, asGlobalAgent());
    }

    public SubscribeExistingPersonAndContactToMailinglist(final ScenarioTest testSuite, final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {

        obtain("Person: %{partnerPersonTradeName}", () ->
                httpGet(loginUser, "/api/hs/office/persons?name=" + uriEncoded("%{partnerPersonTradeName}"))
                        .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In production, data this query could result in multiple outputs. In that case, you have to find out which is the right one."
        );

        obtain(
                "Person: %{subscriberGivenName} %{subscriberFamilyName}", () ->
                httpGet(loginUser, "/api/hs/office/persons?name=%{subscriberFamilyName}")
                        .expecting(HttpStatus.OK).expecting(ContentType.JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In real scenarios there are most likely multiple results and you have to choose the right one."
        );

        obtain("Contact: %{subscriberEMailAddress}", () ->
                httpGet(loginUser, "/api/hs/office/contacts?emailAddress=%{subscriberEMailAddress}")
                        .expecting(HttpStatus.OK).expecting(ContentType.JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In real scenarios there are most likely multiple results and you have to choose the right one."
        );

        val response = withTitle("Subscribe %{subscriberGivenName} %{subscriberFamilyName} to %{mailingList}", () ->
                httpPost(loginUser, "/api/hs/office/relations", usingJsonBody("""
                        {
                           "type": "SUBSCRIBER",
                           "mark": ${mailingList},
                           "anchor.uuid": ${Person: %{partnerPersonTradeName}},
                           "holder.uuid": ${Person: %{subscriberGivenName} %{subscriberFamilyName}},
                           "contact.uuid": ${Contact: %{subscriberEMailAddress}}
                        }
                        """))
                        .expecting(expectedStatus));
        return expectedStatus.isError() ? response : response.expecting(JSON);
    }
}
