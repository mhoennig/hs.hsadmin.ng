package net.hostsharing.hsadminng.hs.office.scenarios.membership.coopshares;

import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;

public class CreateCoopSharesRevertTransaction extends CreateCoopSharesTransaction {

    public CreateCoopSharesRevertTransaction(final ScenarioTest testSuite) {
        super(testSuite);

        requires("CoopShares-Transaction with incorrect shareCount", alias ->
                new CreateCoopSharesSubscriptionTransaction(testSuite)
                        .given("memberNumber", "3101000")
                        .given("reference", "sign %{dateOfIncorrectTransaction}") // same as revertedShareTx
                        .given("shareCount", 100)
                        .given("comment", "coop-shares subscription transaction with wrong share count")
                        .given("transactionDate", "%{dateOfIncorrectTransaction}")
        );
    }

    @Override
    protected HttpResponse run() {
        given("transactionType", "REVERSAL");
        given("shareCount", -100);
        given("revertedShareTx", uuid("CoopShares-Transaction with incorrect shareCount"));
        return super.run();
    }
}
