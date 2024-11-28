package net.hostsharing.hsadminng.hs.office.scenarios.membership.coopassets;

import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;

public class CreateCoopAssetsRevertSimpleTransaction extends CreateCoopAssetsTransaction {

    public CreateCoopAssetsRevertSimpleTransaction(final ScenarioTest testSuite) {
        super(testSuite);

        requires("CoopAssets-Transaction with incorrect assetValue", alias ->
                new CreateCoopAssetsDepositTransaction(testSuite)
                        .given("memberNumber", "%{memberNumber}")
                        .given("reference", "sign %{dateOfIncorrectTransaction}") // same text as relatedAssetTx
                        .given("assetValue", 10)
                        .given("comment", "coop-assets deposit transaction with wrong asset value")
                        .given("transactionDate", "%{dateOfIncorrectTransaction}")
        );
    }

    @Override
    protected HttpResponse run() {
        given("transactionType", "REVERSAL");
        given("assetValue", -10);
        given("reference", "sign %{dateOfIncorrectTransaction}"); // same text as relatedAssetTx
        given("revertedAssetTx", uuid("CoopAssets-Transaction with incorrect assetValue"));
        given("transactionDate", "%{dateOfIncorrectTransaction}");
        return super.run();
    }
}
