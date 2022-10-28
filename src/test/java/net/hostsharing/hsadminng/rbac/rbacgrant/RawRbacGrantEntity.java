package net.hostsharing.hsadminng.rbac.rbacgrant;

import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "rbacgrants_ev")
@Getter
@Setter
@Builder
@ToString
@Immutable
@NoArgsConstructor
@AllArgsConstructor
public class RawRbacGrantEntity {

    @Id
    private UUID uuid;

    @Column(name = "grantedbyroleidname", updatable = false, insertable = false)
    private String grantedByRoleIdName;

    @Column(name = "grantedbyroleuuid", updatable = false, insertable = false)
    private UUID grantedByRoleUuid;

    @Column(name = "ascendantidname", updatable = false, insertable = false)
    private String ascendantIdName;

    @Column(name = "ascendantuuid", updatable = false, insertable = false)
    private UUID ascendingUuid;

    @Column(name = "descendantidname", updatable = false, insertable = false)
    private String descendantIdName;

    @Column(name = "descendantuuid", updatable = false, insertable = false)
    private UUID descendantUuid;

    @Column(name = "assumed", updatable = false, insertable = false)
    private boolean assumed;

    public String toDisplay() {
        // @formatter:off
        return "{ grant " + descendantIdName +
                    " to " + ascendantIdName +
                    " by " + ( grantedByRoleUuid == null
                        ? "system"
                        : grantedByRoleIdName ) +
                    ( assumed ? " and assume" : "") +
                " }";
        // @formatter:on
    }


    @NotNull
    public static List<String> grantDisplaysOf(final List<RawRbacGrantEntity> roles) {
        return roles.stream().map(RawRbacGrantEntity::toDisplay).collect(Collectors.toList());
    }
}
