package net.hostsharing.hsadminng.hs.accounts.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.springframework.http.HttpStatus.OK;

public class AccountCanViewTheirOwnMemberships extends BaseAccountUseCase<AccountCanViewTheirOwnMemberships> {

    public AccountCanViewTheirOwnMemberships(final ScenarioTest scenarioTest, final FakeLoginUser asLoginUser) {
        super(scenarioTest, asLoginUser);
    }

    @Override
    protected HttpResponse run() {
        obtain(
                "personUuid",
                () -> httpGet( asLoginUser, "/api/hs/accounts/accounts")
                        .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].person.uuid"),
                "Fetch the account for the current subject to resolve the related person."
        );

        withTitle("Resolve partner trade name", () ->
                httpGet( asLoginUser,
                        "/api/hs/office/relations?relationType=REPRESENTATIVE&personUuid=%{personUuid}")
                        .expecting(OK).expecting(JSON).expectArrayElements(1)
                        .extractValue("[0].anchor.tradeName", "partnerTradeName")
        );

        withTitle("Resolve partner UUID", () ->
                httpGet( asLoginUser,
                        "/api/hs/office/partners?name=" + uriEncoded("%{partnerTradeName}")
                )
                        .expecting(OK).expecting(JSON).expectArrayElements(1)
                        .extractUuidAlias("[0].uuid", "partnerUuid")
        );

        return withTitle("View their memberships", () ->
                httpGet( asLoginUser, "/api/hs/office/memberships?partnerUuid=%{partnerUuid}")
                        .expecting(OK).expecting(JSON)
        );
    }

    @Override
    protected void verify(final HttpResponse response) {
        final var expectedMembershipsJson = ScenarioTest.resolve("%{expectedMembershipsJson}", DROP_COMMENTS);
        lenientlyEquals(expectedMembershipsJson).matches(response.getBody());
    }
}
