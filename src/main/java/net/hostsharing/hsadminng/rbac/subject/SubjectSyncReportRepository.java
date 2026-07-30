package net.hostsharing.hsadminng.rbac.subject;

import io.micrometer.core.annotation.Timed;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface SubjectSyncReportRepository extends Repository<SubjectSyncReportEntity, UUID> {

    @Timed("app.rbac.subjects.repo.saveSyncReport")
    SubjectSyncReportEntity save(SubjectSyncReportEntity report);

    // the most recent report of that reporter within the look-back window, or null if there is none
    @Timed("app.rbac.subjects.repo.findLatestReport")
    SubjectSyncReportEntity findFirstByReportedByAndReportedAtGreaterThanOrderByReportedAtDesc(
            String reportedBy, OffsetDateTime notBefore);

    @Modifying
    @Query("delete from SubjectSyncReportEntity r where r.reportedAt < :cutoff")
    @Timed("app.rbac.subjects.repo.deleteReportedBefore")
    int deleteReportedBefore(@Param("cutoff") OffsetDateTime cutoff);
}
