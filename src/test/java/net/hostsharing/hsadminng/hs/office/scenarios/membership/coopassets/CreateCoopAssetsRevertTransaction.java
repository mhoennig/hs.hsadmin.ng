package net.hostsharing.hsadminng.hs.office.scenarios.membership.coopassets;

import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;

public class CreateCoopAssetsRevertTransaction extends CreateCoopAssetsTransaction {

    public CreateCoopAssetsRevertTransaction(final ScenarioTest testSuite) {
        super(testSuite);

        requires("CoopAssets-Transaction with incorrect assetValue", alias ->
                new CreateCoopAssetsDepositTransaction(testSuite)
                        .given("memberNumber", "%{memberNumber}")
                        .given("reference", "sign %{dateOfIncorrectTransaction}") // same as revertedAssetTx
                        .given("assetValue", 10)
                        .given("comment", "coop-assets deposit transaction with wrong asset value")
                        .given("transactionDate", "%{dateOfIncorrectTransaction}")
        );
    }

    @Override
    protected HttpResponse run() {
        given("transactionType", "REVERSAL");
        given("assetValue", -100);
        given("revertedAssetTx", uuid("CoopAssets-Transaction with incorrect assetValue"));
        return super.run();
    }
}
