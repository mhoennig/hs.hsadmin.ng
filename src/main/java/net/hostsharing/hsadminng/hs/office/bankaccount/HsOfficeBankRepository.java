package net.hostsharing.hsadminng.hs.office.bankaccount;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface HsOfficeBankRepository extends Repository<HsOfficeBankEntity, HsOfficeBankEntity.Key> {

    @Timed("app.office.banks.repo.findByCountryCodeAndBankCode")
    Optional<HsOfficeBankEntity> findByCountryCodeAndBankCode(String countryCode, String bankCode);

    @Timed("app.office.banks.repo.findAllByCountryCode")
    List<HsOfficeBankEntity> findAllByCountryCode(String countryCode);

    @Timed("app.office.banks.repo.findLastImport")
    @Query(nativeQuery = true, value = """
            select sourceUrl as "sourceUrl", startedAt as "importedAt", bankCount as "bankCount"
                from hs_office.bank_directory_import
                where outcome = 'IMPORTED'
                order by startedAt desc
                limit 1
            """)
    Optional<LastImport> findLastImport();

    @Timed("app.office.banks.repo.findLastSuccessfulImportRunAt")
    @Query(nativeQuery = true, value = """
            select max(startedAt)
                from hs_office.bank_directory_import
                where outcome in ('IMPORTED', 'UNCHANGED')
            """)
    Optional<Instant> findLastSuccessfulImportRunAt();

    @Timed("app.office.banks.repo.findRecentImportRuns")
    @Query(nativeQuery = true, value = """
            select startedAt as "startedAt", outcome as "outcome", sourceUrl as "sourceUrl", bankCount as "bankCount",
                    failureKind as "failureKind", failureRequest as "failureRequest",
                    failureHttpStatus as "failureHttpStatus", failureLine as "failureLine"
                from hs_office.bank_directory_import
                order by startedAt desc
                limit :limit
            """)
    List<ImportRun> findRecentImportRuns(int limit);

    interface LastImport {

        String getSourceUrl();

        Instant getImportedAt();

        Integer getBankCount();
    }

    interface ImportRun {

        Instant getStartedAt();

        String getOutcome();

        String getSourceUrl();

        Integer getBankCount();

        String getFailureKind();

        String getFailureRequest();

        Integer getFailureHttpStatus();

        Integer getFailureLine();
    }
}
