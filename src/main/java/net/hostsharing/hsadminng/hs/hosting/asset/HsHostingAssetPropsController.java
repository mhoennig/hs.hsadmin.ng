package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.hs.hosting.asset.validator.HsHostingAssetValidator;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.api.HsHostingAssetPropsApi;
import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetTypeResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
public class HsHostingAssetPropsController implements HsHostingAssetPropsApi {

    @Override
    public ResponseEntity<List<String>> listAssetTypes() {
        final var resource = HsHostingAssetValidator.types().stream()
                .map(Enum::name)
                .toList();
        return ResponseEntity.ok(resource);
    }

    @Override
    public ResponseEntity<List<Object>> listAssetTypeProps(
            final HsHostingAssetTypeResource assetType) {

        final var propValidators = HsHostingAssetValidator.forType(HsHostingAssetType.of(assetType));
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
