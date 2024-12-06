package net.hostsharing.hsadminng.hs.office.scenarios.membership.coopassets;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;

public class CreateCoopAssetsLossTransaction extends CreateCoopAssetsTransaction {

    public CreateCoopAssetsLossTransaction(final ScenarioTest testSuite) {
        super(testSuite);
        introduction("Usually, a loss transaction goes along with a disbursal transaction.");
    }

    @Override
    protected HttpResponse run() {
        given("transactionType", "LOSS");
        given("assetValue", "-%{valueLost}");
        return super.run();
    }
}
