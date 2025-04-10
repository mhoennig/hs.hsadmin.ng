package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.IPV6_NUMBER;

class HsIPv6NumberHostingAssetValidator extends HostingAssetEntityValidator {

    // Simple pattern to check only max length and valid characters (hex digits and colons).
    // A robust validation is done via isValidIPv6Address.
    private static final Pattern SIMPLE_IPV6_REGEX_PATTERN = Pattern.compile("^[0-9a-fA-F:]{1,39}$");

    HsIPv6NumberHostingAssetValidator() {
        super(
                IPV6_NUMBER,
                AlarmContact.isOptional(),

                NO_EXTRA_PROPERTIES
        );
    }

    @Override
    public List<String> validateEntity(final HsHostingAsset assetEntity) {
        final var violations = super.validateEntity(assetEntity);

        if (!isValidIPv6Address(assetEntity.getIdentifier())) {
            violations.add("'identifier' expected to be a valid IPv6 address but is '" + assetEntity.getIdentifier() + "'");
        }

        return violations;
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAsset assetEntity) {
        return SIMPLE_IPV6_REGEX_PATTERN;
    }

    private boolean isValidIPv6Address(final String identifier) {
        try {
            return InetAddress.getByName(identifier) instanceof java.net.Inet6Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
