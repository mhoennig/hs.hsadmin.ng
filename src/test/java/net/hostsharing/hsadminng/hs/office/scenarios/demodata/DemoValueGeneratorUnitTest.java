package net.hostsharing.hsadminng.hs.office.scenarios.demodata;

import lombok.val;
import net.hostsharing.hsadminng.hs.office.scenarios.demodata.WordLists.Gender;
import org.iban4j.BicUtil;
import org.iban4j.IbanUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DemoValueGeneratorUnitTest {

    private static final long SEED = 42L;

    private static final Set<String> CITIES_WITH_A_RESERVED_LOCAL_NETWORK =
            Set.of("Berlin", "Hamburg", "Frankfurt am Main", "München", "Köln");

    // spelled out from Bundesnetzagentur Mitteilung 148/2021, so that a wrong constant fails instead of agreeing
    private static final String RESERVED_FOR_MEDIA_PRODUCTIONS = "\\+49 (?:"
            + "30 23125\\d{3}|40 66969\\d{3}|69 90009\\d{3}|89 99998\\d{3}|221 4710\\d{3}"
            + "|171 39200\\d{2}|176 040690\\d{2}"
            + "|152 28817386|152 28895456|152 54599371"
            + "|172 9925904|172 9968532|172 9973185|172 9973186|172 9980752"
            + "|174 9091317|174 9464308)";

    private DemoValueGenerator generator() {
        return new DemoValueGenerator(SEED, Set.of());
    }

    @Test
    void generatesReproducibleValuesForTheSameSeed() {
        val first = IntStream.range(0, 50).mapToObj(n -> generatorSequence(new DemoValueGenerator(SEED, Set.of()))).toList();
        val second = IntStream.range(0, 50).mapToObj(n -> generatorSequence(new DemoValueGenerator(SEED, Set.of()))).toList();

        assertThat(first).isEqualTo(second);
    }

    @Test
    void generatesDifferentValuesForDifferentSeeds() {
        val fromSeed1 = generatorSequence(new DemoValueGenerator(1L, Set.of()));
        val fromSeed2 = generatorSequence(new DemoValueGenerator(2L, Set.of()));

        assertThat(fromSeed1).isNotEqualTo(fromSeed2);
    }

    @Test
    void derivesSalutationFromGender() {
        val generator = generator();

        IntStream.range(0, 200).forEach(n -> {
            val person = generator.person();
            assertThat(person.salutation()).isEqualTo(person.gender() == Gender.F ? "Frau" : "Herr");
        });
    }

    @Test
    void neverDrawsAnExcludedName() {
        val excluded = new DemoValueGenerator(SEED, Set.of()).person().fullName();
        val generator = new DemoValueGenerator(SEED, Set.of(excluded));

        IntStream.range(0, 500).forEach(n -> assertThat(generator.person().fullName()).isNotEqualTo(excluded));
    }

    @Test
    void generatesIbansAndBicsAcceptedByTheApiValidation() {
        val generator = generator();

        IntStream.range(0, 200).forEach(n -> {
            val bankAccount = generator.bankAccount("Demo Holder");
            assertThatCode(() -> IbanUtil.validate(bankAccount.iban())).doesNotThrowAnyException();
            assertThatCode(() -> BicUtil.validate(bankAccount.bic())).doesNotThrowAnyException();
        });
    }

    @Test
    void marksGeneratedBicsAsNonRoutableTestBics() {
        val generator = generator();

        IntStream.range(0, 50).forEach(n ->
                assertThat(generator.bankAccount("Demo Holder").bic()).matches("^[A-Z]{6}.0$"));
    }

    @Test
    void usesOnlyPhoneNumbersReservedForMediaProductions() {
        val generator = generator();

        IntStream.range(0, 500).forEach(n ->
                assertThat(generator.phoneNumber(generator.postalAddress().location()))
                        .matches(RESERVED_FOR_MEDIA_PRODUCTIONS));
    }

    @Test
    void usesALandlineNumberOnlyWhereItsAreaCodeMatchesTheCity() {
        val generator = generator();

        IntStream.range(0, 300).forEach(n -> {
            val location = generator.postalAddress().location();
            val phoneNumber = generator.phoneNumber(location);
            if (CITIES_WITH_A_RESERVED_LOCAL_NETWORK.contains(location.city())) {
                assertThat(phoneNumber).startsWith("+49 " + location.areaCode() + " ");
            } else {
                assertThat(phoneNumber)
                        .as("a city without a reserved local network can only get a mobile number")
                        .matches("\\+49 1\\d\\d .*");
            }
        });
    }

    @Test
    void usesHouseNumbersUnlikelyToAddressARealPerson() {
        val generator = generator();

        IntStream.range(0, 100).forEach(n -> {
            val houseNumber = generator.postalAddress().street().replaceAll("^.*\\D(?=\\d)", "");
            assertThat(Integer.parseInt(houseNumber)).isGreaterThanOrEqualTo(100);
        });
    }

    @Test
    void usesOnlyReservedEmailDomains() {
        val generator = generator();

        assertThat(generator.domainOf("Nordlicht Verlag GmbH")).isEqualTo("nordlicht-verlag-gmbh.example.org");
        assertThat(generator.emailAddress("Größenwahn Süd", "example.org")).isEqualTo("groessenwahn-sued@example.org");
    }

    @Test
    void clustersJoiningDatesAtTheStartOfTheYear() {
        val generator = generator();

        val inJanuary = IntStream.range(0, 1000)
                .mapToObj(n -> generator.joiningDate(2000, 2025))
                .filter(date -> date.getMonthValue() == 1)
                .count();

        // a uniform distribution would yield about 85 of 1000
        assertThat(inJanuary).isGreaterThan(300);
    }

    @Test
    void generatesBirthdaysWithinTheRequestedAgeRange() {
        val generator = generator();
        val today = LocalDate.of(2026, 8, 5);

        IntStream.range(0, 200).forEach(n -> {
            val birthday = generator.birthday(20, 80, today);
            val age = today.getYear() - birthday.getYear();
            assertThat(age).isBetween(20, 81);
        });
    }

    @Test
    void usesTheAddressNameFieldAsSupplementNotAsRepetition() {
        val generator = generator();

        val supplements = IntStream.range(0, 400)
                .mapToObj(n -> generator.postalAddressNameSupplement())
                .toList();

        assertThat(supplements).as("the field may well stay empty").containsNull();
        assertThat(supplements).filteredOn(java.util.Objects::nonNull)
                .as("a supplement is a department or a c/o line")
                .allSatisfy(supplement -> assertThat(supplement).satisfiesAnyOf(
                        value -> assertThat(value).startsWith("c/o "),
                        value -> assertThat(value).isIn(
                                "Buchhaltung", "Geschäftsführung", "Rechnungsstelle",
                                "Sekretariat", "Technik", "Vertrieb")));
    }

    @Test
    void providesEnoughUniqueTradeNamesForTheLargestPlannedDataset() {
        val generator = generator();

        val tradeNames = IntStream.range(0, 4000).mapToObj(n -> generator.uniqueTradeName("GmbH")).toList();

        assertThat(tradeNames).doesNotHaveDuplicates();
    }

    @Test
    void providesEnoughUniquePersonNamesForTheLargestPlannedDataset() {
        val generator = generator();

        val fullNames = IntStream.range(0, 15000).mapToObj(n -> generator.uniquePerson().fullName()).toList();

        assertThat(fullNames).doesNotHaveDuplicates();
    }

    @Test
    void composesNamesFromPrefixAndStem() {
        val generator = generator();

        val familyNames = IntStream.range(0, 400).mapToObj(n -> generator.person().familyName()).toList();

        assertThat(familyNames).as("the curated names stay the majority")
                .anySatisfy(name -> assertThat(WordLists.FAMILY_NAMES.stream().map(WordLists.Weighted::value))
                        .contains(name));
        assertThat(familyNames).as("compounds provide the long tail")
                .anySatisfy(name -> assertThat(name).matches(
                        "^(" + String.join("|", WordLists.FAMILY_NAME_PREFIXES) + ")"
                                + "(" + String.join("|", WordLists.FAMILY_NAME_STEMS) + ")$"));
    }

    @Test
    void namesOwnerManagedFirmsAfterTheirOwner() {
        val generator = generator();

        IntStream.range(0, 200).forEach(n -> {
            val firm = generator.uniqueOwnerManagedTradeName("GmbH");
            val owner = generator.uniquePersonWithFamilyName(firm.ownerFamilyName());

            assertThat(firm.tradeName())
                    .as("the firm carries the owner's surname")
                    .startsWith(firm.ownerFamilyName() + " ")
                    .endsWith(" GmbH");
            assertThat(owner.familyName()).isEqualTo(firm.ownerFamilyName());
        });
    }

    @Test
    void keepsTheBankAccountHolderWithinTheColumnLength() {
        val suffix = " (P-90123/1/aktuell)";

        val short_ = GenerateDemoDataset.bankAccountHolder("Rosenhof Elektro GmbH", suffix);
        val long_ = GenerateDemoDataset.bankAccountHolder(
                "Uhlenhorst Haustechnik und Anlagenbau UG (haftungsbeschränkt)", suffix);

        assertThat(short_).isEqualTo("Rosenhof Elektro GmbH" + suffix);
        assertThat(long_).endsWith(suffix)
                .as("an overflow of varchar(64) surfaces as HTTP 409, so it has to be cut here")
                .hasSizeLessThanOrEqualTo(GenerateDemoDataset.BANKACCOUNT_HOLDER_MAX_LENGTH);
    }

    @Test
    void namesTheContactsOfOnePartnerAfterTheirPurpose() {
        assertThat(GenerateDemoDataset.contactCaptionOf("Rosenhof Elektro GmbH", "Hauptkontakt", "P-90123"))
                .isEqualTo("Rosenhof Elektro GmbH - Hauptkontakt (P-90123)");
        assertThat(GenerateDemoDataset.contactCaptionOf("Rosenhof Elektro GmbH", "Rechnungsstelle", "D-9012301"))
                .isEqualTo("Rosenhof Elektro GmbH - Rechnungsstelle (D-9012301)");
        assertThat(GenerateDemoDataset.contactCaptionOf("Jana Schäfer", "private Mailinglisten-Adresse"))
                .isEqualTo("Jana Schäfer - private Mailinglisten-Adresse");
    }

    @Test
    void keepsTheContactCaptionWithinTheColumnLength() {
        val caption = GenerateDemoDataset.contactCaptionOf("N".repeat(200), "Rechnungsstelle", "D-9012301");

        assertThat(caption).endsWith(" - Rechnungsstelle (D-9012301)")
                .as("an overflow of varchar(128) surfaces as HTTP 409, so it has to be cut here")
                .hasSizeLessThanOrEqualTo(GenerateDemoDataset.CONTACT_CAPTION_MAX_LENGTH);
    }

    @Test
    void loadsAllWordLists() {
        assertThat(WordLists.GIVEN_NAMES).hasSizeGreaterThan(50);
        assertThat(WordLists.FAMILY_NAMES).hasSizeGreaterThan(50);
        assertThat(WordLists.LOCATIONS).hasSizeGreaterThan(20);
        assertThat(WordLists.STREETS).hasSizeGreaterThan(20);
        assertThat(WordLists.COMPANY_NAME_PARTS).hasSizeGreaterThan(10);
        assertThat(WordLists.COMPANY_BRANCH_WORDS).hasSizeGreaterThan(10);
        assertThat(WordLists.LEGAL_FORMS).hasSizeGreaterThan(5);
        assertThat(WordLists.BANKS).hasSizeGreaterThan(3);
    }

    private String generatorSequence(final DemoValueGenerator generator) {
        val person = generator.person();
        val address = generator.postalAddress();
        return String.join("|",
                person.fullName(),
                person.salutation(),
                generator.tradeName(),
                address.street(),
                address.location().city(),
                generator.phoneNumber(address.location()),
                generator.bankAccount(person.fullName()).iban(),
                generator.joiningDate(2000, 2025).toString());
    }
}
