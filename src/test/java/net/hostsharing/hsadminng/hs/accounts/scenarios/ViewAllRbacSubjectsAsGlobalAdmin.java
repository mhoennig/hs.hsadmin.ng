package net.hostsharing.hsadminng.hs.accounts.scenarios;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import lombok.val;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.resolveJsonArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

public class ViewAllRbacSubjectsAsGlobalAdmin extends UseCase<ViewAllRbacSubjectsAsGlobalAdmin> {

    private final FakeLoginUser loginUser;

    public ViewAllRbacSubjectsAsGlobalAdmin(
            final ScenarioTest testSuite,
            final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;

        introduction("""
                This scenario verifies that a global admin who has not assumed any role
                sees all subjects across all realms.
                """);
    }

    @Override
    protected HttpResponse run() {
        return withTitle(
                "Fetch visible RBAC subjects as a global admin without an assumed role",
                () -> httpGet(
                        loginUser,
                        "/api/rbac/subjects")
                        .expecting(OK).expecting(JSON));
    }

    @Override
    @SneakyThrows
    protected void verify(final HttpResponse response) {
        val subjects = objectMapper.<List<Map<String, Object>>>readValue(
                response.getBody(),
                new TypeReference<>() {
                });
        val subjectNames = subjects.stream()
                .map(subject -> (String) subject.get("name"))
                .toList();
        val expectedSubjectNames = subjectNamesFrom("%{expectedSubjectNames}");

        assertThat(subjectNames)
                .contains(expectedSubjectNames.toArray(String[]::new));
    }

    private List<String> subjectNamesFrom(final String propertyName) {
        return resolveJsonArray(propertyName)
                .stream()
                .map(subject -> (String) subject.get("name"))
                .toList();
    }
}
