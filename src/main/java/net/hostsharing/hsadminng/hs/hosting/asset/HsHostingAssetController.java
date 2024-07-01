package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRepository;
import net.hostsharing.hsadminng.hs.hosting.asset.validators.HsHostingAssetEntityProcessor;
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
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

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

        final var resources = mapper.mapList(entities, HsHostingAssetResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }


    @Override
    @Transactional
    public ResponseEntity<HsHostingAssetResource> addAsset(
            final String currentUser,
            final String assumedRoles,
            final HsHostingAssetInsertResource body) {

        context.define(currentUser, assumedRoles);

        final var entity = mapper.map(body, HsHostingAssetEntity.class, RESOURCE_TO_ENTITY_POSTMAPPER);

        final var mapped = new HsHostingAssetEntityProcessor(entity)
                .validateEntity()
                .prepareForSave()
                .saveUsing(assetRepo::save)
                .validateContext()
                .mapUsing(e -> mapper.map(e, HsHostingAssetResource.class))
                .revampProperties();

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/hosting/assets/{id}")
                        .buildAndExpand(mapped.getUuid())
                        .toUri();
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

        final var entity = assetRepo.findByUuid(assetUuid).orElseThrow();

        new HsHostingAssetEntityPatcher(em, entity).apply(body);

        final var mapped = new HsHostingAssetEntityProcessor(entity)
                .validateEntity()
                .prepareForSave()
                .saveUsing(assetRepo::save)
                .validateContext()
                .mapUsing(e -> mapper.map(e, HsHostingAssetResource.class))
                .revampProperties();

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

    @SuppressWarnings("unchecked")
    final BiConsumer<HsHostingAssetEntity, HsHostingAssetResource> ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource)
            -> HsHostingAssetEntityValidatorRegistry.forType(entity.getType())
                .revampProperties(entity, (Map<String, Object>) resource.getConfig());
}
