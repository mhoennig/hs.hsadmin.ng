package net.hostsharing.hsadminng.hs.booking.item.validators;

import static net.hostsharing.hsadminng.hs.validation.EnumerationProperty.enumerationProperty;
import static net.hostsharing.hsadminng.hs.validation.IntegerProperty.integerProperty;

class HsPrivateCloudBookingItemValidator extends HsBookingItemEntityValidator {

    HsPrivateCloudBookingItemValidator() {
        super(
            integerProperty("CPUs").min(4).max(128).required().asTotalLimit(),
            integerProperty("RAM").unit("GB").min(4).max(512).required().asTotalLimit(),
            integerProperty("SSD").unit("GB").min(100).max(4000).step(25).required().asTotalLimit(),
            integerProperty("HDD").unit("GB").min(0).max(16000).step(25).withDefault(0).asTotalLimit(),
            integerProperty("Traffic").unit("GB").min(1000).max(40000).step(250).required().asTotalLimit(),
            enumerationProperty("SLA-Infrastructure").values("BASIC", "EXT8H", "EXT4H", "EXT2H").withDefault("BASIC")
        );
    }
}
