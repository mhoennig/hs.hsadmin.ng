package net.hostsharing.hsadminng.hs.office.scenarios.contact;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;

public class QueryRelations extends UseCase<QueryRelations> {

    public QueryRelations(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {
        final List<String> queryParams = new ArrayList<>();
        addIfPresent(queryParams, "personUuid");
        addIfPresent(queryParams, "anchorPersonUuid");
        addIfPresent(queryParams, "holderPersonUuid");
        addIfPresent(queryParams, "relationType");
        addIfPresent(queryParams, "mark");
        addIfPresent(queryParams, "personData");
        addIfPresent(queryParams, "contactUuid");
        addIfPresent(queryParams, "contactData");

        final String queryString = queryParams.isEmpty() ? "" : "?" + String.join("&", queryParams);

        return withTitle("Query Relations", () ->
                httpGet(asGlobalAgent(), "/api/hs/office/relations" + queryString)
                        .expecting(HttpStatus.OK)
                        .expecting(JSON)
        );
    }

    private void addIfPresent(final List<String> queryParams, final String name) {
        final String resolved = ScenarioTest.resolve("%{" + name + "???}", DROP_COMMENTS);
        if (!resolved.isEmpty()) {
            queryParams.add(name + "=" + uriEncoded("%{" + name + "}"));
        }
    }
}
