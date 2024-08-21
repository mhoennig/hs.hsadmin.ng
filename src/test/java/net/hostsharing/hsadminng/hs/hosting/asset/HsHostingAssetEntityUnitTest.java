package net.hostsharing.hsadminng.hs.hosting.asset;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.CLOUD_SERVER_BOOKING_ITEM_REAL_ENTITY;
import static org.assertj.core.api.Assertions.assertThat;

class HsHostingAssetEntityUnitTest {

    final HsHostingAssetRealEntity givenParentAsset = HsHostingAssetRealEntity.builder()
            .bookingItem(CLOUD_SERVER_BOOKING_ITEM_REAL_ENTITY)
            .type(HsHostingAssetType.MANAGED_SERVER)
            .identifier("vm1234")
            .caption("some managed asset")
            .config(Map.ofEntries(
                    entry("CPU", 2),
                    entry("SSD-storage", 512),
                    entry("HDD-storage", 2048)))
            .build();
    final HsHostingAssetRealEntity givenWebspace = HsHostingAssetRealEntity.builder()
            .bookingItem(CLOUD_SERVER_BOOKING_ITEM_REAL_ENTITY)
            .type(HsHostingAssetType.MANAGED_WEBSPACE)
            .parentAsset(givenParentAsset)
            .identifier("xyz00")
            .caption("some managed webspace")
            .config(Map.ofEntries(
                    entry("CPU", 2),
                    entry("SSD-storage", 512),
                    entry("HDD-storage", 2048)))
            .build();
    final HsHostingAssetRealEntity givenUnixUser = HsHostingAssetRealEntity.builder()
            .type(HsHostingAssetType.UNIX_USER)
            .parentAsset(givenWebspace)
            .identifier("xyz00-web")
            .caption("some unix-user")
            .config(Map.ofEntries(
                    entry("SSD-soft-quota", 128),
                    entry("SSD-hard-quota", 256),
                    entry("HDD-soft-quota", 256),
                    entry("HDD-hard-quota", 512)))
            .build();
    final HsHostingAssetRealEntity givenDomainHttpSetup = HsHostingAssetRealEntity.builder()
            .type(HsHostingAssetType.DOMAIN_HTTP_SETUP)
            .parentAsset(givenWebspace)
            .identifier("example.org")
            .assignedToAsset(givenUnixUser)
            .caption("some domain setup")
            .config(Map.ofEntries(
                    entry("option-htdocsfallback", true),
                    entry("use-fcgiphpbin", "/usr/lib/cgi-bin/php"),
                    entry("validsubdomainnames", "*")))
            .build();

    @Test
    void toStringContainsAllPropertiesAndResourcesSortedByKey() {

        assertThat(givenWebspace.toString()).isEqualToIgnoringWhitespace(
                "HsHostingAsset(MANAGED_WEBSPACE, xyz00, some managed webspace, MANAGED_SERVER:vm1234, D-1234500:test project:test cloud server booking item, { \"CPU\": 2, \"HDD-storage\": 2048, \"SSD-storage\": 512 })");

        assertThat(givenUnixUser.toString()).isEqualToIgnoringWhitespace(
                "HsHostingAsset(UNIX_USER, xyz00-web, some unix-user, MANAGED_WEBSPACE:xyz00, { \"HDD-hard-quota\": 512, \"HDD-soft-quota\": 256, \"SSD-hard-quota\": 256, \"SSD-soft-quota\": 128 })");

        assertThat(givenDomainHttpSetup.toString()).isEqualToIgnoringWhitespace(
                "HsHostingAsset(DOMAIN_HTTP_SETUP, example.org, some domain setup, MANAGED_WEBSPACE:xyz00, UNIX_USER:xyz00-web, { \"option-htdocsfallback\": true, \"use-fcgiphpbin\": \"/usr/lib/cgi-bin/php\", \"validsubdomainnames\": \"*\" })");
    }

    @Test
    void toShortStringContainsOnlyMemberNumberAndCaption() {

        assertThat(givenWebspace.toShortString()).isEqualTo("MANAGED_WEBSPACE:xyz00");
        assertThat(givenUnixUser.toShortString()).isEqualTo("UNIX_USER:xyz00-web");
        assertThat(givenDomainHttpSetup.toShortString()).isEqualTo("DOMAIN_HTTP_SETUP:example.org");
    }
}
