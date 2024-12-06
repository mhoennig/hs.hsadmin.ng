package net.hostsharing.hsadminng.hs.office.scenarios.membership.coopassets;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;

public class CreateCoopAssetsClearingTransaction extends CreateCoopAssetsTransaction {

    public CreateCoopAssetsClearingTransaction(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {
        given("transactionType", "CLEARING");
        given("assetValue", "-%{valueToClear}");
        return super.run();
    }
}
