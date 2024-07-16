package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetEntity;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.IPV6_NUMBER;

class HsIPv6NumberHostingAssetValidator extends HostingAssetEntityValidator {

    // simplified pattern, the real check is done by letting Java parse the address
    private static final Pattern IPV6_REGEX = Pattern.compile("([a-f0-9:]+:+)+[a-f0-9]+");

    HsIPv6NumberHostingAssetValidator() {
        super(
                IPV6_NUMBER,
                AlarmContact.isOptional(),

                NO_EXTRA_PROPERTIES
        );
    }

    @Override
    public List<String> validateEntity(final HsHostingAssetEntity assetEntity) {
        final var violations = super.validateEntity(assetEntity);

        if (!isValidIPv6Address(assetEntity.getIdentifier())) {
            violations.add("'identifier' expected to be a valid IPv6 address, but is '" + assetEntity.getIdentifier() + "'");
        }

        return violations;
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAssetEntity assetEntity) {
        return IPV6_REGEX;
    }

    private boolean isValidIPv6Address(final String identifier) {
        try {
            return InetAddress.getByName(identifier) instanceof java.net.Inet6Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
