
package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItem;
import net.hostsharing.hsadminng.hs.validation.PropertiesProvider;

import jakarta.persistence.EntityManager;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import static net.hostsharing.hsadminng.hs.hosting.asset.validators.Dns.REGISTRAR_LEVEL_DOMAINS;
import static net.hostsharing.hsadminng.hs.validation.StringProperty.stringProperty;

class HsDomainSetupBookingItemValidator extends HsBookingItemEntityValidator {

    public static final String FQDN_REGEX = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,12}";
    public static final String DOMAIN_NAME_PROPERTY_NAME = "domainName";
    public static final String TARGET_UNIX_USER_PROPERTY_NAME = "targetUnixUser";
    public static final String WEBSPACE_NAME_REGEX = "[a-z][a-z0-9]{2}[0-9]{2}";
    public static final String TARGET_UNIX_USER_NAME_REGEX = "^"+WEBSPACE_NAME_REGEX+"$|^"+WEBSPACE_NAME_REGEX+"-[a-z0-9\\._-]+$";
    public static final String VERIFICATION_CODE_PROPERTY_NAME = "verificationCode";

    HsDomainSetupBookingItemValidator() {
        super(
                stringProperty(DOMAIN_NAME_PROPERTY_NAME).writeOnce()
                        .maxLength(253)
                        .matchesRegEx(FQDN_REGEX).describedAs("is not a (non-top-level) fully qualified domain name")
                        .notMatchesRegEx(REGISTRAR_LEVEL_DOMAINS).describedAs("is a forbidden registrar-level domain name")
                        .required(),
                // TODO.legacy: remove the following property once we give up legacy compatibility
                stringProperty(TARGET_UNIX_USER_PROPERTY_NAME).writeOnce()
                        .maxLength(253)
                        .matchesRegEx(TARGET_UNIX_USER_NAME_REGEX).describedAs("is not a valid unix-user name")
                        .writeOnce()
                        .required(),
                stringProperty(VERIFICATION_CODE_PROPERTY_NAME)
                        .minLength(12)
                        .maxLength(64)
                        .initializedBy(HsDomainSetupBookingItemValidator::generateVerificationCode)
        );
    }

    @Override
    public List<String> validateEntity(final HsBookingItem bookingItem) {
        final var violations = new ArrayList<String>();
        final var domainName = bookingItem.getDirectValue(DOMAIN_NAME_PROPERTY_NAME, String.class);
        if (!bookingItem.isLoaded() &&
                domainName.matches("hostsharing.(com|net|org|coop|de)")) {
            violations.add("'" + bookingItem.toShortString() + ".resources." + DOMAIN_NAME_PROPERTY_NAME + "' = '" + domainName
                    + "' is a forbidden Hostsharing domain name");
        }
        violations.addAll(super.validateEntity(bookingItem));
        return violations;
    }

    private static String generateVerificationCode(final EntityManager em, final PropertiesProvider propertiesProvider) {
        final var alphaNumeric = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        final var secureRandom = new SecureRandom();
        final var sb = new StringBuilder();
        for (int i = 0; i < 40; ++i) {
            if ( i > 0 && i % 4 == 0 ) {
                sb.append("-");
            }
            sb.append(alphaNumeric.charAt(secureRandom.nextInt(alphaNumeric.length())));
        }
        return sb.toString();
    }

}
