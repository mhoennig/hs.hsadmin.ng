package net.hostsharing.hsadminng.hs.booking.item.validators;

import static net.hostsharing.hsadminng.hs.validation.BooleanProperty.booleanProperty;
import static net.hostsharing.hsadminng.hs.validation.EnumerationProperty.enumerationProperty;
import static net.hostsharing.hsadminng.hs.validation.IntegerProperty.integerProperty;

class HsCloudServerBookingItemValidator extends HsBookingItemEntityValidator {

    HsCloudServerBookingItemValidator() {
        super(
            // @formatter:off
            booleanProperty("active")                                       .withDefault(true),

            integerProperty("CPUs")                 .min(  1)   .max(   32) .required(),
            integerProperty("RAM").unit("GB")       .min(  1)   .max(  128) .required(),
            integerProperty("SSD").unit("GB")       .min(  0)   .max( 1000) .step(25).required(), // (1)
            integerProperty("HDD").unit("GB")       .min(  0)   .max( 4000) .step(250).withDefault(0),
            integerProperty("Traffic").unit("GB")   .min(250)   .max(10000) .step(250).required(),

            enumerationProperty("SLA-Infrastructure").values("BASIC", "EXT8H", "EXT4H", "EXT2H").optional()
            // @formatter:on
        );

        // (q) We do have pre-existing CloudServers without SSD, just HDD, thus SSD starts with min=0.
        // TODO.impl: Validation that SSD+HDD is at minimum 25 GB is missing.
        // e.g. validationGroup("SSD", "HDD").min(0);
    }
}
