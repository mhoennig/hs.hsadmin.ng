package net.hostsharing.hsadminng.hs.office.scenarios.demodata;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

/**
 * Subscribes an existing person to a mailing list, anchored at the partner person as the account
 * tooling expects it. What ends up in the list is the main email address of the given contact,
 * see the view hs_integration.subscription.
 */
public class AddDemoSubscription extends UseCase<AddDemoSubscription> {

    public AddDemoSubscription(final ScenarioTest testSuite, final String resultAlias) {
        super(testSuite, resultAlias);
    }

    @Override
    protected HttpResponse run() {
        return httpPost(asGlobalAgent(), "/api/hs/office/relations", usingJsonBody("""
                {
                   "type": "SUBSCRIBER",
                   "mark": ${mailingList},
                   "anchor.uuid": ${Person: %{partnerPersonName}},
                   "holder.uuid": ${Person: %{holderGivenName} %{holderFamilyName}},
                   "contact.uuid": ${Contact: %{contactCaption}}
                }
                """))
                .expecting(CREATED).expecting(JSON);
    }

    @Override
    protected void verify(final UseCase<AddDemoSubscription>.HttpResponse response) {
        verify(
                "Verify the Subscription Got Created",
                () -> httpGet(asGlobalAgent(), "/api/hs/office/relations/" + response.getLocationUuid())
                        .expecting(OK).expecting(JSON),
                path("mark").contains("%{mailingList}")
        );
    }
}
