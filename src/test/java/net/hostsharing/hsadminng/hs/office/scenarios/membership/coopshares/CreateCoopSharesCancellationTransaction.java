package net.hostsharing.hsadminng.hs.office.scenarios.membership.coopshares;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;

public class CreateCoopSharesCancellationTransaction extends CreateCoopSharesTransaction {

    public CreateCoopSharesCancellationTransaction(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {
        given("transactionType", "CANCELLATION");
        given("shareCount", "-%{sharesToCancel}");
        return super.run();
    }
}
