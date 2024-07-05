package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import lombok.SneakyThrows;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType;
import net.hostsharing.hsadminng.system.SystemProcess;

import java.util.List;
import java.util.regex.Pattern;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static net.hostsharing.hsadminng.hs.validation.ArrayProperty.arrayOf;
import static net.hostsharing.hsadminng.hs.validation.BooleanProperty.booleanProperty;
import static net.hostsharing.hsadminng.hs.validation.IntegerProperty.integerProperty;
import static net.hostsharing.hsadminng.hs.validation.StringProperty.stringProperty;

class HsDomainDnsSetupHostingAssetValidator extends HsHostingAssetEntityValidator {

    // according to  RFC 1035 (section 5) and RFC 1034
    static final String RR_REGEX_NAME = "([a-z0-9\\.-]+|@)\\s+";
    static final String RR_REGEX_TTL = "(([1-9][0-9]*[mMhHdDwW]{0,1})+\\s+)*";
    static final String RR_REGEX_IN = "IN\\s+"; // record class IN for Internet
    static final String RR_RECORD_TYPE = "[A-Z]+\\s+";
    static final String RR_RECORD_DATA = "[^;].*";
    static final String RR_COMMENT = "(;.*)*";

    static final String RR_REGEX_TTL_IN =
            RR_REGEX_NAME + RR_REGEX_TTL + RR_REGEX_IN + RR_RECORD_TYPE + RR_RECORD_DATA + RR_COMMENT;

    static final String RR_REGEX_IN_TTL =
            RR_REGEX_NAME  + RR_REGEX_IN + RR_REGEX_TTL + RR_RECORD_TYPE + RR_RECORD_DATA + RR_COMMENT;

    HsDomainDnsSetupHostingAssetValidator() {
        super(  BookingItem.mustBeNull(),
                ParentAsset.mustBeOfType(HsHostingAssetType.DOMAIN_SETUP),
                AssignedToAsset.mustBeNull(),
                AlarmContact.isOptional(),

                integerProperty("TTL").min(0).withDefault(21600),
                booleanProperty("auto-SOA-RR").withDefault(true),
                booleanProperty("auto-NS-RR").withDefault(true),
                booleanProperty("auto-MX-RR").withDefault(true),
                booleanProperty("auto-A-RR").withDefault(true),
                booleanProperty("auto-AAAA-RR").withDefault(true),
                booleanProperty("auto-MAILSERVICES-RR").withDefault(true),
                booleanProperty("auto-AUTOCONFIG-RR").withDefault(true), // TODO.spec: does that already exist?
                booleanProperty("auto-AUTODISCOVER-RR").withDefault(true),
                booleanProperty("auto-DKIM-RR").withDefault(true),
                booleanProperty("auto-SPF-RR").withDefault(true),
                booleanProperty("auto-WILDCARD-MX-RR").withDefault(true),
                booleanProperty("auto-WILDCARD-A-RR").withDefault(true),
                booleanProperty("auto-WILDCARD-AAAA-RR").withDefault(true),
                booleanProperty("auto-WILDCARD-DKIM-RR").withDefault(true), // TODO.spec: check, if that really works
                booleanProperty("auto-WILDCARD-SPF-RR").withDefault(true),
                arrayOf(
                        stringProperty("user-RR").matchesRegEx(RR_REGEX_TTL_IN, RR_REGEX_IN_TTL).required()
                ).optional());
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAssetEntity assetEntity) {
        return  Pattern.compile("^" + assetEntity.getParentAsset().getIdentifier() + "$");
    }

    @Override
    public void preprocessEntity(final HsHostingAssetEntity entity) {
        super.preprocessEntity(entity);
        if (entity.getIdentifier() == null) {
            ofNullable(entity.getParentAsset()).ifPresent(pa -> entity.setIdentifier(pa.getIdentifier()));
        }
    }

    @Override
    @SneakyThrows
    public List<String> validateContext(final HsHostingAssetEntity assetEntity) {
        final var result = super.validateContext(assetEntity);

        // TODO.spec: define which checks should get raised to error level
        final var namedCheckZone = new SystemProcess("named-checkzone", assetEntity.getIdentifier());
        if (namedCheckZone.execute(toZonefileString(assetEntity)) != 0) {
            // yes, named-checkzone writes error messages to stdout
            stream(namedCheckZone.getStdOut().split("\n"))
                    .map(line -> line.replaceAll(" stream-0x[0-9a-f:]+", ""))
                    .forEach(result::add);
        }
        return result;
    }

    String toZonefileString(final HsHostingAssetEntity assetEntity) {
        // TODO.spec: we need to expand the templates (auto-...) in the same way as in Saltstack
        return """
              $ORIGIN {domain}.
              $TTL {ttl}

              ; these records are just placeholders to create a valid zonefile for the validation
              @        1814400  IN  SOA     {domain}. root.{domain} ( 1999010100 10800 900 604800 86400 )
              @                 IN  NS      ns
  
              {userRRs}
              """
                    .replace("{domain}", assetEntity.getIdentifier())
                    .replace("{ttl}", getPropertyValue(assetEntity, "TTL"))
                    .replace("{userRRs}", getPropertyValues(assetEntity, "user-RR") );
    }
}
