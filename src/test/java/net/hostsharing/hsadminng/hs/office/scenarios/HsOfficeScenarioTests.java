package net.hostsharing.hsadminng.hs.office.scenarios;

import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.hs.office.scenarios.contact.AddPhoneNumberToContactData;
import net.hostsharing.hsadminng.hs.office.scenarios.contact.AmendContactData;
import net.hostsharing.hsadminng.hs.office.scenarios.contact.RemovePhoneNumberFromContactData;
import net.hostsharing.hsadminng.hs.office.scenarios.contact.ReplaceContactData;
import net.hostsharing.hsadminng.hs.office.scenarios.debitor.CreateExternalDebitorForPartner;
import net.hostsharing.hsadminng.hs.office.scenarios.debitor.CreateSelfDebitorForPartner;
import net.hostsharing.hsadminng.hs.office.scenarios.debitor.CreateSepaMandateForDebitor;
import net.hostsharing.hsadminng.hs.office.scenarios.debitor.DeleteDebitor;
import net.hostsharing.hsadminng.hs.office.scenarios.debitor.DontDeleteDefaultDebitor;
import net.hostsharing.hsadminng.hs.office.scenarios.debitor.FinallyDeleteSepaMandateForDebitor;
import net.hostsharing.hsadminng.hs.office.scenarios.debitor.InvalidateSepaMandateForDebitor;
import net.hostsharing.hsadminng.hs.office.scenarios.membership.CancelMembership;
import net.hostsharing.hsadminng.hs.office.scenarios.membership.CreateMembership;
import net.hostsharing.hsadminng.hs.office.scenarios.membership.coopassets.CreateCoopAssetsDepositTransaction;
import net.hostsharing.hsadminng.hs.office.scenarios.membership.coopassets.CreateCoopAssetsDisbursalTransaction;
import net.hostsharing.hsadminng.hs.office.scenarios.membership.coopassets.CreateCoopAssetsRevertSimpleTransaction;
import net.hostsharing.hsadminng.hs.office.scenarios.membership.coopassets.CreateCoopAssetsRevertTransferTransaction;
import net.hostsharing.hsadminng.hs.office.scenarios.membership.coopassets.CreateCoopAssetsTransferTransaction;
import net.hostsharing.hsadminng.hs.office.scenarios.membership.coopshares.CreateCoopSharesCancellationTransaction;
import net.hostsharing.hsadminng.hs.office.scenarios.membership.coopshares.CreateCoopSharesRevertTransaction;
import net.hostsharing.hsadminng.hs.office.scenarios.membership.coopshares.CreateCoopSharesSubscriptionTransaction;
import net.hostsharing.hsadminng.hs.office.scenarios.partner.AddOperationsContactToPartner;
import net.hostsharing.hsadminng.hs.office.scenarios.partner.AddRepresentativeToPartner;
import net.hostsharing.hsadminng.hs.office.scenarios.partner.CreatePartner;
import net.hostsharing.hsadminng.hs.office.scenarios.partner.DeletePartner;
import net.hostsharing.hsadminng.hs.office.scenarios.person.ShouldUpdatePersonData;
import net.hostsharing.hsadminng.hs.office.scenarios.subscription.RemoveOperationsContactFromPartner;
import net.hostsharing.hsadminng.hs.office.scenarios.subscription.SubscribeToMailinglist;
import net.hostsharing.hsadminng.hs.office.scenarios.subscription.UnsubscribeFromMailinglist;
import net.hostsharing.hsadminng.hs.scenarios.Produces;
import net.hostsharing.hsadminng.hs.scenarios.Requires;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.test.IgnoreOnFailureExtension;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;

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
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@ExtendWith(IgnoreOnFailureExtension.class)
class HsOfficeScenarioTests extends ScenarioTest {

