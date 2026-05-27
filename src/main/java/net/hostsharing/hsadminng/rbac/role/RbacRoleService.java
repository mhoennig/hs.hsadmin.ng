package net.hostsharing.hsadminng.rbac.role;

import lombok.val;
import net.hostsharing.hsadminng.persistence.BaseEntity;
import net.hostsharing.hsadminng.repr.Stringifyable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


/// Just a APIs to programmatically handle RBAC roles.
@Service
public class RbacRoleService{

    @Autowired
    private RbacRoleRepository rbacRoleRepo;

    public RbacRoleEntity rbacRole(final BaseEntity<?> rbacEntity, final RbacRoleType roleType) {
        val personAdminRole = rbacRoleRepo.findByObjectUuidAndRoleType(rbacEntity.getUuid(), roleType);
        if (personAdminRole == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "no ADMIN role not found for %s %s".formatted(
                            Stringifyable.toShortString(rbacEntity),
                            rbacEntity.getUuid()
                    ));
        }
        return personAdminRole;
    }
}
