package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.validation.HsEntityValidator;


import static net.hostsharing.hsadminng.hs.validation.BooleanPropertyValidator.booleanProperty;
import static net.hostsharing.hsadminng.hs.validation.EnumerationPropertyValidator.enumerationProperty;
import static net.hostsharing.hsadminng.hs.validation.IntegerPropertyValidator.integerProperty;

class HsManagedWebspaceBookingItemValidator extends HsEntityValidator<HsBookingItemEntity, HsBookingItemType> {

    public HsManagedWebspaceBookingItemValidator() {
        super(
            integerProperty("SSD").unit("GB").min(1).max(100).step(1).required(),
            integerProperty("HDD").unit("GB").min(0).max(250).step(10).optional(),
            integerProperty("Traffic").unit("GB").min(10).max(1000).step(10).required(),
            enumerationProperty("SLA-Platform").values("BASIC", "EXT24H").optional(),
            integerProperty("Daemons").min(0).max(10).optional(),
            booleanProperty("Online Office Server").optional()
        );
    }
}
