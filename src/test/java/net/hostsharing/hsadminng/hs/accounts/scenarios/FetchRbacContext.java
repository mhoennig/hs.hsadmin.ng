package net.hostsharing.hsadminng.hs.accounts.scenarios;

import lombok.SneakyThrows;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.resolve;
import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.resolveJsonArray;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

public class FetchRbacContext extends UseCase<FetchRbacContext> {

    public FetchRbacContext(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("Fetches the RBAC context for the login user / current subject.");
    }

    @Override
    protected HttpResponse run() {
        return obtain(
                "RBAC Context", () ->
                        httpGet(
                                "/api/rbac/context", req -> req
                                        .header("Authorization", resolve("Bearer %{subjectName}", DROP_COMMENTS))
                                        .header("assumed-roles", resolve("%{assumedRoles}", DROP_COMMENTS))
                        )
                        .expecting(OK).expecting(JSON).expectObject()
                                .extractValue("subject.name", "returnedSubjectName")
                                .extractValue("assumedRoles", "returnedAssumedRoles")
                                .extractValue("globalAdmin", "returnedGlobalAdmin")
        ).expecting(OK).expecting(JSON);
    }

    @Override
    @SneakyThrows
    protected void verify(final UseCase<FetchRbacContext>.HttpResponse response) {

        // HOWTO: assert in UseCase.verify()

        assertThat(resolve("%{returnedSubjectName}", DROP_COMMENTS))
                .isEqualTo(resolve("%{subjectName}", DROP_COMMENTS));

        assertThat(resolveJsonArray("%{returnedAssumedRoles}")
                .stream().map(m -> m.get("roleName")).toList())
                .isEqualTo(List.of(resolve("%{assumedRoles}", DROP_COMMENTS).split(";")));

        assertThat(resolve("%{returnedGlobalAdmin}", DROP_COMMENTS))
                .isEqualTo(resolve("%{expectedToBeGlobalAdmin}", DROP_COMMENTS));

        super.verify(response);
    }
}
