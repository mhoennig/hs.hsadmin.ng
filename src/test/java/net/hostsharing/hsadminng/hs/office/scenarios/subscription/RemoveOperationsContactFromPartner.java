package net.hostsharing.hsadminng.hs.office.scenarios.subscription;

import net.hostsharing.hsadminng.hs.office.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

public class RemoveOperationsContactFromPartner extends UseCase<RemoveOperationsContactFromPartner> {

    public RemoveOperationsContactFromPartner(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {

        obtain("Operations-Contact: %{operationsContactPerson}", () ->
                        httpGet("/api/hs/office/relations?relationType=OPERATIONS&name=" + uriEncoded("%{operationsContactPerson}"))
                                .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In production data this query could result in multiple outputs. In that case, you have to find out which is the right one."
        );

        return httpDelete("/api/hs/office/relations/" + uuid("Operations-Contact: %{operationsContactPerson}"))
                .expecting(NO_CONTENT);
    }
}
