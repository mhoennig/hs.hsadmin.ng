package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRealEntity;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_WEBSPACE;
import static org.assertj.core.api.Assertions.assertThat;

class HsManagedWebspaceBookingItemValidatorUnitTest {

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
        final var mangedServerBookingItemEntity = HsBookingItemRealEntity.builder()
                .type(MANAGED_WEBSPACE)
                .project(project)
                .caption("Test Managed-Webspace")
                .resources(Map.ofEntries(
                        entry("CPU", 2),
                        entry("RAM", 25),
                        entry("Traffic", 250),
                        entry("SLA-EMail", true)
                ))
                .build();

        // when
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(em, mangedServerBookingItemEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'D-12345:Test-Project:Test Managed-Webspace.resources.CPU' is not expected but is set to '2'",
                "'D-12345:Test-Project:Test Managed-Webspace.resources.RAM' is not expected but is set to '25'",
                "'D-12345:Test-Project:Test Managed-Webspace.resources.SSD' is required but missing",
                "'D-12345:Test-Project:Test Managed-Webspace.resources.SLA-EMail' is not expected but is set to 'true'"
                );
    }

    @Test
    void containsAllValidations() {
        // when
        final var validator = HsBookingItemEntityValidatorRegistry.forType(MANAGED_WEBSPACE);

        // then
        assertThat(validator.properties()).map(Map::toString).containsExactlyInAnyOrder(
                "{type=integer, propertyName=SSD, unit=GB, min=1, max=2000, step=1, required=true}",
                "{type=integer, propertyName=HDD, unit=GB, min=0, max=10000, step=10}",
                "{type=integer, propertyName=Traffic, unit=GB, min=10, max=64000, step=10, requiresAtMaxOneOf=[Bandwidth, Traffic]}",
                "{type=integer, propertyName=Bandwidth, unit=GB, min=10, max=1000, step=10, requiresAtMaxOneOf=[Bandwidth, Traffic]}",
                "{type=integer, propertyName=Multi, min=1, max=100, step=1, defaultValue=1}",
                "{type=integer, propertyName=Daemons, min=0, max=16, defaultValue=0}",
                "{type=boolean, propertyName=Online Office Server}",
                "{type=enumeration, propertyName=SLA-Platform, values=[BASIC, EXT24H], defaultValue=BASIC}");
    }
}
