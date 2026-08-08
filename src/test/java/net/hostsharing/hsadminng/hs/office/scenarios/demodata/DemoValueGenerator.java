package net.hostsharing.hsadminng.hs.office.scenarios.demodata;

import lombok.val;
import net.hostsharing.hsadminng.hs.office.scenarios.demodata.WordLists.Gender;
import net.hostsharing.hsadminng.hs.office.scenarios.demodata.WordLists.Location;
import net.hostsharing.hsadminng.hs.office.scenarios.demodata.WordLists.Weighted;
import org.iban4j.CountryCode;
import org.iban4j.Iban;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Draws the values for the generated demo data, reproducibly for a given seed.
 * Must stay strictly sequential: java.util.Random is reproducible, ThreadLocalRandom and parallel streams are not.
 */
public class DemoValueGenerator {

    public record Person(String givenName, String familyName, Gender gender, String salutation, String title) {

        public String fullName() {
            return givenName + " " + familyName;
        }
    }

    public record PostalAddress(String street, Location location) {}

    public record BankAccount(String holder, String iban, String bic, String bankName) {}

    public record OwnerManagedTradeName(String tradeName, String ownerFamilyName) {}

    private static final List<String> TITLES = List.of("Dr.", "Prof. Dr.", "Dipl.-Ing.");

    private static final List<String> DEPARTMENTS = List.of(
            "Buchhaltung", "Geschäftsführung", "Rechnungsstelle", "Sekretariat", "Technik", "Vertrieb");

    private static final int TITLE_PERCENTAGE = 5;

    private static final int MAX_DRAW_ATTEMPTS = 10000;

    // the numbers the Bundesnetzagentur reserved for media productions, Mitteilung 148/2021:
    // five local networks with 1000 numbers each, keyed by their area code
    private static final Map<String, String> RESERVED_LANDLINE_BLOCKS = Map.of(
            "30", "23125",  // Berlin
            "40", "66969",  // Hamburg
            "69", "90009",  // Frankfurt am Main
            "89", "99998",  // München
            "221", "4710"); // Köln

    private static final int RESERVED_LANDLINE_BLOCK_SIZE = 1000;

    // the same Mitteilung, two blocks of 100 numbers each
    private static final List<String> RESERVED_MOBILE_BLOCKS = List.of("171 39200", "176 040690");

    private static final int RESERVED_MOBILE_BLOCK_SIZE = 100;

    private static final List<String> RESERVED_MOBILE_NUMBERS = List.of(
            "152 28817386", "152 28895456", "152 54599371",
            "172 9925904", "172 9968532", "172 9973185", "172 9973186", "172 9980752",
            "174 9091317", "174 9464308");

    // German names compose well, which lifts the pool from a few hundred into the ten-thousands;
    // the curated lists stay the frequent majority, the compounds are the long tail
    private static final int COMPOUND_FAMILY_NAME_PERCENTAGE = 25;
    private static final int COMPOSED_COMPANY_PART_PERCENTAGE = 60;

    // owner-managed firms typically carry the owner's surname: Schwarzmüller GmbH with Herbert Schwarzmüller
    private static final int OWNER_MANAGED_PERCENTAGE = 35;

    private final Random random;

    // names of the fixed person set must not be drawn, or the by-name lookup becomes ambiguous
    private final Set<String> excludedFullNames;

    // several use-cases look up their partner by name expecting exactly one hit, so names have to stay unique
    private final Set<String> usedNames = new HashSet<>();

    public DemoValueGenerator(final long seed, final Set<String> excludedFullNames) {
        this.random = new Random(seed);
        this.excludedFullNames = Set.copyOf(excludedFullNames);
    }

    public Person person() {
        Person candidate;
        do {
            candidate = drawPerson();
        } while (excludedFullNames.contains(candidate.fullName()));
        return candidate;
    }

    /**
     * A person of the fixed set keeps its real name. Gender and salutation stay unknown rather than
     * being guessed from the given name, which would address real people wrongly.
     */
    public Person personNamed(final String givenName, final String familyName) {
        usedNames.add(givenName + " " + familyName);
        return new Person(givenName, familyName, null, null, null);
    }

    public Person uniquePerson() {
        Person candidate;
        do {
            candidate = person();
        } while (!usedNames.add(candidate.fullName()));
        return candidate;
    }

