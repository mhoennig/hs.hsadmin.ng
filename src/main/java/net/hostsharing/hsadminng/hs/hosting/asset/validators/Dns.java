package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.mapper.Array;
import org.apache.commons.collections4.EnumerationUtils;

import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;

public class Dns {

    public static final String[] REGISTRAR_LEVEL_DOMAINS = Array.of(
            "[^.]+", // top-level-domains
            "(co|org|gov|ac|sch)\\.uk",
            "(com|net|org|edu|gov|asn|id)\\.au",
            "(co|ne|or|ac|go)\\.jp",
            "(com|net|org|gov|edu|ac)\\.cn",
            "(com|net|org|gov|edu|mil|art)\\.br",
            "(co|net|org|gen|firm|ind)\\.in",
            "(com|net|org|gob|edu)\\.mx",
            "(gov|edu)\\.it",
            "(co|net|org|govt|ac|school|geek|kiwi)\\.nz",
            "(co|ne|or|go|re|pe)\\.kr"
    );
    public static final Pattern[] REGISTRAR_LEVEL_DOMAIN_PATTERN = stream(REGISTRAR_LEVEL_DOMAINS)
            .map(Pattern::compile)
            .toArray(Pattern[]::new);

    private final static Map<String, Result> fakeResults = new HashMap<>();

    public static Optional<String> superDomain(final String domainName) {
        final var parts = domainName.split("\\.", 2);
        if (parts.length == 2) {
            return Optional.of(parts[1]);
        }
        return Optional.empty();
    }

    public static boolean isRegistrarLevelDomain(final String domainName) {
        return stream(REGISTRAR_LEVEL_DOMAIN_PATTERN)
                .anyMatch(p -> p.matcher(domainName).matches());
    }

    /**
     * @param domainName a fully qualified domain name
     * @return true if `domainName` can be registered at a registrar, false if it's a subdomain of such or a registrar-level domain itself
     */
    public static boolean isRegistrableDomain(final String domainName) {
        return !isRegistrarLevelDomain(domainName) &&
                superDomain(domainName).map(Dns::isRegistrarLevelDomain).orElse(false);
    }

    public static void fakeResultForDomain(final String domainName, final Result fakeResult) {
        fakeResults.put(domainName, fakeResult);
    }

    public static void resetFakeResults() {
        fakeResults.clear();
    }

    public enum Status {
        SUCCESS,
        NAME_NOT_FOUND,
        INVALID_NAME,
        SERVICE_UNAVAILABLE,
        UNKNOWN_FAILURE
    }

    public record Result(Status status, List<String> records, NamingException exception) {


        public static Result fromRecords(final NamingEnumeration<?> recordEnumeration) {
            final List<String> records = recordEnumeration == null
                    ? emptyList()
                    : EnumerationUtils.toList(recordEnumeration).stream().map(Object::toString).toList();
            return new Result(Status.SUCCESS, records, null);
        }

        public static Result fromRecords(final String... records) {
            return new Result(Status.SUCCESS, stream(records).toList(), null);
        }

        public static Result fromException(final NamingException exception) {
            return switch (exception) {
                case ServiceUnavailableException exc -> new Result(Status.SERVICE_UNAVAILABLE, emptyList(), exc);
                case NameNotFoundException exc -> new Result(Status.NAME_NOT_FOUND, emptyList(), exc);
                case InvalidNameException exc -> new Result(Status.INVALID_NAME, emptyList(), exc);
                case NamingException exc -> new Result(Status.UNKNOWN_FAILURE, emptyList(), exc);
            };
        }
    }

    private final String domainName;

    public Dns(final String domainName) {
        this.domainName = domainName;
    }

    public Result fetchRecordsOfType(final String recordType) {
        if (fakeResults.containsKey(domainName)) {
            return fakeResults.get(domainName);
        }

        try {
            final var env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            final Attribute records = new InitialDirContext(env)
                    .getAttributes(domainName, new String[] { recordType })
                    .get(recordType);
            return Result.fromRecords(records != null ? records.getAll() : null);
        } catch (final NamingException exception) {
            return Result.fromException(exception);
        }
    }

    public static void main(String[] args) {
        final var result = new Dns("example.org").fetchRecordsOfType("TXT");
        System.out.println(result);
    }

}
