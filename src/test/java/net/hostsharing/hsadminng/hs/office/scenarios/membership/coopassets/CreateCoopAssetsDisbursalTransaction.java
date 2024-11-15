package net.hostsharing.hsadminng.hs.office.scenarios.membership.coopassets;

import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;

public class CreateCoopAssetsDisbursalTransaction extends CreateCoopAssetsTransaction {

    public CreateCoopAssetsDisbursalTransaction(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {
        given("transactionType", "DISBURSAL");
        given("assetValue", "-%{valueToDisburse}");
        return super.run();
    }
}
