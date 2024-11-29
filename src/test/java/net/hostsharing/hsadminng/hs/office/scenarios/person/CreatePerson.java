package net.hostsharing.hsadminng.hs.office.scenarios.person;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import org.springframework.http.HttpStatus;

public class CreatePerson extends UseCase<CreatePerson> {

    public CreatePerson(final ScenarioTest testSuite, final String resultAlias) {
        super(testSuite, resultAlias);
    }

    @Override
    protected HttpResponse run() {

        return withTitle("Create the Person", () ->
                httpPost("/api/hs/office/persons", usingJsonBody("""
                    {
                        "personType": ${personType},
                        "tradeName": ${tradeName}
                    }
                    """))
                    .expecting(HttpStatus.CREATED).expecting(ContentType.JSON)
        );
    }
}
