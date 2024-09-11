package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAsset;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.validators.Dns.superDomain;
import static net.hostsharing.hsadminng.hs.hosting.asset.validators.HsDomainHttpSetupHostingAssetValidator.SUBDOMAIN_NAME_REGEX;

class HsDomainSetupHostingAssetValidator extends HostingAssetEntityValidator {

    public static final String FQDN_REGEX = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,12}";
    public static final String DOMAIN_NAME_PROPERTY_NAME = "domainName";

    HsDomainSetupHostingAssetValidator() {
        super(
                DOMAIN_SETUP,
                AlarmContact.isOptional(),

                NO_EXTRA_PROPERTIES);
    }

    @Override
    public List<String> validateEntity(final HsHostingAsset assetEntity) {
        final var violations = super.validateEntity(assetEntity);
        if (!violations.isEmpty() || assetEntity.isLoaded()) {
            // it makes no sense to do DNS-based validation
            //  if the entity is already persisted or
            //  if the identifier (domain name) or structure is already invalid
            return violations;
        }

        final var domainName = assetEntity.getIdentifier();
        final var dnsResult = new Dns(domainName).fetchRecordsOfType("TXT");
        final Supplier<String> getCode = () -> assetEntity.getBookingItem().getDirectValue("verificationCode", String.class);
        switch (dnsResult.status()) {
            case Dns.Status.SUCCESS: {
                final var expectedTxtRecordValue = "Hostsharing-domain-setup-verification-code=" + getCode.get();
                final var verificationFound = findTxtRecord(dnsResult, expectedTxtRecordValue)
                        .or(() -> superDomain(domainName)
                                .flatMap(superDomainName -> findTxtRecord(
                                        new Dns(superDomainName).fetchRecordsOfType("TXT"),
                                        expectedTxtRecordValue))
                        );
                if (verificationFound.isEmpty()) {
                    violations.add(
                            "[DNS] no TXT record '" + expectedTxtRecordValue +
                                    "' found for domain name '" + domainName + "' (nor in its super-domain)");
                }
                break;
            }

            case Dns.Status.NAME_NOT_FOUND: {
                if (isDnsVerificationRequiredForUnregisteredDomain(assetEntity)) {
                    final var superDomain = superDomain(domainName);
                    final var expectedTxtRecordValue = "Hostsharing-domain-setup-verification-code=" + getCode.get();
                    final var verificationFoundInSuperDomain = superDomain.flatMap(superDomainName -> findTxtRecord(
                            new Dns(superDomainName).fetchRecordsOfType("TXT"),
                            expectedTxtRecordValue));
                    if (verificationFoundInSuperDomain.isEmpty()) {
                        violations.add(
                                "[DNS] no TXT record '" + expectedTxtRecordValue +
                                        "' found for domain name '" + superDomain.orElseThrow() + "'");
                    }
                }
                // otherwise no DNS verification to be able to setup DNS for domains to register
                break;
            }

            case Dns.Status.INVALID_NAME:
                violations.add("[DNS] invalid domain name '" + assetEntity.getIdentifier() + "'");
                break;

            case Dns.Status.SERVICE_UNAVAILABLE:
            case Dns.Status.UNKNOWN_FAILURE:
                violations.add("[DNS] lookup failed for domain name '" + assetEntity.getIdentifier() + "': " + dnsResult.exception());
                break;
        }
        return violations;
    }

    @Override
    protected Pattern identifierPattern(final HsHostingAsset assetEntity) {
        if (assetEntity.getBookingItem() != null) {
            final var bookingItemDomainName = assetEntity.getBookingItem()
                    .getDirectValue(DOMAIN_NAME_PROPERTY_NAME, String.class);
            return Pattern.compile(bookingItemDomainName, Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        }
        final var parentDomainName = assetEntity.getParentAsset().getIdentifier();
        return Pattern.compile(SUBDOMAIN_NAME_REGEX + "\\." + parentDomainName.replace(".", "\\."), Pattern.CASE_INSENSITIVE);
    }

    private static boolean isDnsVerificationRequiredForUnregisteredDomain(final HsHostingAsset assetEntity) {
        return !Dns.isRegistrableDomain(assetEntity.getIdentifier())
                && assetEntity.getParentAsset() == null;
    }


    private static Optional<String> findTxtRecord(final Dns.Result result, final String expectedTxtRecordValue) {
        return result.records().stream()
                .filter(r -> r.contains(expectedTxtRecordValue))
                .findAny();
    }
}
