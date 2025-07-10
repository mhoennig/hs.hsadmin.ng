package net.hostsharing.hsadminng.hs.accounts;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HsCredentialsContextRbacEntityUnitTest {

    @Test
    void toShortString() {
        final var entity = HsCredentialsContextRbacEntity.builder()
                .uuid(UUID.randomUUID())
                .type("SSH")
                .qualifier("prod")
                .build();
        assertEquals("loginContext(SSH:prod)", entity.toShortString());
    }
}
