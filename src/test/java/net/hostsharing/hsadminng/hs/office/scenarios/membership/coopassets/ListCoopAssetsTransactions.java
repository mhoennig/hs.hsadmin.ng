package net.hostsharing.hsadminng.hs.office.scenarios.membership.coopassets;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;
import static org.springframework.http.HttpStatus.OK;

public class ListCoopAssetsTransactions extends UseCase<ListCoopAssetsTransactions> {

    public ListCoopAssetsTransactions(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                Whoever may see a coop-assets-transaction may also see the transactions it links to.
                Thus the listing resolves the reversal/transfer/adoption links of each visible transaction,
                even if a linked counterpart belongs to another membership.
                """);
    }

    @Override
    protected HttpResponse run() {

        obtain("#{Find }membershipUuid", () ->
                httpGet(asGlobalAgent(), "/api/hs/office/memberships/%{memberNumber}")
                        .expecting(OK).expecting(JSON),
                response -> response.getFromBody("uuid")
        );

        return withTitle("List the Coop-Assets-Transactions of %{memberNumber} in the Given Date-Range", () ->
                httpGet(asGlobalAgent(),
                        "/api/hs/office/coopassetstransactions?membershipUuid=%{membershipUuid}"
                                + "&fromValueDate=%{fromValueDate}&toValueDate=%{toValueDate}")
                        .expecting(OK).expecting(JSON)
                        .expectArrayElements(1));
    }

    @Override
    protected void verify(final HttpResponse response) {
        // the TRANSFER transaction lists its ADOPTION counterpart although that belongs to the adopting membership
        path("$[0].transactionType").contains("TRANSFER").accept(response);
        path("$[0].assetValue").contains("-%{transferredValue}").accept(response);
        path("$[0].adoptionAssetTx.transactionType").contains("ADOPTION").accept(response);
        path("$[0].adoptionAssetTx.assetValue").contains("%{transferredValue}").accept(response);
        path("$[0].adoptionAssetTx.['membership.memberNumber']").contains("%{adoptingMemberNumber}").accept(response);
        response.extractValue("$[0].uuid", "transferAssetTxUuid");

        obtain("#{Find }adoptingMembershipUuid", () ->
                httpGet(asGlobalAgent(), "/api/hs/office/memberships/%{adoptingMemberNumber}")
                        .expecting(OK).expecting(JSON),
                membershipResponse -> membershipResponse.getFromBody("uuid")
        );

        verify("Verify the Adopting Member's Listing Links Back to the TRANSFER-Transaction",
                () -> httpGet(asGlobalAgent(),
                        "/api/hs/office/coopassetstransactions?membershipUuid=%{adoptingMembershipUuid}"
                                + "&fromValueDate=%{fromValueDate}&toValueDate=%{toValueDate}")
                        .expecting(OK).expecting(JSON)
                        .expectArrayElements(1),
                path("$[0].transactionType").contains("ADOPTION"),
                path("$[0].assetValue").contains("%{transferredValue}"),
                path("$[0].transferAssetTx.uuid").contains("%{transferAssetTxUuid}"),
                path("$[0].transferAssetTx.['membership.memberNumber']").contains("%{memberNumber}")
        );
    }
}
