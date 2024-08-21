package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRbacEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealEntity;
import net.hostsharing.hsadminng.mapper.Array;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_HTTP_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.UNIX_USER;
import static org.assertj.core.api.Assertions.assertThat;

class HsDomainHttpSetupHostingAssetValidatorUnitTest {

    static final HsHostingAssetRealEntity validDomainSetupEntity = HsHostingAssetRealEntity.builder()
                .type(DOMAIN_SETUP)
                .identifier("example.org")
                .build();

    static HsHostingAssetRbacEntity.HsHostingAssetRbacEntityBuilder<?, ?> validEntityBuilder() {
        return HsHostingAssetRbacEntity.builder()
                .type(DOMAIN_HTTP_SETUP)
                .parentAsset(validDomainSetupEntity)
                .assignedToAsset(HsHostingAssetRealEntity.builder().type(UNIX_USER).build())
                .identifier("example.org|HTTP")
                .config(Map.ofEntries(
                        entry("passenger-errorpage", true),
                        entry("fcgi-php-bin", "/usr/bin/whatsoever"),
                        entry("subdomains", Array.of("www", "test")
                    )
                ));
    }

    @Test
    void containsExpectedProperties() {
        // when
        final var validator = HostingAssetEntityValidatorRegistry.forType(DOMAIN_HTTP_SETUP);

        // then
        assertThat(validator.properties()).map(Map::toString).containsExactlyInAnyOrder(
                "{type=boolean, propertyName=htdocsfallback, defaultValue=true}",
                "{type=boolean, propertyName=indexes, defaultValue=true}",
                "{type=boolean, propertyName=cgi, defaultValue=true}",
                "{type=boolean, propertyName=passenger, defaultValue=true}",
                "{type=boolean, propertyName=passenger-errorpage}",
                "{type=boolean, propertyName=fastcgi, defaultValue=true}",
                "{type=boolean, propertyName=autoconfig, defaultValue=true}",
                "{type=boolean, propertyName=greylisting, defaultValue=true}",
                "{type=boolean, propertyName=includes, defaultValue=true}",
                "{type=boolean, propertyName=letsencrypt, defaultValue=true}",
                "{type=boolean, propertyName=multiviews, defaultValue=true}",
                "{type=string, propertyName=fcgi-php-bin, matchesRegEx=[^/.*], provided=[/usr/lib/cgi-bin/php], defaultValue=/usr/lib/cgi-bin/php}",
                "{type=string, propertyName=passenger-nodejs, matchesRegEx=[^/.*], provided=[/usr/bin/node], defaultValue=/usr/bin/node}",
                "{type=string, propertyName=passenger-python, matchesRegEx=[^/.*], provided=[/usr/bin/python3], defaultValue=/usr/bin/python3}",
                "{type=string, propertyName=passenger-ruby, matchesRegEx=[^/.*], provided=[/usr/bin/ruby], defaultValue=/usr/bin/ruby}",
                "{type=string[], propertyName=subdomains, elementsOf={type=string, propertyName=subdomains, matchesRegEx=[(\\*|(?!-)[A-Za-z0-9-]{1,63}(?<!-))], required=true}}"
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
        assertThat(givenEntity.getIdentifier()).isEqualTo("example.org|HTTP");
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
                "'identifier' expected to match '^\\Qexample.org|HTTP\\E$', but is 'example.org'"
        );
    }

    @Test
    void acceptsValidIdentifier() {
        // given
        final var givenEntity = validEntityBuilder().identifier(validDomainSetupEntity.getIdentifier()+"|HTTP").build();
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
                .parentAsset(HsHostingAssetRealEntity.builder().type(MANAGED_WEBSPACE).build())
                .assignedToAsset(null)
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(mangedServerHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(mangedServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'DOMAIN_HTTP_SETUP:example.org|HTTP.bookingItem' must be null but is of type CLOUD_SERVER",
                "'DOMAIN_HTTP_SETUP:example.org|HTTP.parentAsset' must be of type DOMAIN_SETUP but is of type MANAGED_WEBSPACE",
                "'DOMAIN_HTTP_SETUP:example.org|HTTP.assignedToAsset' must be of type UNIX_USER but is null");
    }

    @Test
    void acceptsValidEntity() {
        // given
        final var givenEntity = validEntityBuilder().build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var errors = validator.validateEntity(givenEntity);

        // then
        assertThat(errors).isEmpty();
    }

    @Test
    void rejectsInvalidProperties() {
        // given
        final var mangedServerHostingAssetEntity = validEntityBuilder()
                .config(Map.ofEntries(
                        entry("htdocsfallback", "false"),
                        entry("fcgi-php-bin", "false"),
                        entry("subdomains", Array.of("", "@", "example.com"))
                ))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(mangedServerHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(mangedServerHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'DOMAIN_HTTP_SETUP:example.org|HTTP.config.htdocsfallback' is expected to be of type Boolean, but is of type String",
                "'DOMAIN_HTTP_SETUP:example.org|HTTP.config.fcgi-php-bin' is expected to match any of [^/.*] but 'false' does not match",
                "'DOMAIN_HTTP_SETUP:example.org|HTTP.config.subdomains' is expected to match any of [(\\*|(?!-)[A-Za-z0-9-]{1,63}(?<!-))] but '' does not match",
                "'DOMAIN_HTTP_SETUP:example.org|HTTP.config.subdomains' is expected to match any of [(\\*|(?!-)[A-Za-z0-9-]{1,63}(?<!-))] but '@' does not match",
                "'DOMAIN_HTTP_SETUP:example.org|HTTP.config.subdomains' is expected to match any of [(\\*|(?!-)[A-Za-z0-9-]{1,63}(?<!-))] but 'example.com' does not match");
    }
}
