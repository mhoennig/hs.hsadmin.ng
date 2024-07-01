package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.stream;
import static java.util.List.of;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.PRIVATE_CLOUD;
import static org.assertj.core.api.Assertions.assertThat;

class HsManagedServerBookingItemValidatorUnitTest {

    final HsBookingDebitorEntity debitor = HsBookingDebitorEntity.builder()
            .debitorNumber(12345)
            .build();
    final HsBookingProjectEntity project = HsBookingProjectEntity.builder()
            .debitor(debitor)
            .caption("Test-Project")
            .build();

    @Test
    void validatesProperties() {
        // given
        final var mangedServerBookingItemEntity = HsBookingItemEntity.builder()
                .type(MANAGED_SERVER)
                .project(project)
                .resources(Map.ofEntries(
                        entry("CPUs", 2),
                        entry("RAM", 25),
                        entry("SSD", 25),
                        entry("Traffic", 250),
                        entry("SLA-Platform", "BASIC"),
                        entry("SLA-EMail", true)
                ))
                .build();

        // when
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(mangedServerBookingItemEntity);

        // then
        assertThat(result).containsExactly("'D-12345:Test-Project:null.resources.SLA-EMail' is expected to be false because SLA-Platform=BASIC but is true");
    }

    @Test
    void containsAllValidations() {
        // when
        final var validator = HsBookingItemEntityValidatorRegistry.forType(MANAGED_SERVER);

        // then
        assertThat(validator.properties()).map(Map::toString).containsExactlyInAnyOrder(
                "{type=integer, propertyName=CPUs, min=1, max=32, required=true}",
                "{type=integer, propertyName=RAM, unit=GB, min=1, max=128, required=true}",
                "{type=integer, propertyName=SSD, unit=GB, min=25, max=1000, step=25, required=true, isTotalsValidator=true, thresholdPercentage=200}",
                "{type=integer, propertyName=HDD, unit=GB, min=0, max=4000, step=250, defaultValue=0, isTotalsValidator=true, thresholdPercentage=200}",
                "{type=integer, propertyName=Traffic, unit=GB, min=250, max=10000, step=250, required=true, isTotalsValidator=true, thresholdPercentage=200}",
                "{type=enumeration, propertyName=SLA-Platform, values=[BASIC, EXT8H, EXT4H, EXT2H], defaultValue=BASIC}",
                "{type=boolean, propertyName=SLA-EMail}", // TODO.impl: falseIf-validation is missing in output
                "{type=boolean, propertyName=SLA-Maria}",
                "{type=boolean, propertyName=SLA-PgSQL}",
                "{type=boolean, propertyName=SLA-Office}",
                "{type=boolean, propertyName=SLA-Web}");
    }

    @Test
    void validatesExceedingPropertyTotals() {
        // given
        final var subCloudServerBookingItemEntity = HsBookingItemEntity.builder()
                .type(CLOUD_SERVER)
                .resources(ofEntries(
                        entry("CPUs", 2),
                        entry("RAM", 10),
                        entry("SSD", 50),
                        entry("Traffic", 2500)
                ))
                .build();
        final HsBookingItemEntity subManagedServerBookingItemEntity = HsBookingItemEntity.builder()
                .type(MANAGED_SERVER)
                .resources(ofEntries(
                        entry("CPUs", 3),
                        entry("RAM", 20),
                        entry("SSD", 100),
                        entry("Traffic", 3000)
                ))
                .build();
        final var privateCloudBookingItemEntity = HsBookingItemEntity.builder()
                .type(PRIVATE_CLOUD)
                .project(project)
                .resources(ofEntries(
                        entry("CPUs", 4),
                        entry("RAM", 20),
                        entry("SSD", 100),
                        entry("Traffic", 5000)
                ))
                .subBookingItems(of(
                        subManagedServerBookingItemEntity,
                        subCloudServerBookingItemEntity
                ))
                .build();

        subManagedServerBookingItemEntity.setParentItem(privateCloudBookingItemEntity);
        subCloudServerBookingItemEntity.setParentItem(privateCloudBookingItemEntity);

        // when
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(subManagedServerBookingItemEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'D-12345:Test-Project:null.resources.CPUs' maximum total is 4, but actual total CPUs is 5",
                "'D-12345:Test-Project:null.resources.RAM' maximum total is 20 GB, but actual total RAM is 30 GB",
                "'D-12345:Test-Project:null.resources.SSD' maximum total is 100 GB, but actual total SSD is 150 GB",
                "'D-12345:Test-Project:null.resources.Traffic' maximum total is 5000 GB, but actual total Traffic is 5500 GB"
        );
    }

