package net.hostsharing.hsadminng.hs.hosting.asset.factories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsHostingAssetAutoInsertResource;
import net.hostsharing.hsadminng.hs.booking.item.BookingItemCreatedAppEvent;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.mapper.StandardMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class HsBookingItemCreatedListener implements ApplicationListener<BookingItemCreatedAppEvent> {

    @Autowired
    private EntityManagerWrapper emw;

    @Autowired
    private ObjectMapper jsonMapper;

    @Autowired
    private StandardMapper standardMapper;

    @Override
    @SneakyThrows
    public void onApplicationEvent(@NotNull BookingItemCreatedAppEvent bookingItemCreatedAppEvent) {
        if (containsAssetJson(bookingItemCreatedAppEvent)) {
            createRelatedHostingAsset(bookingItemCreatedAppEvent);
        }
    }

    private static boolean containsAssetJson(final BookingItemCreatedAppEvent bookingItemCreatedAppEvent) {
        return bookingItemCreatedAppEvent.getEntity().getAssetJson() != null;
    }

    private void createRelatedHostingAsset(final BookingItemCreatedAppEvent event) throws JsonProcessingException {
        final var newBookingItemRealEntity = event.getEntity().getBookingItem();
        final var asset = jsonMapper.readValue(event.getEntity().getAssetJson(), HsHostingAssetAutoInsertResource.class);
        final var factory = switch (newBookingItemRealEntity.getType()) {
            case PRIVATE_CLOUD, CLOUD_SERVER, MANAGED_SERVER ->
                    forNowNoAutomaticHostingAssetCreationPossible(emw, newBookingItemRealEntity, asset, standardMapper);
            case MANAGED_WEBSPACE -> new ManagedWebspaceHostingAssetFactory(emw, newBookingItemRealEntity, asset, standardMapper);
            case DOMAIN_SETUP -> new DomainSetupHostingAssetFactory(emw, newBookingItemRealEntity, asset, standardMapper);
        };
        if (factory != null) {
            final var statusMessage = factory.createAndPersist();
            // TODO.impl: once we implement retry, we need to amend this code (persist/merge/delete)
            if (statusMessage != null) {
                event.getEntity().setStatusMessage(statusMessage);
                emw.persist(event.getEntity());
            }
        }
    }

    private HostingAssetFactory forNowNoAutomaticHostingAssetCreationPossible(
            final EntityManagerWrapper emw,
            final HsBookingItemRealEntity fromBookingItem,
            final HsHostingAssetAutoInsertResource asset,
            final StandardMapper standardMapper
    ) {
        return new HostingAssetFactory(emw, fromBookingItem, asset, standardMapper) {

            @Override
            protected HsHostingAsset create() {
                // TODO.impl: we should validate the asset JSON, but some violations are un-avoidable at that stage
                throw new ValidationException("waiting for manual setup of hosting asset for booking item of type " + fromBookingItem.getType());
            }
        };
    }
}
