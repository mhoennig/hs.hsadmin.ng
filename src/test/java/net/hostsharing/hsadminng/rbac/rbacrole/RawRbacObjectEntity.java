package net.hostsharing.hsadminng.rbac.rbacrole;

import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.Immutable;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rbacobject") // TODO: create view rbacobject_ev
@Getter
@Setter
@ToString
@Immutable
@NoArgsConstructor
@AllArgsConstructor
public class RawRbacObjectEntity {

    @Id
    private UUID uuid;

    @Column(name="objecttable")
    private String objectTable;

    @NotNull
    public static List<String> objectDisplaysOf(@NotNull final List<RawRbacObjectEntity> roles) {
        return roles.stream().map(e -> e.objectTable+ "#" + e.uuid).sorted().toList();
    }
}
