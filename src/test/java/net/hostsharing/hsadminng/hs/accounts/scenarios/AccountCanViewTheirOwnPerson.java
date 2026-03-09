package net.hostsharing.hsadminng.hs.accounts.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asSubject;
import static org.springframework.http.HttpStatus.OK;

public class AccountCanViewTheirOwnPerson extends BaseAccountUseCase<AccountCanViewTheirOwnPerson> {

    public AccountCanViewTheirOwnPerson(final ScenarioTest scenarioTest, final FakeLoginUser asLoginUser) {
        super(scenarioTest, asLoginUser);
    }

    @Override
    protected HttpResponse run() {
        obtain(
                "personUuid",
                () -> httpGet( asSubject("%{subjectName}"),
                        "/api/hs/accounts/accounts"
                )
                        .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].person.uuid"),
                "Fetch the account for the current subject to resolve the related person."
        );

        return withTitle("View Own Person", () ->
                httpGet( asSubject("%{subjectName}"),
                        "/api/hs/office/persons/%{personUuid}"
                )
                        .expecting(OK).expecting(JSON)
        );
    }

    @Override
    protected void verify(final HttpResponse response) {
        path("uuid").contains("%{personUuid}").accept(response);
        path("givenName").contains("%{personGivenName}").accept(response);
        path("familyName").contains("%{personFamilyName}").accept(response);
    }
}
