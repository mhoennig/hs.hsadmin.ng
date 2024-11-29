package net.hostsharing.hsadminng.hs.office.scenarios.membership.coopshares;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;

public class CreateCoopSharesSubscriptionTransaction extends CreateCoopSharesTransaction {

    public CreateCoopSharesSubscriptionTransaction(final ScenarioTest testSuite) {
        super(testSuite);

        given("transactionType", "SUBSCRIPTION");
    }
}
