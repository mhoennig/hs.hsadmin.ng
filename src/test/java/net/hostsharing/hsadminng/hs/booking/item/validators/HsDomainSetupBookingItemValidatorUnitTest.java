package net.hostsharing.hsadminng.hs.booking.item.validators;

import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorEntity;
import net.hostsharing.hsadminng.hs.booking.item.HsBookingItemRealEntity;
import net.hostsharing.hsadminng.hs.booking.project.HsBookingProjectRealEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.EntityManager;
import java.util.Map;

import static java.util.Map.entry;
import static net.hostsharing.hsadminng.hs.booking.item.HsBookingItemType.DOMAIN_SETUP;
import static org.apache.commons.lang3.StringUtils.right;
import static org.assertj.core.api.Assertions.assertThat;

class HsDomainSetupBookingItemValidatorUnitTest {

    public static final String TOO_LONG_DOMAIN_NAME = "asdfghijklmnopqrstuvwxyz0123456789.".repeat(8) + "example.org";
    final HsBookingDebitorEntity debitor = HsBookingDebitorEntity.builder()
            .debitorNumber(12345)
            .build();
    final HsBookingProjectRealEntity project = HsBookingProjectRealEntity.builder()
            .debitor(debitor)
            .caption("Test-Project")
            .build();
    private EntityManager em;

