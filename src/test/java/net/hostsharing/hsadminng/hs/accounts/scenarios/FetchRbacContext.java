package net.hostsharing.hsadminng.hs.accounts.scenarios;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import java.util.Arrays;
import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static java.util.function.Predicate.not;
import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.resolve;
import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.resolveJsonArray;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

public class FetchRbacContext extends UseCase<FetchRbacContext> {

    private final FakeLoginUser loginUser;

    public FetchRbacContext(final ScenarioTest testSuite, final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;

        introduction("Fetches the RBAC context for the login user / current subject.");
    }

    @Override
    protected HttpResponse run() {
        return obtain(
                "RBAC Context", () ->
                        httpGet(loginUser,
                                "/api/rbac/context", req -> req
                                        .header("Hostsharing-Assumed-Roles", resolve("%{assumedRoles}", DROP_COMMENTS))
                        )
                        .expecting(OK).expecting(JSON).expectObject()
                                .extractValue("subject.name", "returnedSubjectName")
                                .extractValue("subject.type", "returnedSubjectType")
                                .extractValue("claimedGroups", "returnedClaimedGroups")
                                .extractValue("effectiveGroups", "returnedEffectiveGroups")
                                .extractValue("assumedRoles", "returnedAssumedRoles")
                                .extractValue("globalAdmin", "returnedGlobalAdmin")
        ).expecting(OK).expecting(JSON);
    }

    @Override
    @SneakyThrows
    protected void verify(final UseCase<FetchRbacContext>.HttpResponse response) {

        // HOWTO: assert in UseCase.verify()

        assertThat(resolve("%{returnedSubjectName}", DROP_COMMENTS))
                .isEqualTo(resolve("%{subjectName}", DROP_COMMENTS));

        assertThat(resolve("%{returnedSubjectType}", DROP_COMMENTS))
                .isEqualTo(resolve("%{expectedSubjectType}", DROP_COMMENTS));

        assertThat(resolveJsonArray("%{returnedAssumedRoles}")
                .stream().map(m -> m.get("roleIdName")).toList())
                .isEqualTo(expectedAssumedRoles());

        assertThat(resolve("%{returnedGlobalAdmin}", DROP_COMMENTS))
                .isEqualTo(resolve("%{expectedToBeGlobalAdmin}", DROP_COMMENTS));

        assertThat(resolveStringArray("%{returnedClaimedGroups}"))
                .isEqualTo(resolveStringArray("%{expectedClaimedGroups}"));

        assertThat(resolveJsonArray("%{returnedEffectiveGroups}")
                .stream().map(m -> m.get("name")).toList())
                .isEqualTo(resolveJsonArray("%{expectedEffectiveGroups}")
                        .stream().map(m -> m.get("name")).toList());

        super.verify(response);
    }

    private List<String> expectedAssumedRoles() {
        return Arrays.stream(resolve("%{assumedRoles}", DROP_COMMENTS).split(";"))
                .filter(not(String::isBlank))
                .toList();
    }

    @SneakyThrows
    private static List<String> resolveStringArray(final String text) {
        return new ObjectMapper().readValue(resolve(text, DROP_COMMENTS), new TypeReference<>() {});
    }
}
