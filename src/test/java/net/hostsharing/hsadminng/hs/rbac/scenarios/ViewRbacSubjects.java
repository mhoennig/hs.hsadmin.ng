package net.hostsharing.hsadminng.hs.rbac.scenarios;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import lombok.val;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.hasProperty;
import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.resolve;
import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.subjectNamesFrom;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

/**
 * Fetches the visible RBAC subjects as the given login user, parameterized by the scenario test:
 * <ul>
 *     <li>given "assumedRoleIdName": sent as Hostsharing-Assumed-Roles header, if set</li>
 *     <li>using "assumedRoleDescription": describes the assumed role in the report, e.g. "a non-global role",
 *         where the concrete role id would be misleading</li>
 *     <li>given "organizationFilter": fetches only the subjects of that organization, if set</li>
 *     <li>expected "expectedExactSubjectNames": the response must contain exactly these subjects</li>
 *     <li>expected "expectedSubjectNames": the response must contain at least these subjects</li>
 *     <li>expected "unexpectedSubjectNames": the response must not contain any of these subjects</li>
 * </ul>
 * The scenario-specific semantics belong into the test method: its name and the introduction.
 */
public class ViewRbacSubjects extends UseCase<ViewRbacSubjects> {

    private final FakeLoginUser loginUser;

    public ViewRbacSubjects(
            final ScenarioTest testSuite,
            final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;
    }

    @Override
    protected HttpResponse run() {
        final var uri = hasProperty("organizationFilter")
                ? "/api/rbac/subjects?organization=&{organizationFilter}"
                : "/api/rbac/subjects";
        if (hasProperty("assumedRoleIdName")) {
            return withTitle(
                    "Fetch visible RBAC subjects while assuming "
                            + (hasProperty("assumedRoleDescription")
                                    ? "%{assumedRoleDescription}"
                                    : "the role %{assumedRoleIdName}"),
                    () -> httpGet(
                            loginUser,
                            uri,
                            request -> request.header(
                                    "Hostsharing-Assumed-Roles",
                                    resolve("%{assumedRoleIdName}", DROP_COMMENTS)))
                            .limitReportedResultTo(5)
                            .expecting(OK).expecting(JSON));
        }
        return withTitle(
                hasProperty("organizationFilter")
                        ? "Fetch visible RBAC subjects of organization %{organizationFilter}"
                        : "Fetch visible RBAC subjects",
                () -> httpGet(
                        loginUser,
                        uri)
                        .limitReportedResultTo(5)
                        .expecting(OK).expecting(JSON));
    }

    @Override
    @SneakyThrows
    protected void verify(final HttpResponse response) {
        assertThat(hasProperty("expectedExactSubjectNames")
                || hasProperty("expectedSubjectNames")
                || hasProperty("unexpectedSubjectNames"))
                .as("at least one of expectedExactSubjectNames, expectedSubjectNames or unexpectedSubjectNames must be given")
                .isTrue();

        val subjects = objectMapper.<List<Map<String, Object>>>readValue(
                response.getBody(),
                new TypeReference<>() {
                });
        val subjectNames = subjects.stream()
                .map(subject -> (String) subject.get("name"))
                .toList();

        if (hasProperty("organizationFilter")) {
            assertThat(subjects)
                    .extracting(subject -> subject.get("organization"))
                    .containsOnly(resolve("%{organizationFilter}", DROP_COMMENTS));
        }
        if (hasProperty("expectedExactSubjectNames")) {
            assertThat(subjectNames)
                    .containsExactlyInAnyOrderElementsOf(subjectNamesFrom("%{expectedExactSubjectNames}"));
        }
        if (hasProperty("expectedSubjectNames")) {
            assertThat(subjectNames)
                    .contains(subjectNamesFrom("%{expectedSubjectNames}").toArray(String[]::new));
        }
        if (hasProperty("unexpectedSubjectNames")) {
            assertThat(subjectNames)
                    .doesNotContain(subjectNamesFrom("%{unexpectedSubjectNames}").toArray(String[]::new));
        }
    }
}
