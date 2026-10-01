package net.hostsharing.hsadminng.hs.office.bankaccount;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.val;
import net.hostsharing.hsadminng.rbac.test.RbacDataJpaTest;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@RbacDataJpaTest
@Tag("officeIntegrationTest")
class HsOfficeIbanBankCodeIntegrationTest {

    @PersistenceContext
    EntityManager em;

    @Test
    void ibanBankCodeMatchesIban4jForEverySupportedCountry() {
        val mismatches = Arrays.stream(CountryCode.values())
                .map(HsOfficeIbanBankCodeIntegrationTest::randomIbanOrNull)
                .filter(Objects::nonNull)
                .filter(iban -> !iban.getBankCode().equals(ibanBankCode(iban)))
                .map(iban -> iban.getCountryCode() + ": " + iban + " -> " + ibanBankCode(iban)
                        + " instead of " + iban.getBankCode())
                .toList();
        assertThat(mismatches).isEmpty();
    }

    private static Iban randomIbanOrNull(final CountryCode countryCode) {
        try {
            return Iban.random(countryCode);
        } catch (final RuntimeException notSupported) {
            return null;
        }
    }

    private String ibanBankCode(final Iban iban) {
        return (String) em.createNativeQuery("select hs_office.iban_bank_code(:iban, :len)")
                .setParameter("iban", iban.toString())
                .setParameter("len", iban.getBankCode().length())
                .getSingleResult();
    }
}
