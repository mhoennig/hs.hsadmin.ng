package net.hostsharing.hsadminng.hs.office.scenarios.demodata;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

/**
 * Adds a natural person with its own contact as holder of a relation to the partner person; a person
 * of that name which an earlier relation already created gets reused, so that one human stays one person.
 * Unlike AddRepresentativeToPartner this resolves the partner through the alias kept by CreatePartner
 * and verifies by uuid, because looking up and verifying by name cannot stay unambiguous in mass data.
 */
public class AddDemoRelation extends UseCase<AddDemoRelation> {

    public AddDemoRelation(final ScenarioTest testSuite, final String resultAlias) {
        super(testSuite, resultAlias);
    }

    @Override
    protected HttpResponse run() {
        obtainOnce("Person: %{holderGivenName} %{holderFamilyName}", () ->
                httpPost(asGlobalAgent(), "/api/hs/office/persons", usingJsonBody("""
                        {
                            "personType": "NATURAL_PERSON",
                            "familyName": ${holderFamilyName},
                            "givenName": ${holderGivenName}
                        }
                        """))
                        .expecting(CREATED).expecting(JSON)
        );

        obtain("Contact: %{contactCaption}", () ->
                httpPost(asGlobalAgent(), "/api/hs/office/contacts", usingJsonBody("""
                        {
                            "caption": ${contactCaption},
                            "postalAddress": {
                                %{postalAddress}
                            },
                            "phoneNumbers": {
                                "office": ${phoneNumber}
                            },
                            "emailAddresses": {
                                "main": ${emailAddress}
                            }
                        }
                        """))
                        .expecting(CREATED).expecting(JSON)
        );

        return httpPost(asGlobalAgent(), "/api/hs/office/relations", usingJsonBody("""
                {
                   "type": ${relationType},
                   "anchor.uuid": ${Person: %{partnerPersonName}},
                   "holder.uuid": ${Person: %{holderGivenName} %{holderFamilyName}},
                   "contact.uuid": ${Contact: %{contactCaption}}
                }
                """))
                .expecting(CREATED).expecting(JSON);
    }

    @Override
    protected void verify(final UseCase<AddDemoRelation>.HttpResponse response) {
        verify(
                "Verify the Relation Got Created",
                () -> httpGet(asGlobalAgent(), "/api/hs/office/relations/" + response.getLocationUuid())
                        .expecting(OK).expecting(JSON),
                path("type").contains("%{relationType}")
        );
    }
}
