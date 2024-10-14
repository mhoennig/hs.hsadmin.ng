package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRbacEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealEntity;
import net.hostsharing.hsadminng.mapper.Array;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetTestEntities.MANAGED_WEBSPACE_HOSTING_ASSET_REAL_TEST_ENTITY;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_DNS_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.validators.HsDomainDnsSetupHostingAssetValidator.RR_COMMENT;
import static net.hostsharing.hsadminng.hs.hosting.asset.validators.HsDomainDnsSetupHostingAssetValidator.RR_RECORD_DATA;
import static net.hostsharing.hsadminng.hs.hosting.asset.validators.HsDomainDnsSetupHostingAssetValidator.RR_RECORD_TYPE;
import static net.hostsharing.hsadminng.hs.hosting.asset.validators.HsDomainDnsSetupHostingAssetValidator.RR_REGEX_IN;
import static net.hostsharing.hsadminng.hs.hosting.asset.validators.HsDomainDnsSetupHostingAssetValidator.RR_REGEX_NAME;
import static net.hostsharing.hsadminng.hs.hosting.asset.validators.HsDomainDnsSetupHostingAssetValidator.RR_REGEX_TTL;
import static org.assertj.core.api.Assertions.assertThat;

class HsDomainDnsSetupHostingAssetValidatorUnitTest {

    static final HsHostingAssetRealEntity validDomainSetupEntity = HsHostingAssetRealEntity.builder()
            .type(DOMAIN_SETUP)
            .identifier("example.org")
            .build();

    private EntityManager em;

    static HsHostingAssetRbacEntity.HsHostingAssetRbacEntityBuilder<?, ?> validEntityBuilder() {
        return HsHostingAssetRbacEntity.builder()
                .type(DOMAIN_DNS_SETUP)
                .parentAsset(validDomainSetupEntity)
                .assignedToAsset(MANAGED_WEBSPACE_HOSTING_ASSET_REAL_TEST_ENTITY)
                .identifier("example.org|DNS")
                .config(Map.ofEntries(
                        entry("TTL", 21600),
                        entry("auto-SOA", true),
                        entry("auto-NS-RR", true),
                        entry("auto-MX-RR", true),
                        entry("auto-A-RR", true),
                        entry("auto-AAAA-RR", true),
                        entry("auto-MAILSERVICES-RR", true),
                        entry("auto-AUTOCONFIG-RR", true),
                        entry("auto-AUTODISCOVER-RR", true),
                        entry("auto-DKIM-RR", true),
                        entry("auto-SPF-RR", true),
                        entry("auto-WILDCARD-MX-RR", true),
                        entry("auto-WILDCARD-A-RR", true),
                        entry("auto-WILDCARD-AAAA-RR", true),
                        entry("auto-WILDCARD-SPF-RR", true),
                        entry("user-RR", Array.of(
                                "www            IN          CNAME example.com. ; www.example.com is an alias for example.com",
                                "test1          IN 1h30m    CNAME example.com.",
                                "test2 1h30m    IN          CNAME example.com.",
                                "ns             IN          A     192.0.2.2; IPv4 address for ns.example.com",
                                "_acme-challenge.PAULCHEN-VS.core.example.org. 60 IN CNAME _acme-challenge.core.example.org.acme-pki.de.")
                        )
                ));
    }

    @BeforeEach
    void reset() {
        HsDomainDnsSetupHostingAssetValidator.addZonefileErrorsTo(null);
    }

