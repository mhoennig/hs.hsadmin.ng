package net.hostsharing.hsadminng.hs.office.scenarios.membership;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.hasProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

public class CreateMembership extends UseCase<CreateMembership> {

    public CreateMembership(final ScenarioTest testSuite) {
        super(testSuite);
    }

    public CreateMembership(final ScenarioTest testSuite, final String resultAlias) {
        super(testSuite, resultAlias);
    }

    @Override
    protected HttpResponse run() {
        assertThat(hasProperty("partnerNumber") ^ hasProperty("partnerName"))
                .as("exactly one of partnerNumber and partnerName has to be given")
                .isTrue();

        using("partnerAlias", hasProperty("partnerNumber") ? obtainPartnerByNumber() : obtainPartnerByName());

        return httpPost(asGlobalAgent(), "/api/hs/office/memberships", usingJsonBody("""
                {
                   "partner.uuid": ${%{partnerAlias}},
                   "memberNumberSuffix": ${%{memberNumberSuffix???}???00},
                   "status": "ACTIVE",
                   "validFrom": ${validFrom},
                   "membershipFeeBillable": ${membershipFeeBillable}
                }
                """))
                .expecting(HttpStatus.CREATED).expecting(ContentType.JSON);
    }

    /** A human user knows the partner by name and would have to look up its number first. */
    private String obtainPartnerByName() {
        final var alias = "Partner: %{partnerName}";
        obtain(alias, () ->
                httpGet(asGlobalAgent(), "/api/hs/office/partners?name=&{partnerName}")
                        .expecting(OK).expecting(JSON),
                response -> response.expectArrayElements(1).getFromBody("[0].uuid"),
                "In production data, this query could result in multiple outputs."
                        + " In that case, you have to find out which is the right one."
        );
        return alias;
    }

    /** For generated data, where a name is not necessarily unique and the number is known anyway. */
    private String obtainPartnerByNumber() {
        final var alias = "Partner: %{partnerNumber}";
        obtain(alias, () ->
                httpGet(asGlobalAgent(), "/api/hs/office/partners/%{partnerNumber}")
                        .expecting(OK).expecting(JSON),
                response -> response.getFromBody("uuid"),
                "The partner-number identifies the partner; a lookup by name could not, because a natural person"
                        + " carries its name in two columns and any name can be the prefix of a longer one."
        );
        return alias;
    }

    @Override
    protected void verify(final UseCase<CreateMembership>.HttpResponse response) {
        verify(
                "Verify That the Membership Got Created",
                () -> httpGet(asGlobalAgent(), "/api/hs/office/memberships/" + response.getLocationUuid())
                        .expecting(OK).expecting(JSON),
                path("validFrom").contains("%{validFrom}"),
                path("status").contains("ACTIVE")
        );
    }
}
