package net.hostsharing.hsadminng.rbac.rbacuser;

import java.util.UUID;

public interface RbacUserPermission {

    UUID getRoleUuid();
    String getRoleName();
    UUID getPermissionUuid();
    String getOp();
    String getOpTableName();
    String getObjectTable();
    String getObjectIdName();
    UUID getObjectUuid();
}
