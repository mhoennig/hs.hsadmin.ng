package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.hs.hosting.asset.validator.HsHostingAssetValidator;
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

import jakarta.validation.ValidationException;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;


@RestController
public class HsHostingAssetController implements HsHostingAssetsApi {

    @Autowired
    private Context context;

    @Autowired
    private Mapper mapper;

    @Autowired
    private HsHostingAssetRepository assetRepo;

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

        final var saved = assetRepo.save(valid(entityToSave));

        final var uri =
                MvcUriComponentsBuilder.fromController(getClass())
                        .path("/api/hs/hosting/assets/{id}")
                        .buildAndExpand(saved.getUuid())
                        .toUri();
        final var mapped = mapper.map(saved, HsHostingAssetResource.class);
        return ResponseEntity.created(uri).body(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<HsHostingAssetResource> getAssetByUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID serverUuid) {

        context.define(currentUser, assumedRoles);

        final var result = assetRepo.findByUuid(serverUuid);
        return result
                .map(serverEntity -> ResponseEntity.ok(
                        mapper.map(serverEntity, HsHostingAssetResource.class)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteAssetUuid(
            final String currentUser,
            final String assumedRoles,
            final UUID serverUuid) {
        context.define(currentUser, assumedRoles);

        final var result = assetRepo.deleteByUuid(serverUuid);
        return result == 0
                ? ResponseEntity.notFound().build()
                : ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<HsHostingAssetResource> patchAsset(
            final String currentUser,
            final String assumedRoles,
            final UUID serverUuid,
            final HsHostingAssetPatchResource body) {

        context.define(currentUser, assumedRoles);

        final var current = assetRepo.findByUuid(serverUuid).orElseThrow();

        new HsHostingAssetEntityPatcher(current).apply(body);

        final var saved = assetRepo.save(current);
        final var mapped = mapper.map(saved, HsHostingAssetResource.class);
        return ResponseEntity.ok(mapped);
    }

    private HsHostingAssetEntity valid(final HsHostingAssetEntity entityToSave) {
        final var violations = HsHostingAssetValidator.forType(entityToSave.getType()).validate(entityToSave);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations.toString());
        }
        return entityToSave;
    }

    @SuppressWarnings("unchecked")
    final BiConsumer<HsHostingAssetInsertResource, HsHostingAssetEntity> RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        entity.putConfig(KeyValueMap.from(resource.getConfig()));
    };
}
