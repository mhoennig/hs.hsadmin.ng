package net.hostsharing.hsadminng.hs.office.scenarios.contact;

import lombok.val;
import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.junit.platform.commons.util.StringUtils.isNotBlank;

public class QueryRelationMarks extends UseCase<QueryRelationMarks> {

    private final FakeLoginUser loginUser;

    public QueryRelationMarks(final ScenarioTest testSuite, final FakeLoginUser loginUser) {
        super(testSuite);
        this.loginUser = loginUser;
    }

    @Override
    protected HttpResponse run() {
        val queryParams = new ArrayList<String>();
        addIfPresent(queryParams, "relationType");
        val queryString = queryParams.isEmpty() ? "" : "?" + String.join("&", queryParams);

        return withTitle("Query Distinct Relation-Marks" + (queryParams.isEmpty() ? "" : " of Type %{relationType}"), () ->
                httpGet(loginUser, "/api/hs/office/relations/marks" + queryString)
                        .expecting(HttpStatus.OK)
                        .expecting(JSON)
        );
    }

    @Override
    protected void verify(final HttpResponse response) {
        val expectedMarks = Arrays.stream(ScenarioTest.resolve("%{marks???}", DROP_COMMENTS).split(","))
                .map(String::trim)
                .filter(mark -> isNotBlank(mark))
                .toList();
        response.expectArrayElements(expectedMarks.size());
        for (int i = 0; i < expectedMarks.size(); ++i) {
            response.path("[" + i + "].mark").isEqualTo(expectedMarks.get(i));
        }
    }

    private void addIfPresent(final List<String> queryParams, final String name) {
        val resolved = ScenarioTest.resolve("%{" + name + "???}", DROP_COMMENTS);
        if (!resolved.isEmpty()) {
            queryParams.add(name + "=" + uriEncoded("%{" + name + "}"));
        }
    }
}
