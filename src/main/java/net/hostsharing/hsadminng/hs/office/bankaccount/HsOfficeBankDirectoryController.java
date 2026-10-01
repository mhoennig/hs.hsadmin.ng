package net.hostsharing.hsadminng.hs.office.bankaccount;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.api.HsOfficeBankDirectoryApi;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankDirectoryImportFailureKindResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankDirectoryImportFailureResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankDirectoryImportResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankDirectoryImportRunOutcomeResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankDirectoryImportRunResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeBankDirectoryImportStatusResource;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestController
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
public class HsOfficeBankDirectoryController implements HsOfficeBankDirectoryApi {

    /** Lets the SEPA mandates report their issues without this package knowing them. */
    public interface SepaMandateIssues {

        int countValidSepaMandatesWithIssues();
    }

    @Autowired
    private HsOfficeBankRepository bankRepo;

    @Autowired
    private SepaMandateIssues sepaMandateIssues;

    @Override
    @PreAuthorize("permitAll()") // public, so external monitoring can poll it without credentials
    @Transactional(readOnly = true)
    @Timed("app.office.bankDirectory.api.getBankDirectoryImportStatus")
    public ResponseEntity<HsOfficeBankDirectoryImportStatusResource> getBankDirectoryImportStatus(final Integer limit) {
        val result = new HsOfficeBankDirectoryImportStatusResource();
        bankRepo.findLastImport().ifPresent(lastImport -> result.setCurrentImport(toResource(lastImport)));
        bankRepo.findLastSuccessfulImportRunAt().ifPresent(startedAt -> result.setLastSuccessfulRunAt(utc(startedAt)));
        result.setRecentRuns(bankRepo.findRecentImportRuns(limit).stream().map(this::toResource).toList());
        result.setValidSepaMandatesWithIssues(sepaMandateIssues.countValidSepaMandatesWithIssues());
        return ResponseEntity.ok(result);
    }

    private HsOfficeBankDirectoryImportResource toResource(final HsOfficeBankRepository.LastImport lastImport) {
        val resource = new HsOfficeBankDirectoryImportResource();
        resource.setSourceUrl(lastImport.getSourceUrl());
        resource.setImportedAt(utc(lastImport.getImportedAt()));
        resource.setBankCount(lastImport.getBankCount());
        return resource;
    }

    private HsOfficeBankDirectoryImportRunResource toResource(final HsOfficeBankRepository.ImportRun run) {
        val resource = new HsOfficeBankDirectoryImportRunResource();
        resource.setStartedAt(utc(run.getStartedAt()));
        resource.setOutcome(HsOfficeBankDirectoryImportRunOutcomeResource.fromValue(run.getOutcome()));
        resource.setSourceUrl(run.getSourceUrl());
        resource.setBankCount(run.getBankCount());
        if (run.getFailureKind() != null) {
            val failure = new HsOfficeBankDirectoryImportFailureResource();
            failure.setKind(HsOfficeBankDirectoryImportFailureKindResource.fromValue(run.getFailureKind()));
            failure.setRequest(run.getFailureRequest());
            failure.setHttpStatus(run.getFailureHttpStatus());
            failure.setLine(run.getFailureLine());
            resource.setFailure(failure);
        }
        return resource;
    }

    private static OffsetDateTime utc(final Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
