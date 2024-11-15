package net.hostsharing.hsadminng.hs.office.scenarios.partner;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.office.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public class CreatePartner extends UseCase<CreatePartner> {

    public CreatePartner(final ScenarioTest testSuite, final String resultAlias) {
        super(testSuite, resultAlias);
    }

    public CreatePartner(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("A partner can be a client or a vendor, currently we only use them for clients.");
    }

    @Override
    protected HttpResponse run() {

        obtain("Person: Hostsharing eG", () ->
            httpGet("/api/hs/office/persons?name=Hostsharing+eG")
                    .expecting(OK).expecting(JSON),
                    response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "Even in production data we expect this query to return just a single result." // TODO.impl: add constraint?
        );

        obtain("Person: %{%{tradeName???}???%{givenName???} %{familyName???}}", () ->
            httpPost("/api/hs/office/persons", usingJsonBody("""
                    {
                        "personType": ${personType???},
                        "tradeName": ${tradeName???},
                        "givenName": ${givenName???},
                        "familyName": ${familyName???}
                    }
                    """))
                    .expecting(HttpStatus.CREATED).expecting(ContentType.JSON)
        );

        obtain("Contact: %{contactCaption}", () ->
            httpPost("/api/hs/office/contacts", usingJsonBody("""
                    {
                        "caption": ${contactCaption},
                        "postalAddress": {
                            %{postalAddress???}
                        },
                        "phoneNumbers": {
                            "office": ${officePhoneNumber???}
                        },
                        "emailAddresses": {
                            "main": ${emailAddress???}
                        }
                    }
                    """))
                    .expecting(HttpStatus.CREATED).expecting(ContentType.JSON)
        );

        return httpPost("/api/hs/office/partners", usingJsonBody("""
                {
                    "partnerNumber": ${partnerNumber},
                    "partnerRel": {
                         "anchor.uuid": ${Person: Hostsharing eG},
                         "holder.uuid": ${Person: %{%{tradeName???}???%{givenName???} %{familyName???}}},
                         "contact.uuid": ${Contact: %{contactCaption}}
                    },
                    "details": {
                        "registrationOffice": "Registergericht Hamburg",
                        "registrationNumber": "1234567"
                    }
                }
                """))
                .expecting(HttpStatus.CREATED).expecting(ContentType.JSON);
    }

    @Override
    protected void verify(final UseCase<CreatePartner>.HttpResponse response) {
        verify(
                "Verify the New Partner Relation",
                () -> httpGet("/api/hs/office/relations?relationType=PARTNER&contactData=&{contactCaption}")
                        .expecting(OK).expecting(JSON).expectArrayElements(1)
        );
    }
}
