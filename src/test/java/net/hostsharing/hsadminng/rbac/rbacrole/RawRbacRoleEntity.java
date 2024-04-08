package net.hostsharing.hsadminng.rbac.rbacrole;

import lombok.*;
import org.hibernate.annotations.Formula;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.Immutable;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

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

    @Formula("objectTable||'#'||objectIdName||':'||roleType")
    private String roleName;

    @NotNull
    public static List<String> distinctRoleNamesOf(@NotNull final List<RawRbacRoleEntity> roles) {
        // TODO: remove .distinct() once partner.person + partner.contract are removed
        return roles.stream().map(RawRbacRoleEntity::getRoleName).sorted().distinct().toList();
    }

}
