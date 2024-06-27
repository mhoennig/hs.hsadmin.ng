package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRepository;
import net.hostsharing.hsadminng.hs.hosting.asset.validators.HsHostingAssetEntityValidatorRegistry;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.api.HsHostingAssetsApi;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetInsertResource;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetPatchResource;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetResource;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetTypeResource;
import net.hostsharing.hsadminng.mapper.KeyValueMap;
import net.hostsharing.hsadminng.mapper.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import static net.hostsharing.hsadminng.hs.hosting.asset.validators.HsHostingAssetEntityValidatorRegistry.validated;

@RestController
public class HsHostingAssetController implements HsHostingAssetsApi {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private Context context;

    @Autowired
    private Mapper mapper;

    @Autowired
    private HsHostingAssetRepository assetRepo;

    @Autowired
    private HsBookingItemRepository bookingItemRepo;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsHostingAssetResource>> listAssets(
            final String currentUser,
            final String assumedRoles,
            final UUID debitorUuid,
            final UUID parentAssetUuid,
            final HsHostingAssetTypeResource type) {
        context.define(currentUser, assumedRoles);

        final var entities = assetRepo.findAllByCriteria(debitorUuid, parentAssetUuid, HsHostingAssetType.of(type));

        final var resources = mapper.mapList(entities, HsHostingAssetResource.class);
        return ResponseEntity.ok(resources);
    }


    @Override
    @Transactional
    public ResponseEntity<HsHostingAssetResource> addAsset(
            final String currentUser,
            final String assumedRoles,
            final HsHostingAssetInsertResource body) {

        context.define(currentUser, assumedRoles);

        final var entityToSave = mapper.map(body, HsHostingAssetEntity.class, RESOURCE_TO_ENTITY_POSTMAPPER);

        final var saved = validated(assetRepo.save(entityToSave));

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/hosting/assets/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsHostingAssetResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<HsHostingAssetResource> getAssetByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID assetUuid) {

        context.define(currentUser, assumedRoles);

        final var result = assetRepo.findByUuid(assetUuid);
        return result
                .map(assetEntity -> ResponseEntity.ok(
                        mapper.map(assetEntity, HsHostingAssetResource.class, ENTITY_TO_RESOURCE_POSTMAPPER)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteAssetUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID assetUuid) {
        context.define(currentUser, assumedRoles);

        final var result = assetRepo.deleteByUuid(assetUuid);
        return result == 0
                ? ResponseEntity.notFound().build()
                : ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<HsHostingAssetResource> patchAsset(
            final String currentUser,
            final String assumedRoles,
            final UUID assetUuid,
            final HsHostingAssetPatchResource body) {

        context.define(currentUser, assumedRoles);

        final var current = assetRepo.findByUuid(assetUuid).orElseThrow();

        new HsHostingAssetEntityPatcher(em, current).apply(body);

//      TODO.refa: draft for an alternative API
//      validate(current) // self-validation, hashing passwords etc.
//          .then(HsHostingAssetEntityValidatorRegistry::prepareForSave) // hashing passwords etc.
//          .then(assetRepo::save)
//          .then(HsHostingAssetEntityValidatorRegistry::validateInContext)
//          // In this last step we need the entity and the mapped resource instance,
//          // which is exactly what a postmapper takes as arguments.
//          .then(this::mapToResource) using postProcessProperties to remove write-only + add read-only properties

        final var saved = validated(assetRepo.save(current));
        final var mapped = mapper.map(saved, HsHostingAssetResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(mapped);
    }

    final BiConsumer<HsHostingAssetInsertResource, HsHostingAssetEntity> RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        entity.putConfig(KeyValueMap.from(resource.getConfig()));
        if (resource.getBookingItemUuid() != null) {
            entity.setBookingItem(bookingItemRepo.findByUuid(resource.getBookingItemUuid())
                    .orElseThrow(() -> new EntityNotFoundException("ERROR: [400] bookingItemUuid %s not found".formatted(
                            resource.getBookingItemUuid()))));
        }
        if (resource.getParentAssetUuid() != null) {
            entity.setParentAsset(assetRepo.findByUuid(resource.getParentAssetUuid())
                    .orElseThrow(() -> new EntityNotFoundException("ERROR: [400] parentAssetUuid %s not found".formatted(
                            resource.getParentAssetUuid()))));
        }
    };

    final BiConsumer<HsHostingAssetEntity, HsHostingAssetResource> ENTITY_TO_RESOURCE_POSTMAPPER
            = HsHostingAssetEntityValidatorRegistry::postprocessProperties;
}
