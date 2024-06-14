package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectEntity;
import org.junit.jupiter.api.Test;

import static java.util.List.of;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.PRIVATE_CLOUD;
import static org.assertj.core.api.Assertions.assertThat;

class HsPrivateCloudBookingItemValidatorUnitTest {

    final HsBookingDebitorEntity debitor = HsBookingDebitorEntity.builder()
            .debitorNumber(12345)
            .build();
    final HsBookingProjectEntity project = HsBookingProjectEntity.builder()
            .debitor(debitor)
            .caption("Test-Project")
            .build();

    @Test
    void validatesPropertyTotals() {
        // given
        final var privateCloudBookingItemEntity = HsBookingItemEntity.builder()
                .type(PRIVATE_CLOUD)
                .resources(ofEntries(
                        entry("CPUs", 4),
                        entry("RAM", 20),
                        entry("SSD", 100),
                        entry("Traffic", 5000)
                ))
                .subBookingItems(of(
                        HsBookingItemEntity.builder()
                                .type(MANAGED_SERVER)
                                .resources(ofEntries(
                                        entry("CPUs", 2),
                                        entry("RAM", 10),
                                        entry("SSD", 50),
                                        entry("Traffic", 2500)
                                ))
                                .build(),
                        HsBookingItemEntity.builder()
                                .type(CLOUD_SERVER)
                                .resources(ofEntries(
                                        entry("CPUs", 2),
                                        entry("RAM", 10),
                                        entry("SSD", 50),
                                        entry("Traffic", 2500)
                                ))
                                .build()
                ))
                .build();

        // when
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(privateCloudBookingItemEntity);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void validatesExceedingPropertyTotals() {
        // given
        final var privateCloudBookingItemEntity = HsBookingItemEntity.builder()
                .project(project)
                .type(PRIVATE_CLOUD)
                .resources(ofEntries(
                        entry("CPUs", 4),
                        entry("RAM", 20),
                        entry("SSD", 100),
                        entry("Traffic", 5000)
                ))
                .subBookingItems(of(
                        HsBookingItemEntity.builder()
                                .type(MANAGED_SERVER)
                                .resources(ofEntries(
                                        entry("CPUs", 3),
                                        entry("RAM", 20),
                                        entry("SSD", 100),
                                        entry("Traffic", 3000)
                                ))
                                .build(),
                        HsBookingItemEntity.builder()
                                .type(CLOUD_SERVER)
                                .resources(ofEntries(
                                        entry("CPUs", 2),
                                        entry("RAM", 10),
                                        entry("SSD", 50),
                                        entry("Traffic", 2500)
                                ))
                                .build()
                ))
                .build();

        // when
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(privateCloudBookingItemEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'D-12345:Test-Project:null.resources.CPUs' maximum total is 4, but actual total CPUs 5",
                "'D-12345:Test-Project:null.resources.RAM' maximum total is 20 GB, but actual total RAM 30 GB",
                "'D-12345:Test-Project:null.resources.SSD' maximum total is 100 GB, but actual total SSD 150 GB",
                "'D-12345:Test-Project:null.resources.Traffic' maximum total is 5000 GB, but actual total Traffic 5500 GB"
        );
    }

}
