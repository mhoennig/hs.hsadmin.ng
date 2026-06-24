package net.hostsharing.hsadminng.hs.accounts.scenarios;

import io.restassured.http.ContentType;
import lombok.val;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import java.util.List;

import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.resolve;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;

public class AssumeBookingProjectAdminRoleAsGroupMember extends UseCase<AssumeBookingProjectAdminRoleAsGroupMember> {

    private final FakeLoginUser groupMember;

    public AssumeBookingProjectAdminRoleAsGroupMember(
            final ScenarioTest testSuite,
            final FakeLoginUser groupMember) {
        super(testSuite);
        this.groupMember = groupMember;

        introduction("""
                This scenario verifies that a user whose JWT contains a GROUP Subject can assume
                a hs_booking.project ADMIN role which was granted to that group.
                For this, the USER-subject itself does not need any grants at all.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        resolveProjectUuid();

        return withTitleAndRequestInfo(
                "Assume a booking project ADMIN role using a group-assignment",
                """
                        The booking project ADMIN role was granted to the group '%{nameOfGroupSubject}'.
                        The user '%{nameOfUserSubject}' is a member of the group '%{nameOfGroupSubject}'.
                        Therefore, the user '%{nameOfUserSubject}' is now expected to be able to assume that role.
                        """,
                () ->
                        httpGet(
                                groupMember,
                                "/api/rbac/context",
                                request -> request.header(
                                        "Hostsharing-Assumed-Roles",
                                        bookingProjectAdminRoleName()))
                                .expecting(expectedStatus).expecting(ContentType.JSON).expectObject()
        );
    }

    @Override
    protected void verify(final HttpResponse response) {
        assertThat(response.<String>getFromBody("subject.name"))
                .isEqualTo(resolve("%{nameOfUserSubject}", DROP_COMMENTS));
        assertThat(response.<List<String>>getFromBody("assumedRoles[*].roleName"))
                .containsExactly(bookingProjectAdminRoleName());
        assertThat(response.<List<String>>getFromBody("assumedRoles[*].roleIdName"))
                .containsExactly(resolve("%{expectedAssumedRoleIdName}", DROP_COMMENTS));
        assertCanListAssumedProject();
    }

    private void resolveProjectUuid() {
        withTitleAndRequestInfo(
                "Prerequisite: Resolving the project's UUID",
                """
                        In a real-world scenario, there could be multiple results and the user has to select the correct one.
                        """,
                () -> {
                    val response = httpGet(
                            groupMember,
                            "/api/hs/booking/projects")
                            .expecting(HttpStatus.OK).expecting(ContentType.JSON);
                    val projectCaptions = response.<List<String>>getFromBody("$[*].caption");
                    val projectCaption = resolve("%{projectCaption}", DROP_COMMENTS);
                    assertThat(projectCaptions)
                            .as("project visible before assuming its ADMIN role")
                            .contains(projectCaption);
                    return response.extractUuidAlias("[" + projectCaptions.indexOf(projectCaption) + "].uuid", "projectUuid");
                }
        );
    }

    private void assertCanListAssumedProject() {
        val bookingProjects = withTitleAndRequestInfo(
                "List booking projects after assuming the project role",
                """
                        With the assumed project role, the user expected to see the booking project through the assigned group.
                        """,
                () -> httpGet(
                        groupMember,
                        "/api/hs/booking/projects",
                        request -> request.header(
                                "Hostsharing-Assumed-Roles",
                                bookingProjectAdminRoleName()))
                        .expecting(HttpStatus.OK).expecting(ContentType.JSON)
        );

        assertThat(bookingProjects.<List<String>>getFromBody("$[*].caption"))
                .contains(resolve("%{projectCaption}", DROP_COMMENTS));
    }

    private String bookingProjectAdminRoleName() {
        return resolve("hs_booking.project#%{projectUuid}:ADMIN", DROP_COMMENTS);
    }
}