    @Test
    void acceptsRegisterableDomainWithGeneratedVerificationCode() {
        // given
        final var domainSetupBookingItemEntity = HsBookingItemRealEntity.builder()
                .type(DOMAIN_SETUP)
                .project(project)
                .caption("Test-Domain")
                .resources(Map.ofEntries(
                        entry("domainName", "example.org"),
                        entry("targetUnixUser", "xyz00")
                ))
                .build();

        // when
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(em, domainSetupBookingItemEntity);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void acceptsRegisterableDomainWithExplicitVerificationCode() {
        // given
        final var domainSetupBookingItemEntity = HsBookingItemRealEntity.builder()
                .type(DOMAIN_SETUP)
                .project(project)
                .caption("Test-Domain")
                .resources(Map.ofEntries(
                        entry("domainName", "example.org"),
                        entry("targetUnixUser", "xyz00"),
                        entry("verificationCode", "1234-5678-9100")
                ))
                .build();

        // when
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(em, domainSetupBookingItemEntity);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void acceptsMaximumDomainNameLength() {
        final var domainSetupBookingItemEntity = HsBookingItemRealEntity.builder()
                .type(DOMAIN_SETUP)
                .project(project)
                .caption("Test-Domain")
                .resources(Map.ofEntries(
                        entry("domainName", right(TOO_LONG_DOMAIN_NAME, 253)),
                        entry("targetUnixUser", "xyz00")
                ))
                .build();

        // when
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(em, domainSetupBookingItemEntity);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void rejectsTooLongTotalName() {
        final var domainSetupBookingItemEntity = HsBookingItemRealEntity.builder()
                .type(DOMAIN_SETUP)
                .project(project)
                .caption("Test-Domain")
                .resources(Map.ofEntries(
                        entry("domainName", right(TOO_LONG_DOMAIN_NAME, 254)),
                        entry("targetUnixUser", "xyz00")
                ))
                .build();

        // when
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(em, domainSetupBookingItemEntity);

        // then
        assertThat(result).contains("'D-12345:Test-Project:Test-Domain.resources.domainName' length is expected to be at max 253 but length of 'dfghijklmnopqrstuvwxyz0123456789.asdfghijklmnopqrstuvwxyz0123456789.asdfghijklmnopqrstuvwxyz0123456789.asdfghijklmnopqrstuvwxyz0123456789.asdfghijklmnopqrstuvwxyz0123456789.asdfghijklmnopqrstuvwxyz0123456789.asdfghijklmnopqrstuvwxyz0123456789.example.org' is 254");
    }

    @Test
    void acceptsValidUnixUser() {
        final var domainSetupBookingItemEntity = HsBookingItemRealEntity.builder()
                .type(DOMAIN_SETUP)
                .project(project)
                .caption("Test-Domain")
                .resources(Map.ofEntries(
                        entry("domainName", "example.com"),
                        entry("targetUnixUser", "xyz00-test")
                ))
                .build();

        // when
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(em, domainSetupBookingItemEntity);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void rejectsInvalidUnixUser() {
        final var domainSetupBookingItemEntity = HsBookingItemRealEntity.builder()
                .type(DOMAIN_SETUP)
                .project(project)
                .caption("Test-Domain")
                .resources(Map.ofEntries(
                        entry("domainName", "example.com"),
                        entry("targetUnixUser", "xyz00test")
                ))
                .build();

        // when
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(em, domainSetupBookingItemEntity);

        // then
        assertThat(result).contains("'D-12345:Test-Project:Test-Domain.resources.targetUnixUser' = 'xyz00test' is not a valid unix-user name");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "de", "com", "net", "org", "actually-any-top-level-domain",
            "co.uk", "org.uk", "gov.uk", "ac.uk", "sch.uk",
            "com.au", "net.au", "org.au", "edu.au", "gov.au", "asn.au", "id.au",
            "co.jp", "ne.jp", "or.jp", "ac.jp", "go.jp",
            "com.cn", "net.cn", "org.cn", "gov.cn", "edu.cn", "ac.cn",
            "com.br", "net.br", "org.br", "gov.br", "edu.br", "mil.br", "art.br",
            "co.in", "net.in", "org.in", "gen.in", "firm.in", "ind.in",
            "com.mx", "net.mx", "org.mx", "gob.mx", "edu.mx",
            "gov.it", "edu.it",
            "co.nz", "net.nz", "org.nz", "govt.nz", "ac.nz", "school.nz", "geek.nz", "kiwi.nz",
            "co.kr", "ne.kr", "or.kr", "go.kr", "re.kr", "pe.kr"
    })
    void rejectRegistrarLevelDomain(final String secondLevelRegistrarDomain) {
        // given
        final var domainSetupBookingItemEntity = HsBookingItemRealEntity.builder()
                .type(DOMAIN_SETUP)
                .project(project)
                .caption("Test-Domain")
                .resources(Map.ofEntries(
                        entry("domainName", secondLevelRegistrarDomain),
                        entry("targetUnixUser", "xyz00")
                ))
                .build();

        // when
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(em, domainSetupBookingItemEntity);

        // then
        assertThat(result).contains(
                "'D-12345:Test-Project:Test-Domain.resources.domainName' = '" +
                secondLevelRegistrarDomain +
                "' is a forbidden registrar-level domain name");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "hostsharing.net", "hostsharing.org", "hostsharing.com", "hostsharing.coop", "hostsharing.de"
    })
    void rejectHostsharingDomain(final String secondLevelRegistrarDomain) {
        // given
        final var domainSetupBookingItemEntity = HsBookingItemRealEntity.builder()
                .type(DOMAIN_SETUP)
                .project(project)
                .caption("Test-Domain")
                .resources(Map.ofEntries(
                        entry("domainName", secondLevelRegistrarDomain),
                        entry("targetUnixUser", "xyz00")
                ))
                .build();

        // when
        final var result = HsBookingItemEntityValidatorRegistry.doValidate(em, domainSetupBookingItemEntity);

        // then
        assertThat(result).containsExactly(
                "'D-12345:Test-Project:Test-Domain.resources.domainName' = '" +
                        secondLevelRegistrarDomain +
                        "' is a forbidden Hostsharing domain name");
    }

    @Test
    void containsAllValidations() {
        // when
        final var validator = HsBookingItemEntityValidatorRegistry.forType(DOMAIN_SETUP);

        // then
        assertThat(validator.properties()).map(Map::toString).containsExactlyInAnyOrder(
                "{type=string, propertyName=domainName, matchesRegEx=[^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,12}], matchesRegExDescription=is not a (non-top-level) fully qualified domain name, notMatchesRegEx=[[^.]+, (co|org|gov|ac|sch)\\.uk, (com|net|org|edu|gov|asn|id)\\.au, (co|ne|or|ac|go)\\.jp, (com|net|org|gov|edu|ac)\\.cn, (com|net|org|gov|edu|mil|art)\\.br, (co|net|org|gen|firm|ind)\\.in, (com|net|org|gob|edu)\\.mx, (gov|edu)\\.it, (co|net|org|govt|ac|school|geek|kiwi)\\.nz, (co|ne|or|go|re|pe)\\.kr], notMatchesRegExDescription=is a forbidden registrar-level domain name, maxLength=253, required=true, writeOnce=true}",
                "{type=string, propertyName=targetUnixUser, matchesRegEx=[^[a-z][a-z0-9]{2}[0-9]{2}$|^[a-z][a-z0-9]{2}[0-9]{2}-[a-z0-9\\._-]+$], matchesRegExDescription=is not a valid unix-user name, maxLength=253, required=true, writeOnce=true}",
                "{type=string, propertyName=verificationCode, minLength=12, maxLength=64, computed=IN_INIT}");
    }
}
