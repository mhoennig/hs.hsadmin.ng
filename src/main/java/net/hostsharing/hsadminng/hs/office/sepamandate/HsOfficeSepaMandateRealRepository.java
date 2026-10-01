package net.hostsharing.hsadminng.hs.office.sepamandate;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

/** Reads the tables themselves, because the bank-directory check runs without any login and has to see all SEPA mandates. */
public interface HsOfficeSepaMandateRealRepository extends Repository<HsOfficeSepaMandateEntity, UUID> {

    @Timed("app.office.sepaMandates.repo.findBankAccountsOfValidSepaMandates")
    @Query(nativeQuery = true, value = """
            select bankAccount.iban as "iban", bankAccount.bic as "bic"
                from hs_office.sepamandate mandate
                join hs_office.bankaccount bankAccount on bankAccount.uuid = mandate.bankAccountUuid
                where not isempty(mandate.validity)
                  and (upper_inf(mandate.validity) or upper(mandate.validity) > current_date)
            """)
    List<BankAccountOfSepaMandate> findBankAccountsOfValidSepaMandates();

    interface BankAccountOfSepaMandate {

        String getIban();

        String getBic();
    }
}
