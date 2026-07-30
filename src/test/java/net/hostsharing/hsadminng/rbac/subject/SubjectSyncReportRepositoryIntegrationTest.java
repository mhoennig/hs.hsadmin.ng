package net.hostsharing.hsadminng.rbac.subject;

import net.hostsharing.hsadminng.rbac.context.ContextBasedTest;
import net.hostsharing.hsadminng.rbac.test.RbacDataJpaTest;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@RbacDataJpaTest
@Tag("generalIntegrationTest")
class SubjectSyncReportRepositoryIntegrationTest extends ContextBasedTest {

    @Autowired
    SubjectSyncReportRepository repository;

    @Test
    void savesReportAndFindsItWithItsStatisticsOnlyWithinTheInterval() {
        val reporter = uniqueReporter();
        val now = OffsetDateTime.now();
        save(reporter, now.minusMinutes(1), Map.of("processed", 1234, "created", 3));

        val latest = latest(reporter, now.minusHours(1));
        assertThat(latest).isNotNull();
        assertThat(latest.getReportedAt()).isCloseTo(now.minusMinutes(1), within(1, ChronoUnit.SECONDS));
        assertThat(((Number) latest.getStatistics().get("processed")).intValue()).isEqualTo(1234);
        assertThat(((Number) latest.getStatistics().get("created")).intValue()).isEqualTo(3);

        assertThat(latest(reporter, now.minusSeconds(30)))
                .as("a report older than the look-back window is ignored")
                .isNull();
    }

    @Test
    void findsTheMostRecentReportOfThatReporter() {
        val reporter = uniqueReporter();
        val now = OffsetDateTime.now();
        save(reporter, now.minusMinutes(5), Map.of("run", 1));
        save(reporter, now.minusMinutes(1), Map.of("run", 2));
        save(uniqueReporter(), now, Map.of("run", 99)); // a different reporter must not interfere

        val latest = latest(reporter, now.minusHours(1));
        assertThat(latest.getReportedAt()).isCloseTo(now.minusMinutes(1), within(1, ChronoUnit.SECONDS));
        assertThat(((Number) latest.getStatistics().get("run")).intValue()).isEqualTo(2);
    }

    @Test
    void deleteReportedBeforeRemovesOnlyOlderRows() {
        val reporter = uniqueReporter();
        val now = OffsetDateTime.now();
        save(reporter, now.minusDays(10), Map.of("old", true));
        save(reporter, now.minusMinutes(1), Map.of("recent", true));

        val deleted = repository.deleteReportedBefore(now.minusDays(1));

        assertThat(deleted).isGreaterThanOrEqualTo(1);
        assertThat(latest(reporter, now.minusHours(1)))
                .as("the recent report survives the retention delete")
                .isNotNull();
    }

    private SubjectSyncReportEntity latest(final String reporter, final OffsetDateTime notBefore) {
        return repository.findFirstByReportedByAndReportedAtGreaterThanOrderByReportedAtDesc(reporter, notBefore);
    }

    private void save(final String reporter, final OffsetDateTime reportedAt, final Map<String, Object> statistics) {
        repository.save(SubjectSyncReportEntity.builder()
                .uuid(UUID.randomUUID())
                .reportedAt(reportedAt)
                .reportedBy(reporter)
                .statistics(statistics)
                .build());
    }

    private static String uniqueReporter() {
        return "hsadminng.keycloak.sync." + UUID.randomUUID();
    }
}
