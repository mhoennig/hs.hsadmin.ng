package net.hostsharing.hsadminng.hs.rbac.scenarios;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import lombok.val;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.resolve;
import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.subjectNamesFrom;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;

public class ListSubjectsFilteredByOrganization extends UseCase<ListSubjectsFilteredByOrganization> {

    private final FakeLoginUser loginUser;

    public ListSubjectsFilteredByOrganization(final ScenarioTest testSuite, final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;

        introduction("""
                The subjects of a single organization can be listed via
                `GET /api/rbac/subjects?organization={organization}`; the filter matches the
                stored organization value. The given subjects of that organization are
                synchronized first, deliberately with an organization which is not derivable
                from the USER name.
                """);
    }

    @Override
    protected HttpResponse run(final HttpStatus expectedStatus) {
        withTitle(
                "Given: a USER %{userName} with the explicitly given organization %{organization}",
                () -> httpPut(
                        loginUser,
                        "/api/rbac/subjects/%{userUuid}",
                        usingJsonBody("""
                                {
                                    "name": ${userName},
                                    "organization": ${organization},
                                    "type": "USER"
                                }
                                """))
                        .expecting(CREATED));
        withTitle(
                "Given: a GROUP %{groupName} of the same organization",
                () -> httpPut(
                        loginUser,
                        "/api/rbac/subjects/%{groupUuid}",
                        usingJsonBody("""
                                {
                                    "name": ${groupName},
                                    "organization": ${organization},
                                    "type": "GROUP"
                                }
                                """))
                        .expecting(CREATED));
        return withTitle(
                "List the subjects of organization %{organization}",
                () -> httpGet(loginUser, "/api/rbac/subjects?organization=&{organization}")
                        .expecting(expectedStatus).expecting(JSON));
    }

    @Override
    @SneakyThrows
    protected void verify(final HttpResponse response) {
        val subjects = objectMapper.<List<Map<String, Object>>>readValue(
                response.getBody(),
                new TypeReference<>() {
                });

        // the response contains at least the two given subjects of the filtered organization
        val subjectNames = subjects.stream()
                .map(subject -> (String) subject.get("name"))
                .toList();
        assertThat(subjectNames).contains(
                resolve("%{userName}", DROP_COMMENTS),
                resolve("%{groupName}", DROP_COMMENTS));

        // and subjects of other organizations are not included
        assertThat(subjects)
                .extracting(subject -> subject.get("organization"))
                .containsOnly(resolve("%{organization}", DROP_COMMENTS));
        assertThat(subjectNames)
                .doesNotContain(subjectNamesFrom("%{unexpectedSubjectNames}").toArray(String[]::new));
    }
}
