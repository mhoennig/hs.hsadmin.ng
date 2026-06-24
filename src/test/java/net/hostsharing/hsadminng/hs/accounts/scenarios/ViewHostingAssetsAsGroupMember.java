package net.hostsharing.hsadminng.hs.accounts.scenarios;

import lombok.val;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.resolve;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;

public class ViewHostingAssetsAsGroupMember extends UseCase<ViewHostingAssetsAsGroupMember> {

    private final FakeLoginUser groupMember;

    public ViewHostingAssetsAsGroupMember(
            final ScenarioTest testSuite,
            final FakeLoginUser groupMember) {
        super(testSuite);
        this.groupMember = groupMember;

        introduction("""
                This scenario verifies which hosting assets are visible to a user whose JWT contains a GROUP Subject.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        withTitleAndRequestInfo(
                "Prerequisite: Resolving the project's UUID",
                """
                        In a real-world scenario, there could be multiple results and the user has to select the correct one.
                        """,
                () -> {
                    val response = httpGet(
                            groupMember,
                            "/api/hs/booking/projects")
                            .expecting(HttpStatus.OK).expecting(JSON);
                    val projectCaptions = response.<List<String>>getFromBody("$[*].caption");
                    val projectCaption = resolve("%{projectCaption}", DROP_COMMENTS);
                    assertThat(projectCaptions)
                            .as("project visible before assuming its ADMIN role")
                            .contains(projectCaption);
                    return response.extractUuidAlias("[" + projectCaptions.indexOf(projectCaption) + "].uuid", "projectUuid");
                }
        );

        return withTitleAndRequestInfo(
                "Fetch visible hosting assets via assigned group",
                """
                        The group '%{nameOfGroupSubject}' has the role 'hs_booking.project#%{projectUuid}:ADMIN'.
                        The user '%{nameOfUserSubject}' is a member of the group '%{nameOfGroupSubject}'.
                        Therefore, hosting assets below the booking project '%{projectCaption}' are expected to be visible.
                        """,
                () -> httpGet(
                        groupMember,
                        "/api/hs/hosting/assets?projectUuid=%{projectUuid}",
                        request -> request.header(
                                "Hostsharing-Assumed-Roles",
                                projectAdminRoleName()))
                        .expecting(expectedStatus).expecting(JSON)
        );
    }

    @Override
    protected void verify(final HttpResponse response) {
        assertThat(response.<List<String>>getFromBody("$[*].identifier"))
                .contains("fir01", "vm1011");
    }

    private String projectAdminRoleName() {
        return resolve("hs_booking.project#%{projectUuid}:ADMIN", DROP_COMMENTS);
    }
}
