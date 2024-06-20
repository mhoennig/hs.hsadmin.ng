package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.List.of;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.PRIVATE_CLOUD;
import static org.assertj.core.api.Assertions.assertThat;

class HsCloudServerBookingItemValidatorUnitTest {

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
        final var cloudServerBookingItemEntity = HsBookingItemEntity.builder()
                .type(CLOUD_SERVER)
                .project(project)
                .caption("Test-Server")
                .resources(Map.ofEntries(
                        entry("CPUs", 2),
                        entry("RAM", 25),
                        entry("SSD", 25),
                        entry("Traffic", 250),
                        entry("SLA-EMail", true)
                ))
                .build();

        // when
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(cloudServerBookingItemEntity);

        // then
        assertThat(result).containsExactly("'D-12345:Test-Project:Test-Server.resources.SLA-EMail' is not expected but is set to 'true'");
    }

    @Test
    void containsAllValidations() {
        // when
        final var validator = HsBookingItemEntityValidatorRegistry.forType(CLOUD_SERVER);

        // then
        assertThat(validator.properties()).map(Map::toString).containsExactlyInAnyOrder(
                "{type=boolean, propertyName=active, required=false, defaultValue=true, isTotalsValidator=false}",
                "{type=integer, propertyName=CPUs, min=1, max=32, required=true, isTotalsValidator=false}",
                "{type=integer, propertyName=RAM, unit=GB, min=1, max=128, required=true, isTotalsValidator=false}",
                "{type=integer, propertyName=SSD, unit=GB, min=0, max=1000, step=25, required=true, isTotalsValidator=false}",
                "{type=integer, propertyName=HDD, unit=GB, min=0, max=4000, step=250, required=false, defaultValue=0, isTotalsValidator=false}",
                "{type=integer, propertyName=Traffic, unit=GB, min=250, max=10000, step=250, required=true, isTotalsValidator=false}",
                "{type=enumeration, propertyName=SLA-Infrastructure, values=[BASIC, EXT8H, EXT4H, EXT2H], required=false, isTotalsValidator=false}");
    }

    @Test
    void validatesExceedingPropertyTotals() {
        // given
        final var subCloudServerBookingItemEntity = HsBookingItemEntity.builder()
                .type(CLOUD_SERVER)
                .caption("Test Cloud-Server")
                .resources(ofEntries(
                        entry("CPUs", 2),
                        entry("RAM", 10),
                        entry("SSD", 50),
                        entry("Traffic", 2500)
                ))
                .build();
        final HsBookingItemEntity subManagedServerBookingItemEntity = HsBookingItemEntity.builder()
                .type(MANAGED_SERVER)
                .caption("Test Managed-Server")
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
                .caption("Test Cloud")
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
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(subCloudServerBookingItemEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'D-12345:Test-Project:Test Cloud.resources.CPUs' maximum total is 4, but actual total CPUs is 5",
                "'D-12345:Test-Project:Test Cloud.resources.RAM' maximum total is 20 GB, but actual total RAM is 30 GB",
                "'D-12345:Test-Project:Test Cloud.resources.SSD' maximum total is 100 GB, but actual total SSD is 150 GB",
                "'D-12345:Test-Project:Test Cloud.resources.Traffic' maximum total is 5000 GB, but actual total Traffic is 5500 GB"
        );
    }
}
