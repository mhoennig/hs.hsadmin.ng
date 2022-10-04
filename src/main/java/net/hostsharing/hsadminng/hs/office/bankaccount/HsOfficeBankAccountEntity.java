package net.hostsharing.hsadminng.hs.office.bankaccount;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.hostsharing.hsadminng.Stringify;
import net.hostsharing.hsadminng.Stringifyable;
import net.hostsharing.hsadminng.errors.DisplayName;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

import static net.hostsharing.hsadminng.Stringify.stringify;

@Entity
@Table(name = "hs_office_bankaccount_rv")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@DisplayName("BankAccount")
public class HsOfficeBankAccountEntity implements Stringifyable {

    private static Stringify<HsOfficeBankAccountEntity> toString = stringify(HsOfficeBankAccountEntity.class, "bankAccount")
            .withProp(Fields.holder, HsOfficeBankAccountEntity::getHolder)
            .withProp(Fields.iban, HsOfficeBankAccountEntity::getIban)
            .withProp(Fields.bic, HsOfficeBankAccountEntity::getBic);

    private @Id UUID uuid;
    private String holder;

    private String iban;

    private String bic;

    @Override
    public String toString() {
        return toString.apply(this);
    }

    @Override
    public String toShortString() {
        return holder;
    }
}
