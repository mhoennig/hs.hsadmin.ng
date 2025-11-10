package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;
import org.springframework.http.HttpStatus;

import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.asGlobalAgent;

public class DontDeleteDefaultDebitor extends UseCase<DontDeleteDefaultDebitor> {

    public DontDeleteDefaultDebitor(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {
        httpDelete(asGlobalAgent(), "/api/hs/office/debitors/&{Debitor: Test AG - main debitor}")
                // TODO.spec: should be CONFLICT or CLIENT_ERROR for Debitor "00"  - but how to delete Partners?
                .expecting(HttpStatus.NO_CONTENT);
        return null;
    }
}
