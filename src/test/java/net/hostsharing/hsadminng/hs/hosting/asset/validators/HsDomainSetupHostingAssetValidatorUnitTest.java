package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType;
import net.hostsharing.hsadminng.hs.booking.item.validators.HsBookingItemEntityValidatorRegistry;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRealEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRbacEntity;
import net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetRealEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.CLOUD_SERVER;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.DOMAIN_SETUP;
import static net.hostsharing.hsadminng.hs.hosting.asset.HsHostingAssetType.MANAGED_SERVER;
import static org.assertj.core.api.Assertions.assertThat;

class HsDomainSetupHostingAssetValidatorUnitTest {

    public static final Dns.Result DOMAIN_NOT_REGISTERED = Dns.Result.fromException(new NameNotFoundException(
            "domain not registered"));

    static HsHostingAssetRbacEntity.HsHostingAssetRbacEntityBuilder<?, ?> validEntityBuilder(
            final String domainName,
            final Function<HsBookingItemRealEntity.HsBookingItemRealEntityBuilder<?, ?>, HsBookingItemRealEntity> buildBookingItem) {
        final var project = HsBookingProjectRealEntity.builder().build();
        final var bookingItem = buildBookingItem.apply(
                HsBookingItemRealEntity.builder()
                        .project(project)
                        .type(HsBookingItemType.DOMAIN_SETUP)
                        .resources(new HashMap<>(ofEntries(
                                entry("domainName", domainName)
                        ))));
        HsBookingItemEntityValidatorRegistry.forType(HsBookingItemType.DOMAIN_SETUP).prepareProperties(null, bookingItem);
        return HsHostingAssetRbacEntity.builder()
                .type(DOMAIN_SETUP)
                .bookingItem(bookingItem)
                .identifier(domainName);
    }

    static HsHostingAssetRbacEntity.HsHostingAssetRbacEntityBuilder<?, ?> validEntityBuilder(final String domainName) {
        return validEntityBuilder(domainName, HsBookingItemRealEntity.HsBookingItemRealEntityBuilder::build);
    }

    @AfterEach
    void cleanup() {
        Dns.resetFakeResults();
    }

    //=====================================================================================================================

    enum InvalidSubDomainNameIdentifierForExampleOrg {
        IDENTICAL("example.org"),
        TOO_LONG("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz0123456890123456789.example.org"),
        DASH_AT_BEGINNING("-sub.example.org"),
        DOT(".example.org"),
        DOT_AT_BEGINNING(".sub.example.org"),
        DOUBLE_DOT("sub..example.com.");

        final String domainName;

        InvalidSubDomainNameIdentifierForExampleOrg(final String domainName) {
            this.domainName = domainName;
        }
    }

    @ParameterizedTest
    @EnumSource(InvalidSubDomainNameIdentifierForExampleOrg.class)
    void rejectsInvalidIdentifier(final InvalidSubDomainNameIdentifierForExampleOrg testCase) {
        // given
        final var givenEntity = validEntityBuilder(testCase.domainName)
                .bookingItem(null)
                .parentAsset(HsHostingAssetRealEntity.builder().type(DOMAIN_SETUP).identifier("example.org").build())
                .build();
        // fakeValidDnsVerification(givenEntity);
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var result = validator.validateEntity(givenEntity);

        // then
        assertThat(result).contains(
                "'identifier' expected to match '(\\*|(?!-)[A-Za-z0-9-]{1,63}(?<!-))\\.example\\.org', but is '"
                        + testCase.domainName + "'"
        );
    }

    enum ValidSubDomainNameIdentifier {
        SIMPLE("sub.example.org"),
        MAX_LENGTH("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz01234568901.example.org"),
        MIN_LENGTH("x.example.org"),
        WITH_DASH("example-test.example.org");

        final String domainName;

        ValidSubDomainNameIdentifier(final String domainName) {
            this.domainName = domainName;
        }
    }