    public String uniqueTradeName() {
        return uniqueTradeName(pick(WordLists.LEGAL_FORMS));
    }

    public String oneOfLegalForms() {
        return pick(WordLists.LEGAL_FORMS);
    }

    public boolean isOwnerManaged() {
        return percentageChance(OWNER_MANAGED_PERCENTAGE);
    }

    /**
     * An owner-managed firm named after its owner, so that a representative can carry the same surname.
     * The surname itself is the name core and therefore has to be unique among the trade names.
     */
    public OwnerManagedTradeName uniqueOwnerManagedTradeName(final String legalForm) {
        for (int attempt = 0; attempt < MAX_DRAW_ATTEMPTS; attempt++) {
            val surname = percentageChance(COMPOUND_FAMILY_NAME_PERCENTAGE)
                    ? composed(WordLists.FAMILY_NAME_PREFIXES, WordLists.FAMILY_NAME_STEMS)
                    : pick(WordLists.FAMILY_NAMES);
            val core = percentageChance(50) ? surname : surname + " " + oneOf(WordLists.COMPANY_BRANCH_WORDS);
            if (!excludedFullNames.contains(core) && usedNames.add(core)) {
                return new OwnerManagedTradeName(core + " " + legalForm, surname);
            }
        }
        throw new IllegalStateException(
                "exhausted the owner-managed trade names; extend family-name-compounds.csv for this customer count");
    }

    /** Draws a person with a given surname, for the owner of an owner-managed firm. */
    public Person uniquePersonWithFamilyName(final String familyName) {
        for (int attempt = 0; attempt < MAX_DRAW_ATTEMPTS; attempt++) {
            val givenName = pick(WordLists.GIVEN_NAMES);
            val candidate = new Person(
                    givenName.name(), familyName, givenName.gender(),
                    givenName.gender() == Gender.F ? "Frau" : "Herr",
                    percentageChance(TITLE_PERCENTAGE) ? oneOf(TITLES) : null);
            if (!excludedFullNames.contains(candidate.fullName()) && usedNames.add(candidate.fullName())) {
                return candidate;
            }
        }
        throw new IllegalStateException("exhausted the given names for family name: " + familyName);
    }

    /**
     * Uniqueness has to hold for the name core without the legal form, because the partner lookup
     * matches name fragments: "X Y GmbH" would also be found when searching for "X Y GmbH &amp; Co. KG".
     */
    public String uniqueTradeName(final String legalForm) {
        for (int attempt = 0; attempt < MAX_DRAW_ATTEMPTS; attempt++) {
            val part = percentageChance(COMPOSED_COMPANY_PART_PERCENTAGE)
                    ? composed(WordLists.COMPANY_PART_PREFIXES, WordLists.COMPANY_PART_STEMS)
                    : oneOf(WordLists.COMPANY_NAME_PARTS);
            val core = part + " " + oneOf(WordLists.COMPANY_BRANCH_WORDS);
            if (!excludedFullNames.contains(core) && usedNames.add(core)) {
                return core + " " + legalForm;
            }
        }
        throw new IllegalStateException(
                "exhausted the trade-name combinations; extend company-names.csv for this customer count");
    }

    private Person drawPerson() {
        val givenName = pick(WordLists.GIVEN_NAMES);
        val familyName = percentageChance(COMPOUND_FAMILY_NAME_PERCENTAGE)
                ? composed(WordLists.FAMILY_NAME_PREFIXES, WordLists.FAMILY_NAME_STEMS)
                : pick(WordLists.FAMILY_NAMES);
        val salutation = givenName.gender() == Gender.F ? "Frau" : "Herr";
        val title = percentageChance(TITLE_PERCENTAGE) ? TITLES.get(random.nextInt(TITLES.size())) : null;
        return new Person(givenName.name(), familyName, givenName.gender(), salutation, title);
    }

    public String tradeName() {
        val part = percentageChance(COMPOSED_COMPANY_PART_PERCENTAGE)
                ? composed(WordLists.COMPANY_PART_PREFIXES, WordLists.COMPANY_PART_STEMS)
                : oneOf(WordLists.COMPANY_NAME_PARTS);
        return part + " " + oneOf(WordLists.COMPANY_BRANCH_WORDS) + " " + pick(WordLists.LEGAL_FORMS);
    }

