package net.hostsharing.hsadminng.rbac.subject;

import java.util.UUID;

public interface RbacSubjectPermission {

    UUID getRoleUuid();
    String getRoleName();
    UUID getPermissionUuid();
    String getOp();
    String getOpTableName();
    String getObjectTable();
    String getObjectIdName();
    UUID getObjectUuid();
}
