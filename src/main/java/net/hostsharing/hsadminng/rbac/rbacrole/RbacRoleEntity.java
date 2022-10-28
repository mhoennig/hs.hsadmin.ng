package net.hostsharing.hsadminng.rbac.rbacrole;

import lombok.*;
import org.hibernate.annotations.Formula;
import org.springframework.data.annotation.Immutable;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "rbacrole_rv")
@Getter
@Setter
@ToString
@Immutable
@NoArgsConstructor
@AllArgsConstructor
public class RbacRoleEntity {

    @Id
    @GeneratedValue
    private UUID uuid;

    @Column(name = "objectuuid")
    private UUID objectUuid;

    @Column(name = "objecttable")
    private String objectTable;

    @Column(name = "objectidname")
    private String objectIdName;

    @Column(name = "roletype")
    @Enumerated(EnumType.STRING)
    private RbacRoleType roleType;

    @Formula("objectTable||'#'||objectIdName||'.'||roleType")
    private String roleName;
}
