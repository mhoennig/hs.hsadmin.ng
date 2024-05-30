package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_WEBSPACE;
import static net.hostsharing.hsadminng.hs.booking.item.validators.HsBookingItemEntityValidators.forType;
import static org.assertj.core.api.Assertions.assertThat;

class HsManagedWebspaceBookingItemValidatorUnitTest {

    @Test
    void validatesProperties() {
        // given
        final var mangedServerBookingItemEntity = HsBookingItemEntity.builder()
                .type(MANAGED_WEBSPACE)
                .resources(Map.ofEntries(
                        entry("CPUs", 2),
                        entry("RAM", 25),
                        entry("SSD", 25),
                        entry("Traffic", 250),
                        entry("SLA-EMail", true)
                ))
                .build();
        final var validator = forType(mangedServerBookingItemEntity.getType());

        // when
        final var result = validator.validate(mangedServerBookingItemEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'resources.CPUs' is not expected but is set to '2'",
                "'resources.SLA-EMail' is not expected but is set to 'true'",
                "'resources.RAM' is not expected but is set to '25'");
    }

    @Test
    void containsAllValidations() {
        // when
        final var validator = forType(MANAGED_WEBSPACE);

        // then
        assertThat(validator.properties()).map(Map::toString).containsExactlyInAnyOrder(
            "{type=integer, propertyName=SSD, required=true, unit=GB, min=1, max=100, step=1}",
            "{type=integer, propertyName=HDD, required=false, unit=GB, min=0, max=250, step=10}",
            "{type=integer, propertyName=Traffic, required=true, unit=GB, min=10, max=1000, step=10}",
            "{type=enumeration, propertyName=SLA-Platform, required=false, values=[BASIC, EXT24H]}",
            "{type=integer, propertyName=Daemons, required=false, unit=null, min=0, max=10, step=null}",
            "{type=boolean, propertyName=Online Office Server, required=false, falseIf=null}");
    }
}
