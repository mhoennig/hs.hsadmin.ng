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

    @Column(name = "username", updatable = false, insertable = false)
    private String userName;

    @Column(name = "roleidname", updatable = false, insertable = false)
    private String roleIdName;

    private boolean managed;
    private boolean assumed;
    private boolean empowered;

    @Id
    @Column(name = "useruuid")
    private UUID userUuid;

    @Id
    @Column(name = "roleuuid")
    private UUID roleUuid;

    @Column(name = "objecttable", updatable = false, insertable = false)
    private String objectTable;

    @Column(name = "objectuuid", updatable = false, insertable = false)
    private UUID objectUuid;

    @Column(name = "objectidname", updatable = false, insertable = false)
    private String objectIdName;

    @Column(name = "roletype", updatable = false, insertable = false)
    @Enumerated(EnumType.STRING)
    private RbacRoleType roleType;

    public String toDisplay() {
        return "grant( " + userName + " -> " + roleIdName + ": " +
            (managed ? "managed " : "") +
            (assumed ? "assumed " : "") +
            (empowered ? "empowered " : "") +
            ")";
    }
}
