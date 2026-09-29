package net.hostsharing.hsadminng.hs.office.bankaccount;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface HsOfficeBankRepository extends Repository<HsOfficeBankEntity, HsOfficeBankEntity.Key> {

    @Timed("app.office.banks.repo.findByCountryCodeAndBankCode")
    Optional<HsOfficeBankEntity> findByCountryCodeAndBankCode(String countryCode, String bankCode);
}
