package net.hostsharing.hsadminng.hs.office.scenarios.membership.coopassets;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.office.scenarios.membership.CreateMembership;
import net.hostsharing.hsadminng.hs.office.scenarios.partner.CreatePartner;

import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;

public class CreateCoopAssetsTransferTransaction extends CreateCoopAssetsTransaction {

    public CreateCoopAssetsTransferTransaction(final ScenarioTest testSuite) {
        super(testSuite);

        requires("Partner: New AG", alias -> new CreatePartner(testSuite, alias)
                .given("partnerNumber", toPartnerNumber("%{adoptingMemberNumber}"))
                .given("personType", "LEGAL_PERSON")
                .given("tradeName", "New AG")
                .given("contactCaption", "New AG - Board of Directors")
                .given("emailAddress", "board-of-directors@new-ag.example.org")
        );

        requires("Membership: %{adoptingMemberNumber} - New AG", alias -> new CreateMembership(testSuite)
                .given("memberNumber", toPartnerNumber("%{adoptingMemberNumber}"))
                .given("partnerName", "New AG")
                .given("validFrom", "2024-11-15")
                .given("newStatus", "ACTIVE")
                .given("membershipFeeBillable", "true")
        );
    }

    @Override
    protected HttpResponse run() {
        introduction("Additionally to the TRANSFER, the ADOPTION is automatically booked for the receiving member.");

        given("memberNumber", "%{transferringMemberNumber}");
        given("transactionType", "TRANSFER");
        given("assetValue", "-%{valueToTransfer}");
        return super.run();
    }

    private String toPartnerNumber(final String resolvableString) {
        final var memberNumber = ScenarioTest.resolve(resolvableString, DROP_COMMENTS);
        return "P-" + memberNumber.substring("M-".length(), 7);
    }
}
