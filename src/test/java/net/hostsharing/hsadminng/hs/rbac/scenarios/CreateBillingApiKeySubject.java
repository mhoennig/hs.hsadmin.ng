package net.hostsharing.hsadminng.hs.rbac.scenarios;

import lombok.val;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class CreateBillingApiKeySubject extends UseCase<CreateBillingApiKeySubject> {

    private final FakeLoginUser loginUser;

    public CreateBillingApiKeySubject(final ScenarioTest testSuite, final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;

        introduction("""
                The endpoint-scope `hs.office:billing-mda` is made for the billing system HSBilling NG MDA:
                it allows just to list the debitors and the SEPA-mandates, both with their query-parameters,
                and to fetch a single debitor by its debitor number.
                The global ADMIN role granted to its API_KEY subject makes all debitors and SEPA-mandates visible.
                Like every API-key, it can also use the self-inspection endpoints, e.g. `GET /api/rbac/context`
                to find out its subject when debugging the API-key; all other endpoints are rejected.

                On a stage, `tools/api-key-create hsadminng.billing.mda hs.office:billing-mda` does all of these steps.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {

        val creationResponse = withTitle("Create the API_KEY Subject for Billing", () ->
                httpPost(loginUser, "/api/rbac/subjects", usingJsonBody("""
                        {
                            "uuid": ${subjectUuid},
                            "name": ${subjectName},
                            "type": "API_KEY",
                            "scopes": ["hs.office:billing-mda"]
                        }
                        """))
                        // deliberately shows the clear-text API-key, it's only valid in the temporary test-database
                        .reportWithResponse()
                        .expecting(expectedStatus));
        if (creationResponse.getStatus() != CREATED) {
            return creationResponse;
        }
        using("apiKey", creationResponse.getFromBody("apiKey"));

        withTitleAndRequestInfo(
                "Prerequisite: Resolve the UUID of the global ADMIN role",
                """
                        The grant API needs the UUID of the role which we want to grant.
                        """,
                () -> httpGet(loginUser, "/api/rbac/roles?name=" + uriEncoded("rbac.global#global:ADMIN"))
                        .expecting(OK).expecting(JSON).expectArrayElements(1)
                        .extractUuidAlias("[0].uuid", "globalAdminRoleUuidToGrant")
        );

        withTitle("Grant the global ADMIN role to the API_KEY Subject", () ->
                httpPost(loginUser, "/api/rbac/grants", usingJsonBody("""
                        {
                          "assumed": true,
                          "grantedRole.uuid": "%{globalAdminRoleUuidToGrant}",
                          "granteeSubject.uuid": "%{subjectUuid}"
                        }
                        """),
                        request -> request.header("Hostsharing-Assumed-Roles", "rbac.global#global:ADMIN"))
                        .expecting(CREATED).expecting(JSON)
        );
        keep("ApiKey: %{subjectName}", creationResponse.getFromBody("apiKey"));

        return creationResponse;
    }

    @Override
    protected void verify(final HttpResponse response) {
        assertThat(response.<Object>getFromBody("scopes"))
                .as("the created API-key echoes its scope").hasToString("[\"hs.office:billing-mda\"]");
        assertThat(response.<String>getFromBody("apiKey"))
                .as("the response contains the clear-text API-key").startsWith("hsak_%s.".formatted(
                        ScenarioTest.resolve("%{subjectName}", DROP_COMMENTS)));
    }
}
