package net.hostsharing.hsadminng.hs.office.scenarios.subscription;

import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.office.scenarios.UseCase;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

public class RemoveOperationsContactFromPartner extends UseCase<RemoveOperationsContactFromPartner> {

    public RemoveOperationsContactFromPartner(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {

        obtain("Operations-Contact: %{operationsContactPerson}",
                () ->
                        httpGet("/api/hs/office/relations?relationType=OPERATIONS&name=" + uriEncoded(
                                "%{operationsContactPerson}"))
                                .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In production, data this query could result in multiple outputs. In that case, you have to find out which is the right one."
        );

        return withTitle("Delete the Contact", () ->
                httpDelete("/api/hs/office/relations/&{Operations-Contact: %{operationsContactPerson}}")
                        .expecting(NO_CONTENT)
        );
    }

    @Override
    protected void verify(final UseCase<RemoveOperationsContactFromPartner>.HttpResponse response) {
        verify(
                "Verify the New OPERATIONS Relation",
                () -> httpGet("/api/hs/office/relations/&{Operations-Contact: %{operationsContactPerson}}")
                        .expecting(NOT_FOUND)
        );
    }
}
