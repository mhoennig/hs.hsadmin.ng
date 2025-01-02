package net.hostsharing.hsadminng.rbac.grant;

import lombok.*;
import net.hostsharing.hsadminng.rbac.role.RbacRoleType;
import org.springframework.data.annotation.Immutable;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(schema = "rbac", name = "grant_rv")
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
    private String granteeSubjectName;

    @Id
    @Column(name = "subjectuuid")
    private UUID granteeSubjectUuid;

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
        return new RbacGrantId(granteeSubjectUuid, grantedRoleUuid);
    }

    public String toDisplay() {
        return "{ grant role:" + grantedRoleIdName +
                " to user:" + granteeSubjectName +
                " by role:" + grantedByRoleIdName +
                (assumed ? " and assume" : "") +
                " }";
    }
}
