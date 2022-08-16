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

    @Column(name = "grantedroleidname", updatable = false, insertable = false)
    private String grantedRoleIdName;

    @Column(name = "username", updatable = false, insertable = false)
    private String granteeUserName;

    private boolean assumed;

    @Column(name = "grantedbyroleuuid", updatable = false, insertable = false)
    private UUID grantedByRoleUuid;

    @Id
    @Column(name = "grantedroleuuid")
    private UUID grantedRoleUuid;

    @Id
    @Column(name = "useruuid")
    private UUID granteeUserUuid;

    @Column(name = "objecttable", updatable = false, insertable = false)
    private String objectTable;

    @Column(name = "objectuuid", updatable = false, insertable = false)
    private UUID objectUuid;

    @Column(name = "objectidname", updatable = false, insertable = false)
    private String objectIdName;

    @Column(name = "grantedroletype", updatable = false, insertable = false)
    @Enumerated(EnumType.STRING)
    private RbacRoleType grantedRoleType;

    public String toDisplay() {
        return "{ grant " + (assumed ? "assumed " : "") +
            "role " + grantedRoleIdName + " to user " + granteeUserName + " by role " + grantedByRoleIdName + " }";
    }
}