    @Test
    void containsExpectedProperties() {
        // when
        final var validator = HostingAssetEntityValidatorRegistry.forType(DOMAIN_DNS_SETUP);

        // then
        assertThat(validator.properties()).map(Map::toString).containsExactlyInAnyOrder(
                "{type=integer, propertyName=TTL, min=0, defaultValue=21600}",
                "{type=boolean, propertyName=auto-SOA, defaultValue=true}",
                "{type=boolean, propertyName=auto-NS-RR, defaultValue=true}",
                "{type=boolean, propertyName=auto-MX-RR, defaultValue=true}",
                "{type=boolean, propertyName=auto-A-RR, defaultValue=true}",
                "{type=boolean, propertyName=auto-AAAA-RR, defaultValue=true}",
                "{type=boolean, propertyName=auto-MAILSERVICES-RR, defaultValue=true}",
                "{type=boolean, propertyName=auto-AUTOCONFIG-RR, defaultValue=true}",
                "{type=boolean, propertyName=auto-AUTODISCOVER-RR, defaultValue=true}",
                "{type=boolean, propertyName=auto-DKIM-RR, defaultValue=true}",
                "{type=boolean, propertyName=auto-SPF-RR, defaultValue=true}",
                "{type=boolean, propertyName=auto-WILDCARD-MX-RR, defaultValue=true}",
                "{type=boolean, propertyName=auto-WILDCARD-A-RR, defaultValue=true}",
                "{type=boolean, propertyName=auto-WILDCARD-AAAA-RR, defaultValue=true}",
                "{type=boolean, propertyName=auto-WILDCARD-SPF-RR, defaultValue=true}",
                "{type=string[], propertyName=user-RR, elementsOf={type=string, propertyName=user-RR, matchesRegEx=[(\\*\\.)?([a-zA-Z0-9\\._-]+|@)[ \t]+(([1-9][0-9]*[mMhHdDwW]?)+[ \t]+)?[iI][nN][ \t]+[a-zA-Z]+[ \t]+(([^;]+)|(\".*\")|(\\(.*\\)))[ \t]*(;.*)?, (\\*\\.)?([a-zA-Z0-9\\._-]+|@)[ \t]+[iI][nN][ \t]+(([1-9][0-9]*[mMhHdDwW]?)+[ \t]+)?[a-zA-Z]+[ \t]+(([^;]+)|(\".*\")|(\\(.*\\)))[ \t]*(;.*)?], required=true}}"
        );
    }

    @Test
    void preprocessesTakesIdentifierFromParent() {
        // given
        final var givenEntity = validEntityBuilder().build();
        assertThat(givenEntity.getParentAsset().getIdentifier()).as("preconditon failed").isEqualTo("example.org");
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        validator.preprocessEntity(givenEntity);

        // then
        assertThat(givenEntity.getIdentifier()).isEqualTo("example.org|DNS");
    }

    @Test
    void rejectsInvalidIdentifier() {
        // given
        final var givenEntity = validEntityBuilder().identifier("example.org").build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var result = validator.validateEntity(givenEntity);

        // then
        assertThat(result).containsExactly(
                "'identifier' expected to match '^\\Qexample.org|DNS\\E$', but is 'example.org'"
        );
    }

