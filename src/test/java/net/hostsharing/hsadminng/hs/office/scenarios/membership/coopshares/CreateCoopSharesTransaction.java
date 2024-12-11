package net.hostsharing.hsadminng.hs.office.scenarios.membership.coopshares;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public abstract class CreateCoopSharesTransaction extends UseCase<CreateCoopSharesTransaction> {

    public CreateCoopSharesTransaction(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {

        obtain("#{Find }membershipUuid", () ->
            httpGet("/api/hs/office/memberships/%{memberNumber}")
                    .expecting(OK).expecting(JSON),
            response -> response.getFromBody("uuid")
        );

        return withTitle("Create the Coop-Shares-%{transactionType} Transaction", () ->
                httpPost("/api/hs/office/coopsharestransactions", usingJsonBody("""
                {
                    "membership.uuid": ${membershipUuid},
                    "transactionType": ${transactionType},
                    "reference": ${reference},
                    "shareCount": ${shareCount},
                    "comment": ${comment},
                    "valueDate": ${transactionDate},
                    "revertedShareTx.uuid": ${revertedShareTx???}
                }
                """))
                .expecting(HttpStatus.CREATED).expecting(ContentType.JSON)
        );
    }

    @Override
    protected void verify(final HttpResponse response) {
        verify("Verify Coop-Shares %{transactionType}-Transaction",
                () -> httpGet("/api/hs/office/coopsharestransactions/" + response.getLocationUuid())
                        .expecting(HttpStatus.OK).expecting(ContentType.JSON),
                path("transactionType").contains("%{transactionType}"),
                path("shareCount").contains("%{shareCount}"),
                path("comment").contains("%{comment}"),
                path("valueDate").contains("%{transactionDate}")
        );
    }
}
