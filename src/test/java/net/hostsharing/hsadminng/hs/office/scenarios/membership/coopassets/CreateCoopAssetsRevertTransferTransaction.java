package net.hostsharing.hsadminng.hs.office.scenarios.membership.coopassets;

import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static net.hostsharing.hsadminng.hs.scenarios.ScenarioTest.resolveTyped;

public class CreateCoopAssetsRevertTransferTransaction extends CreateCoopAssetsTransaction {

    public CreateCoopAssetsRevertTransferTransaction(final ScenarioTest testSuite) {
        super(testSuite);

        requires("Accidental CoopAssets-TRANSFER-Transaction", alias ->
                new CreateCoopAssetsTransferTransaction(testSuite)
                        .given("reference", "transfer %{dateOfIncorrectTransaction}")
                        .given("valueToTransfer", "%{transferredValue}")
                        .given("comment", "accidental transfer of assets from %{transferringMemberNumber} to %{adoptingMemberNumber}")
                        .given("transactionDate", "%{dateOfIncorrectTransaction}")
        );
    }

    @Override
    protected HttpResponse run() {
        given("transactionType", "REVERSAL");
        given("assetValue", "%{transferredValue}");
        given("reference", "sign %{dateOfIncorrectTransaction}"); // same text as relatedAssetTx
        given("revertedAssetTx", uuid("Accidental CoopAssets-TRANSFER-Transaction"));
        given("transactionDate", "%{dateOfIncorrectTransaction}");
        return super.run();
    }

    @Override
    protected void verify(final UseCase<CreateCoopAssetsTransaction>.HttpResponse response) {
        super.verify(response);

        final var revertedAssetTxUuid = response.getFromBody("revertedAssetTx.uuid");
        given("negativeAssetValue", resolveTyped("%{transferredValue}", BigDecimal.class).negate());

        verify("Verify Reverted Coop-Assets TRANSFER-Transaction",
                () -> httpGet("/api/hs/office/coopassetstransactions/" + revertedAssetTxUuid)
                        .expecting(HttpStatus.OK).expecting(ContentType.JSON),
                path("assetValue").contains("%{negativeAssetValue}"),
                path("comment").contains("%{comment}"),
                path("valueDate").contains("%{transactionDate}")
        );

        final var adoptionAssetTxUuid = response.getFromBody("revertedAssetTx.['adoptionAssetTx.uuid']");

        verify("Verify Related Coop-Assets ADOPTION-Transaction Also Got Reverted",
                () -> httpGet("/api/hs/office/coopassetstransactions/" + adoptionAssetTxUuid)
                        .expecting(HttpStatus.OK).expecting(ContentType.JSON),
                path("reversalAssetTx.['transferAssetTx.uuid']").contains(revertedAssetTxUuid.toString())
        );
    }
}
