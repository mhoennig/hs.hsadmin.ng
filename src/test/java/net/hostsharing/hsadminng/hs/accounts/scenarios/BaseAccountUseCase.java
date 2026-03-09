package net.hostsharing.hsadminng.hs.accounts.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

public abstract class BaseAccountUseCase<T extends UseCase<?>> extends UseCase<T> {

    protected final FakeLoginUser asLoginUser;

    public BaseAccountUseCase(final ScenarioTest testSuite, final FakeLoginUser asLoginUser) {
        super(testSuite);
        this.asLoginUser = asLoginUser;
    }
}
