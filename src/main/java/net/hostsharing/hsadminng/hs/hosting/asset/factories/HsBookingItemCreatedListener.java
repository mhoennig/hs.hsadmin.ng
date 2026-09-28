package net.hostsharing.hsadminng.hs.hosting.asset.factories;

import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;
import lombok.val;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsHostingAssetAutoInsertResource;
import net.hostsharing.hsadminng.hs.booking.item.BookingItemCreatedAppEvent;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRbacRepository;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRbacRepository;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("!only-prod-schema")
public class HsBookingItemCreatedListener implements ApplicationListener<BookingItemCreatedAppEvent> {

    @Autowired
    private EntityManagerWrapper emw;

    @Autowired
    private ObjectMapper jsonMapper;

    @Autowired
    private StrictMapper StrictMapper;

    @Autowired
    private HsOfficeContactRbacRepository rbacContactRepo;

    @Autowired
    private HsHostingAssetRbacRepository rbacAssetRepo;

    @Override
    @SneakyThrows
    public void onApplicationEvent(@NotNull BookingItemCreatedAppEvent bookingItemCreatedAppEvent) {
        createRelatedHostingAsset(bookingItemCreatedAppEvent);
    }

    private void createRelatedHostingAsset(final BookingItemCreatedAppEvent event) throws JacksonException {
        final var newBookingItemRealEntity = event.getEntity().getBookingItem();
        final var asset = deserializedAsset(event);
        final var factory = switch (newBookingItemRealEntity.getType()) {
            // these wait for manual setup in any case, thus even without a requested hosting asset
            case PRIVATE_CLOUD, CLOUD_SERVER, MANAGED_SERVER ->
                    forNowNoAutomaticHostingAssetCreationPossible(emw, newBookingItemRealEntity, asset, StrictMapper);
            case MANAGED_WEBSPACE -> asset == null ? null
                    : new ManagedWebspaceHostingAssetFactory(emw, newBookingItemRealEntity, asset, StrictMapper);
            case DOMAIN_SETUP -> asset == null ? null
                    : new DomainSetupHostingAssetFactory(
                            emw, newBookingItemRealEntity, asset, StrictMapper, rbacContactRepo, rbacAssetRepo);
        };
        if (factory != null) {
            factory.validateRequest();
            final var statusMessage = factory.createAndPersist();
            // TODO.impl: once we implement retry, we need to amend this code (persist/merge/delete)
            if (statusMessage != null) {
                event.getEntity().setStatusMessage(statusMessage);
                emw.persist(event.getEntity());
            }
        }
    }

    private HsHostingAssetAutoInsertResource deserializedAsset(final BookingItemCreatedAppEvent event)
            throws JacksonException {
        val assetJson = event.getEntity().getAssetJson();
        // event entities written before the JSON text "null" got suppressed still carry it
        return assetJson == null ? null : jsonMapper.readValue(assetJson, HsHostingAssetAutoInsertResource.class);
    }

    private HostingAssetFactory forNowNoAutomaticHostingAssetCreationPossible(
            final EntityManagerWrapper emw,
            final HsBookingItemRealEntity fromBookingItem,
            final HsHostingAssetAutoInsertResource asset,
            final StrictMapper StrictMapper
    ) {
        return new HostingAssetFactory(emw, fromBookingItem, asset, StrictMapper) {

            @Override
            protected HsHostingAsset create() {
                // TODO.impl: we should validate the asset JSON, but some violations are un-avoidable at that stage
                throw new ValidationException("waiting for manual setup of hosting asset for booking item of type " + fromBookingItem.getType());
            }
        };
    }
}
