package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.validators.HsBookingItemEntityValidators.forType;
import static org.assertj.core.api.Assertions.assertThat;

class HsManagedServerBookingItemValidatorUnitTest {

    @Test
    void validatesProperties() {
        // given
        final var validator = HsBookingItemEntityValidators.forType(MANAGED_SERVER);
        final var mangedServerBookingItemEntity = HsBookingItemEntity.builder()
                .type(MANAGED_SERVER)
                .resources(Map.ofEntries(
                        entry("CPUs", 2),
                        entry("RAM", 25),
                        entry("SSD", 25),
                        entry("Traffic", 250),
                        entry("SLA-EMail", true)
                ))
                .build();

        // when
        final var result = validator.validate(mangedServerBookingItemEntity);

        // then
        assertThat(result).containsExactly("'resources.SLA-EMail' is expected to be false because resources.SLA-Platform=BASIC but is true");
    }

    @Test
    void containsAllValidations() {
        // when
        final var validator = forType(MANAGED_SERVER);

        // then
        assertThat(validator.properties()).map(Map::toString).containsExactlyInAnyOrder(
                "{type=integer, propertyName=CPUs, required=true, unit=null, min=1, max=32, step=null}",
                "{type=integer, propertyName=RAM, required=true, unit=GB, min=1, max=128, step=null}",
                "{type=integer, propertyName=SSD, required=true, unit=GB, min=25, max=1000, step=25}",
                "{type=integer, propertyName=HDD, required=false, unit=GB, min=0, max=4000, step=250}",
                "{type=integer, propertyName=Traffic, required=true, unit=GB, min=250, max=10000, step=250}",
                "{type=enumeration, propertyName=SLA-Platform, required=false, values=[BASIC, EXT8H, EXT4H, EXT2H]}",
                "{type=boolean, propertyName=SLA-EMail, required=false, falseIf={SLA-Platform=BASIC}}",
                "{type=boolean, propertyName=SLA-Maria, required=false, falseIf={SLA-Platform=BASIC}}",
                "{type=boolean, propertyName=SLA-PgSQL, required=false, falseIf={SLA-Platform=BASIC}}",
                "{type=boolean, propertyName=SLA-Office, required=false, falseIf={SLA-Platform=BASIC}}",
                "{type=boolean, propertyName=SLA-Web, required=false, falseIf={SLA-Platform=BASIC}}");
    }
}
