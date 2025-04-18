package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRealEntity;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;

import static java.util.List.of;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.PRIVATE_CLOUD;
import static net.hostsharing.hsadminng.hs.booking.project.TestHsBookingProject.PROJECT_TEST_ENTITY;
import static org.assertj.core.api.Assertions.assertThat;

class HsPrivateCloudBookingItemValidatorUnitTest {

    final HsBookingDebitorEntity debitor = HsBookingDebitorEntity.builder()
            .debitorNumber(12345)
            .build();
    final HsBookingProjectRealEntity project = HsBookingProjectRealEntity.builder()
            .debitor(debitor)
            .caption("Test-Project")
            .build();
    private EntityManager em;

    @Test
    void validatesPropertyTotals() {
        // given
        final var privateCloudBookingItemEntity = HsBookingItemRealEntity.builder()
                .type(PRIVATE_CLOUD)
                .project(PROJECT_TEST_ENTITY)
                .caption("myPC")
                .resources(ofEntries(
                        entry("CPU", 4),
                        entry("RAM", 20),
                        entry("SSD", 100),
                        entry("Traffic", 5000),
                        entry("SLA-Platform EXT4H", 2),
                        entry("SLA-EMail", 2)
                ))
                .subBookingItems(of(
                        HsBookingItemRealEntity.builder()
                                .type(MANAGED_SERVER)
                                .caption("myMS-1")
                                .resources(ofEntries(
                                        entry("CPU", 2),
                                        entry("RAM", 10),
                                        entry("SSD", 50),
                                        entry("Traffic", 2500),
                                        entry("SLA-Platform", "EXT4H"),
                                        entry("SLA-EMail", true)
                                ))
                                .build(),
                        HsBookingItemRealEntity.builder()
                                .type(CLOUD_SERVER)
                                .caption("myMS-2")
                                .resources(ofEntries(
                                        entry("CPU", 2),
                                        entry("RAM", 10),
                                        entry("SSD", 50),
                                        entry("Traffic", 2500),
                                        entry("SLA-Platform", "EXT4H"),
                                        entry("SLA-EMail", true)
                                ))
                                .build()
                ))
                .build();

        // when
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(em, privateCloudBookingItemEntity);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void validatesExceedingPropertyTotals() {
        // given
        final var privateCloudBookingItemEntity = HsBookingItemRealEntity.builder()
                .project(project)
                .type(PRIVATE_CLOUD)
                .caption("myPC")
                .resources(ofEntries(
                        entry("CPU", 4),
                        entry("RAM", 20),
                        entry("SSD", 100),
                        entry("Traffic", 5000),
                        entry("SLA-Platform EXT2H", 1),
                        entry("SLA-EMail", 1)
                ))
                .subBookingItems(of(
                        HsBookingItemRealEntity.builder()
                                .type(MANAGED_SERVER)
                                .caption("myMS-1")
                                .resources(ofEntries(
                                        entry("CPU", 3),
                                        entry("RAM", 20),
                                        entry("SSD", 100),
                                        entry("Traffic", 3000),
                                        entry("SLA-Platform", "EXT2H"),
                                        entry("SLA-EMail", true)
                                ))
                                .build(),
                        HsBookingItemRealEntity.builder()
                                .type(CLOUD_SERVER)
                                .caption("myMS-2")
                                .resources(ofEntries(
                                        entry("CPU", 2),
                                        entry("RAM", 10),
                                        entry("SSD", 50),
                                        entry("Traffic", 2500),
                                        entry("SLA-Platform", "EXT2H"),
                                        entry("SLA-EMail", true),
                                        entry("SLA-Maria", true),
                                        entry("SLA-PgSQL", true),
                                        entry("SLA-Office", true),
                                        entry("SLA-Web", true)
                                ))
                                .build()
                ))
                .build();

        // when
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(em, privateCloudBookingItemEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'D-12345:Test-Project:myPC.resources.CPU' maximum total is 4, but actual total CPU is 5",
                "'D-12345:Test-Project:myPC.resources.RAM' maximum total is 20 GB, but actual total RAM is 30 GB",
                "'D-12345:Test-Project:myPC.resources.SSD' maximum total is 100 GB, but actual total SSD is 150 GB",
                "'D-12345:Test-Project:myPC.resources.Traffic' maximum total is 5000 GB, but actual total Traffic is 5500 GB",
                "'D-12345:Test-Project:myPC.resources.SLA-Platform EXT2H maximum total is 1, but actual total for SLA-Platform=EXT2H is 2",
                "'D-12345:Test-Project:myPC.resources.SLA-EMail' maximum total is 1, but actual total SLA-EMail is 2",
                "'D-12345:Test-Project:myPC.resources.SLA-Maria' maximum total is 0, but actual total SLA-Maria is 1",
                "'D-12345:Test-Project:myPC.resources.SLA-PgSQL' maximum total is 0, but actual total SLA-PgSQL is 1",
                "'D-12345:Test-Project:myPC.resources.SLA-Office' maximum total is 0, but actual total SLA-Office is 1",
                "'D-12345:Test-Project:myPC.resources.SLA-Web' maximum total is 0, but actual total SLA-Web is 1"
                );
    }
}
