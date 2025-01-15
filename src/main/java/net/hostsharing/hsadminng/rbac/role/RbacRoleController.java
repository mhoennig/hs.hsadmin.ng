package net.hostsharing.hsadminng.rbac.role;

import io.micrometer.core.annotation.Timed;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.rbac.generated.api.v1.api.RbacRolesApi;
import net.hostsharing.hsadminng.rbac.generated.api.v1.model.RbacRoleResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RbacRoleController implements RbacRolesApi {

    @Autowired
    private Context context;

    @Autowired
    private StrictMapper mapper;

    @Autowired
    private RbacRoleRepository rbacRoleRepository;

    @Override
    @Transactional(readOnly = true)
    @Timed("app.rbac.roles.api.getListOfRoles")
    public ResponseEntity<List<RbacRoleResource>> getListOfRoles(
            final String currentSubject,
            final String assumedRoles) {

        context.define(currentSubject, assumedRoles);

        final List<RbacRoleEntity> result = rbacRoleRepository.findAll();

        return ResponseEntity.ok(mapper.mapList(result, RbacRoleResource.class));
    }

}
