package net.hostsharing.hsadminng.hs.office.scenarios.membership.coopassets;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;

public class CreateCoopAssetsLimitationTransaction extends CreateCoopAssetsTransaction {

    public CreateCoopAssetsLimitationTransaction(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {
        given("transactionType", "LIMITATION");
        given("assetValue", "-%{valueForLimitation}");
        return super.run();
    }
}