    @Nested
    @Order(10)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class PartnerScenarios {

        @Test
        @Order(1010)
        @Produces(explicitly = "Partner: P-31010 - Test AG", implicitly = { "Person: Test AG", "Contact: Test AG - Hamburg" })
        void shouldCreateLegalPersonAsPartner() {
            new CreatePartner(scenarioTest)
                    .given("partnerNumber", "P-31010")
                    .given("personType", "LEGAL_PERSON")
                    .given("tradeName", "Test AG")
                    .given("contactCaption", "Test AG - Hamburg")
                    .given(
                            "postalAddress", """
                                            "firm": "Test AG",
                                            "street": "Shanghai-Allee 1",
                                            "zipcode": "20123",
                                            "city": "Hamburg",
                                            "country": "Germany"
                                    """)
                    .given("officePhoneNumber", "+49 40 654321-0")
                    .given("emailAddress", "hamburg@test-ag.example.org")
                    .doRun()
                    .keep();
        }

        @Test
        @Order(1011)
        @Produces(explicitly = "Partner: P-31011 - Michelle Matthieu",
                implicitly = { "Person: Michelle Matthieu", "Contact: Michelle Matthieu" })
        void shouldCreateNaturalPersonAsPartner() {
            new CreatePartner(scenarioTest)
                    .given("partnerNumber", "P-31011")
                    .given("personType", "NATURAL_PERSON")
                    .given("givenName", "Michelle")
                    .given("familyName", "Matthieu")
                    .given("contactCaption", "Michelle Matthieu")
                    .given(
                            "postalAddress", """
                                            "name": "Michelle Matthieu",
                                            "street": "An der Wandse 34",
                                            "zipcode": "22123",
                                            "city": "Hamburg",
                                            "country": "Germany"
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
            new AddRepresentativeToPartner(scenarioTest)
                    .given("partnerPersonTradeName", "Test AG")
                    .given("representativeFamilyName", "Trust")
                    .given("representativeGivenName", "Tracy")
                    .given(
                            "representativePostalAddress", """
                                            "name": "Michelle Matthieu",
                                            "street": "An der Alster 100",
                                            "zipcode": "20000",
                                            "city": "Hamburg",
                                            "country": "Germany"
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
            new AddOperationsContactToPartner(scenarioTest)
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
            new RemoveOperationsContactFromPartner(scenarioTest)
                    .given("operationsContactPerson", "Dennis Krause")
                    .doRun();
        }

        @Test
        @Order(1090)
        void shouldDeletePartner() {
            new DeletePartner(scenarioTest)
                    .given("partnerNumber", "P-31020")
                    .doRun();
        }
    }

    @Nested
    @Order(11)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class PartnerContactScenarios {

        @Test
        @Order(1100)
        @Requires("Partner: P-31011 - Michelle Matthieu")
        void shouldAmendContactData() {
            new AmendContactData(scenarioTest)
                    .given("partnerName", "Matthieu")
                    .given("newEmailAddress", "michelle@matthieu.example.org")
                    .doRun();
        }

        @Test
        @Order(1101)
        @Requires("Partner: P-31011 - Michelle Matthieu")
        void shouldAddPhoneNumberToContactData() {
            new AddPhoneNumberToContactData(scenarioTest)
                    .given("partnerName", "Matthieu")
                    .given("phoneNumberKeyToAdd", "mobile")
                    .given("phoneNumberToAdd", "+49 152 1234567")
                    .doRun();
        }

        @Test
        @Order(1102)
        @Requires("Partner: P-31011 - Michelle Matthieu")
        void shouldRemovePhoneNumberFromContactData() {
            new RemovePhoneNumberFromContactData(scenarioTest)
                    .given("partnerName", "Matthieu")
                    .given("phoneNumberKeyToRemove", "office")
                    .doRun();
        }

        @Test
        @Order(1103)
        @Requires("Partner: P-31010 - Test AG")
        void shouldReplaceContactData() {
            new ReplaceContactData(scenarioTest)
                    .given("partnerName", "Test AG")
                    .given("newContactCaption", "Test AG - China")
                    .given("newPostalAddress", """
                                     "firm": "Test AG",
                                     "name": "Fi Zhong-Kha",
                                     "building": "Thi Chi Koh Building",
                                     "street": "No.2 Commercial Second Street",
                                     "district": "Niushan Wei Wu",
                                     "city": "Dongguan City",
                                     "province": "Guangdong Province",
                                     "country": "China"
                            """)
                    .given("newOfficePhoneNumber", "++15 999 654321")
                    .given("newEmailAddress", "norden@test-ag.example.org")
                    .doRun();
        }
    }

    @Nested
    @Order(12)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class PartnerPersonScenarios {

        @Test
        @Order(1201)
        @Requires("Partner: P-31011 - Michelle Matthieu")
        void shouldUpdatePersonData() {
            new ShouldUpdatePersonData(scenarioTest)
                    .given("oldFamilyName", "Matthieu")
                    .given("newFamilyName", "Matthieu-Zhang")
                    .doRun();
        }
    }

    @Nested
    @Order(20)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DebitorScenarios {
        @Test
        @Order(2010)
        @Requires("Partner: P-31010 - Test AG")
        @Produces("Debitor: D-3101000 - Test AG - main debitor")
        void shouldCreateSelfDebitorForPartner() {
            new CreateSelfDebitorForPartner(scenarioTest)
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
        @Produces("Debitor: D-3101001 - Test AG - main debitor")
        void shouldCreateExternalDebitorForPartner() {
            new CreateExternalDebitorForPartner(scenarioTest)
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
        @Produces(explicitly = "Debitor: D-3101000 - Test AG - delete debitor", permanent = false)
        void shouldDeleteDebitor() {
            new DeleteDebitor(scenarioTest)
                    .given("partnerNumber", "P-31020")
                    .given("debitorSuffix", "02")
                    .doRun();
        }

        @Test
        @Order(2020)
        @Requires("Debitor: D-3101000 - Test AG - main debitor")
        @Disabled("see TODO.spec in DontDeleteDefaultDebitor")
        void shouldNotDeleteDefaultDebitor() {
            new DontDeleteDefaultDebitor(scenarioTest)
                    .given("partnerNumber", "P-31010")
                    .given("debitorSuffix", "00")
                    .doRun();
        }
    }

    @Nested
    @Order(31)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class SepaMandateScenarios {

        @Test
        @Order(3100)
        @Requires("Debitor: D-3101000 - Test AG - main debitor")
        @Produces("SEPA-Mandate: Test AG")
        void shouldCreateSepaMandateForDebitor() {
            new CreateSepaMandateForDebitor(scenarioTest)
                    // existing debitor
                    .given("debitorNumber", "D-3101000")

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
            new InvalidateSepaMandateForDebitor(scenarioTest)
                    .given("bankAccountIBAN", "DE02701500000000594937")
                    .given("mandateValidUntil", "2025-09-30")
                    .doRun();
        }

        @Test
        @Order(3109)
        @Requires("SEPA-Mandate: Test AG")
        void shouldFinallyDeleteSepaMandateForDebitor() {
            new FinallyDeleteSepaMandateForDebitor(scenarioTest)
                    .given("bankAccountIBAN", "DE02701500000000594937")
                    .doRun();
        }
    }

    @Nested
    @Order(40)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class MembershipScenarios {

        @Test
        @Order(4000)
        @Requires("Partner: P-31010 - Test AG")
        @Produces("Membership: M-3101000 - Test AG")
        void shouldCreateMembershipForPartner() {
            new CreateMembership(scenarioTest)
                    .given("partnerName", "Test AG")
                    .given("validFrom", "2024-10-15")
                    .given("newStatus", "ACTIVE")
                    .given("membershipFeeBillable", "true")
                    .doRun()
                    .keep();
        }

        @Test
        @Order(4090)
        @Requires("Membership: M-3101000 - Test AG")
        void shouldCancelMembershipOfPartner() {
            new CancelMembership(scenarioTest)
                    .given("memberNumber", "M-3101000")
                    .given("validTo", "2025-12-30")
                    .given("newStatus", "CANCELLED")
                    .doRun();
        }
    }

    @Nested
    @Order(42)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CoopSharesScenarios {

        @Test
        @Order(4201)
        @Requires("Membership: M-3101000 - Test AG")
        @Produces("Coop-Shares M-3101000 - Test AG - SUBSCRIPTION Transaction")
        void shouldSubscribeCoopShares() {
            new CreateCoopSharesSubscriptionTransaction(scenarioTest)
                    .given("memberNumber", "M-3101000")
                    .given("reference", "sign 2024-01-15")
                    .given("shareCount", 100)
                    .given("comment", "Signing the Membership")
                    .given("transactionDate", "2024-01-15")
                    .doRun();
        }

        @Test
        @Order(4202)
        @Requires("Membership: M-3101000 - Test AG")
        void shouldRevertCoopSharesSubscription() {
            new CreateCoopSharesRevertTransaction(scenarioTest)
                    .given("memberNumber", "M-3101000")
                    .given("comment", "reverting some incorrect transaction")
                    .given("dateOfIncorrectTransaction", "2024-02-15")
                    .doRun();
        }

        @Test
        @Order(4202)
        @Requires("Coop-Shares M-3101000 - Test AG - SUBSCRIPTION Transaction")
        @Produces("Coop-Shares M-3101000 - Test AG - CANCELLATION Transaction")
        void shouldCancelCoopSharesSubscription() {
            new CreateCoopSharesCancellationTransaction(scenarioTest)
                    .given("memberNumber", "M-3101000")
                    .given("reference", "cancel 2024-01-15")
                    .given("sharesToCancel", 8)
                    .given("comment", "Cancelling 8 Shares")
                    .given("transactionDate", "2024-02-15")
                    .doRun();
        }
    }

    @Nested
    @Order(43)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CoopAssetsScenarios {

        @Test
        @Order(4301)
        @Requires("Membership: M-3101000 - Test AG")
        @Produces("Coop-Assets: M-3101000 - Test AG - DEPOSIT Transaction")
        void shouldSubscribeCoopAssets() {
            new CreateCoopAssetsDepositTransaction(scenarioTest)
                    .given("memberNumber", "M-3101000")
                    .given("reference", "sign 2024-01-15")
                    .given("assetValue", 100 * 64)
                    .given("comment", "disposal for initial shares")
                    .given("transactionDate", "2024-01-15")
                    .doRun();
        }

        @Test
        @Order(4302)
        @Requires("Membership: M-3101000 - Test AG")
        void shouldRevertCoopAssetsSubscription() {
            new CreateCoopAssetsRevertSimpleTransaction(scenarioTest)
                    .given("memberNumber", "M-3101000")
                    .given("comment", "reverting some incorrect transaction")
                    .given("dateOfIncorrectTransaction", "2024-02-15")
                    .doRun();
        }

        @Test
        @Order(4303)
        @Requires("Coop-Assets: M-3101000 - Test AG - DEPOSIT Transaction")
        @Produces("Coop-Assets: M-3101000 - Test AG - DISBURSAL Transaction")
        void shouldDisburseCoopAssets() {
            new CreateCoopAssetsDisbursalTransaction(scenarioTest)
                    .given("memberNumber", "M-3101000")
                    .given("reference", "cancel 2024-01-15")
                    .given("valueToDisburse", 8 * 64)
                    .given("comment", "disbursal according to shares cancellation")
                    .given("transactionDate", "2024-02-15")
                    .doRun();
        }

        @Test
        @Order(4304)
        @Requires("Coop-Assets: M-3101000 - Test AG - DEPOSIT Transaction")
        @Produces(explicitly = "Coop-Assets: M-3101000 - Test AG - TRANSFER Transaction", implicitly = "Membership: M-4303000 - New AG")
        void shouldTransferCoopAssets() {
            new CreateCoopAssetsTransferTransaction(scenarioTest)
                    .given("transferringMemberNumber", "M-3101000")
                    .given("adoptingMemberNumber", "M-4303000")
                    .given("reference", "transfer 2024-12-31")
                    .given("valueToTransfer", 2 * 64)
                    .given("comment", "transfer assets from M-3101000 to M-4303000")
                    .given("transactionDate", "2024-12-31")
                    .doRun();
        }

        @Test
        @Order(4305)
        @Requires("Coop-Assets: M-3101000 - Test AG - TRANSFER Transaction")
        void shouldRevertCoopAssetsTransferIncludingRelatedAssetAdoption() {
            new CreateCoopAssetsRevertTransferTransaction(scenarioTest)
                    .given("transferringMemberNumber", "M-3101000")
                    .given("adoptingMemberNumber", "M-4303000")
                    .given("transferredValue", 2 * 64)
                    .given("comment", "reverting some incorrect transfer transaction")
                    .given("dateOfIncorrectTransaction", "2024-02-15")
                    .doRun();
        }
    }

    @Nested
    @Order(50)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class SubscriptionScenarios {

        @Test
        @Order(5000)
        @Requires("Person: Test AG")
        @Produces("Subscription: Michael Miller to operations-announce")
        void shouldSubscribeNewPersonAndContactToMailinglist() {
            new SubscribeToMailinglist(scenarioTest)
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
            new UnsubscribeFromMailinglist(scenarioTest)
                    .given("mailingList", "operations-announce")
                    .given("subscriberEMailAddress", "michael.miller@example.org")
                    .doRun();
        }
    }
}