    @Test
    void validatesExceedingTotals() {
        // given
        final var managedWebspaceBookingItem = HsBookingItemEntity.builder()
                .type(MANAGED_WEBSPACE)
                .project(project)
                .caption("test Managed-Webspace")
                .resources(ofEntries(
                        entry("SSD", 100),
                        entry("Traffic", 1000),
                        entry("Multi", 1)
                ))
                .relatedHostingAsset(HsHostingAssetEntity.builder()
                        .type(HsHostingAssetType.MANAGED_WEBSPACE)
                        .identifier("abc00")
                        .subHostingAssets(concat(
                                generate(26, HsHostingAssetType.UNIX_USER, "xyz00-%c%c"),
                                generateDbUsersWithDatabases(3, HsHostingAssetType.PGSQL_USER,
                                        "xyz00_%c%c",
                                        1, HsHostingAssetType.PGSQL_DATABASE
                                ),
                                generateDbUsersWithDatabases(3, HsHostingAssetType.MARIADB_USER,
                                        "xyz00_%c%c",
                                        2, HsHostingAssetType.MARIADB_DATABASE
                                ),
                                generateDomainEmailSetupsWithEMailAddresses(26, HsHostingAssetType.DOMAIN_EMAIL_SETUP,
                                        "%c%c.example.com",
                                        10, HsHostingAssetType.EMAIL_ADDRESS
                                )
                            ))
                        .build()
                )
                .build();

        // when
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(managedWebspaceBookingItem);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'D-12345:Test-Project:test Managed-Webspace.resources.Multi=1 allows at maximum 25 unix users, but 26 found",
                "'D-12345:Test-Project:test Managed-Webspace.resources.Multi=1 allows at maximum 5 database users, but 6 found",
                "'D-12345:Test-Project:test Managed-Webspace.resources.Multi=1 allows at maximum 5 databases, but 9 found",
                "'D-12345:Test-Project:test Managed-Webspace.resources.Multi=1 allows at maximum 250 databases, but 260 found"
        );
    }

    @SafeVarargs
    private List<HsHostingAssetEntity> concat(final List<HsHostingAssetEntity>... hostingAssets) {
        return stream(hostingAssets)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<HsHostingAssetEntity> generate(final int count, final HsHostingAssetType hostingAssetType,
            final String identifierPattern) {
        return IntStream.range(0, count)
                        .mapToObj(number -> HsHostingAssetEntity.builder()
                                    .type(hostingAssetType)
                                    .identifier(identifierPattern.formatted((number/'a')+'a', (number%'a')+'a'))
                                    .build())
                .toList();
    }

    private List<HsHostingAssetEntity> generateDbUsersWithDatabases(
            final int userCount,
            final HsHostingAssetType directAssetType,
            final String directAssetIdentifierFormat,
            final int dbCount,
            final HsHostingAssetType subAssetType) {
        return IntStream.range(0, userCount)
                .mapToObj(n -> HsHostingAssetEntity.builder()
                        .type(directAssetType)
                        .identifier(directAssetIdentifierFormat.formatted((n/'a')+'a', (n%'a')+'a'))
                        .subHostingAssets(
                                generate(dbCount, subAssetType, "%c%c.example.com".formatted((n/'a')+'a', (n%'a')+'a'))
                        )
                        .build())
                .toList();
    }

    private List<HsHostingAssetEntity> generateDomainEmailSetupsWithEMailAddresses(
            final int domainCount,
            final HsHostingAssetType directAssetType,
            final String directAssetIdentifierFormat,
            final int emailAddressCount,
            final HsHostingAssetType subAssetType) {
        return IntStream.range(0, domainCount)
                .mapToObj(n -> HsHostingAssetEntity.builder()
                        .type(directAssetType)
                        .identifier(directAssetIdentifierFormat.formatted((n/'a')+'a', (n%'a')+'a'))
                        .subHostingAssets(
                                generate(emailAddressCount, subAssetType, "xyz00_%c%c%%c%%c".formatted((n/'a')+'a', (n%'a')+'a'))
                        )
                        .build())
                .toList();
    }

}
