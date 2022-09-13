package net.hostsharing.hsadminng.rbac.rbacrole;

import lombok.*;
import org.hibernate.annotations.Formula;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.Immutable;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "rbacrole_ev")
@Getter
@Setter
@ToString
@Immutable
@NoArgsConstructor
@AllArgsConstructor
public class RawRbacRoleEntity {

    @Id
    private UUID uuid;

    @Column(name="objectuuid")
    private UUID objectUuid;

    @Column(name="objecttable")
    private String objectTable;

    @Column(name="objectidname")
    private String objectIdName;

    @Column(name="roletype")
    @Enumerated(EnumType.STRING)
    private RbacRoleType roleType;

    @Formula("objectTable||'#'||objectIdName||'.'||roleType")
    private String roleName;

    @NotNull
    public static List<String> roleNamesOf(@NotNull final List<RawRbacRoleEntity> roles) {
        return roles.stream().map(RawRbacRoleEntity::getRoleName).collect(Collectors.toList());
    }

}
