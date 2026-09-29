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

import java.io.Serializable;

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
        BUNDESBANK  // reserved for an import of the Bundesbank bank-code file
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

    /** Compares BICs ignoring the optional 'XXX' branch code suffix. */
    public boolean hasBic(final String otherBic) {
        return bic != null && normalized(bic).equals(normalized(otherBic));
    }

    private static String normalized(final String bic) {
        return bic.length() == 11 && bic.endsWith("XXX") ? bic.substring(0, 8) : bic;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private String countryCode;
        private String bankCode;
    }
}
