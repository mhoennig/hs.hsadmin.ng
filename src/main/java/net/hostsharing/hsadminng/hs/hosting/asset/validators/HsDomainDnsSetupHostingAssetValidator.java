package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import lombok.SneakyThrows;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.system.SystemProcess;

import java.util.List;
import java.util.regex.Pattern;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_DNS_SETUP;
import static net.hostsharing.hsadminng.hs.validation.ArrayProperty.arrayOf;
import static net.hostsharing.hsadminng.hs.validation.BooleanProperty.booleanProperty;
import static net.hostsharing.hsadminng.hs.validation.IntegerProperty.integerProperty;
import static net.hostsharing.hsadminng.hs.validation.StringProperty.stringProperty;

// TODO.legacy: make package private once we've migrated the legacy data
public class HsDomainDnsSetupHostingAssetValidator extends HostingAssetEntityValidator {

    // according to  RFC 1035 (section 5) and RFC 1034
    static final String RR_REGEX_NAME = "(\\*\\.)?([a-zA-Z0-9\\._-]+|@)[ \t]+";
    static final String RR_REGEX_TTL = "(([1-9][0-9]*[mMhHdDwW]?)+[ \t]+)?";
    static final String RR_REGEX_IN = "[iI][nN][ \t]+"; // record class IN for Internet
    static final String RR_RECORD_TYPE = "[a-zA-Z]+[ \t]+";
    static final String RR_RECORD_DATA = "(([^;]+)|(\".*\")|(\\(.*\\)))[ \t]*";
    static final String RR_COMMENT = "(;.*)?";

    static final String RR_REGEX_TTL_IN =
            RR_REGEX_NAME + RR_REGEX_TTL + RR_REGEX_IN + RR_RECORD_TYPE + RR_RECORD_DATA + RR_COMMENT;

    static final String RR_REGEX_IN_TTL =
            RR_REGEX_NAME  + RR_REGEX_IN + RR_REGEX_TTL + RR_RECORD_TYPE + RR_RECORD_DATA + RR_COMMENT;
    public static final String IDENTIFIER_SUFFIX = "|DNS";

    private static List<String> zoneFileErrors = null; // TODO.legacy: remove once legacy data is migrated

    HsDomainDnsSetupHostingAssetValidator() {
        super(
                DOMAIN_DNS_SETUP,
                AlarmContact.isOptional(),

                integerProperty("TTL").min(0).withDefault(21600),
                booleanProperty("auto-SOA").withDefault(true),
                booleanProperty("auto-NS-RR").withDefault(true),
                booleanProperty("auto-MX-RR").withDefault(true),
                booleanProperty("auto-A-RR").withDefault(true),
                booleanProperty("auto-AAAA-RR").withDefault(true),
                booleanProperty("auto-MAILSERVICES-RR").withDefault(true),
                booleanProperty("auto-AUTOCONFIG-RR").withDefault(true),
                booleanProperty("auto-AUTODISCOVER-RR").withDefault(true),
                booleanProperty("auto-DKIM-RR").withDefault(true),
                booleanProperty("auto-SPF-RR").withDefault(true),
                booleanProperty("auto-WILDCARD-MX-RR").withDefault(true),
                booleanProperty("auto-WILDCARD-A-RR").withDefault(true),
                booleanProperty("auto-WILDCARD-AAAA-RR").withDefault(true),
                booleanProperty("auto-WILDCARD-SPF-RR").withDefault(true),
                arrayOf(
                        stringProperty("user-RR").matchesRegEx(RR_REGEX_TTL_IN, RR_REGEX_IN_TTL).required()
                ).optional());
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAsset assetEntity) {
        return Pattern.compile("^" + Pattern.quote(assetEntity.getParentAsset().getIdentifier() + IDENTIFIER_SUFFIX) + "$");
    }

    @Override
    public void preprocessEntity(final HsHostingAsset entity) {
        super.preprocessEntity(entity);
        if (entity.getIdentifier() == null) {
            ofNullable(entity.getParentAsset()).ifPresent(pa -> entity.setIdentifier(pa.getIdentifier() + IDENTIFIER_SUFFIX));
        }
    }

    @Override
    @SneakyThrows
    public List<String> validateContext(final HsHostingAsset assetEntity) {
        final var result = super.validateContext(assetEntity);

        // TODO.spec: define which checks should get raised to error level
        final var namedCheckZone = new SystemProcess("named-checkzone", fqdn(assetEntity));
        final var zonefileString = toZonefileString(assetEntity);
        final var zoneFileErrorResult = zoneFileErrors != null ? zoneFileErrors : result;
        if (namedCheckZone.execute(zonefileString) != 0) {
            // yes, named-checkzone writes error messages to stdout, not stderr
            stream(namedCheckZone.getStdOut().split("\n"))
                    .map(line -> line.replaceAll(" stream-0x[0-9a-f]+:", "line "))
                    .map(line -> "[" + assetEntity.getIdentifier() + "] " + line)
                    .forEach(zoneFileErrorResult::add);
            if (!namedCheckZone.getStdErr().isEmpty()) {
                result.add("unexpected stderr output for " + namedCheckZone.getCommand() + ": " + namedCheckZone.getStdErr());
            }
        }
        return result;
    }

