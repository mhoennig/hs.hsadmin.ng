package net.hostsharing.hsadminng.rbac.subject;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(schema = "rbac", name = "subject_sync_report")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectSyncReportEntity {

    @Id
    private UUID uuid;

    @Column(name = "reported_at")
    private OffsetDateTime reportedAt;

    @Column(name = "reported_by")
    private String reportedBy;

    @Type(JsonType.class)
    @Column(name = "statistics")
    private Map<String, Object> statistics;
}
