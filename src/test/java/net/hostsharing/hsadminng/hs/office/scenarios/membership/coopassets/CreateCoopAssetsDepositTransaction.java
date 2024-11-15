package net.hostsharing.hsadminng.hs.office.scenarios.membership.coopassets;

import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;

public class CreateCoopAssetsDepositTransaction extends CreateCoopAssetsTransaction {

    public CreateCoopAssetsDepositTransaction(final ScenarioTest testSuite) {
        super(testSuite);

        given("transactionType", "DEPOSIT");
    }
}