    String toZonefileString(final HsHostingAsset assetEntity) {
        // TODO.spec: we need to expand the templates (auto-...) in the same way as in Saltstack, with proper IP-numbers etc.
        // TODO.impl: auto-AUTOCONFIG-RR auto-AUTODISCOVER-RR missing
        return """
                $TTL {ttl}

                {auto-SOA}
                {auto-NS-RR}
                {auto-MX-RR}
                {auto-A-RR}
                {auto-AAAA-RR}
                {auto-DKIM-RR}
                {auto-SPF-RR}
                
                {auto-WILDCARD-MX-RR}
                {auto-WILDCARD-A-RR}
                {auto-WILDCARD-AAAA-RR}
                {auto-WILDCARD-SPF-RR}
                
                {userRRs}
                """
                .replace("{ttl}", assetEntity.getDirectValue("TTL", Integer.class, 43200).toString())
                .replace("{auto-SOA}", assetEntity.getDirectValue("auto-SOA", Boolean.class, false).equals(true)
                        ? """
                            {domain}.   IN     SOA h00.hostsharing.net. hostmaster.hostsharing.net. (
                                                            1303649373      ; serial secs since Jan 1 1970
                                                                    6H      ; refresh (>=10000)
                                                                    1H      ; retry (>=1800)
                                                                    1W      ; expire
                                                                    1H      ; minimum
                                                            )
                            """
                        : "; no auto-SOA"
                )
                .replace("{auto-NS-RR}", assetEntity.getDirectValue("auto-NS-RR", Boolean.class, true)
                        ? """
                            {domain}.      IN      NS      dns1.hostsharing.net.
                            {domain}.      IN      NS      dns2.hostsharing.net.
                            {domain}.      IN      NS      dns3.hostsharing.net.
                            """
                        : "; no auto-NS-RR")
                .replace("{auto-MX-RR}", assetEntity.getDirectValue("auto-MX-RR", Boolean.class, true)
                        ?   """
                            {domain}.      IN      MX      30 mailin1.hostsharing.net.
                            {domain}.      IN      MX      30 mailin2.hostsharing.net.
                            {domain}.      IN      MX      30 mailin3.hostsharing.net.
                            """
                        : "; no auto-MX-RR")
                .replace("{auto-A-RR}",  assetEntity.getDirectValue("auto-A-RR", Boolean.class, true)
                        ? "{domain}.      IN  A       83.223.95.160" // arbitrary IP-number
                        : "; no auto-A-RR")
                .replace("{auto-AAAA-RR}",  assetEntity.getDirectValue("auto-AAA-RR", Boolean.class, true)
                        ? "{domain}.      IN  AAAA    2a01:37:1000::53df:5fa0:0" // arbitrary IP-number
                        : "; no auto-AAAA-RR")
                .replace("{auto-DKIM-RR}", assetEntity.getDirectValue("auto-DKIM-RR", Boolean.class, true)
                        ? "default._domainkey 21600 IN TXT \"v=DKIM1; h=sha256; k=rsa; s=email; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCmdM9d15bqe94zbHVcKKpUF875XoCWHKRap/sG3NJZ9xZ/BjfGXmqoEYeFNpX3CB7pOXhH5naq4N+6gTjArTviAiVThHXyebhrxaf1dVS4IUC6raTEyQrWPZUf7ZxXmcCYvOdV4jIQ8GRfxwxqibIJcmMiufXTLIgRUif5uaTgFwIDAQAB\""
                        : "; no auto-DKIM-RR")
                .replace("{auto-SPF-RR}", assetEntity.getDirectValue("auto-SPF-RR", Boolean.class, true)
                        ? "{domain}.      IN  TXT      \"v=spf1 include:spf.hostsharing.net ?all\""
                        : "; no auto-SPF-RR")
                .replace("{auto-WILDCARD-MX-RR}", assetEntity.getDirectValue("auto-SPF-RR", Boolean.class, true)
                        ? """
                            *.{domain}.      IN      MX      30 mailin1.hostsharing.net.
                            *.{domain}.      IN      MX      30 mailin1.hostsharing.net.
                            *.{domain}.      IN      MX      30 mailin1.hostsharing.net.
                            """
                        : "; no auto-WILDCARD-MX-RR")
                .replace("{auto-WILDCARD-A-RR}", assetEntity.getDirectValue("auto-WILDCARD-A-RR", Boolean.class, true)
                        ? "*.{domain}.      IN  A       83.223.95.160" // arbitrary IP-number
                        : "; no auto-WILDCARD-A-RR")
                .replace("{auto-WILDCARD-AAAA-RR}", assetEntity.getDirectValue("auto-WILDCARD-AAAA-RR", Boolean.class, true)
                        ? "*.{domain}.      IN  AAAA    2a01:37:1000::53df:5fa0:0" // arbitrary IP-number
                        : "; no auto-WILDCARD-AAAA-RR")
                .replace("{auto-WILDCARD-SPF-RR}", assetEntity.getDirectValue("auto-WILDCARD-SPF-RR", Boolean.class, true)
                        ? "*.{domain}.      IN  TXT      \"v=spf1 include:spf.hostsharing.net ?all\""
                        : "; no auto-WILDCARD-SPF-RR")
                .replace("{domain}", fqdn(assetEntity))
                .replace("{userRRs}", getPropertyValues(assetEntity, "user-RR"));
    }

    private String fqdn(final HsHostingAsset assetEntity) {
        return assetEntity.getIdentifier().substring(0, assetEntity.getIdentifier().length() - IDENTIFIER_SUFFIX.length());
    }

    public static void addZonefileErrorsTo(final List<String> zoneFileErrors) {
        HsDomainDnsSetupHostingAssetValidator.zoneFileErrors = zoneFileErrors;
    }
}
