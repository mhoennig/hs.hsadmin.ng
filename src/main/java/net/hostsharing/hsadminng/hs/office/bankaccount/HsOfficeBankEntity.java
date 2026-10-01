package net.hostsharing.hsadminng.hs.office.bankaccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.val;

import java.io.Serializable;
import java.util.Optional;

/** A bank from the bank directory, keyed by the country code and the national bank code of its IBANs. */
@Entity
@Table(schema = "hs_office", name = "bank")
@IdClass(HsOfficeBankEntity.Key.class)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsOfficeBankEntity {

    public enum Source {
        DEMO,
        EXISTING,   // seeded once from the existing bank accounts, thus names are unknown, and BIC might be wrong
        MANUAL,     // manually added by admin
        BUNDESBANK  // imported by HsOfficeBankDirectoryImporter
    }

    @Id
    @Column(name = "countrycode")
    private String countryCode;

    @Id
    @Column(name = "bankcode")
    private String bankCode;

    private String bic;

    private String name;

    @Enumerated(EnumType.STRING)
    private Source source;

    /** Compares BICs ignoring blanks, case and the optional 'XXX' branch code suffix. */
    public boolean hasBic(final String otherBic) {
        return bic != null && normalized(bic).equals(normalized(otherBic));
    }

    private static String normalized(final String bic) {
        val compact = bic.replace(" ", "").toUpperCase();
        return compact.length() == 11 && compact.endsWith("XXX") ? compact.substring(0, 8) : compact;
    }

    /** Bank accounts entered before the bank directory was complete might not match it; a bank without BIC matches any. */
    public static boolean matches(final HsOfficeBankEntity bankOrNull, final String bic) {
        return bankOrNull != null && (bankOrNull.bic == null || bankOrNull.hasBic(bic));
    }

    public static Optional<String> germanBankCodeOf(final String iban) {
        val compact = iban.replace(" ", "").toUpperCase();
        return compact.startsWith("DE") && compact.length() >= 12 ? Optional.of(compact.substring(4, 12)) : Optional.empty();
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private String countryCode;
        private String bankCode;
    }
}
