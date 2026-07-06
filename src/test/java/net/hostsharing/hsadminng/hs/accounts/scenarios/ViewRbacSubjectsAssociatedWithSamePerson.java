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
import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.resolve;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class ViewRbacSubjectsAssociatedWithSamePerson extends UseCase<ViewRbacSubjectsAssociatedWithSamePerson> {

    private final FakeLoginUser loginUser;

    public ViewRbacSubjectsAssociatedWithSamePerson(
            final ScenarioTest testSuite,
            final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;

        introduction("""
                This scenario verifies that subject visibility includes the group subjects
                of other organizations in which the same natural person also holds a user account.
                Because the person behind the current account also has an account in organization "abc",
                the current account can see the groups of organization "abc", such as "/abc-Team".
                """);
    }

    @Override
    protected HttpResponse run() {
        obtain(
                "Person: %{thePersonsGivenName} %{thePersonsFamilyName}", () ->
                        httpGet(
                                asGlobalAgent(),
                                "/api/hs/office/persons?name=%{thePersonsFamilyName}&type=NATURAL_PERSON")
                                .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In production, this lookup would need a more precise selector."
        );

        // TODO.test[Taiga#471]: mutating setup (POSTs as global agent) inside a use case whose main action acts as loginUser;
        //            once the sync-API is implemented, follow the convention and
        //            split multi-subject workflows into separate use cases connected via @Produces/@Requires
        withTitle(
                "Create group subject %{nameOfAssociatedGroupSubjectFromAnotherOrg}",
                () -> httpPost(
                        asGlobalAgent(),
                        "/api/rbac/subjects",
                        usingJsonBody("""
                                {
                                  "name": ${nameOfAssociatedGroupSubjectFromAnotherOrg},
                                  "type": "GROUP"
                                }
                                """))
                        .expecting(CREATED).expecting(JSON));

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

        withTitle(
                "Precondition: %{theOtherAccountsSubjectName} is associated with %{nameOfAssociatedGroupSubjectFromAnotherOrg}",
                () -> httpGet(
                        FakeLoginUser.asSubject("%{theOtherAccountsSubjectName}")
                                .withGroups("%{nameOfAssociatedGroupSubjectFromAnotherOrg}"),
                        "/api/rbac/context")
                        .expecting(OK).expecting(JSON)
                        .expectObject()
                        .extractValue("effectiveGroups[0].name", "returnedAssociatedGroupSubject")
        );
        assertThat(resolve("%{returnedAssociatedGroupSubject}", DROP_COMMENTS))
                .isEqualTo(resolve("%{nameOfAssociatedGroupSubjectFromAnotherOrg}", DROP_COMMENTS));

        return withTitle(
                "Fetch visible RBAC subjects",
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
        val subjectNames = subjects
                .stream()
                .map(subject -> (String) subject.get("name"))
                .toList();

        assertThat(subjectNames)
                .contains(resolve("%{nameOfAssociatedGroupSubjectFromAnotherOrg}", DROP_COMMENTS));
    }
}
