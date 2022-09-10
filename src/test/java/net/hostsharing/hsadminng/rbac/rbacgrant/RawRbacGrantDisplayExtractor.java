package net.hostsharing.hsadminng.rbac.rbacgrant;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class RawRbacGrantDisplayExtractor {

    @NotNull
    public static List<String> grantDisplaysOf(final List<RawRbacGrantEntity> roles) {
        return roles.stream().map(RawRbacGrantEntity::toDisplay).collect(Collectors.toList());
    }
}
