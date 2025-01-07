package net.hostsharing.hsadminng.rbac.role;

import jakarta.persistence.Table;
import java.util.UUID;

public interface WithRoleId  {
    UUID getUuid();

    /**
     * @return the RBAC-Role-Id of the given `rbacRoleType` for this entity instance.
     */
    default String roleId(final RbacRoleType rbacRoleType) {
        if ( getUuid() == null ) {
            throw new IllegalStateException("UUID missing => role can't be determined");
        }
        final Table tableAnnot = getClass().getAnnotation(Table.class);
        final var qualifiedTableName = tableAnnot.schema() + "." + tableAnnot.name();
        return qualifiedTableName + "#" + getUuid() + ":" + rbacRoleType.name();
    }
}
