package net.hostsharing.hsadminng.hs.accounts.scenarios;

import lombok.SneakyThrows;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;


import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.resolve;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

public class CurrentLoginUser extends UseCase<CurrentLoginUser> {

    public CurrentLoginUser(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("Fetches data about the current login user.");
    }

    @Override
    protected HttpResponse run() {

        obtain("Person: %{personGivenName}", () ->
                        httpGet("/api/hs/office/persons?name=" + uriEncoded("%{personGivenName}"))
                                .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In production, data this query could result in multiple outputs. In that case, you have to find out which is the right one."
        );

        return obtain(
                "Current Login User", () ->
                        httpGet(
                                "/api/hs/accounts/current", req -> req
                                        .header("Authorization", resolve("Bearer %{subjectName}", DROP_COMMENTS))
                        )
                        .expecting(OK).expecting(JSON).expectObject()
                                .extractValue("subject.name", "returnedSubjectName")
                                .extractValue("person.givenName", "returnedGivenName")
                                .extractValue("globalAdmin", "returnedGlobalAdmin")
        ).expecting(OK).expecting(JSON);
    }

    @Override
    @SneakyThrows
    protected void verify(final UseCase<CurrentLoginUser>.HttpResponse response) {

        assertThat(resolve("%{returnedSubjectName}", DROP_COMMENTS))
                .isEqualTo(resolve("%{subjectName}", DROP_COMMENTS));

        assertThat(resolve("%{returnedGivenName}", DROP_COMMENTS))
                .isEqualTo(resolve("%{personGivenName}", DROP_COMMENTS));

        assertThat(resolve("%{returnedGlobalAdmin}", DROP_COMMENTS))
                .isEqualTo(resolve("%{expectedToBeGlobalAdmin}", DROP_COMMENTS));

        super.verify(response);
    }
}
