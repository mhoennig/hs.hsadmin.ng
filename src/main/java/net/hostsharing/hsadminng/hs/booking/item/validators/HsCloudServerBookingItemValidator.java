package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;

import static net.hostsharing.hsadminng.hs.validation.EnumerationPropertyValidator.enumerationProperty;
import static net.hostsharing.hsadminng.hs.validation.IntegerPropertyValidator.integerProperty;

class HsCloudServerBookingItemValidator extends HsEntityValidator<HsBookingItemEntity, HsBookingItemType> {

    HsCloudServerBookingItemValidator() {
        super(
            integerProperty("CPUs").min(1).max(32).required(),
            integerProperty("RAM").unit("GB").min(1).max(128).required(),
            integerProperty("SSD").unit("GB").min(25).max(1000).step(25).required(),
            integerProperty("HDD").unit("GB").min(0).max(4000).step(250).optional(),
            integerProperty("Traffic").unit("GB").min(250).max(10000).step(250).required(),
            enumerationProperty("SLA-Infrastructure").values("BASIC", "EXT8H", "EXT4H", "EXT2H").optional()
        );
    }
}