    @ParameterizedTest
    @EnumSource(ValidSubDomainNameIdentifier.class)
    void acceptsValidIdentifier(final ValidSubDomainNameIdentifier testCase) {
        // given
        final var givenEntity = validEntityBuilder(testCase.domainName).identifier(testCase.domainName).build();
        fakeValidDnsVerification(givenEntity);
        final var validator = HostingAssetEntityValidatorRegistry.forType(givenEntity.getType());

        // when
        final var result = validator.validateEntity(givenEntity);

        // then
        assertThat(result).isEmpty();
    }

    private static void fakeValidDnsVerification(final HsHostingAssetRbacEntity givenEntity) {
        final var expectedHash = givenEntity.getBookingItem().getDirectValue("verificationCode", String.class);
        Dns.fakeResultForDomain(
                givenEntity.getIdentifier(),
                Dns.Result.fromRecords("Hostsharing-domain-setup-verification-code=" + expectedHash));
    }

    @Test
    void containsNoProperties() {
        // when
        final var validator = HostingAssetEntityValidatorRegistry.forType(CLOUD_SERVER);

        // then
        assertThat(validator.properties()).map(Map::toString).isEmpty();
    }

    @Test
    void validatesReferencedEntities() {
        // given
        final var domainSetupHostingAssetEntity = validEntityBuilder(
                "example.org",
                bib -> bib.type(HsBookingItemType.CLOUD_SERVER).build())
                .parentAsset(HsHostingAssetRealEntity.builder().type(CLOUD_SERVER).build())
                .assignedToAsset(HsHostingAssetRealEntity.builder().type(MANAGED_SERVER).build())
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(domainSetupHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(domainSetupHostingAssetEntity);

        // then
        assertThat(result).contains(
                "'DOMAIN_SETUP:example.org.bookingItem' or parentItem must be null but is of type CLOUD_SERVER",
                "'DOMAIN_SETUP:example.org.parentAsset' must be null or of type DOMAIN_SETUP but is of type CLOUD_SERVER",
                "'DOMAIN_SETUP:example.org.assignedToAsset' must be null but is of type MANAGED_SERVER");
    }

    @Test
    void rejectsDomainNameNotMatchingBookingItemDomainName() {
        // given
        final var domainSetupHostingAssetEntity = validEntityBuilder(
                "not-matching-booking-item-domain-name.org",
                bib -> bib.resources(new HashMap<>(ofEntries(
                        entry("domainName", "example.org")
                ))).build()
        ).build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(domainSetupHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(domainSetupHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'identifier' expected to match 'example.org', but is 'not-matching-booking-item-domain-name.org'");
    }

    @ParameterizedTest
    @ValueSource(strings = { "not-matching-booking-item-domain-name.org", "indirect.subdomain.example.org" })
    void rejectsDomainNameWhichIsNotADirectSubdomainOfParentAsset(final String newDomainName) {
        // given
        final var domainSetupHostingAssetEntity = validEntityBuilder(newDomainName)
                .bookingItem(null)
                .parentAsset(createValidParentDomainSetupAsset("example.org"))
                .build();
        final var validator = HostingAssetEntityValidatorRegistry.forType(domainSetupHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(domainSetupHostingAssetEntity);

        // then
        assertThat(result).containsExactlyInAnyOrder(
                "'identifier' expected to match '(\\*|(?!-)[A-Za-z0-9-]{1,63}(?<!-))\\.example\\.org', " +
                        "but is '" + newDomainName + "'");
    }

    @Test
    void rejectsIfNeitherBookingItemNorParentAssetAreSet() {

        // given
        final var domainSetupHostingAssetEntity = validEntityBuilder("example.org")
                .bookingItem(null)
                .parentAsset(null)
                .build();
        Dns.fakeResultForDomain(
                domainSetupHostingAssetEntity.getIdentifier(),
                new Dns.Result(Dns.Status.NAME_NOT_FOUND, null, null));
        final var validator = HostingAssetEntityValidatorRegistry.forType(domainSetupHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(domainSetupHostingAssetEntity);

        // then
        assertThat(result).containsExactly("'DOMAIN_SETUP:example.org.bookingItem' must be of type DOMAIN_SETUP but is null");
    }

    enum DnsLookupFailureTestCase {
        SERVICE_UNAVAILABLE(
                new ServiceUnavailableException("no Internet connection"),
                "[DNS] lookup failed for domain name 'example.org': javax.naming.ServiceUnavailableException: no Internet connection"),
        NAME_NOT_FOUND(
                new NameNotFoundException("domain name not found"),
                null), // no
        INVALID_NAME(
                new InvalidNameException("domain name too long or whatever"),
                "[DNS] invalid domain name 'example.org'"),
        UNKNOWN_FAILURE(
                new NamingException("some other problem"),
                "[DNS] lookup failed for domain name 'example.org': javax.naming.NamingException: some other problem");

        public final NamingException givenException;
        public final String expectedErrorMessage;

        DnsLookupFailureTestCase(final NamingException givenException, final String expectedErrorMessage) {
            this.givenException = givenException;
            this.expectedErrorMessage = expectedErrorMessage;
        }
    }

    @ParameterizedTest
    @EnumSource(DnsLookupFailureTestCase.class)
    void handlesDnsLookupFailures(final DnsLookupFailureTestCase testCase) {

        // given
        final var domainSetupHostingAssetEntity = validEntityBuilder("example.org").build();
        Dns.fakeResultForDomain(
                domainSetupHostingAssetEntity.getIdentifier(),
                Dns.Result.fromException(testCase.givenException));
        final var validator = HostingAssetEntityValidatorRegistry.forType(domainSetupHostingAssetEntity.getType());

        // when
        final var result = validator.validateEntity(domainSetupHostingAssetEntity);

        // then
        if (testCase.expectedErrorMessage != null) {
            assertThat(result).containsExactly(testCase.expectedErrorMessage);
        } else {
            assertThat(result).isEmpty();
        }
    }

    //=====================================================================================================================

    @Test
    void rejectsSetupOfRegistrar1stLevelDomain() {
        domainSetupFor("org").notRegistered()
                .isRejectedWithCauseForbidden("registrar-level domain name");
    }

    @Test
    void rejectsSetupOfRegistrar2ndLevelDomain() {
        domainSetupFor("co.uk").notRegistered()
                .isRejectedWithCauseForbidden("registrar-level domain name");
    }

    @Test
    void rejectsSetupOfHostsharingDomain() {
        domainSetupFor("hostsharing.net").notRegistered()
                .isRejectedWithCauseForbidden("Hostsharing domain name");
    }

    @Test
    void allowSetupOfAvailableRegistrableDomain() {
        domainSetupFor("example.com").notRegistered()
                .isAccepted();
    }

    @Test
    void allowSetupOfAvailableRegistrable2ndLevelDomain() {
        domainSetupFor("example.co.uk").notRegistered()
                .isAccepted();
    }

    @Test
    void rejectSetupOfRegisteredRegistrable2ndLevelDomainWithoutVerification() {
        domainSetupFor("example.co.uk").registered()
                .isRejectedWithCauseMissingVerificationIn("example.co.uk");
    }

    @Test
    void allowSetupOfRegisteredRegistrable2ndLevelDomainWithVerification() {
        domainSetupFor("example.co.uk").registeredWithVerification()
                .isAccepted();
    }

    @Test
    void rejectSetupOfExistingRegistrableDomainWithoutValidDnsVerification() {
        domainSetupFor("example.com").registered()
                .isRejectedWithCauseMissingVerificationIn("example.com");
    }

    @Test
    void allowSetupOfExistingRegistrableDomainWithValidDnsVerification() {
        domainSetupFor("example.org").registeredWithVerification()
                .isAccepted();
    }

    @Test
    void allowSetupOfUnregisteredSubdomainIfSuperDomainParentAssetIsSpecified() {
        domainSetupFor("sub.example.org").notRegistered().withParentAsset("example.org")
                .isAccepted();
    }

    @Test
    void rejectSetupOfUnregisteredSubdomainIfWrongParentAssetIsSpecified() {
        domainSetupFor("sub.example.org").notRegistered().withParentAsset("example.net")
                .isRejectedDueToInvalidIdentifier();
    }

    @Test
    void allowSetupOfUnregisteredSubdomainWithValidDnsVerificationInSuperDomain() {
        domainSetupFor("sub.example.org").notRegistered().withVerificationIn("example.org")
                .isAccepted();
    }

    @Test
    void rejectSetupOfExistingRegistrableDomainWithInvalidDnsVerification() {
        domainSetupFor("example.com").registeredWithInvalidVerification()
                .isRejectedWithCauseMissingVerificationIn("example.com");
    }

    @Test
    void acceptSetupOfRegisteredSubdomainWithInvalidDnsVerificationButValidDnsVerificationInSuperDomain() {
        domainSetupFor("sub.example.com").registeredWithInvalidVerification().withVerificationIn("example.com")
                .isAccepted();
    }

    @Test
    void rejectSetupOfUnregisteredSubdomainWithoutParentAssetAndWithoutDnsVerificationInSuperDomain() {
        domainSetupFor("sub.example.org").notRegistered()
                .isRejectedWithCauseMissingVerificationIn("example.org");
    }

    @Test
    void rejectSetupOfUnregisteredSubdomainOfUnregisteredSuperDomain() {
        domainSetupFor("sub.sub.example.org").notRegistered()
                .isRejectedWithCauseDomainNameNotFound("sub.example.org");
    }

    @Test
    void acceptSetupOfUnregisteredSubdomainWithParentAssetEvenWithoutDnsVerificationInSuperDomain() {
        domainSetupWithParentAssetFor("sub.example.org").notRegistered()
                .isAccepted();
    }

    @Test
    void allowSetupOfExistingSubdomainWithValidDnsVerificationInSuperDomain() {
        domainSetupFor("sub.example.org").registered()
                .withVerificationIn("example.org")
                .isAccepted();
    }

    @Test
    void rejectSetupOfExistingSubdomainWithoutDnsVerification() {
        domainSetupFor("sub.example.org").registered()
                .isRejectedWithCauseMissingVerificationIn("sub.example.org");
    }

    @Test
    void allowSetupOfRegistrableDomainWithUserDefinedVerificationCode() {
        domainSetupFor("example.edu.it").notRegistered().withUserDefinedVerificationCode("ABCD-EFGH-IJKL-MNOP")
                .withVerificationIn("example.edu.it")
                .isAccepted();
    }

    @Test
    void rejectSetupOfRegistrableDomainWithInvalidUserDefinedVerificationCode() {
        domainSetupFor("example.edu.it").notRegistered().withUserDefinedVerificationCode("ABCD-EFGH-IJKL-MNOP")
                .withVerificationIn("example.edu.it", "SOME-OTHER-CODE")
                .isRejectedWithCauseMissingVerificationIn("example.edu.it");
    }

    //====================================================================================================================

    private static HsHostingAssetRealEntity createValidParentDomainSetupAsset(final String parentDomainName) {
        final var bookingItem = HsBookingItemRealEntity.builder()
                .type(HsBookingItemType.DOMAIN_SETUP)
                .resources(ofEntries(
                        entry("domainName", parentDomainName)
                ))
                .build();
        final var parentAsset = HsHostingAssetRealEntity.builder()
                .type(DOMAIN_SETUP)
                .bookingItem(bookingItem)
                .identifier(parentDomainName).build();
        return parentAsset;
    }

    class DomainSetupBuilder {

        private final HsHostingAssetRbacEntity domainAsset;

        public DomainSetupBuilder(final String domainName) {
            domainAsset = validEntityBuilder(domainName).build();
        }

        public DomainSetupBuilder(final HsHostingAssetRealEntity parentAsset, final String domainName) {
            domainAsset = validEntityBuilder(domainName)
                    .bookingItem(null)
                    .parentAsset(parentAsset)
                    .build();
        }

        DomainSetupBuilder notRegistered() {
            Dns.fakeResultForDomain(domainAsset.getIdentifier(), DOMAIN_NOT_REGISTERED);
            return this;
        }

        DomainSetupBuilder registered() {
            Dns.fakeResultForDomain(
                    domainAsset.getIdentifier(),
                    Dns.Result.fromRecords());
            return this;
        }

        DomainSetupBuilder registeredWithInvalidVerification() {
            Dns.fakeResultForDomain(
                    domainAsset.getIdentifier(),
                    Dns.Result.fromRecords("Hostsharing-domain-setup-verification-code=SOME-DEFINITELY-WRONG-HASH"));
            return this;
        }

        DomainSetupBuilder registeredWithVerification() {
            withVerificationIn(domainAsset.getIdentifier());
            return this;
        }

        DomainSetupBuilder withUserDefinedVerificationCode(final String verificationCode) {
            domainAsset.getBookingItem().getResources().put("verificationCode", verificationCode);
            return this;
        }

        DomainSetupBuilder withVerificationIn(final String domainName, final String verificationCode) {
            assertThat(verificationCode).as("explicit verificationCode must not be null").isNotNull();
            Dns.fakeResultForDomain(
                    domainName,
                    Dns.Result.fromRecords("Hostsharing-domain-setup-verification-code=" + verificationCode));
            return this;
        }

        DomainSetupBuilder withVerificationIn(final String domainName) {
            assertThat(expectedVerificationCode()).as("no expectedHash available").isNotNull();
            Dns.fakeResultForDomain(
                    domainName,
                    Dns.Result.fromRecords("Hostsharing-domain-setup-verification-code=" + expectedVerificationCode()));
            return this;
        }

        void isRejectedWithCauseMissingVerificationIn(final String domainName) {
            assertThat(expectedVerificationCode()).as("no expectedHash available").isNotNull();
            assertThat(validate()).containsAnyOf(
                    "[DNS] no TXT record 'Hostsharing-domain-setup-verification-code=" + expectedVerificationCode()
                            + "' found for domain name '" + domainName + "' (nor in its super-domain)",
                    "[DNS] no TXT record 'Hostsharing-domain-setup-verification-code=" + expectedVerificationCode()
                            + "' found for domain name '" + domainName + "'");
        }

        void isRejectedWithCauseForbidden(final String type) {
            assertThat(validate()).contains(
                    "'D-???????:null:null.resources.domainName' = '" + domainAsset.getIdentifier() + "' is a forbidden " + type
            );
        }

        void isRejectedDueToInvalidIdentifier() {
            assertThat(validate()).contains(
                    "'identifier' expected to match '(\\*|(?!-)[A-Za-z0-9-]{1,63}(?<!-))\\.example\\.net', but is 'sub.example.org'"
            );
        }

        void isRejectedWithCauseDomainNameNotFound(final String domainName) {
            assertThat(validate()).contains(
                    "[DNS] lookup failed for domain name '" + domainName + "': javax.naming.NameNotFoundException: domain not registered"
            );
        }

        void isAccepted() {
            assertThat(validate()).isEmpty();
        }

        private String expectedVerificationCode() {
            return domainAsset.getBookingItem().getDirectValue("verificationCode", String.class);
        }

        private List<String> validate() {
            if ( domainAsset.getBookingItem() != null ) {
                final var biValidation = HsBookingItemEntityValidatorRegistry.forType(HsBookingItemType.DOMAIN_SETUP)
                        .validateEntity(domainAsset.getBookingItem());
                if (!biValidation.isEmpty()) {
                    return biValidation;
                }
            }

            return HostingAssetEntityValidatorRegistry.forType(DOMAIN_SETUP)
                    .validateEntity(domainAsset);
        }

        DomainSetupBuilder withParentAsset(final String parentAssetDomainName) {
            domainAsset.setBookingItem(null);
            domainAsset.setParentAsset(HsHostingAssetRealEntity.builder().type(DOMAIN_SETUP).identifier(parentAssetDomainName).build());
            return this;
        }
    }

    private DomainSetupBuilder domainSetupFor(final String domainName) {
        return new DomainSetupBuilder(domainName);
    }

    private DomainSetupBuilder domainSetupWithParentAssetFor(final String domainName) {
        return new DomainSetupBuilder(
                HsHostingAssetRealEntity.builder()
                        .type(DOMAIN_SETUP)
                        .identifier(Dns.superDomain(domainName).orElseThrow())
                        .build(),
                domainName);
    }
}
