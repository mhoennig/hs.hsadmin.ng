package net.hostsharing.hsadminng.hs.accounts;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HsProfileScopeRbacEntityUnitTest {

    @Test
    void toShortStringContainsJustTypeAndQualifier() {
        final var entity = HsProfileScopeRbacEntity.builder()
                .uuid(UUID.randomUUID())
                .type("SSH")
                .qualifier("prod")
                .publicAccess(true)
                .build();
        assertEquals("SSH:prod", entity.toShortString());
    }

    @Test
    void toStringContainsAllNonNullFields() {
        final var entity = HsProfileScopeRbacEntity.builder()
                .uuid(UUID.randomUUID())
                .type("SSH")
                .qualifier("prod")
                .publicAccess(true)
                .build();
        assertEquals("scope(SSH:prod:PUBLIC)", entity.toString());
    }
}
