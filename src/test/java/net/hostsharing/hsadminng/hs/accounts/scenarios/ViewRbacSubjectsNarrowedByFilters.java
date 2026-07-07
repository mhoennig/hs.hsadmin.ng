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
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.resolveJsonArray;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class ViewRbacSubjectsNarrowedByFilters extends UseCase<ViewRbacSubjectsNarrowedByFilters> {

    private final FakeLoginUser loginUser;
    private String unfilteredBody;
    private String nameFilteredBody;

    public ViewRbacSubjectsNarrowedByFilters(
            final ScenarioTest testSuite,
            final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;

        introduction("""
                This scenario verifies that the optional name and type filters only narrow the
                otherwise-visible subjects and can never make additional subjects visible.
                The acting user has a rich visible set: subjects of its own realm and the groups
                of another organization in which its natural person also holds a user account.
                """);
    }

    @Override
    protected HttpResponse run() {
        // TODO.test[Taiga#471]: setup POSTs run as global agent while the main action acts as loginUser
        //          as soon as the sync-API is implemented, to follow the convention and
        //          split via @Produces/@Requires (CreateAccountForExistingPerson + a new CreateGroupSubject)
        withTitle(
                "Create group subject %{nameOfSamePersonGroupFromAnotherOrg}",
                () -> httpPost(
                        asGlobalAgent(),
                        "/api/rbac/subjects",
                        usingJsonBody("""
                                {
                                  "name": ${nameOfSamePersonGroupFromAnotherOrg},
                                  "type": "GROUP"
                                }
                                """))
                        .expecting(CREATED).expecting(JSON));

        obtain(
                "Person: %{thePersonsGivenName} %{thePersonsFamilyName}", () ->
                        httpGet(
                                asGlobalAgent(),
                                "/api/hs/office/persons?name=%{thePersonsFamilyName}&type=NATURAL_PERSON")
                                .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In production, this lookup would need a more precise selector."
        );

        withTitle(
                "Create same-person account %{theOtherAccountsSubjectName}",
                () -> httpPost(
                        asGlobalAgent(),
                        "/api/hs/accounts/accounts",
                        // TODO.impl[Taiga#471]: use subjectUuid
                        usingJsonBody("""
                                {
                                  "person.uuid": ${Person: %{thePersonsGivenName} %{thePersonsFamilyName}},
                                  "subjectName": ${theOtherAccountsSubjectName},
                                  "globalUid": %{theOtherAccountsGlobalUid},
                                  "globalGid": %{theOtherAccountsGlobalGid}
                                }
                                """))
                        .expecting(CREATED).expecting(JSON));

        unfilteredBody = withTitle(
                "Fetch all visible RBAC subjects without a filter",
                () -> httpGet(
                        loginUser,
                        "/api/rbac/subjects")
                        .expecting(OK).expecting(JSON))
                .getBody();

        nameFilteredBody = withTitle(
                "Fetch visible RBAC subjects narrowed by the name filter",
                () -> httpGet(
                        loginUser,
                        "/api/rbac/subjects?name=%{nameFilterPrefix}")
                        .expecting(OK).expecting(JSON))
                .getBody();

        return withTitle(
                "Fetch visible RBAC subjects narrowed by the type filter",
                () -> httpGet(
                        loginUser,
                        "/api/rbac/subjects?type=GROUP")
                        .expecting(OK).expecting(JSON));
    }

    @Override
    @SneakyThrows
    protected void verify(final HttpResponse response) {
        val unfilteredResponse = withTitle(
                "Fetch all visible RBAC subjects without a filter",
                () -> httpGet(
                        loginUser,
                        "/api/rbac/subjects")
                        .expecting(OK).expecting(JSON));
        val allNames = namesOf(subjectsFrom(unfilteredResponse.getBody()));

        // --- the type filter narrows to GROUP subjects, but never widens ---
        val typeFiltered = subjectsFrom(response.getBody());
        val typeFilteredNames = namesOf(typeFiltered);
        assertThat(typeFiltered)
                .allMatch(subject -> "GROUP".equals(subject.get("type")));
        // all three visibility sources contribute to the otherwise-visible set
        assertThat(typeFilteredNames)
                .contains(expectedNamesFrom("%{expectedSubjectNamesWithTypeFilter}").toArray(String[]::new));
        assertNarrowsButNeverWidens(allNames, typeFilteredNames, "%{unexpectedSubjectNamesWithTypeFilter}");

        // --- the name filter narrows to the requested prefix, but never widens ---
        val nameFilteredResponse = withTitle(
                "Fetch visible RBAC subjects narrowed by the name filter",
                () -> httpGet(
                        loginUser,
                        "/api/rbac/subjects?name=%{nameFilterPrefix}")
                        .expecting(OK).expecting(JSON));
        val nameFilterPrefix = ScenarioTest.resolve("%{nameFilterPrefix}", DROP_COMMENTS);
        val nameFilteredNames = namesOf(subjectsFrom(nameFilteredResponse.getBody()));
        assertThat(nameFilteredNames)
                .allMatch(name -> name.startsWith(nameFilterPrefix));
        assertThat(nameFilteredNames)
                .contains(expectedNamesFrom("%{expectedSubjectNamesWithNameFilter}").toArray(String[]::new));
        assertNarrowsButNeverWidens(allNames, nameFilteredNames, "%{unexpectedSubjectNamesWithNameFilter}");
    }

    private void assertNarrowsButNeverWidens(
            final List<String> allNames,
            final List<String> filteredNames,
            final String narrowedAwayProperty) {
        // the filter can never widen: the filtered set is a subset of the unfiltered set
        assertThat(allNames).containsAll(filteredNames);

        // the filter actually narrows: subjects which are visible without it are dropped by it
        val narrowedAway = expectedNamesFrom(narrowedAwayProperty);
        assertThat(allNames).contains(narrowedAway.toArray(String[]::new));
        assertThat(filteredNames).doesNotContainAnyElementsOf(narrowedAway);
    }

    @SneakyThrows
    private List<Map<String, Object>> subjectsFrom(final String body) {
        return objectMapper.readValue(body, new TypeReference<>() {
        });
    }

    private static List<String> namesOf(final List<Map<String, Object>> subjects) {
        return subjects.stream().map(subject -> (String) subject.get("name")).toList();
    }

    private List<String> expectedNamesFrom(final String propertyName) {
        return resolveJsonArray(propertyName)
                .stream()
                .map(subject -> (String) subject.get("name"))
                .toList();
    }
}
