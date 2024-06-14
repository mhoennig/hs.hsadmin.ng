package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import static net.hostsharing.hsadminng.hs.validation.IntegerProperty.integerProperty;

class HsManagedServerHostingAssetValidator extends HsHostingAssetEntityValidator {

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
