package net.hostsharing.hsadminng.rbac.rbacrole;

import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.generated.api.v1.api.RbacrolesApi;
import net.hostsharing.hsadminng.generated.api.v1.model.RbacRoleResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.util.List;

import static net.hostsharing.hsadminng.Mapper.mapList;

@RestController

public class RbacRoleController implements RbacrolesApi {

    @Autowired
    private Context context;

    @Autowired
    private RbacRoleRepository rbacRoleRepository;

    @Override
    @Transactional
    public ResponseEntity<List<RbacRoleResource>> listRoles(final String currentUser, final String assumedRoles) {
        context.setCurrentUser(currentUser);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }
        return ResponseEntity.ok(mapList(rbacRoleRepository.findAll(), RbacRoleResource.class));
    }
}
