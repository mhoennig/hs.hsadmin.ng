package net.hostsharing.hsadminng.hs.booking.item.validators;

import static net.hostsharing.hsadminng.hs.validation.IntegerProperty.integerProperty;

class HsPrivateCloudBookingItemValidator extends HsBookingItemEntityValidator {

    HsPrivateCloudBookingItemValidator() {
        super(
            // @formatter:off
            integerProperty("CPUs")                     .min(  1).max(  128).required().asTotalLimit(),
            integerProperty("RAM").unit("GB")           .min(  1).max(  512).required().asTotalLimit(),
            integerProperty("SSD").unit("GB")           .min( 25).max( 4000).step(25).required().asTotalLimit(),
            integerProperty("HDD").unit("GB")           .min(  0).max(16000).step(250).withDefault(0).asTotalLimit(),
            integerProperty("Traffic").unit("GB")       .min(250).max(40000).step(250).required().asTotalLimit(),

//          Alternatively we could specify it similarly to "Multi" option but exclusively counting:
//          integerProperty("Resource-Points")          .min(4).max(100).required()
//                  .each("CPUs").countsAs(64)
//                  .each("RAM").countsAs(64)
//                  .each("SSD").countsAs(18)
//                  .each("HDD").countsAs(2)
//                  .each("Traffic").countsAs(1),

            integerProperty("SLA-Infrastructure EXT8H") .min(  0).max(   20).withDefault(0).asTotalLimitFor("SLA-Infrastructure", "EXT8H"),
            integerProperty("SLA-Infrastructure EXT4H") .min(  0).max(   20).withDefault(0).asTotalLimitFor("SLA-Infrastructure", "EXT4H"),
            integerProperty("SLA-Infrastructure EXT2H") .min(  0).max(   20).withDefault(0).asTotalLimitFor("SLA-Infrastructure", "EXT2H"),

            integerProperty("SLA-Platform EXT8H")       .min(  0).max(   20).withDefault(0).asTotalLimitFor("SLA-Platform", "EXT8H"),
            integerProperty("SLA-Platform EXT4H")       .min(  0).max(   20).withDefault(0).asTotalLimitFor("SLA-Platform", "EXT4H"),
            integerProperty("SLA-Platform EXT2H")       .min(  0).max(   20).withDefault(0).asTotalLimitFor("SLA-Platform", "EXT2H"),

            integerProperty("SLA-EMail")                .min(  0).max(   20).withDefault(0).asTotalLimit(),
            integerProperty("SLA-Maria")                .min(  0).max(   20).withDefault(0).asTotalLimit(),
            integerProperty("SLA-PgSQL")                .min(  0).max(   20).withDefault(0).asTotalLimit(),
            integerProperty("SLA-Office")               .min(  0).max(   20).withDefault(0).asTotalLimit(),
            integerProperty("SLA-Web")                  .min(  0).max(   20).withDefault(0).asTotalLimit()
            // @formatter:on
        );
    }
}
