package net.hostsharing.hsadminng.rbac.rbacrole;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.rbac.generated.api.v1.api.RbacRolesApi;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacRoleResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static net.hostsharing.hsadminng.mapper.Mapper.mapList;

@RestController

public class RbacRoleController implements RbacRolesApi {

    @Autowired
    private Context context;

    @Autowired
    private RbacRoleRepository rbacRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<List<RbacRoleResource>> listRoles(
            final String currentUser,
            final String assumedRoles) {

        context.define(currentUser, assumedRoles);

        return ResponseEntity.ok(mapList(rbacRoleRepository.findAll(), RbacRoleResource.class));
    }

}
