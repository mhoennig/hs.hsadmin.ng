package net.hostsharing.hsadminng.hs.office.sepamandate;

import com.vladmihalcea.hibernate.type.range.PostgreSQLRangeType;
import com.vladmihalcea.hibernate.type.range.Range;
import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.stringify.Stringify;
import net.hostsharing.hsadminng.stringify.Stringifyable;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

import static net.hostsharing.hsadminng.mapper.PostgresDateRange.*;
import static net.hostsharing.hsadminng.stringify.Stringify.stringify;

@Entity
@Table(name = "hs_office_sepamandate_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayName("SEPA-Mandate")
public class HsOfficeSepaMandateEntity implements Stringifyable {

    private static Stringify<HsOfficeSepaMandateEntity> stringify = stringify(HsOfficeSepaMandateEntity.class)
            .withProp(e -> e.getBankAccount().getIban())
            .withProp(HsOfficeSepaMandateEntity::getReference)
            .withProp(HsOfficeSepaMandateEntity::getAgreement)
            .withProp(e -> e.getValidity().asString())
            .withSeparator(", ")
            .quotedValues(false);

    @Id
    @GeneratedValue
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "debitoruuid")
    private HsOfficeDebitorEntity debitor;

    @ManyToOne
    @JoinColumn(name = "bankaccountuuid")
    private HsOfficeBankAccountEntity bankAccount;

    private @Column(name = "reference") String reference;

    @Column(name="agreement", columnDefinition = "date")
    private LocalDate agreement;

    @Column(name = "validity", columnDefinition = "daterange")
    @Type(PostgreSQLRangeType.class)
    @Builder.Default
    private Range<LocalDate> validity = Range.infinite(LocalDate.class);

    public void setValidFrom(final LocalDate validFrom) {
        setValidity(toPostgresDateRange(validFrom, getValidTo()));
    }

    public void setValidTo(final LocalDate validTo) {
        setValidity(toPostgresDateRange(getValidFrom(), validTo));
    }

    public LocalDate getValidFrom() {
        return lowerInclusiveFromPostgresDateRange(getValidity());
    }

    public LocalDate getValidTo() {
        return upperInclusiveFromPostgresDateRange(getValidity());
    }

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return reference;
    }

}
