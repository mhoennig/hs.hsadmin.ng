package net.hostsharing.hsadminng.hs.office.scenarios.debitor;

import lombok.val;

import net.hostsharing.hsadminng.hs.scenarios.PathAssertion;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.hs.scenarios.UseCase;

import static io.restassured.http.ContentType.JSON;
import static net.hostsharing.hsadminng.hs.scenarios.TemplateResolver.Resolver.DROP_COMMENTS;
import static org.springframework.http.HttpStatus.OK;

public class ListDebitorsWithCompleteMasterData extends BillingUseCase<ListDebitorsWithCompleteMasterData> {

    public ListDebitorsWithCompleteMasterData(final ScenarioTest testSuite) {
        super(testSuite);

        introduction("""
                The billing system needs the complete debitor number, the partner person and the contact of each debitor.
                The list of debitors contains these data, thus no further request per debitor is necessary.
                """ + HINTS_FOR_BILLING + API_KEY_HINT);
    }

    static final String HINTS_FOR_BILLING = """

            The billing system needs these properties of a debitor:

            - if needed, `debitorRel.holder` (same as `partner.partnerRel.holder`), for the name of the customer.
            - `debitorRel.holder` for the name of the invoice recipient.
            - `debitorRel.contact` for the postal address and the email addresses.
              Attention: `postalAddress` can contain further properties, depending on what the back-office entered.
              All the properties in this JSON object will be joined into a single multiline text at some point after migration.
            - `debitorNumber` for the complete debitor number.
            - `billable` to determine whether an invoice gets generated at all.
            - `vatId`, `vatCountryCode`, `vatBusiness` and `vatReverseCharge`
              for the VAT calculation and the VAT data on the invoice.
              A later change renames `vatCountryCode` to `vatRegion`.
            """;

    @Override
    protected HttpResponse run() {
        return withTitle("Fetch the List of Debitors", () ->
                httpGet(asBillingSystem(), "/api/hs/office/debitors?partnerNumber=" + uriEncoded("%{partnerNumber}"))
                        .expecting(OK).expecting(JSON),
                "Without the query parameter `partnerNumber`, the list contains all debitors which are visible to the caller."
        );
    }

    @Override
    protected void verify(final UseCase<ListDebitorsWithCompleteMasterData>.HttpResponse response) {
        response.expectArrayElements(1);
        verifyDebitorAt(response, 0);
    }

    static void verifyDebitorAt(final UseCase<?>.HttpResponse response, final int index) {
        val debitor = "[" + index + "]";

        new PathAssertion(debitor + ".debitorNumber").contains("%{debitorNumber}").accept(response);

        new PathAssertion(debitor + ".partner.partnerNumber").contains("%{partnerNumber}").accept(response);
        new PathAssertion(debitor + ".partner.partnerRel.holder.personType").contains("%{partnerPersonType}").accept(response);
        new PathAssertion(debitor + ".partner.partnerRel.holder.givenName")
                .contains("%{partnerPersonGivenName}").accept(response);
        // other scenarios amend the family name, thus only its begin is stable
        response.path(debitor + ".partner.partnerRel.holder.familyName")
                .startsWith(ScenarioTest.resolve("%{partnerPersonFamilyName}", DROP_COMMENTS));

        val contact = debitor + ".debitorRel.contact";
        new PathAssertion(contact + ".caption").contains("%{contactCaption}").accept(response);
        new PathAssertion(contact + ".postalAddress.name").contains("%{contactPostalName}").accept(response);
        new PathAssertion(contact + ".postalAddress.street").contains("%{contactStreet}").accept(response);
        new PathAssertion(contact + ".postalAddress.zipcode").contains("%{contactZipcode}").accept(response);
        new PathAssertion(contact + ".postalAddress.city").contains("%{contactCity}").accept(response);
        new PathAssertion(contact + ".postalAddress.country").contains("%{contactCountry}").accept(response);
        // other scenarios amend the email address, thus only its presence is stable
        response.path(contact + ".emailAddresses.main").isNotBlank();
    }
}
