package net.hostsharing.hsadminng.rbac.rbacgrant;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
public class RbacGrantId implements Serializable {

    private UUID granteeUserUuid;
    private UUID grantedRoleUuid;
}
