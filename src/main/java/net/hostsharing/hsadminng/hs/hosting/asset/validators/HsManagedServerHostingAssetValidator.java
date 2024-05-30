package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;

import static net.hostsharing.hsadminng.hs.validation.IntegerPropertyValidator.integerProperty;

class HsManagedServerHostingAssetValidator extends HsEntityValidator<HsHostingAssetEntity, HsHostingAssetType> {

    public HsManagedServerHostingAssetValidator() {
        super(
                integerProperty("CPUs").min(1).max(32).required(),
                integerProperty("RAM").unit("GB").min(1).max(128).required(),
                integerProperty("SSD").unit("GB").min(25).max(1000).step(25).required(),
                integerProperty("HDD").unit("GB").min(0).max(4000).step(250).optional(),
                integerProperty("Traffic").unit("GB").min(250).max(10000).step(250).required()
        );
    }
}