    /** Composes a name from a prefix and a stem, as German names do: Schwarz + müller, Alster + blick. */
    private String composed(final List<String> prefixes, final List<String> stems) {
        return oneOf(prefixes) + oneOf(stems);
    }

    public PostalAddress postalAddress() {
        val street = WordLists.STREETS.get(random.nextInt(WordLists.STREETS.size()));
        // high house numbers keep the street/city combination unlikely to address a real person
        return new PostalAddress(street + " " + (100 + random.nextInt(800)), pick(WordLists.LOCATIONS));
    }

    /**
     * The name field of a postal address is an addition such as a department or a c/o line,
     * never a repetition of the person or company the address belongs to; it may well stay empty.
     */
    public String postalAddressNameSupplement() {
        if (percentageChance(55)) {
            return null;
        }
        if (percentageChance(20)) {
            return "c/o " + person().fullName();
        }
        return oneOf(DEPARTMENTS);
    }

    /**
     * Only reserved numbers, so that no generated contact can ring at a real subscriber.
     * A city outside the five reserved local networks gets a mobile number, which carries no location
     * and therefore cannot contradict the address; a foreign area code would.
     */
    public String phoneNumber(final Location location) {
        val block = RESERVED_LANDLINE_BLOCKS.get(location.areaCode());
        return block == null
                ? reservedMobileNumber()
                : "+49 " + location.areaCode() + " " + block
                        + String.format("%03d", random.nextInt(RESERVED_LANDLINE_BLOCK_SIZE));
    }

    /** Draws over the numbers, not over the entries, so that a block does not outweigh a single number. */
    private String reservedMobileNumber() {
        val fromBlocks = RESERVED_MOBILE_BLOCKS.size() * RESERVED_MOBILE_BLOCK_SIZE;
        val drawn = random.nextInt(fromBlocks + RESERVED_MOBILE_NUMBERS.size());
        return "+49 " + (drawn < fromBlocks
                ? RESERVED_MOBILE_BLOCKS.get(drawn / RESERVED_MOBILE_BLOCK_SIZE)
                        + String.format("%02d", drawn % RESERVED_MOBILE_BLOCK_SIZE)
                : RESERVED_MOBILE_NUMBERS.get(drawn - fromBlocks));
    }

    public String emailAddress(final String localPart, final String domain) {
        return slug(localPart) + "@" + domain;
    }

    public String domainOf(final String name) {
        return slug(name) + ".example.org";
    }

    public BankAccount bankAccount(final String holder) {
        val bank = pick(WordLists.BANKS);
        val accountNumber = String.format("%010d", (long) random.nextInt(1_000_000_000));
        val iban = new Iban.Builder()
                .countryCode(CountryCode.DE)
                .bankCode(bank.bankCode())
                .accountNumber(accountNumber)
                .build();
        return new BankAccount(holder, iban.toString(), bank.bic(), bank.name());
    }

    public LocalDate birthday(final int minAge, final int maxAge, final LocalDate today) {
        val age = minAge + random.nextInt(maxAge - minAge + 1);
        return today.minusYears(age).minusDays(random.nextInt(365));
    }

    /** Joining dates cluster at the start of a year, as they do in reality. */
    public LocalDate joiningDate(final int firstYear, final int lastYear) {
        val year = firstYear + random.nextInt(lastYear - firstYear + 1);
        val dayOfYear = percentageChance(40) ? 1 + random.nextInt(31) : 1 + random.nextInt(365);
        return LocalDate.ofYearDay(year, dayOfYear);
    }

    public boolean percentageChance(final int percentage) {
        return random.nextInt(100) < percentage;
    }

    public int intBetween(final int minInclusive, final int maxInclusive) {
        return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
    }

    public <T> T oneOf(final List<T> values) {
        return values.get(random.nextInt(values.size()));
    }

    public <T> T pick(final List<Weighted<T>> weightedValues) {
        val total = weightedValues.stream().mapToInt(Weighted::weight).sum();
        var remaining = random.nextInt(total);
        for (val candidate : weightedValues) {
            remaining -= candidate.weight();
            if (remaining < 0) {
                return candidate.value();
            }
        }
        return weightedValues.get(weightedValues.size() - 1).value();
    }

    static String slug(final String name) {
        return name.toLowerCase(Locale.ROOT)
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
