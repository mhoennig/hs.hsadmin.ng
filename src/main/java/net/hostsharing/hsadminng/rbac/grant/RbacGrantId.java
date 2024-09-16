package net.hostsharing.hsadminng.rbac.grant;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class RbacGrantId implements Serializable {

    private UUID granteeSubjectUuid;
    private UUID grantedRoleUuid;
}
