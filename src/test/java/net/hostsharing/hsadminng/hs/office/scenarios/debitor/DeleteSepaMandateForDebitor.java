package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import net.hostsharing.hsadminng.hs.office.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;
import org.springframework.http.HttpStatus;


public class DeleteSepaMandateForDebitor extends UseCase<DeleteSepaMandateForDebitor> {

    public DeleteSepaMandateForDebitor(final ScenarioTest testSuite) {
        super(testSuite);
    }

    @Override
    protected HttpResponse run() {
        httpDelete("/api/hs/office/sepamandates/" + uuid("SEPA-Mandate: Test AG"))
                .expecting(HttpStatus.NO_CONTENT);
        return null;
    }
}
