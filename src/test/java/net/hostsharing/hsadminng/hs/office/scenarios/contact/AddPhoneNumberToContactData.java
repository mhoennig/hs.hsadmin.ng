package net.hostsharing.hsadminng.hs.office.scenarios.contact;

import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.office.scenarios.UseCase;
import org.springframework.http.HttpStatus;


import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public class AddPhoneNumberToContactData extends UseCase<AddPhoneNumberToContactData> {

    public AddPhoneNumberToContactData(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {

        obtain(
                "partnerContactUuid",
                () -> httpGet("/api/hs/office/relations?relationType=PARTNER&personData=" + uriEncoded("%{partnerName}"))
                        .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].contact.uuid"),
                "In production, data this query could result in multiple outputs. In that case, you have to find out which is the right one."
        );

        withTitle("Patch the Additional Phone-Number into the Contact", () ->
                httpPatch("/api/hs/office/contacts/%{partnerContactUuid}", usingJsonBody("""
                {
                    "phoneNumbers": {
                        ${phoneNumberKeyToAdd}: ${phoneNumberToAdd}
                    }
                }
                """))
                .expecting(HttpStatus.OK)
        );

        return null;
    }

    @Override
    protected void verify() {
        verify(
                "Verify if the New Phone Number Got Added",
                () -> httpGet("/api/hs/office/relations?relationType=PARTNER&personData=" + uriEncoded("%{partnerName}"))
                        .expecting(OK).expecting(JSON).expectArrayElements(1),
                path("[0].contact.phoneNumbers.%{phoneNumberKeyToAdd}").contains("%{phoneNumberToAdd}")
        );
    }
}
