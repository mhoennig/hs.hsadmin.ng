package net.hostsharing.hsadminng.hs.office.scenarios.contact;

import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.office.scenarios.UseCase;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class ReplaceContactData extends UseCase<ReplaceContactData> {

    public ReplaceContactData(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {

        obtain("partnerRelationUuid", () ->
                httpGet("/api/hs/office/relations?relationType=PARTNER&personData=" + uriEncoded("%{partnerName}"))
                    .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In production, data this query could result in multiple outputs. In that case, you have to find out which is the right one."
        );

        obtain("Contact: %{newContactCaption}", () ->
            httpPost("/api/hs/office/contacts", usingJsonBody("""
                {
                    "caption": ${newContactCaption},
                    "postalAddress": {
                        %{newPostalAddress???}
                    },
                    "phoneNumbers": {
                        "phone": ${newOfficePhoneNumber???}
                    },
                    "emailAddresses": {
                        "main": ${newEmailAddress???}
                    }
                }
                """))
                .expecting(CREATED).expecting(JSON),
                "Please check first if that contact already exists, if so, use it's UUID below.",
                "If any `postalAddress` sub-properties besides those specified in the API " +
                        "(currently `firm`, `name`, `co`, `street`, `zipcode`, `city`, `country`) " +
                        "its values might not appear in external systems.");

        withTitle("Replace the Contact-Reference in the Partner-Relation", () ->
                httpPatch("/api/hs/office/relations/%{partnerRelationUuid}", usingJsonBody("""
                        {
                            "contactUuid": ${Contact: %{newContactCaption}}
                        }
                        """))
                        .expecting(OK)
        );

        return null;
    }

    @Override
    protected void verify() {
        verify(
                "Verify if the Contact-Relation Got Replaced in the Partner-Relation",
                () -> httpGet("/api/hs/office/relations?relationType=PARTNER&personData=" + uriEncoded("%{partnerName}"))
                        .expecting(OK).expecting(JSON).expectArrayElements(1),
                path("[0].contact.caption").contains("%{newContactCaption}")
        );
    }
}
