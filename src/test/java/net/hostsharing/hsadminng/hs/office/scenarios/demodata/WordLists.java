package net.hostsharing.hsadminng.hs.office.scenarios.demodata;

import lombok.SneakyThrows;
import lombok.val;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

/**
 * The curated word lists behind the demo-data generator, loaded from `src/test/resources/demodata`.
 * They are kept in the repository rather than pulled from a library, so that weighting and the
 * exclusion of names used by the fixed person set stay under our control.
 */
public final class WordLists {

    public record Weighted<T>(T value, int weight) {}

    public record Location(String zipcode, String city, String areaCode) {}

    public record Bank(String bankCode, String bic, String name) {}

    public enum Gender { F, M }

    public record GivenName(String name, Gender gender) {}

    public static final List<Weighted<GivenName>> GIVEN_NAMES = loadGivenNames();
    public static final List<Weighted<String>> FAMILY_NAMES = loadWeightedStrings("family-names.csv");
    public static final List<Weighted<Location>> LOCATIONS = loadLocations();
    public static final List<Weighted<Bank>> BANKS = loadBanks();
    public static final List<String> STREETS = loadPlainLines("streets.csv");
    public static final List<String> COMPANY_NAME_PARTS = loadCompanySection("part");
    public static final List<String> COMPANY_BRANCH_WORDS = loadCompanySection("branch");
    public static final List<Weighted<String>> LEGAL_FORMS = loadLegalForms();
    public static final List<String> COMPANY_PART_PREFIXES = loadCompanySection("partPrefix");
    public static final List<String> COMPANY_PART_STEMS = loadCompanySection("partStem");
    public static final List<String> FAMILY_NAME_PREFIXES = loadSection("family-name-compounds.csv", "prefix");
    public static final List<String> FAMILY_NAME_STEMS = loadSection("family-name-compounds.csv", "stem");

    private WordLists() {
    }

    @SneakyThrows
    private static List<String> loadPlainLines(final String resourceName) {
        try (val stream = WordLists.class.getResourceAsStream("/demodata/" + resourceName)) {
            if (stream == null) {
                throw new IllegalStateException("missing demo-data resource: " + resourceName);
            }
            try (val reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .collect(toList());
            }
        }
    }

    private static List<Weighted<String>> loadWeightedStrings(final String resourceName) {
        return loadPlainLines(resourceName).stream()
                .map(line -> line.split(",", 2))
                .map(cols -> new Weighted<>(cols[0].trim(), Integer.parseInt(cols[1].trim())))
                .collect(toList());
    }

    private static List<Weighted<GivenName>> loadGivenNames() {
        return loadPlainLines("given-names.csv").stream()
                .map(line -> line.split(",", 3))
                .map(cols -> new Weighted<>(
                        new GivenName(cols[0].trim(), Gender.valueOf(cols[1].trim())),
                        Integer.parseInt(cols[2].trim())))
                .collect(toList());
    }

    private static List<Weighted<Location>> loadLocations() {
        return loadPlainLines("locations.csv").stream()
                .map(line -> line.split(",", 4))
                .map(cols -> new Weighted<>(
                        new Location(cols[0].trim(), cols[1].trim(), cols[2].trim()),
                        Integer.parseInt(cols[3].trim())))
                .collect(toList());
    }

    private static List<Weighted<Bank>> loadBanks() {
        return loadPlainLines("banks.csv").stream()
                .map(line -> line.split(",", 4))
                .map(cols -> new Weighted<>(
                        new Bank(cols[0].trim(), cols[1].trim(), cols[2].trim()),
                        Integer.parseInt(cols[3].trim())))
                .collect(toList());
    }

    private static List<String> loadSection(final String resourceName, final String section) {
        return new ArrayList<>(sectionsOf(resourceName).getOrDefault(section, List.of()));
    }

    private static Map<String, List<String>> sectionsOf(final String resourceName) {
        return loadPlainLines(resourceName).stream()
                .map(line -> line.split(":", 2))
                .collect(groupingBy(cols -> cols[0].trim(), mapping(cols -> cols[1].trim(), toList())));
    }

    private static List<String> loadCompanySection(final String section) {
        return loadSection("company-names.csv", section);
    }

    private static List<Weighted<String>> loadLegalForms() {
        return sectionsOf("company-names.csv").getOrDefault("legalForm", List.of()).stream()
                .map(entry -> entry.split(",(?=[0-9]+$)"))
                .map(cols -> new Weighted<>(cols[0].trim(), Integer.parseInt(cols[1].trim())))
                .collect(Collectors.toList());
    }
}
