package net.hostsharing.hsadminng.hs.accounts.scenarios;

import lombok.val;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.resolve;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

public class GrantProjectAdminRoleToGroup extends UseCase<GrantProjectAdminRoleToGroup> {

    private final FakeLoginUser grantingUser;

    public GrantProjectAdminRoleToGroup(
            final ScenarioTest testSuite,
            final FakeLoginUser grantingUser) {
        super(testSuite);
        this.grantingUser = grantingUser;

        introduction("""
                This scenario demonstrates how a debitor AGENT
                can grant the ADMIN role of one of their projects to a GROUP Subject.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        withTitleAndRequestInfo(
                "Prerequisite: Resolving the projects UUID",
                """
                    In a real-world scenario, there could be multiple results and the user has to select the correct one.
                    """,
                () -> {
                    val response = httpGet(
                            grantingUser,
                            "/api/hs/booking/projects",
                            request -> request.header(
                                    "Hostsharing-Assumed-Roles",
                                    resolve("%{roleIdNameToAssume}", DROP_COMMENTS)))
                            .expecting(OK).expecting(JSON);
                    assertThat(response.<String>getFromBody("[0].caption"))
                            .isEqualTo(resolve("%{projectCaption}", DROP_COMMENTS));
                    return response.extractUuidAlias("[0].uuid", "projectUuid");
                }
        );
        withTitleAndRequestInfo(
                "Prerequisite: Resolve group subject UUID for '%{nameOfGroupSubject}'",
                """
                    To use the grant API, we need the UUID of the group-subject.
                    The granting user finds the groups they are assigned to, including their UUIDs,
                    in their own RBAC context.
                    """,
                () -> {
                    val response = httpGet(
                            grantingUser,
                            "/api/rbac/context")
                            .expecting(OK).expecting(JSON)
                            .expectObject();
                    assertThat(response.<String>getFromBody("effectiveGroups[0].name"))
                            .isEqualTo(resolve("%{nameOfGroupSubject}", DROP_COMMENTS));
                    return response.extractUuidAlias("effectiveGroups[0].uuid", "groupSubjectUuidToGrantTo");
                }
        );
        withTitleAndRequestInfo(
                "Prerequisite: Resolve project " + "ADMIN" + " role UUID",
                """
                        The grant API needs the UUID of the role which we want to grant.
                        We could use the project's IdName for the assume, but in the real world that could be ambiguous.
                        """,
                () -> httpGet(
                        grantingUser,
                        "/api/rbac/roles?name=" + uriEncoded("hs_booking.project#%{projectIdName}:" + "ADMIN"),
                        request1 -> request1.header("Hostsharing-Assumed-Roles", assumedProjectOwnerRoleByUuid()))
                        .expecting(OK).expecting(JSON).expectArrayElements(1)
                        .extractUuidAlias("[0].uuid", "projectAdminRoleUuidToGrant")
        );

        val existingGrant = withTitleAndRequestInfo(
                "Precondition: Expect that the grant does not yet exist",
                """
                        This check is not necessary in a real user journey.
                        For the test-aspect of this scenario, though, we want to make sure that the role grant does not yet exist.
                        """,
                () -> httpGet(grantingUser, "/api/rbac/grants/%{" + "projectAdminRoleUuidToGrant" + "}/%{groupSubjectUuidToGrantTo}")
                        .reportWithResponse()
        );
        assertThat(existingGrant.getStatus()).isEqualTo(NOT_FOUND);

        return withTitleAndRequestInfo(
                "Grant project ADMIN role to the group-subject without auto-assume",
                """
                        This grant allows members of the group '%{nameOfGroupSubject}' to explicitly assume the project ADMIN role.
                        """,
                () -> httpPost(
                        grantingUser,
                        "/api/rbac/grants",
                        // TODO.impl: assumed=false once we can grant unassumed any yet keep objects visible
                        usingJsonBody("""
                                {
                                  "assumed": true,
                                  "grantedRole.uuid": "%{projectAdminRoleUuidToGrant}",
                                  "granteeSubject.uuid": "%{groupSubjectUuidToGrantTo}"
                                }
                                """),
                        request -> request.header("Hostsharing-Assumed-Roles", assumedProjectOwnerRoleByUuid()))
                        .expecting(CREATED).expecting(JSON)
        );
    }

    private String assumedProjectOwnerRoleByUuid() {
        return resolve("hs_booking.project#%{projectUuid}:OWNER", DROP_COMMENTS);
    }

    @Override
    protected void verify(final HttpResponse response) {
        path("grantedRoleIdName").contains("hs_booking.project#%{projectIdName}:ADMIN").accept(response);
        path("granteeSubjectName").contains("%{nameOfGroupSubject}").accept(response);
        // TODO.impl: assumed=false once we can grant unassumed any yet keep objects visible
        path("assumed").contains("true").accept(response);
    }
}
