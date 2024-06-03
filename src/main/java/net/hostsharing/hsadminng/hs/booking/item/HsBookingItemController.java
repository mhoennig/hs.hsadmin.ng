package net.hostsharing.hsadminng.hs.booking.item;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.api.HsBookingItemsApi;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingItemInsertResource;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingItemPatchResource;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingItemResource;
import net.hostsharing.hsadminng.mapper.KeyValueMap;
import net.hostsharing.hsadminng.mapper.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import static net.hostsharing.hsadminng.hs.booking.item.validators.HsBookingItemEntityValidators.valid;
import static net.hostsharing.hsadminng.mapper.PostgresDateRange.toPostgresDateRange;

@RestController
public class HsBookingItemController implements HsBookingItemsApi {

    @Autowired
    private Context context;

    @Autowired
    private Mapper mapper;

    @Autowired
    private HsBookingItemRepository bookingItemRepo;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsBookingItemResource>> listBookingItemsByProjectUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID projectUuid) {
        context.define(currentUser, assumedRoles);

        final var entities = bookingItemRepo.findAllByProjectUuid(projectUuid);

        final var resources = mapper.mapList(entities, HsBookingItemResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }

    @Override
    @Transactional
    public ResponseEntity<HsBookingItemResource> addBookingItem(
            final String currentUser,
            final String assumedRoles,
            final HsBookingItemInsertResource body) {

        context.define(currentUser, assumedRoles);

        final var entityToSave = mapper.map(body, HsBookingItemEntity.class, RESOURCE_TO_ENTITY_POSTMAPPER);

        final var saved = bookingItemRepo.save(valid(entityToSave));

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/booking/items/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsBookingItemResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<HsBookingItemResource> getBookingItemByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID bookingItemUuid) {

        context.define(currentUser, assumedRoles);

        final var result = bookingItemRepo.findByUuid(bookingItemUuid);
        return result
                .map(bookingItemEntity -> ResponseEntity.ok(
                        mapper.map(bookingItemEntity, HsBookingItemResource.class, ENTITY_TO_RESOURCE_POSTMAPPER)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteBookingIemByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID bookingItemUuid) {
        context.define(currentUser, assumedRoles);

        final var result = bookingItemRepo.deleteByUuid(bookingItemUuid);
        return result == 0
                ? ResponseEntity.notFound().build()
                : ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<HsBookingItemResource> patchBookingItem(
            final String currentUser,
            final String assumedRoles,
            final UUID bookingItemUuid,
            final HsBookingItemPatchResource body) {

        context.define(currentUser, assumedRoles);

        final var current = bookingItemRepo.findByUuid(bookingItemUuid).orElseThrow();

        new HsBookingItemEntityPatcher(current).apply(body);

        final var saved = bookingItemRepo.save(valid(current));
        final var mapped = mapper.map(saved, HsBookingItemResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(mapped);
    }

    final BiConsumer<HsBookingItemEntity, HsBookingItemResource> ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        resource.setValidFrom(entity.getValidity().lower());
        if (entity.getValidity().hasUpperBound()) {
            resource.setValidTo(entity.getValidity().upper().minusDays(1));
        }
    };

    @SuppressWarnings("unchecked")
    final BiConsumer<HsBookingItemInsertResource, HsBookingItemEntity> RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        entity.setValidity(toPostgresDateRange(resource.getValidFrom(), resource.getValidTo()));
        entity.putResources(KeyValueMap.from(resource.getResources()));
    };
}
