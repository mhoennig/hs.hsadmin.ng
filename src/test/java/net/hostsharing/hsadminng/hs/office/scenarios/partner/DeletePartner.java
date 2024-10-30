package net.hostsharing.hsadminng.hs.office.scenarios.partner;

import net.hostsharing.hsadminng.hs.office.scenarios.UseCase;
import net.hostsharing.hsadminng.hs.office.scenarios.ScenarioTest;
import org.springframework.http.HttpStatus;

public class DeletePartner extends UseCase<DeletePartner> {

    public DeletePartner(final ScenarioTest testSuite) {
        super(testSuite);

        requires("Partner: Delete AG", alias -> new CreatePartner(testSuite, alias)
                .given("personType", "LEGAL_PERSON")
                .given("tradeName", "Delete AG")
                .given("contactCaption", "Delete AG - Board of Directors")
                .given("emailAddress",  "board-of-directors@delete-ag.example.org"));
    }

    @Override
    protected HttpResponse run() {
        httpDelete("/api/hs/office/partners/" + uuid("Partner: Delete AG"))
                .expecting(HttpStatus.NO_CONTENT);
        return null;
    }
}
