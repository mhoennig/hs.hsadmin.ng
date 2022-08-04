package net.hostsharing.hsadminng.rbac.rbacuser;

import java.util.UUID;

public interface RbacUserPermission {

    UUID getRoleUuid();
    String getRoleName();
    UUID getPermissionUuid();
    String getOp();
    String getObjectTable();
    String getObjectIdName();
    UUID getObjectUuid();

}
