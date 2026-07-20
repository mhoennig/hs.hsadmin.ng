package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.config.ProvisioningApiKeyBootstrap.HSADMINNG_PROVISIONING_API_KEY_SUBJECT_NAME;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.withApiKey;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.springframework.http.HttpStatus.OK;

public class CreateFurtherApiKeyUsingTheProvisioningApiKey extends UseCase<CreateFurtherApiKeyUsingTheProvisioningApiKey> {

    public CreateFurtherApiKeyUsingTheProvisioningApiKey(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                When starting with a legacy database, there is neither a Keycloak-matching USER subject
                nor any API_KEY subject, thus nobody could create the first API-key via the API.
                If the SHA-256 hash of an API-key is configured in `HSADMINNG_PROVISIONING_API_KEY_SHA256`,
                the application start provisions the API_KEY subject `%s` with the global
                ADMIN role for it, idempotently: an already stored API-key always takes precedence.
                The clear-text API-key exists neither on the application server nor in the database,
                but only at the client. Acting as global-admin, that provisioning API-key can then bootstrap
                everything else via the API, e.g. create further API_KEY subjects, without any
                Keycloak login.
                """.formatted(HSADMINNG_PROVISIONING_API_KEY_SUBJECT_NAME));
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        return withTitle("Create a further API_KEY Subject, authenticated just by the provisioning API-key", () ->
                httpPost(withApiKey(ScenarioTest.PROVISIONING_API_KEY), "/api/rbac/subjects", usingJsonBody("""
                        {
                            "uuid": ${subjectUuid},
                            "name": ${subjectName},
                            "type": "API_KEY"
                        }
                        """))
                        // deliberately shows the clear-text API-key, it's only valid in the temporary test-database
                        .reportWithResponse()
                        .expecting(expectedStatus));
    }

    @Override
    protected void verify(final HttpResponse response) {
        response.path("name").isEqualTo(ScenarioTest.resolve("%{subjectName}", DROP_COMMENTS));
        response.path("type").isEqualTo("API_KEY");

        verify(
                "Verify the provisioning API-key authenticates as its provisioned Subject with the global-admin role",
                () -> httpGet(withApiKey(ScenarioTest.PROVISIONING_API_KEY), "/api/hs/accounts/current")
                        .expecting(OK).expecting(JSON),
                path("subject.name").contains(HSADMINNG_PROVISIONING_API_KEY_SUBJECT_NAME),
                path("subject.type").contains("API_KEY"),
                path("globalAdmin").contains("true")
        );
    }
}
