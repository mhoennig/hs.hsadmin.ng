package net.hostsharing.hsadminng.hs.office.scenarios.person;

import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.office.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public class ShouldUpdatePersonData extends UseCase<ShouldUpdatePersonData> {

    public ShouldUpdatePersonData(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {

        obtain(
                "personUuid",
                () -> httpGet("/api/hs/office/persons?name=" + uriEncoded("%{oldFamilyName}"))
                        .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In production, data this query could result in multiple outputs. In that case, you have to find out which is the right one."
        );

        withTitle("Patch the Additional Phone-Number into the Person", () ->
                httpPatch("/api/hs/office/persons/%{personUuid}", usingJsonBody("""
                {
                    "familyName": ${newFamilyName}
                }
                """))
                .expecting(HttpStatus.OK)
        );

        return null;
    }

    @Override
    protected void verify(final UseCase<ShouldUpdatePersonData>.HttpResponse response) {
        verify(
                "Verify that the Family Name Got Amended",
                () -> httpGet("/api/hs/office/persons/%{personUuid}")
                        .expecting(OK).expecting(JSON),
                path("familyName").contains("%{newFamilyName}")
        );
    }
}
