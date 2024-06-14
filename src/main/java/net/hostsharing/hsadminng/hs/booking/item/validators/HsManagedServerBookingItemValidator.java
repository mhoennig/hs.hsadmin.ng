package net.hostsharing.hsadminng.hs.booking.item.validators;



import static net.hostsharing.hsadminng.hs.validation.BooleanProperty.booleanProperty;
import static net.hostsharing.hsadminng.hs.validation.EnumerationProperty.enumerationProperty;
import static net.hostsharing.hsadminng.hs.validation.IntegerProperty.integerProperty;

class HsManagedServerBookingItemValidator extends HsBookingItemEntityValidator {

    HsManagedServerBookingItemValidator() {
        super(
            integerProperty("CPUs").min(1).max(32).required(),
            integerProperty("RAM").unit("GB").min(1).max(128).required(),
            integerProperty("SSD").unit("GB").min(25).max(1000).step(25).required().asTotalLimit().withThreshold(200),
            integerProperty("HDD").unit("GB").min(0).max(4000).step(250).withDefault(0).asTotalLimit().withThreshold(200),
            integerProperty("Traffic").unit("GB").min(250).max(10000).step(250).required().asTotalLimit().withThreshold(200),
            enumerationProperty("SLA-Platform").values("BASIC", "EXT8H", "EXT4H", "EXT2H").withDefault("BASIC"),
            booleanProperty("SLA-EMail").falseIf("SLA-Platform", "BASIC").withDefault(false),
            booleanProperty("SLA-Maria").falseIf("SLA-Platform", "BASIC").optional(),
            booleanProperty("SLA-PgSQL").falseIf("SLA-Platform", "BASIC").optional(),
            booleanProperty("SLA-Office").falseIf("SLA-Platform", "BASIC").optional(),
            booleanProperty("SLA-Web").falseIf("SLA-Platform", "BASIC").optional()
        );
    }
}
