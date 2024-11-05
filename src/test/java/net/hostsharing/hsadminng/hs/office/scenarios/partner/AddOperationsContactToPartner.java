package net.hostsharing.hsadminng.hs.office.scenarios.partner;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.office.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class AddOperationsContactToPartner extends UseCase<AddOperationsContactToPartner> {

    public AddOperationsContactToPartner(final ScenarioTest testSuite) {
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

        obtain("Person: %{operationsContactGivenName} %{operationsContactFamilyName}", () ->
                httpPost("/api/hs/office/persons", usingJsonBody("""
                        {
                            "personType": "NATURAL_PERSON",
                            "familyName": ${operationsContactFamilyName},
                            "givenName": ${operationsContactGivenName}
                        }
                        """))
                        .expecting(HttpStatus.CREATED).expecting(ContentType.JSON),
                "Please check first if that person already exists, if so, use it's UUID below.",
                "**HINT**: operations contacts are always connected to a partner-person, thus a person which is a holder of a partner-relation."
        );

        obtain("Contact: %{operationsContactGivenName} %{operationsContactFamilyName}", () ->
                httpPost("/api/hs/office/contacts", usingJsonBody("""
                        {
                            "caption": "%{operationsContactGivenName} %{operationsContactFamilyName}",
                            "phoneNumbers": {
                                "main": ${operationsContactPhoneNumber}
                            },
                            "emailAddresses": {
                                "main": ${operationsContactEMailAddress}
                            }
                        }
                        """))
                        .expecting(CREATED).expecting(JSON),
                "Please check first if that contact already exists, if so, use it's UUID below."
        );

        return httpPost("/api/hs/office/relations", usingJsonBody("""
                {
                   "type": "OPERATIONS",
                   "anchorUuid": ${Person: %{partnerPersonTradeName}},
                   "holderUuid": ${Person: %{operationsContactGivenName} %{operationsContactFamilyName}},
                   "contactUuid": ${Contact: %{operationsContactGivenName} %{operationsContactFamilyName}}
                }
                """))
                .expecting(CREATED).expecting(JSON);
    }

    @Override
    protected void verify() {
        verify(
                "Verify the New OPERATIONS Relation",
                () -> httpGet("/api/hs/office/relations?relationType=OPERATIONS&personData=" + uriEncoded(
                        "%{operationsContactFamilyName}"))
                        .expecting(OK).expecting(JSON).expectArrayElements(1),
                path("[0].contact.caption").contains("%{operationsContactGivenName} %{operationsContactFamilyName}")
        );
    }
}
