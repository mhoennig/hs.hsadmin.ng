package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealRepository;
import net.hostsharing.hsadminng.hs.hosting.asset.validators.HostingAssetEntitySaveProcessor;
import net.hostsharing.hsadminng.hs.hosting.asset.validators.HostingAssetEntityValidatorRegistry;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.api.HsHostingAssetsApi;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetInsertResource;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetPatchResource;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetResource;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetTypeResource;
import net.hostsharing.hsadminng.mapper.KeyValueMap;
import net.hostsharing.hsadminng.mapper.StandardMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

@RestController
public class HsHostingAssetController implements HsHostingAssetsApi {

    @Autowired
    private EntityManagerWrapper emw;

    @Autowired
    private Context context;

    @Autowired
    private StandardMapper mapper;

    @Autowired
    private HsHostingAssetRbacRepository rbacAssetRepo;

    @Autowired
    private HsHostingAssetRealRepository realAssetRepo;

    @Autowired
    private HsBookingItemRealRepository realBookingItemRepo;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<HsHostingAssetResource>> listAssets(
            final String currentSubject,
            final String assumedRoles,
            final UUID debitorUuid,
            final UUID parentAssetUuid,
            final HsHostingAssetTypeResource type) {
        context.define(currentSubject, assumedRoles);

        final var entities = rbacAssetRepo.findAllByCriteria(debitorUuid, parentAssetUuid, HsHostingAssetType.of(type));

        final var resources = mapper.mapList(entities, HsHostingAssetResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }


    @Override
    @Transactional
    public ResponseEntity<HsHostingAssetResource> addAsset(
            final String currentSubject,
            final String assumedRoles,
            final HsHostingAssetInsertResource body) {

        context.define(currentSubject, assumedRoles);

        final var entity = mapper.map(body, HsHostingAssetRbacEntity.class, RESOURCE_TO_ENTITY_POSTMAPPER);

        final var mapped = new HostingAssetEntitySaveProcessor(emw, entity)
                .preprocessEntity()
                .validateEntity()
                .prepareForSave()
                .save()
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
            final String currentSubject,
            final String assumedRoles,
            final UUID assetUuid) {

        context.define(currentSubject, assumedRoles);

        final var result = rbacAssetRepo.findByUuid(assetUuid);
        return result
                .map(assetEntity -> ResponseEntity.ok(
                        mapper.map(assetEntity, HsHostingAssetResource.class, ENTITY_TO_RESOURCE_POSTMAPPER)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteAssetUuid(
            final String currentSubject,
            final String assumedRoles,
            final UUID assetUuid) {
        context.define(currentSubject, assumedRoles);

        final var result = rbacAssetRepo.deleteByUuid(assetUuid);
        return result == 0
                ? ResponseEntity.notFound().build()
                : ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<HsHostingAssetResource> patchAsset(
            final String currentSubject,
            final String assumedRoles,
            final UUID assetUuid,
            final HsHostingAssetPatchResource body) {

        context.define(currentSubject, assumedRoles);

        final var entity = rbacAssetRepo.findByUuid(assetUuid).orElseThrow();

        new HsHostingAssetEntityPatcher(emw, entity).apply(body);

        final var mapped = new HostingAssetEntitySaveProcessor(emw, entity)
                .preprocessEntity()
                .validateEntity()
                .prepareForSave()
                .save()
                .validateContext()
                .mapUsing(e -> mapper.map(e, HsHostingAssetResource.class))
                .revampProperties();

        return ResponseEntity.ok(mapped);
    }

    final BiConsumer<HsHostingAssetInsertResource, HsHostingAssetRbacEntity> RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        entity.putConfig(KeyValueMap.from(resource.getConfig()));
        if (resource.getBookingItemUuid() != null) {
            entity.setBookingItem(realBookingItemRepo.findByUuid(resource.getBookingItemUuid())
                    .orElseThrow(() -> new EntityNotFoundException("ERROR: [400] bookingItemUuid %s not found".formatted(
                            resource.getBookingItemUuid()))));
        }
        if (resource.getParentAssetUuid() != null) {
            entity.setParentAsset(realAssetRepo.findByUuid(resource.getParentAssetUuid())
                    .orElseThrow(() -> new EntityNotFoundException("ERROR: [400] parentAssetUuid %s not found".formatted(
                            resource.getParentAssetUuid()))));
        }
    };

    @SuppressWarnings("unchecked")
    final BiConsumer<HsHostingAssetRbacEntity, HsHostingAssetResource> ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource)
            -> resource.setConfig(HostingAssetEntityValidatorRegistry.forType(entity.getType())
                .revampProperties(emw, entity, (Map<String, Object>) resource.getConfig()));
}
