package net.hostsharing.hsadminng.hs.accounts.scenarios;

import lombok.val;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asSubject;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.springframework.http.HttpStatus.OK;

public class AccountCanViewTheirOwnRelations extends BaseAccountUseCase<AccountCanViewTheirOwnRelations> {

    public AccountCanViewTheirOwnRelations(final ScenarioTest scenarioTest, final FakeLoginUser asLoginUser) {
        super(scenarioTest, asLoginUser);
    }

    @Override
    protected HttpResponse run() {
        obtain(
                "personUuid",
                () -> httpGet( asSubject("%{subjectName}"), "/api/hs/accounts/accounts")
                        .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].person.uuid"),
                "Fetch the account for the current subject to resolve the related person."
        );

        return withTitle("View their relations", () ->
                httpGet(asSubject("%{subjectName}"), "/api/hs/office/relations?personUuid%{personUuid}")
                        .expecting(OK).expecting(JSON)
        );
    }

    @Override
    protected void verify(final HttpResponse response) {
        val expectedRelationsJson = ScenarioTest.resolve("%{expectedRelationsJson}", DROP_COMMENTS);
        lenientlyEquals(expectedRelationsJson).matches(response.getBody());
    }
}
