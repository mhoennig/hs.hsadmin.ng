package net.hostsharing.hsadminng.hs.office.scenarios.partner;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.office.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class AddRepresentativeToPartner extends UseCase<AddRepresentativeToPartner> {

    public AddRepresentativeToPartner(final ScenarioTest testSuite) {
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

        obtain("Person: %{representativeGivenName} %{representativeFamilyName}", () ->
            httpPost("/api/hs/office/persons", usingJsonBody("""
                    {
                        "personType": "NATURAL_PERSON",
                        "familyName": ${representativeFamilyName},
                        "givenName": ${representativeGivenName}
                    }
                    """))
                    .expecting(HttpStatus.CREATED).expecting(ContentType.JSON),
            "Please check first if that person already exists, if so, use it's UUID below.",
            "**HINT**: A representative is always a natural person and represents a non-natural-person."
        );

        obtain("Contact: %{representativeGivenName} %{representativeFamilyName}", () ->
            httpPost("/api/hs/office/contacts", usingJsonBody("""
                    {
                        "caption": "%{representativeGivenName} %{representativeFamilyName}",
                        "postalAddress": {
                            %{representativePostalAddress}
                        },
                        "phoneNumbers": {
                            "main": ${representativePhoneNumber}
                        },
                        "emailAddresses": {
                            "main": ${representativeEMailAddress}
                        }
                    }
                    """))
                    .expecting(CREATED).expecting(JSON),
                "Please check first if that contact already exists, if so, use it's UUID below."
        );

        return httpPost("/api/hs/office/relations", usingJsonBody("""
                {
                   "type": "REPRESENTATIVE",
                   "anchorUuid": ${Person: %{partnerPersonTradeName}},
                   "holderUuid": ${Person: %{representativeGivenName} %{representativeFamilyName}},
                   "contactUuid": ${Contact: %{representativeGivenName} %{representativeFamilyName}}
                }
                """))
                .expecting(CREATED).expecting(JSON);
    }

    @Override
    protected void verify(final UseCase<AddRepresentativeToPartner>.HttpResponse response) {
        verify(
                "Verify the REPRESENTATIVE Relation Got Removed",
                () -> httpGet("/api/hs/office/relations?relationType=REPRESENTATIVE&personData=" + uriEncoded("%{representativeFamilyName}"))
                        .expecting(OK).expecting(JSON).expectArrayElements(1),
                path("[0].contact.caption").contains("%{representativeGivenName} %{representativeFamilyName}")
        );
    }
}
