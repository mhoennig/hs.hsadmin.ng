package net.hostsharing.hsadminng.rbac.rbacgrant;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class RbacGrantDisplayExtractor {

    @NotNull
    public static List<String> grantDisplaysOf(final List<RbacGrantEntity> roles) {
        return roles.stream().map(RbacGrantEntity::toDisplay).collect(Collectors.toList());
    }
}
