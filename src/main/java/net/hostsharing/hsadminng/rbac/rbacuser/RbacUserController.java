package net.hostsharing.hsadminng.rbac.rbacuser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import net.hostsharing.hsadminng.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;

@RestController
public class RbacUserController {

    @Autowired
    private Context context;

    @Autowired
    private RbacUserRepository rbacUserRepository;

    @GetMapping(value = "/api/rbacusers")
    @Operation(description = "List accessible RBAC users with optional filter by name.",
        responses = {
            @ApiResponse(responseCode = "200",
                content = @Content(array = @ArraySchema(
                    schema = @Schema(implementation = RbacUserEntity.class)))),
            @ApiResponse(responseCode = "401",
                description = "if the 'current-user' cannot be identified"),
            @ApiResponse(responseCode = "403",
                description = "if the 'current-user' is not allowed to assume any of the roles from 'assumed-roles'") })
    @Transactional
    public List<RbacUserEntity> listUsers(
        @RequestHeader(name = "current-user") String currentUserName,
        @RequestHeader(name = "assumed-roles", required = false) String assumedRoles,
        @RequestParam(name="name", required = false) String userName
    ) {
        context.setCurrentUser(currentUserName);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }
        return rbacUserRepository.findByOptionalNameLike(userName);
    }

    @GetMapping(value = "/api/rbacuser/{userName}/permissions")
    @Operation(description = "List all visible permissions granted to the given user; reduced ", responses = {
        @ApiResponse(responseCode = "200",
            content = @Content(array = @ArraySchema( schema = @Schema(implementation = RbacUserPermission.class)))),
        @ApiResponse(responseCode = "401",
            description = "if the 'current-user' cannot be identified"),
        @ApiResponse(responseCode = "403",
            description = "if the 'current-user' is not allowed to view permissions of the given user") })
    @Transactional
    public List<RbacUserPermission> listUserPermissions(
        @RequestHeader(name = "current-user") String currentUserName,
        @RequestHeader(name = "assumed-roles", required = false) String assumedRoles,
        @PathVariable(name= "userName") String userName
    ) {
        context.setCurrentUser(currentUserName);
        if (assumedRoles != null && !assumedRoles.isBlank()) {
            context.assumeRoles(assumedRoles);
        }
        return rbacUserRepository.findPermissionsOfUser(userName);
    }
}
