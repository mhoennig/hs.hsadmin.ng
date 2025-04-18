package net.hostsharing.hsadminng.hs.office.scenarios.membership.coopassets;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static io.restassured.http.ContentType.JSON;
import static org.springframework.http.HttpStatus.OK;

public abstract class CreateCoopAssetsTransaction extends UseCase<CreateCoopAssetsTransaction> {

    public CreateCoopAssetsTransaction(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {

        obtain("#{Find }membershipUuid", () ->
            httpGet("/api/hs/office/memberships/%{memberNumber}")
                    .expecting(OK).expecting(JSON),
            response -> response.getFromBody("uuid")
        );

        return withTitle("Create the Coop-Assets-%{transactionType} Transaction", () ->
                httpPost("/api/hs/office/coopassetstransactions", usingJsonBody("""
                {
                    "membership.uuid": ${membershipUuid},
                    "transactionType": ${transactionType},
                    "reference": ${reference},
                    "assetValue": ${assetValue},
                    "comment": ${comment},
                    "valueDate": ${transactionDate},
                    "revertedAssetTx.uuid": ${revertedAssetTx???},
                    "adoptingMembership.memberNumber": ${adoptingMemberNumber???}
                }
                """))
                .expecting(HttpStatus.CREATED).expecting(ContentType.JSON)
        );
    }

    @Override
    protected void verify(final HttpResponse response) {
        verify("Verify Coop-Assets %{transactionType}-Transaction",
                () -> httpGet("/api/hs/office/coopassetstransactions/" + response.getLocationUuid())
                        .expecting(HttpStatus.OK).expecting(ContentType.JSON),
                path("transactionType").contains("%{transactionType}"),
                path("assetValue").contains("%{assetValue}"),
                path("comment").contains("%{comment}"),
                path("valueDate").contains("%{transactionDate}")
        );
    }
}
