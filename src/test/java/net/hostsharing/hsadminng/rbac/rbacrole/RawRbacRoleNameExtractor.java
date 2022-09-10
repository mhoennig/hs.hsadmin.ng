package net.hostsharing.hsadminng.rbac.rbacrole;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class RawRbacRoleNameExtractor {

    @NotNull
    public static List<String> roleNamesOf(@NotNull final List<RawRbacRoleEntity> roles) {
        return roles.stream().map(RawRbacRoleEntity::getRoleName).collect(Collectors.toList());
    }

}
