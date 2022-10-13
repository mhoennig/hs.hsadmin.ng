package net.hostsharing.hsadminng.hs.office.sepamandate;

import com.vladmihalcea.hibernate.type.range.PostgreSQLRangeType;
import com.vladmihalcea.hibernate.type.range.Range;
import lombok.*;
import net.hostsharing.hsadminng.errors.DisplayName;
import net.hostsharing.hsadminng.Stringify;
import net.hostsharing.hsadminng.Stringifyable;
import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

import static net.hostsharing.hsadminng.Stringify.stringify;

@Entity
@Table(name = "hs_office_sepamandate_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DisplayName("SEPA-Mandate")
@TypeDef(
        typeClass = PostgreSQLRangeType.class,
        defaultForType = Range.class
)
public class HsOfficeSepaMandateEntity implements Stringifyable {

    private static Stringify<HsOfficeSepaMandateEntity> stringify = stringify(HsOfficeSepaMandateEntity.class)
            .withProp(e -> e.getBankAccount().getIban())
            .withProp(HsOfficeSepaMandateEntity::getReference)
            .withProp(e -> e.getValidity().asString())
            .withSeparator(", ")
            .quotedValues(false);

    private @Id UUID uuid;

    @ManyToOne
    @JoinColumn(name = "debitoruuid")
    private HsOfficeDebitorEntity debitor;

    @ManyToOne
    @JoinColumn(name = "bankaccountuuid")
    private HsOfficeBankAccountEntity bankAccount;

    private @Column(name = "reference") String reference;

    @Column(name="validity", columnDefinition = "daterange")
    private Range<LocalDate> validity;

    @Override
    public String toString() {
        return stringify.apply(this);
    }

    @Override
    public String toShortString() {
        return reference;
    }
}
