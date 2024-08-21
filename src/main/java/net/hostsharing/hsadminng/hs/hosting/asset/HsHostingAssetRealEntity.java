package net.hostsharing.hsadminng.hs.hosting.asset;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "hs_hosting_asset")
@SuperBuilder(builderMethodName = "genericBuilder", toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
public class HsHostingAssetRealEntity extends HsHostingAsset {

    // without this wrapper method, the builder returns a generic entity which cannot resolved in a generic context
    public static HsHostingAssetRealEntityBuilder<HsHostingAssetRealEntity, ?> builder() {
        //noinspection unchecked
        return (HsHostingAssetRealEntityBuilder<HsHostingAssetRealEntity, ?>) genericBuilder();
    }
}
