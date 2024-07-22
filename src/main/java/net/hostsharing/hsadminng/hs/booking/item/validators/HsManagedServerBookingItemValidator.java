package net.hostsharing.hsadminng.hs.booking.item.validators;



import static net.hostsharing.hsadminng.hs.validation.BooleanProperty.booleanProperty;
import static net.hostsharing.hsadminng.hs.validation.EnumerationProperty.enumerationProperty;
import static net.hostsharing.hsadminng.hs.validation.IntegerProperty.integerProperty;

class HsManagedServerBookingItemValidator extends HsBookingItemEntityValidator {

    HsManagedServerBookingItemValidator() {
        super(
            integerProperty("CPU").min(1).max(32).required(),
            integerProperty("RAM").unit("GB").min(1).max(128).required(),
            integerProperty("SSD").unit("GB").min(25).max(2000).step(25).requiresAtLeastOneOf("SSD", "HDD").asTotalLimit().withThreshold(200),
            integerProperty("HDD").unit("GB").min(250).max(10000).step(250).requiresAtLeastOneOf("SSD", "HDD").asTotalLimit().withThreshold(200),
            integerProperty("Traffic").unit("GB").min(250).max(64000).step(250).requiresAtMaxOneOf("Bandwidth", "Traffic").asTotalLimit().withThreshold(200),
            integerProperty("Bandwidth").unit("GB").min(250).max(64000).step(250).requiresAtMaxOneOf("Bandwidth", "Traffic").asTotalLimit().withThreshold(200), // TODO.spec
            enumerationProperty("SLA-Platform").values("BASIC", "EXT8H", "EXT4H", "EXT2H").withDefault("BASIC"),
            booleanProperty("SLA-EMail").falseIf("SLA-Platform", "BASIC").withDefault(false),
            booleanProperty("SLA-Maria").falseIf("SLA-Platform", "BASIC").optional(),
            booleanProperty("SLA-PgSQL").falseIf("SLA-Platform", "BASIC").optional(),
            booleanProperty("SLA-Office").falseIf("SLA-Platform", "BASIC").optional(),
            booleanProperty("SLA-Web").falseIf("SLA-Platform", "BASIC").optional()
        );
    }
}
