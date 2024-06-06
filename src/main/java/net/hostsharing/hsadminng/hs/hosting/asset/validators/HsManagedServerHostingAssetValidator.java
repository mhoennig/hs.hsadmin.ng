package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;

import static net.hostsharing.hsadminng.hs.validation.IntegerPropertyValidator.integerProperty;

class HsManagedServerHostingAssetValidator extends HsEntityValidator<HsHostingAssetEntity, HsHostingAssetType> {

    public HsManagedServerHostingAssetValidator() {
        super(
                integerProperty("monit_min_free_ssd").min(1).max(1000).optional(),
                integerProperty("monit_min_free_hdd").min(1).max(4000).optional(),
                integerProperty("monit_max_ssd_usage").unit("%").min(10).max(100).required(),
                integerProperty("monit_max_hdd_usage").unit("%").min(10).max(100).optional(),
                integerProperty("monit_max_cpu_usage").unit("%").min(10).max(100).required(),
                integerProperty("monit_max_ram_usage").unit("%").min(10).max(100).required()
                // TODO: stringProperty("monit_alarm_email").unit("GB").optional()
        );
    }
}