    @Test
    void acceptsValidIdentifier() {
        // given
        final var givenEntity = validEntityBuilder().identifier(validDomainSetupEntity.getIdentifier()+"|DNS").build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var result = validator.validateEntity(givenEntity);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void rejectsInvalidReferencedEntities() {
        // given
        final var mangedServerHostingAssetEntity = validEntityBuilder()
                .bookingItem(HsBookingItemRealEntity.builder().type(HsBookingItemType.CLOUD_SERVER).build())
                .parentAsset(null)
                .assignedToAsset(HsHostingAssetRealEntity.builder().type(DOMAIN_SETUP).build())
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(mangedServerHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(mangedServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'DOMAIN_DNS_SETUP:example.org|DNS.bookingItem' must be null but is of type CLOUD_SERVER",
                "'DOMAIN_DNS_SETUP:example.org|DNS.parentAsset' must be of type DOMAIN_SETUP but is null",
                "'DOMAIN_DNS_SETUP:example.org|DNS.assignedToAsset' must be of type MANAGED_WEBSPACE but is of type DOMAIN_SETUP");
    }

    @Test
    void acceptsValidEntityItself() {
        // given
        final var givenEntity = validEntityBuilder().build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var errors = validator.validateEntity(givenEntity);

        // then
        assertThat(errors).isEmpty();
    }

    @Test
    void acceptsValidEntityInContext() {
        // given
        final var givenEntity = validEntityBuilder().build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());
        Dns.fakeResultForDomain(givenEntity.getIdentifier(), Dns.Result.fromRecords());

        // when
        final var errors = validator.validateContext(givenEntity);

        // then
        assertThat(errors).isEmpty();
    }

    @Test
    void rejectsInvalidProperties() {
        // given
        final var mangedServerHostingAssetEntity = validEntityBuilder()
                .config(Map.ofEntries(
                        entry("TTL", "1d30m"), // currently only an integer for seconds is implemented here
                        entry("user-RR", Array.of(
                                "@     1814400  IN  1814400 BAD1  TTL only allowed once",
                                "www                        BAD1  Record-Class missing / not enough columns"))
                ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(mangedServerHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(mangedServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'DOMAIN_DNS_SETUP:example.org|DNS.config.TTL' is expected to be of type Integer, but is of type String",
                "'DOMAIN_DNS_SETUP:example.org|DNS.config.user-RR' is expected to match any of [(\\*\\.)?([a-zA-Z0-9\\._-]+|@)[ \t]+(([1-9][0-9]*[mMhHdDwW]?)+[ \t]+)?[iI][nN][ \t]+[a-zA-Z]+[ \t]+(([^;]+)|(\".*\")|(\\(.*\\)))[ \t]*(;.*)?, (\\*\\.)?([a-zA-Z0-9\\._-]+|@)[ \t]+[iI][nN][ \t]+(([1-9][0-9]*[mMhHdDwW]?)+[ \t]+)?[a-zA-Z]+[ \t]+(([^;]+)|(\".*\")|(\\(.*\\)))[ \t]*(;.*)?] but '@     1814400  IN  1814400 BAD1  TTL only allowed once' does not match any",
                "'DOMAIN_DNS_SETUP:example.org|DNS.config.user-RR' is expected to match any of [(\\*\\.)?([a-zA-Z0-9\\._-]+|@)[ \t]+(([1-9][0-9]*[mMhHdDwW]?)+[ \t]+)?[iI][nN][ \t]+[a-zA-Z]+[ \t]+(([^;]+)|(\".*\")|(\\(.*\\)))[ \t]*(;.*)?, (\\*\\.)?([a-zA-Z0-9\\._-]+|@)[ \t]+[iI][nN][ \t]+(([1-9][0-9]*[mMhHdDwW]?)+[ \t]+)?[a-zA-Z]+[ \t]+(([^;]+)|(\".*\")|(\\(.*\\)))[ \t]*(;.*)?] but 'www                        BAD1  Record-Class missing / not enough columns' does not match any");
    }

    @Test
    void validNameMatchesRegEx() {
        assertThat("@ ").matches(RR_REGEX_NAME);
        assertThat("ns ").matches(RR_REGEX_NAME);
        assertThat("example.com. ").matches(RR_REGEX_NAME);
        assertThat("example.ORG. ").matches(RR_REGEX_NAME);
    }

    @Test
    void validTtlMatchesRegEx() {
        assertThat("12400 ").matches(RR_REGEX_TTL);
        assertThat("12400\t\t ").matches(RR_REGEX_TTL);
        assertThat("12400 \t\t").matches(RR_REGEX_TTL);
        assertThat("1h30m ").matches(RR_REGEX_TTL);
        assertThat("30m ").matches(RR_REGEX_TTL);
    }

    @Test
    void validInMatchesRegEx() {
        assertThat("in ").matches(RR_REGEX_IN);
        assertThat("IN ").matches(RR_REGEX_IN);
        assertThat("IN\t\t ").matches(RR_REGEX_IN);
        assertThat("IN \t\t").matches(RR_REGEX_IN);
    }

    @Test
    void validRecordTypeMatchesRegEx() {
        assertThat("a ").matches(RR_RECORD_TYPE);
        assertThat("CNAME ").matches(RR_RECORD_TYPE);
        assertThat("CNAME\t\t ").matches(RR_RECORD_TYPE);
        assertThat("CNAME \t\t").matches(RR_RECORD_TYPE);
    }

    @Test
    void validRecordDataMatchesRegEx() {
        assertThat("example.com.").matches(RR_RECORD_DATA);
        assertThat("example.com.    ").matches(RR_RECORD_DATA);
        assertThat("123.123.123.123").matches(RR_RECORD_DATA);
        assertThat("123.123.123.123 ").matches(RR_RECORD_DATA);
        assertThat("_acme-challenge.core.example.org.acme-pki.de.").matches(RR_RECORD_DATA);
        assertThat("(some more complex argument in parenthesis)").matches(RR_RECORD_DATA);
        assertThat("\"some more complex argument; including a semicolon\"").matches(RR_RECORD_DATA);
    }

    @Test
    void validCommentMatchesRegEx() {
        assertThat("; whatever ; \" really anything").matches(RR_COMMENT);
    }

    @Test
    void generatesZonefile() {
        // given
        final var givenEntity = validEntityBuilder().build();
        final var validator = (HsDomainDnsSetupHostingAssetValidator) HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var zonefile = validator.toZonefileString(givenEntity);

        // then
        assertThat(zonefile).isEqualTo("""
                $TTL 21600

                example.org.   IN     SOA h00.hostsharing.net. hostmaster.hostsharing.net. (
                                                1303649373      ; serial secs since Jan 1 1970
                                                        6H      ; refresh (>=10000)
                                                        1H      ; retry (>=1800)
                                                        1W      ; expire
                                                        1H      ; minimum
                                                )

                example.org.      IN      NS      dns1.hostsharing.net.
                example.org.      IN      NS      dns2.hostsharing.net.
                example.org.      IN      NS      dns3.hostsharing.net.

                example.org.      IN      MX      30 mailin1.hostsharing.net.
                example.org.      IN      MX      30 mailin2.hostsharing.net.
                example.org.      IN      MX      30 mailin3.hostsharing.net.

                example.org.      IN  A       83.223.95.160
                example.org.      IN  AAAA    2a01:37:1000::53df:5fa0:0
                default._domainkey 21600 IN TXT "v=DKIM1; h=sha256; k=rsa; s=email; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCmdM9d15bqe94zbHVcKKpUF875XoCWHKRap/sG3NJZ9xZ/BjfGXmqoEYeFNpX3CB7pOXhH5naq4N+6gTjArTviAiVThHXyebhrxaf1dVS4IUC6raTEyQrWPZUf7ZxXmcCYvOdV4jIQ8GRfxwxqibIJcmMiufXTLIgRUif5uaTgFwIDAQAB"
                example.org.      IN  TXT      "v=spf1 include:spf.hostsharing.net ?all"

                *.example.org.      IN      MX      30 mailin1.hostsharing.net.
                *.example.org.      IN      MX      30 mailin1.hostsharing.net.
                *.example.org.      IN      MX      30 mailin1.hostsharing.net.

                *.example.org.      IN  A       83.223.95.160
                *.example.org.      IN  AAAA    2a01:37:1000::53df:5fa0:0
                *.example.org.      IN  TXT      "v=spf1 include:spf.hostsharing.net ?all"

                www            IN          CNAME example.com. ; www.example.com is an alias for example.com
                test1          IN 1h30m    CNAME example.com.
                test2 1h30m    IN          CNAME example.com.
                ns             IN          A     192.0.2.2; IPv4 address for ns.example.com
                _acme-challenge.PAULCHEN-VS.core.example.org. 60 IN CNAME _acme-challenge.core.example.org.acme-pki.de.
                """);
    }

    @Test
    void rejectsInvalidZonefile() {
        // given
        final var givenEntity = validEntityBuilder().config(Map.ofEntries(
                    entry("user-RR", Array.of(
                        "example.org.        1814400  IN  SOA     example.org. root.example.org (1234 10800 900 604800 86400)",
                        "example.org.        1814400  IN  SOA     example.org. root.example.org (4321 10800 900 604800 86400)"
                        ))
                    ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());
        Dns.fakeResultForDomain("example.org", Dns.Result.fromRecords());

        // when
        final var errors = validator.validateContext(givenEntity);

        // then
        assertThat(errors).containsExactlyInAnyOrder(
                "[example.org|DNS] dns_master_load:line 26: example.org: multiple RRs of singleton type",
                "[example.org|DNS] zone example.org/IN: loading from master file (null) failed: multiple RRs of singleton type",
                "[example.org|DNS] zone example.org/IN: not loaded due to errors."
        );
    }

    @Test
    void acceptsInvalidZonefileWithActiveErrorFilter() {
        // given
        final var givenEntity = validEntityBuilder().config(Map.ofEntries(
                        entry("user-RR", Array.of(
                                "example.org.        1814400  IN  SOA     example.org. root.example.org (1234 10800 900 604800 86400)",
                                "example.org.        1814400  IN  SOA     example.org. root.example.org (4321 10800 900 604800 86400)"
                        ))
                ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());
        Dns.fakeResultForDomain("example.org", Dns.Result.fromRecords());

        // when
        final var zonefileErrors = new ArrayList<String>();
        HsDomainDnsSetupHostingAssetValidator.addZonefileErrorsTo(zonefileErrors);
        final var errors = validator.validateContext(givenEntity);

        // then
        assertThat(errors).isEmpty();
        assertThat(zonefileErrors).containsExactlyInAnyOrder(
                "[example.org|DNS] dns_master_load:line 26: example.org: multiple RRs of singleton type",
                "[example.org|DNS] zone example.org/IN: loading from master file (null) failed: multiple RRs of singleton type",
                "[example.org|DNS] zone example.org/IN: not loaded due to errors."
        );
    }
}
