package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRealEntity;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
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
    final HsBookingProjectRealEntity project = HsBookingProjectRealEntity.builder()
            .debitor(debitor)
            .caption("Test-Project")
            .build();
    private EntityManager em;

    @Test
    void validatesProperties() {
        // given
        final var cloudServerBookingItemEntity = HsBookingItemRealEntity.builder()
                .type(CLOUD_SERVER)
                .project(project)
                .caption("Test-Server")
                .resources(Map.ofEntries(
                        entry("CPU", 2),
                        entry("RAM", 25),
                        entry("SSD", 25),
                        entry("Traffic", 250),
                        entry("SLA-EMail", true)
                ))
                .build();

        // when
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(em, cloudServerBookingItemEntity);

        // then
        assertThat(result).containsExactly("'D-12345:Test-Project:Test-Server.resources.SLA-EMail' is not expected but is set to 'true'");
    }

    @Test
    void containsAllValidations() {
        // when
        final var validator = HsBookingItemEntityValidatorRegistry.forType(CLOUD_SERVER);

        // then
        assertThat(validator.properties()).map(Map::toString).containsExactlyInAnyOrder(
                "{type=boolean, propertyName=active, defaultValue=true}",
                "{type=integer, propertyName=CPU, min=1, max=32, required=true}",
                "{type=integer, propertyName=RAM, unit=GB, min=1, max=8192, required=true}",
                "{type=integer, propertyName=SSD, unit=GB, min=25, max=1000, step=25, requiresAtLeastOneOf=[SDD, HDD]}",
                "{type=integer, propertyName=HDD, unit=GB, min=250, max=4000, step=250, requiresAtLeastOneOf=[SSD, HDD]}",
                "{type=integer, propertyName=Traffic, unit=GB, min=250, max=10000, step=250, requiresAtMaxOneOf=[Bandwidth, Traffic]}",
                "{type=integer, propertyName=Bandwidth, unit=GB, min=250, max=10000, step=250, requiresAtMaxOneOf=[Bandwidth, Traffic]}",
                "{type=enumeration, propertyName=SLA-Infrastructure, values=[BASIC, EXT8H, EXT4H, EXT2H]}");
    }

    @Test
    void validatesExceedingPropertyTotals() {
        // given
        final var subCloudServerBookingItemEntity = HsBookingItemRealEntity.builder()
                .type(CLOUD_SERVER)
                .caption("Test Cloud-Server")
                .resources(ofEntries(
                        entry("CPU", 2),
                        entry("RAM", 10),
                        entry("SSD", 50),
                        entry("Traffic", 2500)
                ))
                .build();
        final HsBookingItemRealEntity subManagedServerBookingItemEntity = HsBookingItemRealEntity.builder()
                .type(MANAGED_SERVER)
                .caption("Test Managed-Server")
                .resources(ofEntries(
                        entry("CPU", 3),
                        entry("RAM", 20),
                        entry("SSD", 100),
                        entry("Traffic", 3000)
                ))
                .build();
        final var privateCloudBookingItemEntity = HsBookingItemRealEntity.builder()
                .type(PRIVATE_CLOUD)
                .project(project)
                .caption("Test Cloud")
                .resources(ofEntries(
                        entry("CPU", 4),
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
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(em, subCloudServerBookingItemEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'D-12345:Test-Project:Test Cloud.resources.CPU' maximum total is 4, but actual total CPU is 5",
                "'D-12345:Test-Project:Test Cloud.resources.RAM' maximum total is 20 GB, but actual total RAM is 30 GB",
                "'D-12345:Test-Project:Test Cloud.resources.SSD' maximum total is 100 GB, but actual total SSD is 150 GB",
                "'D-12345:Test-Project:Test Cloud.resources.Traffic' maximum total is 5000 GB, but actual total Traffic is 5500 GB"
        );
    }
}
