package net.hostsharing.hsadminng.hs.office.scenarios.subscription;

import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

public class UnsubscribeFromMailinglist extends UseCase<UnsubscribeFromMailinglist> {

    public UnsubscribeFromMailinglist(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {

        obtain("Subscription: %{subscriberEMailAddress}", () ->
                httpGet("/api/hs/office/relations?relationType=SUBSCRIBER" +
                            "&mark=" + uriEncoded("%{mailingList}") +
                            "&contactData=" + uriEncoded("%{subscriberEMailAddress}"))
                        .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In production, data this query could result in multiple outputs. In that case, you have to find out which is the right one."
        );

        return withTitle("Delete the Subscriber-Relation by its UUID", () ->
                httpDelete("/api/hs/office/relations/&{Subscription: %{subscriberEMailAddress}}")
                .expecting(NO_CONTENT)
        );
    }
}
