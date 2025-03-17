package net.hostsharing.hsadminng.hs.hosting.asset;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.config.NoSecurityRequirement;
import net.hostsharing.hsadminng.hs.hosting.asset.validators.HostingAssetEntityValidatorRegistry;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.api.HsHostingAssetPropsApi;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetTypeResource;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
@Profile("!only-office")
@NoSecurityRequirement
public class HsHostingAssetPropsController implements HsHostingAssetPropsApi {

    @Override
    @Timed("app.hosting.assets.api.getListOfHostingAssetTypes")
    public ResponseEntity<List<String>> getListOfHostingAssetTypes() {
        final var resource = HostingAssetEntityValidatorRegistry.types().stream()
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(resource);
    }

    @Override
    @Timed("app.hosting.assets.api.getListOfHostingAssetTypeProps")
    public ResponseEntity<List<Object>> getListOfHostingAssetTypeProps(
            final HsHostingAssetTypeResource assetType) {

        final Enum<HsHostingAssetType> type = HsHostingAssetType.of(assetType);
        final var propValidators = HostingAssetEntityValidatorRegistry.forType(type);
        final List<Map<String, Object>> resource = propValidators.properties();
        return ResponseEntity.ok(toListOfObjects(resource));
    }

    private List<Object> toListOfObjects(final List<Map<String, Object>> resource) {
        // OpenApi ony generates List<Object> not List<Map<String, Object>> for the Java interface.
        // But Spring properly converts the List of Maps, thus we can simply cast the type:
        //noinspection rawtypes,unchecked
        return (List) resource;
    }
}
