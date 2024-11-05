package net.hostsharing.hsadminng.hs.office.scenarios;

import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.hs.office.scenarios.contact.AddPhoneNumberToContactData;
import net.hostsharing.hsadminng.hs.office.scenarios.contact.AmendContactData;
import net.hostsharing.hsadminng.hs.office.scenarios.contact.RemovePhoneNumberFromContactData;
import net.hostsharing.hsadminng.hs.office.scenarios.contact.ReplaceContactData;
import net.hostsharing.hsadminng.hs.office.scenarios.debitor.CreateExternalDebitorForPartner;
import net.hostsharing.hsadminng.hs.office.scenarios.debitor.CreateSelfDebitorForPartner;
import net.hostsharing.hsadminng.hs.office.scenarios.debitor.CreateSepaMandateForDebitor;
import net.hostsharing.hsadminng.hs.office.scenarios.debitor.FinallyDeleteSepaMandateForDebitor;
import net.hostsharing.hsadminng.hs.office.scenarios.debitor.DontDeleteDefaultDebitor;
import net.hostsharing.hsadminng.hs.office.scenarios.debitor.InvalidateSepaMandateForDebitor;
import net.hostsharing.hsadminng.hs.office.scenarios.membership.CreateMembership;
import net.hostsharing.hsadminng.hs.office.scenarios.partner.AddOperationsContactToPartner;
import net.hostsharing.hsadminng.hs.office.scenarios.partner.CreatePartner;
import net.hostsharing.hsadminng.hs.office.scenarios.debitor.DeleteDebitor;
import net.hostsharing.hsadminng.hs.office.scenarios.partner.DeletePartner;
import net.hostsharing.hsadminng.hs.office.scenarios.partner.AddRepresentativeToPartner;
import net.hostsharing.hsadminng.hs.office.scenarios.subscription.RemoveOperationsContactFromPartner;
import net.hostsharing.hsadminng.hs.office.scenarios.subscription.SubscribeToMailinglist;
import net.hostsharing.hsadminng.hs.office.scenarios.subscription.UnsubscribeFromMailinglist;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@Tag("scenarioTest")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class },
        properties = {
                "spring.datasource.url=${HSADMINNG_POSTGRES_JDBC_URL:jdbc:tc:postgresql:15.5-bookworm:///scenariosTC}",
                "spring.datasource.username=${HSADMINNG_POSTGRES_ADMIN_USERNAME:ADMIN}",
                "spring.datasource.password=${HSADMINNG_POSTGRES_ADMIN_PASSWORD:password}",
                "hsadminng.superuser=${HSADMINNG_SUPERUSER:superuser-alex@hostsharing.net}"
        }
)
@DirtiesContext
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HsOfficeScenarioTests extends ScenarioTest {

    @Test
    @Order(1010)
    @Produces(explicitly = "Partner: Test AG", implicitly = {"Person: Test AG", "Contact: Test AG - Hamburg"})
    void shouldCreateLegalPersonAsPartner() {
        new CreatePartner(this)
                .given("partnerNumber", 31010)
                .given("personType", "LEGAL_PERSON")
                .given("tradeName", "Test AG")
                .given("contactCaption", "Test AG - Hamburg")
                .given("postalAddress", """
                        Shanghai-Allee 1
                        20123 Hamburg
                        """)
                .given("officePhoneNumber", "+49 40 654321-0")
                .given("emailAddress", "hamburg@test-ag.example.org")
                .doRun()
                .keep();
    }

    @Test
    @Order(1011)
    @Produces(explicitly = "Partner: Michelle Matthieu", implicitly = {"Person: Michelle Matthieu", "Contact: Michelle Matthieu"})
    void shouldCreateNaturalPersonAsPartner() {
        new CreatePartner(this)
                .given("partnerNumber", 31011)
                .given("personType", "NATURAL_PERSON")
                .given("givenName", "Michelle")
                .given("familyName", "Matthieu")
                .given("contactCaption", "Michelle Matthieu")
                .given("postalAddress", """
                        An der Wandse 34
                        22123 Hamburg
                        """)
                .given("officePhoneNumber", "+49 40 123456")
                .given("emailAddress", "michelle.matthieu@example.org")
                .doRun()
                .keep();
    }

    @Test
    @Order(1020)
    @Requires("Person: Test AG")
    @Produces("Representative: Tracy Trust for Test AG")
    void shouldAddRepresentativeToPartner() {
        new AddRepresentativeToPartner(this)
                .given("partnerPersonTradeName", "Test AG")
                .given("representativeFamilyName", "Trust")
                .given("representativeGivenName", "Tracy")
                .given("representativePostalAddress", """
                        An der Alster 100
                        20000 Hamburg
                        """)
                .given("representativePhoneNumber", "+49 40 123456")
                .given("representativeEMailAddress", "tracy.trust@example.org")
                .doRun()
                .keep();
    }

    @Test
    @Order(1030)
    @Requires("Person: Test AG")
    @Produces("Operations-Contact: Dennis Krause for Test AG")
    void shouldAddOperationsContactToPartner() {
        new AddOperationsContactToPartner(this)
                .given("partnerPersonTradeName", "Test AG")
                .given("operationsContactFamilyName", "Krause")
                .given("operationsContactGivenName", "Dennis")
                .given("operationsContactPhoneNumber", "+49 9932 587741")
                .given("operationsContactEMailAddress", "dennis.krause@example.org")
                .doRun()
                .keep();
    }

    @Test
    @Order(1039)
    @Requires("Operations-Contact: Dennis Krause for Test AG")
    void shouldRemoveOperationsContactFromPartner() {
        new RemoveOperationsContactFromPartner(this)
                .given("operationsContactPerson", "Dennis Krause")
                .doRun();
    }

    @Test
    @Order(1090)
    void shouldDeletePartner() {
        new DeletePartner(this)
                .given("partnerNumber", 31020)
                .doRun();
    }

    @Test
    @Order(1100)
    @Requires("Partner: Michelle Matthieu")
    void shouldAmendContactData() {
        new AmendContactData(this)
                .given("partnerName", "Matthieu")
                .given("newEmailAddress", "michelle@matthieu.example.org")
                .doRun();
    }

    @Test
    @Order(1101)
    @Requires("Partner: Michelle Matthieu")
    void shouldAddPhoneNumberToContactData() {
        new AddPhoneNumberToContactData(this)
                .given("partnerName", "Matthieu")
                .given("phoneNumberKeyToAdd", "mobile")
                .given("phoneNumberToAdd", "+49 152 1234567")
                .doRun();
    }

    @Test
    @Order(1102)
    @Requires("Partner: Michelle Matthieu")
    void shouldRemovePhoneNumberFromContactData() {
        new RemovePhoneNumberFromContactData(this)
                .given("partnerName", "Matthieu")
                .given("phoneNumberKeyToRemove", "office")
                .doRun();
    }

    @Test
    @Order(1103)
    @Requires("Partner: Test AG")
    void shouldReplaceContactData() {
        new ReplaceContactData(this)
                .given("partnerName", "Test AG")
                .given("newContactCaption", "Test AG - Norden")
                .given("newPostalAddress", """
                        Am Hafen 11
                        26506 Norden
                        """)
                .given("newOfficePhoneNumber", "+49 4931 654321-0")
                .given("newEmailAddress", "norden@test-ag.example.org")
                .doRun();
    }

    @Test
    @Order(2010)
    @Requires("Partner: Test AG")
    @Produces("Debitor: Test AG - main debitor")
    void shouldCreateSelfDebitorForPartner() {
        new CreateSelfDebitorForPartner(this, "Debitor: Test AG - main debitor")
                .given("partnerPersonTradeName", "Test AG")
                .given("billingContactCaption", "Test AG - billing department")
                .given("billingContactEmailAddress", "billing@test-ag.example.org")
                .given("debitorNumberSuffix", "00") // TODO.impl: could be assigned automatically, but is not yet
                .given("billable", true)
                .given("vatId", "VAT123456")
                .given("vatCountryCode", "DE")
                .given("vatBusiness", true)
                .given("vatReverseCharge", false)
                .given("defaultPrefix", "tst")
                .doRun()
                .keep();
    }

    @Test
    @Order(2011)
    @Requires("Person: Test AG")
    @Produces("Debitor: Billing GmbH")
    void shouldCreateExternalDebitorForPartner() {
        new CreateExternalDebitorForPartner(this)
                .given("partnerPersonTradeName", "Test AG")
                .given("billingContactCaption", "Billing GmbH - billing department")
                .given("billingContactEmailAddress", "billing@test-ag.example.org")
                .given("debitorNumberSuffix", "01")
                .given("billable", true)
                .given("vatId", "VAT123456")
                .given("vatCountryCode", "DE")
                .given("vatBusiness", true)
                .given("vatReverseCharge", false)
                .given("defaultPrefix", "tsx")
                .doRun()
                .keep();
    }

    @Test
    @Order(2020)
    @Requires("Person: Test AG")
    void shouldDeleteDebitor() {
        new DeleteDebitor(this)
                .given("partnerNumber", 31020)
                .given("debitorSuffix", "02")
                .doRun();
    }

    @Test
    @Order(2020)
    @Requires("Debitor: Test AG - main debitor")
    @Disabled("see TODO.spec in DontDeleteDefaultDebitor")
    void shouldNotDeleteDefaultDebitor() {
        new DontDeleteDefaultDebitor(this)
                .given("partnerNumber", 31020)
                .given("debitorSuffix", "00")
                .doRun();
    }

    @Test
    @Order(3100)
    @Requires("Debitor: Test AG - main debitor")
    @Produces("SEPA-Mandate: Test AG")
    void shouldCreateSepaMandateForDebitor() {
        new CreateSepaMandateForDebitor(this)
                // existing debitor
                .given("debitorNumber", "3101000")

                // new sepa-mandate
                .given("mandateReference", "Test AG - main debitor")
                .given("mandateAgreement", "2022-10-12")
                .given("mandateValidFrom", "2024-10-15")

                // new bank-account
                .given("bankAccountHolder", "Test AG - debit bank account")
                .given("bankAccountIBAN", "DE02701500000000594937")
                .given("bankAccountBIC", "SSKMDEMM")
                .doRun()
                .keep();
    }

    @Test
    @Order(3108)
    @Requires("SEPA-Mandate: Test AG")
    void shouldInvalidateSepaMandateForDebitor() {
        new InvalidateSepaMandateForDebitor(this)
                .given("bankAccountIBAN", "DE02701500000000594937")
                .given("mandateValidUntil", "2025-09-30")
                .doRun();
    }

    @Test
    @Order(3109)
    @Requires("SEPA-Mandate: Test AG")
    void shouldFinallyDeleteSepaMandateForDebitor() {
        new FinallyDeleteSepaMandateForDebitor(this)
                .given("bankAccountIBAN", "DE02701500000000594937")
                .doRun();
    }

    @Test
    @Order(4000)
    @Requires("Partner: Test AG")
    void shouldCreateMembershipForPartner() {
        new CreateMembership(this)
                .given("partnerName", "Test AG")
                .given("memberNumberSuffix", "00")
                .given("validFrom", "2024-10-15")
                .given("membershipFeeBillable", "true")
                .doRun();
    }

    @Test
    @Order(5000)
    @Requires("Person: Test AG")
    @Produces("Subscription: Michael Miller to operations-announce")
    void shouldSubscribeNewPersonAndContactToMailinglist() {
        new SubscribeToMailinglist(this)
                // TODO.spec: do we need the personType? or is an operational contact always a natural person? what about distribution lists?
                .given("partnerPersonTradeName", "Test AG")
                .given("subscriberFamilyName", "Miller")
                .given("subscriberGivenName", "Michael")
                .given("subscriberEMailAddress", "michael.miller@example.org")
                .given("mailingList", "operations-announce")
                .doRun()
                .keep();
    }

    @Test
    @Order(5001)
    @Requires("Subscription: Michael Miller to operations-announce")
    void shouldUnsubscribeNewPersonAndContactToMailinglist() {
        new UnsubscribeFromMailinglist(this)
                .given("mailingList", "operations-announce")
                .given("subscriberEMailAddress", "michael.miller@example.org")
                .doRun();
    }
}
