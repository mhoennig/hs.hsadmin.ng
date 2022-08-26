package net.hostsharing.hsadminng.rbac.rbacgrant;

import lombok.*;
import net.hostsharing.hsadminng.rbac.rbacrole.RbacRoleType;
import org.springframework.data.annotation.Immutable;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "rbacgrants_rv")
@IdClass(RbacGrantId.class)
@Getter
@Setter
@Builder
@ToString
@Immutable
@NoArgsConstructor
@AllArgsConstructor
public class RbacGrantEntity {

    @Column(name = "grantedbyroleidname", updatable = false, insertable = false)
    private String grantedByRoleIdName;

    @Column(name = "grantedbyroleuuid", updatable = false, insertable = false)
    private UUID grantedByRoleUuid;

    @Column(name = "grantedroleidname", updatable = false, insertable = false)
    private String grantedRoleIdName;

    @Id
    @Column(name = "grantedroleuuid")
    private UUID grantedRoleUuid;

    @Column(name = "username", updatable = false, insertable = false)
    private String granteeUserName;

    @Id
    @Column(name = "useruuid")
    private UUID granteeUserUuid;

    private boolean assumed;

    @Column(name = "objecttable", updatable = false, insertable = false)
    private String objectTable;

    @Column(name = "objectidname", updatable = false, insertable = false)
    private String objectIdName;

    @Column(name = "objectuuid", updatable = false, insertable = false)
    private UUID objectUuid;

    @Column(name = "grantedroletype", updatable = false, insertable = false)
    @Enumerated(EnumType.STRING)
    private RbacRoleType grantedRoleType;

    RbacGrantId getRbacGrantId() {
        return new RbacGrantId(granteeUserUuid, grantedRoleUuid);
    }

    public String toDisplay() {
        return "{ grant " + (assumed ? "assumed " : "") +
                "role " + grantedRoleIdName + " to user " + granteeUserName + " by role " + grantedByRoleIdName + " }";
    }
}
