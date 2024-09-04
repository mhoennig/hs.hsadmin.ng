package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRealEntity;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.validation.ValidationException;

import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.DOMAIN_SETUP;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.PRIVATE_CLOUD;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_SERVER;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.MANAGED_WEBSPACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class HsBookingItemEntityValidatorUnitTest {
    final HsBookingDebitorEntity debitor = HsBookingDebitorEntity.builder()
            .debitorNumber(12345)
            .build();
    final HsBookingProjectRealEntity project = HsBookingProjectRealEntity.builder()
            .debitor(debitor)
            .caption("test project")
            .build();

    private EntityManager em;

    @Test
    void rejectsInvalidEntity() {
        // given
        final var cloudServerBookingItemEntity = HsBookingItemRealEntity.builder()
                .type(CLOUD_SERVER)
                .project(project)
                .caption("Test-Server")
                .build();

        // when
        final var result = catchThrowable( ()-> HsBookingItemEntityValidatorRegistry.validated(em, cloudServerBookingItemEntity));

        // then
        assertThat(result).isInstanceOf(ValidationException.class)
                .hasMessageContaining(
                        "'D-12345:test project:Test-Server.resources.CPU' is required but missing",
                        "'D-12345:test project:Test-Server.resources.RAM' is required but missing",
                        "'D-12345:test project:Test-Server.resources.SSD' is required but missing",
                        "'D-12345:test project:Test-Server.resources.Traffic' is required but missing");
    }

    @Test
    void listsTypes() {
        // when
        final var result = HsBookingItemEntityValidatorRegistry.types();

        // then
        assertThat(result).containsExactlyInAnyOrder(PRIVATE_CLOUD, CLOUD_SERVER, MANAGED_SERVER, MANAGED_WEBSPACE, DOMAIN_SETUP);
    }
}
