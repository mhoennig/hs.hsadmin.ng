package net.hostsharing.hsadminng.rbac.rbacrole;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class RbacRoleNameExtractor {

    @NotNull
    public static List<String> roleNamesOf(@NotNull final List<RbacRoleEntity> roles) {
        return roles.stream().map(RbacRoleEntity::getRoleName).collect(Collectors.toList());
    }

}
