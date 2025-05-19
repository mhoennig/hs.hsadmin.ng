package net.hostsharing.hsadminng.credentials;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HsCredentialsContextRealEntityUnitTest {

    @Test
    void toShortString() {
        final var entity = HsCredentialsContextRealEntity.builder()
                .uuid(UUID.randomUUID())
                .type("testType")
                .qualifier("testQualifier")
                .build();
        assertEquals("loginContext(testType:testQualifier)", entity.toShortString());
    }
}
