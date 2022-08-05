package net.hostsharing.hsadminng.rbac.rbacuser;

import lombok.*;
import org.springframework.data.annotation.Immutable;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "rbacuser_rv")
@Getter
@Setter
@ToString
@Immutable
@NoArgsConstructor
@AllArgsConstructor
public class RbacUserEntity {

    @Id
    private UUID uuid;

    private String name;
}
