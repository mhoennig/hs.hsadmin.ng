package net.hostsharing.hsadminng.rbac.rbacrole;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import org.springframework.data.annotation.Immutable;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "rbacrole_rv")
@Getter
@Setter
@Immutable
@NoArgsConstructor
@AllArgsConstructor
public class RbacRoleEntity {

    @Id
    private UUID uuid;

    @Column(name="objectuuid")
    private UUID objectUuid;

    @Column(name="roletype")
    @Enumerated(EnumType.STRING)
    private RbacRoleType roleType;

    @Column(name="objecttable")
    private String objectTable;

    @Column(name="objectidname")
    private String objectIdName;

    @Formula("objectTable||'#'||objectIdName||'.'||roleType")
    private String roleName;
}
