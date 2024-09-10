package net.hostsharing.hsadminng.hs.hosting.asset.validators;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DnsUnitTest {

    @Test
    void isRegistrarLevelDomain() {
        assertThat(Dns.isRegistrarLevelDomain("de")).isTrue();
        assertThat(Dns.isRegistrarLevelDomain("example.de")).isFalse();

        assertThat(Dns.isRegistrarLevelDomain("co.uk")).isTrue();
        assertThat(Dns.isRegistrarLevelDomain("example.co.uk")).isFalse();
        assertThat(Dns.isRegistrarLevelDomain("co.uk.com")).isFalse();
    }

    @Test
    void isRegistrableDomain() {
        assertThat(Dns.isRegistrableDomain("de")).isFalse();
        assertThat(Dns.isRegistrableDomain("example.de")).isTrue();
        assertThat(Dns.isRegistrableDomain("sub.example.de")).isFalse();

        assertThat(Dns.isRegistrableDomain("co.uk")).isFalse();
        assertThat(Dns.isRegistrableDomain("example.co.uk")).isTrue();
        assertThat(Dns.isRegistrableDomain("sub.example.co.uk")).isFalse();
    }
}
