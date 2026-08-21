package net.hostsharing.hsadminng.hs.hosting.asset;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRbacRepository;
import net.hostsharing.hsadminng.hs.hosting.asset.validators.HostingAssetEntitySaveProcessor;
import net.hostsharing.hsadminng.hs.hosting.asset.validators.HostingAssetEntityValidatorRegistry;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.api.HsHostingAssetsApi;

import net.hostsharing.hsadminng.rbac.context.Context;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetInsertResource;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetPatchResource;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetResource;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetTypeResource;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRbacRepository;
import net.hostsharing.hsadminng.mapper.KeyValueMap;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

@RestController
@Profile("!only-prod-schema")
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
public class HsHostingAssetController implements HsHostingAssetsApi {

    @Autowired
    private EntityManagerWrapper emw;

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private HsHostingAssetRbacRepository rbacAssetRepo;

    @Autowired
    private HsBookingItemRbacRepository rbacBookingItemRepo;

    @Autowired
    private HsOfficeContactRbacRepository rbacContactRepo;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.hosting.assets.api.getListOfHostingAssets")
    public ResponseEntity<List<HsHostingAssetResource>> getListOfHostingAssets(
            final String assumedRoles,
            final UUID debitorUuid,
            final UUID parentAssetUuid,
            final HsHostingAssetTypeResource type) {
        context.assumeRoles(assumedRoles);

        final var entities = rbacAssetRepo.findAllByCriteria(debitorUuid, parentAssetUuid, HsHostingAssetType.of(type));

        final var resources = mapper.mapList(entities, HsHostingAssetResource.class, ENTITY_TO_RESOURCE_POSTMAPPER);
        return ResponseEntity.ok(resources);
    }


    @Override
    @Transactional
    @Timed("app.hosting.assets.api.postNewHostingAsset")
    public ResponseEntity<HsHostingAssetResource> postNewHostingAsset(
            final String assumedRoles,
            final HsHostingAssetInsertResource body) {

        context.assumeRoles(assumedRoles);

        final var entity = mapper.map(body, HsHostingAssetRbacEntity.class, RESOURCE_TO_ENTITY_POSTMAPPER);

        final var mapped = new HostingAssetEntitySaveProcessor(emw, entity)
                .preprocessEntity()
                .validateEntity()
                .prepareForSave()
                .save()
                .validateContext()
                .mapUsing(e -> mapper.map(e, HsHostingAssetResource.class, ASSIGNED_TO_ASSET_POSTMAPPER))
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
    @Timed("app.hosting.assets.api.getSingleHostingAssetByUuid")
    public ResponseEntity<HsHostingAssetResource> getSingleHostingAssetByUuid(
            final String assumedRoles,
            final UUID assetUuid) {

        context.assumeRoles(assumedRoles);

        final var result = rbacAssetRepo.findByUuid(assetUuid);
        return result
                .map(assetEntity -> ResponseEntity.ok(
                        mapper.map(assetEntity, HsHostingAssetResource.class, ENTITY_TO_RESOURCE_POSTMAPPER)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    @Transactional
    @Timed("app.hosting.assets.api.deleteHostingAssetByUuid")
    public ResponseEntity<Void> deleteHostingAssetByUuid(
            final String assumedRoles,
            final UUID assetUuid) {
        context.assumeRoles(assumedRoles);

        final var result = rbacAssetRepo.deleteByUuid(assetUuid);
        return result == 0
                ? ResponseEntity.notFound().build()
                : ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    @Timed("app.hosting.assets.api.patchHostingAsset")
    public ResponseEntity<HsHostingAssetResource> patchHostingAsset(
            final String assumedRoles,
            final UUID assetUuid,
            final HsHostingAssetPatchResource body) {

        context.assumeRoles(assumedRoles);

        final var entity = rbacAssetRepo.findByUuid(assetUuid).orElseThrow();

        new HsHostingAssetEntityPatcher(rbacContactRepo, rbacAssetRepo, entity).apply(body);

        final var mapped = new HostingAssetEntitySaveProcessor(emw, entity)
                .preprocessEntity()
                .validateEntity()
                .prepareForSave()
                .save()
                .validateContext()
                .mapUsing(e -> mapper.map(e, HsHostingAssetResource.class, ASSIGNED_TO_ASSET_POSTMAPPER))
                .revampProperties();

        return ResponseEntity.ok(mapped);
    }

    // these lookups must resolve via the restricted views: the DB-side insert-check only validates
    // NEW.parentAssetUuid and short-circuits for DOMAIN_SETUP, it never checks bookingItemUuid,
    // and invisible references must yield the same 404 as unknown ones (A-02)
    final BiConsumer<HsHostingAssetInsertResource, HsHostingAssetRbacEntity> RESOURCE_TO_ENTITY_POSTMAPPER = (resource, entity) -> {
        entity.putConfig(KeyValueMap.from("config", resource.getConfig()));
        if (resource.getBookingItemUuid() != null) {
            entity.setBookingItem(rbacBookingItemRepo.findVisibleByUuid(resource.getBookingItemUuid())
                    .orElseThrow(() -> new NoSuchElementException(
                            "cannot find bookingItem by bookingItemUuid: " + resource.getBookingItemUuid())));
        }
        if (resource.getParentAssetUuid() != null) {
            entity.setParentAsset(rbacAssetRepo.findVisibleByUuid(resource.getParentAssetUuid())
                    .orElseThrow(() -> new NoSuchElementException(
                            "cannot find parentAsset by parentAssetUuid: " + resource.getParentAssetUuid())));
        }
        if (resource.getAssignedToAssetUuid() != null) {
            entity.setAssignedToAsset(rbacAssetRepo.findVisibleByUuid(resource.getAssignedToAssetUuid())
                    .orElseThrow(() -> new NoSuchElementException(
                            "cannot find assignedToAsset by assignedToAssetUuid: " + resource.getAssignedToAssetUuid())));
        }
    };

    // ModelMapper in strict mode does not flatten an entity reference into its uuid property
    final BiConsumer<HsHostingAsset, HsHostingAssetResource> ASSIGNED_TO_ASSET_POSTMAPPER = (entity, resource)
            -> resource.setAssignedToAssetUuid(
                    Optional.ofNullable(entity.getAssignedToAsset()).map(HsHostingAsset::getUuid).orElse(null));

    @SuppressWarnings("unchecked")
    final BiConsumer<HsHostingAssetRbacEntity, HsHostingAssetResource> ENTITY_TO_RESOURCE_POSTMAPPER = (entity, resource) -> {
        ASSIGNED_TO_ASSET_POSTMAPPER.accept(entity, resource);
        resource.setConfig(HostingAssetEntityValidatorRegistry.forType(entity.getType())
                .revampProperties(emw, entity, (Map<String, Object>) resource.getConfig()));
    };
}
