package net.hostsharing.hsadminng.hs.accounts.scenarios;

import lombok.val;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asSubject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class CreateGlobalAdminSubjectWithoutAccount extends BaseAccountUseCase<CreateGlobalAdminSubjectWithoutAccount> {

    public CreateGlobalAdminSubjectWithoutAccount(final ScenarioTest testSuite, final FakeLoginUser asLoginUser) {
        super(testSuite, asLoginUser);

        introduction("""
                Creating accounts needs an acting global-admin USER subject, but that subject
                does not need to have an own account (and thus a person) itself.
                Here, such a subject gets created: it is synchronized from Keycloak,
                and the global ADMIN role is granted to it, but no account is created for it.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {

        val subjectSyncResponse = withTitle("Synchronize the new USER Subject from Keycloak", () ->
                httpPut(asLoginUser, "/api/rbac/subjects/%{subjectUuid}", usingJsonBody("""
                        {
                            "name": ${subjectName},
                            "type": "USER"
                        }
                        """))
                        .expecting(CREATED).expecting(JSON),
                "This is what the Keycloak sync program does for each new Keycloak user."
        );

        withTitleAndRequestInfo(
                "Prerequisite: Resolve the UUID of the global ADMIN role",
                """
                        The grant API needs the UUID of the role which we want to grant.
                        """,
                () -> httpGet(asLoginUser, "/api/rbac/roles?name=" + uriEncoded("rbac.global#global:ADMIN"))
                        .expecting(OK).expecting(JSON).expectArrayElements(1)
                        .extractUuidAlias("[0].uuid", "globalAdminRoleUuidToGrant")
        );

        withTitle("Grant the global ADMIN role to the new USER Subject", () ->
                httpPost(asLoginUser, "/api/rbac/grants", usingJsonBody("""
                        {
                          "assumed": true,
                          "grantedRole.uuid": "%{globalAdminRoleUuidToGrant}",
                          "granteeSubject.uuid": "%{subjectUuid}"
                        }
                        """),
                        request -> request.header("Hostsharing-Assumed-Roles", "rbac.global#global:ADMIN"))
                        .expecting(CREATED).expecting(JSON)
        );

        return subjectSyncResponse;
    }

    @Override
    protected void verify(final HttpResponse response) {
        verify(
                "Verify the new global-admin Subject does not have an own Account",
                () -> httpGet(asSubject("%{subjectName}"), "/api/hs/accounts/current")
                        .expecting(OK).expecting(JSON),
                path("subject.name").contains("%{subjectName}"),
                path("globalAdmin").contains("true"),
                currentResponse -> assertThat(currentResponse.<Object>getFromBody("person"))
                        .as("person of an account-less subject").isNull()
        );
    }
}
