package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity.HsHostingAssetEntityBuilder;
import net.hostsharing.hsadminng.mapper.Array;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
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

    static final HsHostingAssetEntity validDomainSetupEntity = HsHostingAssetEntity.builder()
                .type(DOMAIN_SETUP)
                .identifier("example.org")
                .build();

    static HsHostingAssetEntityBuilder validEntityBuilder() {
        return HsHostingAssetEntity.builder()
                .type(DOMAIN_DNS_SETUP)
                .parentAsset(validDomainSetupEntity)
                .identifier("example.org")
                .config(Map.ofEntries(
                        entry("user-RR", Array.of(
                                "@     1814400  IN  XXX     example.org. root.example.org ( 1234 10800 900 604800 86400 )",
                                "www            IN          CNAME example.com. ; www.example.com is an alias for example.com",
                                "test1          IN 1h30m    CNAME example.com.",
                                "test2 1h30m    IN          CNAME example.com.",
                                "ns             IN          A     192.0.2.2; IPv4 address for ns.example.com")
                        )
                ));
    }

    @Test
    void containsExpectedProperties() {
        // when
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(DOMAIN_DNS_SETUP);

        // then
        assertThat(validator.properties()).map(Map::toString).containsExactlyInAnyOrder(
                "{type=integer, propertyName=TTL, min=0, defaultValue=21600}",
                "{type=boolean, propertyName=auto-SOA-RR, defaultValue=true}",
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
                "{type=boolean, propertyName=auto-WILDCARD-DKIM-RR, defaultValue=true}",
                "{type=boolean, propertyName=auto-WILDCARD-SPF-RR, defaultValue=true}",
                "{type=string[], propertyName=user-RR, elementsOf={type=string, propertyName=user-RR, matchesRegEx=[([a-z0-9\\.-]+|@)\\s+(([1-9][0-9]*[mMhHdDwW]{0,1})+\\s+)*IN\\s+[A-Z]+\\s+[^;].*(;.*)*, ([a-z0-9\\.-]+|@)\\s+IN\\s+(([1-9][0-9]*[mMhHdDwW]{0,1})+\\s+)*[A-Z]+\\s+[^;].*(;.*)*], required=true}}"
        );
    }

    @Test
    void preprocessesTakesIdentifierFromParent() {
        // given
        final var givenEntity = validEntityBuilder().build();
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        validator.preprocessEntity(givenEntity);

        // then
        assertThat(givenEntity.getIdentifier()).isEqualTo(givenEntity.getParentAsset().getIdentifier());
    }

    @Test
    void rejectsInvalidIdentifier() {
        // given
        final var givenEntity = validEntityBuilder().identifier("wrong.org").build();
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var result = validator.validateEntity(givenEntity);

        // then
        assertThat(result).containsExactly(
                "'identifier' expected to match '^example.org$', but is 'wrong.org'"
        );
    }

    @Test
    void acceptsValidIdentifier() {
        // given
        final var givenEntity = validEntityBuilder().identifier(validDomainSetupEntity.getIdentifier()).build();
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var result = validator.validateEntity(givenEntity);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void validatesReferencedEntities() {
        // given
        final var mangedServerHostingAssetEntity = validEntityBuilder()
                .parentAsset(HsHostingAssetEntity.builder().build())
                .assignedToAsset(HsHostingAssetEntity.builder().build())
                .bookingItem(HsBookingItemEntity.builder().type(HsBookingItemType.CLOUD_SERVER).build())
                .build();
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(mangedServerHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(mangedServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'DOMAIN_DNS_SETUP:example.org.bookingItem' must be null but is set to D-???????-?:null",
                "'DOMAIN_DNS_SETUP:example.org.parentAsset' must be of type DOMAIN_SETUP but is of type null",
                "'DOMAIN_DNS_SETUP:example.org.assignedToAsset' must be null but is set to D-???????-?:null");
    }

    @Test
    void acceptsValidEntity() {
        // given
        final var givenEntity = validEntityBuilder().build();
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var errors = validator.validateEntity(givenEntity);

        // then
        assertThat(errors).isEmpty();
    }

    @Test
    void recectsInvalidProperties() {
        // given
        final var mangedServerHostingAssetEntity = validEntityBuilder()
                .config(Map.ofEntries(
                        entry("TTL", "1d30m"), // currently only an integer for seconds is implemented here
                        entry("user-RR", Array.of(
                                "@     1814400  IN  1814400 BAD1  TTL only allowed once",
                                "www                        BAD1  Record-Class missing / not enough columns"))
                ))
                .build();
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(mangedServerHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(mangedServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'DOMAIN_DNS_SETUP:example.org.config.TTL' is expected to be of type class java.lang.Integer, but is of type 'String'",
                "'DOMAIN_DNS_SETUP:example.org.config.user-RR' is expected to match any of [([a-z0-9\\.-]+|@)\\s+(([1-9][0-9]*[mMhHdDwW]{0,1})+\\s+)*IN\\s+[A-Z]+\\s+[^;].*(;.*)*, ([a-z0-9\\.-]+|@)\\s+IN\\s+(([1-9][0-9]*[mMhHdDwW]{0,1})+\\s+)*[A-Z]+\\s+[^;].*(;.*)*] but '@     1814400  IN  1814400 BAD1  TTL only allowed once' does not match any",
                "'DOMAIN_DNS_SETUP:example.org.config.user-RR' is expected to match any of [([a-z0-9\\.-]+|@)\\s+(([1-9][0-9]*[mMhHdDwW]{0,1})+\\s+)*IN\\s+[A-Z]+\\s+[^;].*(;.*)*, ([a-z0-9\\.-]+|@)\\s+IN\\s+(([1-9][0-9]*[mMhHdDwW]{0,1})+\\s+)*[A-Z]+\\s+[^;].*(;.*)*] but 'www                        BAD1  Record-Class missing / not enough columns' does not match any");
    }

    @Test
    void validStringMatchesRegEx() {
        assertThat("@ ").matches(RR_REGEX_NAME);
        assertThat("ns ").matches(RR_REGEX_NAME);
        assertThat("example.com. ").matches(RR_REGEX_NAME);

        assertThat("12400 ").matches(RR_REGEX_TTL);
        assertThat("12400\t\t ").matches(RR_REGEX_TTL);
        assertThat("12400 \t\t").matches(RR_REGEX_TTL);
        assertThat("1h30m ").matches(RR_REGEX_TTL);
        assertThat("30m ").matches(RR_REGEX_TTL);

        assertThat("IN ").matches(RR_REGEX_IN);
        assertThat("IN\t\t ").matches(RR_REGEX_IN);
        assertThat("IN \t\t").matches(RR_REGEX_IN);

        assertThat("CNAME ").matches(RR_RECORD_TYPE);
        assertThat("CNAME\t\t ").matches(RR_RECORD_TYPE);
        assertThat("CNAME \t\t").matches(RR_RECORD_TYPE);

        assertThat("example.com.").matches(RR_RECORD_DATA);
        assertThat("123.123.123.123").matches(RR_RECORD_DATA);
        assertThat("(some more complex argument in parenthesis)").matches(RR_RECORD_DATA);
        assertThat("\"some more complex argument; including a semicolon\"").matches(RR_RECORD_DATA);

        assertThat("; whatever ; \" really anything").matches(RR_COMMENT);
    }

    @Test
    void generatesZonefile() {
        // given
        final var givenEntity = validEntityBuilder().build();
        final var validator = (HsDomainDnsSetupHostingAssetValidator) HsHostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var zonefile = validator.toZonefileString(givenEntity);

        // then
        assertThat(zonefile).isEqualTo("""
                $ORIGIN example.org.
                $TTL 21600

                ; these records are just placeholders to create a valid zonefile for the validation
                @        1814400  IN  SOA     example.org. root.example.org ( 1999010100 10800 900 604800 86400 )
                @                 IN  NS      ns

                @     1814400  IN  XXX     example.org. root.example.org ( 1234 10800 900 604800 86400 )
                www            IN          CNAME example.com. ; www.example.com is an alias for example.com
                test1          IN 1h30m    CNAME example.com.
                test2 1h30m    IN          CNAME example.com.
                ns             IN          A     192.0.2.2; IPv4 address for ns.example.com
                """);
    }

    @Test
    void rejectsInvalidZonefile() {
        // given
        final var givenEntity = validEntityBuilder().config(Map.ofEntries(
                    entry("user-RR", Array.of(
                        "example.org.        1814400  IN  SOA     example.org. root.example.org (1234 10800 900 604800 86400)"
                        ))
                    ))
                .build();
        final var validator = HsHostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var errors = validator.validateContext(givenEntity);

        // then
        assertThat(errors).containsExactlyInAnyOrder(
                "dns_master_load: example.org: multiple RRs of singleton type",
                "zone example.org/IN: loading from master file (null) failed: multiple RRs of singleton type",
                "zone example.org/IN: not loaded due to errors."
        );
    }
}
