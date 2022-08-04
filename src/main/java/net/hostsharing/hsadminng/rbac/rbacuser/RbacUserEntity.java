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
//@SqlResultSetMapping(
//    name = "rbacUserPermissionMapping",
//    classes = {
//        @ConstructorResult(
//            targetClass = RbacUserPermission.class,
//            columns = {
//                @ColumnResult(name = "roleUuid", type = UUID.class),
//                @ColumnResult(name = "oleName", type = String.class),
//                @ColumnResult(name = "permissionUuid", type = UUID.class),
//                @ColumnResult(name = "op", type=String.class),
//                @ColumnResult(name = "objectTable", type=String.class),
//                @ColumnResult(name = "objectIdName", type =String.class),
//                @ColumnResult(name = "objectUuid", type = UUID.class),
//                @ColumnResult(name = "campId", type = Integer.class),
//                @ColumnResult(name = "userCount", type = Byte.class)
//            }
//        )
//    }
//)
//@NamedNativeQuery(
//    name = "grantedPermissions",
//    query = "SELECT * FROM grantedPermissions(:userName)",
//    resultSetMapping = "rbacUserPermissionMapping"
//)
public class RbacUserEntity {

    @Id
    private UUID uuid;

    private String name;
}
