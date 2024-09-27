package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.hs.booking.item.BookingItemCreatedEvent;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.validators.HostingAssetEntitySaveProcessor;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HsBookingItemCreatedListener implements ApplicationListener<BookingItemCreatedEvent> {

    @Autowired
    private EntityManagerWrapper emw;

    @Override
    public void onApplicationEvent(final BookingItemCreatedEvent event) {
        System.out.println("Received newly created booking item: " + event.getNewBookingItem());
        final var newBookingItemRealEntity =
                emw.getReference(HsBookingItemRealEntity.class, event.getNewBookingItem().getUuid());
        final var newHostingAsset = switch (newBookingItemRealEntity.getType()) {
            case PRIVATE_CLOUD -> null;
            case CLOUD_SERVER -> null;
            case MANAGED_SERVER -> null;
            case MANAGED_WEBSPACE -> null;
            case DOMAIN_SETUP -> createDomainSetupHostingAsset(newBookingItemRealEntity);
        };
        if (newHostingAsset != null) {
            try {
                new HostingAssetEntitySaveProcessor(emw, newHostingAsset)
                        .preprocessEntity()
                        .validateEntity()
                        .prepareForSave()
                        .save()
                        .validateContext();
            } catch (final Exception e) {
                // TODO.impl: store status in a separate field, maybe enum+message
                newBookingItemRealEntity.getResources().put("status", e.getMessage());
            }
        }
    }

    private HsHostingAsset createDomainSetupHostingAsset(final HsBookingItemRealEntity fromBookingItem) {
        return HsHostingAssetRbacEntity.builder()
                .bookingItem(fromBookingItem)
                .type(HsHostingAssetType.DOMAIN_SETUP)
                .identifier(fromBookingItem.getDirectValue("domainName", String.class))
                .subHostingAssets(List.of(
                        // TARGET_UNIX_USER_PROPERTY_NAME
                ))
                .build();
    }
}
