package net.hostsharing.hsadminng.rbac.rbacgrant;

import lombok.*;
import org.springframework.data.annotation.Immutable;

import javax.persistence.*;
import java.util.UUID;

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

    @Column(name = "descenantuuid", updatable = false, insertable = false)
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
}
