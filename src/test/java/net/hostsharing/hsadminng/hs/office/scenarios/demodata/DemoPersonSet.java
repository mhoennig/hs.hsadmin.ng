package net.hostsharing.hsadminng.hs.office.scenarios.demodata;

import lombok.SneakyThrows;
import lombok.val;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The optional set of fixed persons, read from a CSV in the very format that
 * tools/create-accounts-from-csv consumes, so that the same file also drives the account creation
 * after a Keycloak sync. Only the names are taken from it; everything else about these persons
 * is generated like for all others.
 */
public final class DemoPersonSet {

    static final String CSV_PATH_PROPERTY = "hsadminng.demodata.personsCsv";

    /** Staff logins get no person of their own; they share the person of the member login of the same human. */
    static final String STAFF_LOGIN_PREFIX = "hostsharing";

    /** A member prefix doubles as the debitor default prefix, which has to match '^[a-z0-9]{3}$'. */
    static final int MEMBER_PREFIX_LENGTH = 3;

    static final String OUTPUT_FILE_SUFFIX = "-with-person-uuids.csv";

    private static final String OUTPUT_HEADER = "\"subjectName\",\"subjectUuid\",\"givenName\",\"familyName\",\"personUuid\"";

    private static final Set<String> HEADER_FIRST_COLUMNS = Set.of("subjectName", "username");

    public record FixedPerson(String subjectName, String subjectUuid, String givenName, String familyName) {

        String fullName() {
            return givenName + " " + familyName;
        }

        String prefix() {
            val separator = subjectName.indexOf('-');
            return separator < 0 ? subjectName : subjectName.substring(0, separator);
        }

        boolean isStaffLogin() {
            return STAFF_LOGIN_PREFIX.equals(prefix());
        }

        /**
         * Pairs the staff login with the member login of the same human. Their given names are not
         * always spelled alike ("Marc" vs. "Marc O."), thus only the first part of it takes part.
         */
        String humanKey() {
            return (familyName + "|" + givenName.trim().split("\\s+")[0]).toLowerCase(Locale.ROOT);
        }
    }

    /** All member logins sharing a prefix belong to one customer, because that is what a prefix denotes. */
    public record FixedCustomer(String prefix, List<FixedPerson> persons) {

        boolean isNaturalPerson() {
            return prefix.length() == MEMBER_PREFIX_LENGTH;
        }

        /** Only a member prefix is short enough to serve as the debitor default prefix. */
        String defaultPrefix() {
            return isNaturalPerson() ? prefix : null;
        }

        String tradeNameCore() {
            return Character.toUpperCase(prefix.charAt(0)) + prefix.substring(1);
        }

        FixedPerson holder() {
            return persons.get(0);
        }
    }

    private DemoPersonSet() {
    }

    public static List<FixedPerson> configured() {
        val path = configuredCsvPath();
        return path == null ? List.of() : load(path);
    }

    static Path configuredCsvPath() {
        val path = System.getProperty(CSV_PATH_PROPERTY);
        return path == null || path.isBlank() ? null : Path.of(path);
    }

    /** Derived rather than configurable, so that input and output cannot drift apart. */
    static Path outputPathFor(final Path csvFile) {
        val fileName = csvFile.getFileName().toString().replaceFirst("\\.csv$", "");
        return csvFile.resolveSibling(fileName + OUTPUT_FILE_SUFFIX);
    }

    @SneakyThrows
    static List<FixedPerson> load(final Path csvFile) {
        return Files.readAllLines(csvFile, StandardCharsets.UTF_8).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(DemoPersonSet::columnsOf)
                .filter(columns -> !isHeader(columns[0]))
                .map(columns -> new FixedPerson(columns[0], columns[1], columns[2], columns[3]))
                .toList();
    }

    static List<FixedCustomer> customersOf(final List<FixedPerson> fixedPersons) {
        val byPrefix = new LinkedHashMap<String, List<FixedPerson>>();
        fixedPersons.stream()
                .filter(person -> !person.isStaffLogin())
                .forEach(person -> byPrefix.computeIfAbsent(person.prefix(), prefix -> new ArrayList<>()).add(person));
        return byPrefix.entrySet().stream()
                .map(entry -> new FixedCustomer(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
    }

    static Set<String> fullNamesOf(final List<FixedPerson> fixedPersons) {
        return fixedPersons.stream().map(FixedPerson::fullName).collect(Collectors.toSet());
    }

    /** Neither a fixed name nor a fixed trade name may be drawn, or the by-name lookups become ambiguous. */
    static Set<String> reservedNamesOf(final List<FixedPerson> fixedPersons) {
        return Stream.concat(
                        fullNamesOf(fixedPersons).stream(),
                        customersOf(fixedPersons).stream()
                                .filter(customer -> !customer.isNaturalPerson())
                                .map(FixedCustomer::tradeNameCore))
                .collect(Collectors.toSet());
    }

    /**
     * Writes the input file back with the personUuid column filled in, which is what
     * tools/create-accounts-from-csv needs and what the enrich-tooling would otherwise have to
     * look up after the restore.
     */
    @SneakyThrows
    static void writeWithPersonUuids(
            final Path targetFile,
            final List<FixedPerson> fixedPersons,
            final Map<String, String> personUuidByHumanKey) {
        val lines = Stream.concat(
                Stream.of(OUTPUT_HEADER),
                fixedPersons.stream().map(person -> csvRow(person, personUuidByHumanKey.get(person.humanKey()))));
        Files.write(targetFile, (Iterable<String>) lines::iterator, StandardCharsets.UTF_8);
    }

    private static String csvRow(final FixedPerson person, final String personUuid) {
        return Stream.of(
                        person.subjectName(), person.subjectUuid(), person.givenName(), person.familyName(),
                        personUuid == null ? "" : personUuid)
                .map(value -> "\"" + value.replace("\"", "\"\"") + "\"")
                .collect(Collectors.joining(","));
    }

    private static boolean isHeader(final String firstColumn) {
        return HEADER_FIRST_COLUMNS.stream().anyMatch(header -> header.equalsIgnoreCase(firstColumn));
    }

    private static String[] columnsOf(final String line) {
        val columns = line.split(",", -1);
        if (columns.length < 4) {
            throw new IllegalArgumentException(
                    "expected the columns subjectName, subjectUuid, givenName, familyName, personUuid but got: " + line);
        }
        for (int n = 0; n < columns.length; n++) {
            columns[n] = columns[n].trim().replaceAll("^\"|\"$", "").trim();
        }
        return columns;
    }
}
