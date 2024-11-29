package net.hostsharing.hsadminng.hs.scenarios;

import static org.assertj.core.api.Assumptions.assumeThat;

public class UseCaseNotImplementedYet extends UseCase<UseCaseNotImplementedYet> {

    public UseCaseNotImplementedYet(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {
        assumeThat(false).isTrue(); // makes the test gray
        return null;
    }
}
