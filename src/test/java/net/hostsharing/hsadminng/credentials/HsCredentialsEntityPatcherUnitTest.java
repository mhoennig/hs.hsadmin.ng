package net.hostsharing.hsadminng.credentials;

import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.credentials.generated.api.v1.model.ContextResource;
import net.hostsharing.hsadminng.credentials.generated.api.v1.model.CredentialsPatchResource;
import net.hostsharing.hsadminng.rbac.test.PatchUnitTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@TestInstance(PER_CLASS)
@ExtendWith(MockitoExtension.class)
class HsCredentialsEntityPatcherUnitTest extends PatchUnitTestBase<
        CredentialsPatchResource,
        HsCredentialsEntity
        > {

    private static final UUID INITIAL_CREDENTIALS_UUID = UUID.randomUUID();

    private static final Boolean INITIAL_ACTIVE = true;
    private static final String INITIAL_EMAIL_ADDRESS = "initial@example.com";
    private static final String INITIAL_TOTP_SECRET = "initial_2fa";
    private static final String INITIAL_SMS_NUMBER = "initial_sms";
    private static final String INITIAL_PHONE_PASSWORD = "initial_phone_pw";

    private static final Boolean PATCHED_ACTIVE = false;
    private static final String PATCHED_EMAIL_ADDRESS = "patched@example.com";
    private static final String PATCHED_TOTP_SECRET = "patched_2fa";
    private static final String PATCHED_SMS_NUMBER = "patched_sms";
    private static final String PATCHED_PHONE_PASSWORD = "patched_phone_pw";

    // Contexts
    private static final UUID CONTEXT_UUID_1 = UUID.randomUUID();
    private static final UUID CONTEXT_UUID_2 = UUID.randomUUID();
    private static final UUID CONTEXT_UUID_3 = UUID.randomUUID();

    private final HsCredentialsContextRealEntity initialContextEntity1 = HsCredentialsContextRealEntity.builder()
            .uuid(CONTEXT_UUID_1)
            .type("HSADMIN")
            .qualifier("prod")
            .build();
    private final HsCredentialsContextRealEntity initialContextEntity2 = HsCredentialsContextRealEntity.builder()
            .uuid(CONTEXT_UUID_2)
            .type("SSH")
            .qualifier("dev")
            .build();

    private ContextResource patchContextResource2;
    private ContextResource patchContextResource3;

    // This is what em.find should return for CONTEXT_UUID_3
    private final HsCredentialsContextRealEntity newContextEntity3 = HsCredentialsContextRealEntity.builder()
            .uuid(CONTEXT_UUID_3)
            .type("HSADMIN")
            .qualifier("test")
            .build();

    private final Set<HsCredentialsContextRealEntity> initialContextEntities = Set.of(initialContextEntity1, initialContextEntity2);
    private List<ContextResource> patchedContextResources;
    private final Set<HsCredentialsContextRealEntity> expectedPatchedContextEntities = Set.of(initialContextEntity2, newContextEntity3);

    @Mock
    private EntityManager em;

    @BeforeEach
    void initMocks() {
        // Mock em.find for contexts that are part of the patch and need to be fetched
        lenient().when(em.find(eq(HsCredentialsContextRealEntity.class), eq(CONTEXT_UUID_1))).thenReturn(initialContextEntity1);
        lenient().when(em.find(eq(HsCredentialsContextRealEntity.class), eq(CONTEXT_UUID_2))).thenReturn(initialContextEntity2);
        lenient().when(em.find(eq(HsCredentialsContextRealEntity.class), eq(CONTEXT_UUID_3))).thenReturn(newContextEntity3);

        patchContextResource2 = new ContextResource();
        patchContextResource2.setUuid(CONTEXT_UUID_2);
        patchContextResource2.setType("SSH");
        patchContextResource2.setQualifier("dev");

        patchContextResource3 = new ContextResource();
        patchContextResource3.setUuid(CONTEXT_UUID_3);
        patchContextResource3.setType("HSADMIN");
        patchContextResource3.setQualifier("test");

        patchedContextResources = List.of(patchContextResource2, patchContextResource3);
    }

    @Override
    protected HsCredentialsEntity newInitialEntity() {
        final var entity = new HsCredentialsEntity();
        entity.setUuid(INITIAL_CREDENTIALS_UUID);
        entity.setActive(INITIAL_ACTIVE);
        entity.setEmailAddress(INITIAL_EMAIL_ADDRESS);
        entity.setTotpSecret(INITIAL_TOTP_SECRET);
        entity.setSmsNumber(INITIAL_SMS_NUMBER);
        entity.setPhonePassword(INITIAL_PHONE_PASSWORD);
        // Ensure loginContexts is a mutable set for the patcher
        entity.setLoginContexts(new HashSet<>(initialContextEntities));
        return entity;
    }

    @Override
    protected CredentialsPatchResource newPatchResource() {
        return new CredentialsPatchResource();
    }

    @Override
    protected HsCredentialsEntityPatcher createPatcher(final HsCredentialsEntity entity) {
        final var contextMapper = new CredentialContextResourceToEntityMapper(em, mock(MessageTranslator.class));
        return new HsCredentialsEntityPatcher(contextMapper, entity);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new SimpleProperty<>(
                        "active",
                        CredentialsPatchResource::setActive,
                        PATCHED_ACTIVE,
                        HsCredentialsEntity::setActive,
                        PATCHED_ACTIVE)
                    .notNullable(),
                new JsonNullableProperty<>(
                        "emailAddress",
                        CredentialsPatchResource::setEmailAddress,
                        PATCHED_EMAIL_ADDRESS,
                        HsCredentialsEntity::setEmailAddress,
                        PATCHED_EMAIL_ADDRESS),
                new JsonNullableProperty<>(
                        "totpSecret",
                        CredentialsPatchResource::setTotpSecret,
                        PATCHED_TOTP_SECRET,
                        HsCredentialsEntity::setTotpSecret,
                        PATCHED_TOTP_SECRET),
                new JsonNullableProperty<>(
                        "smsNumber",
                        CredentialsPatchResource::setSmsNumber,
                        PATCHED_SMS_NUMBER,
                        HsCredentialsEntity::setSmsNumber,
                        PATCHED_SMS_NUMBER),
                new JsonNullableProperty<>(
                        "phonePassword",
                        CredentialsPatchResource::setPhonePassword,
                        PATCHED_PHONE_PASSWORD,
                        HsCredentialsEntity::setPhonePassword,
                        PATCHED_PHONE_PASSWORD),
                new SimpleProperty<>(
                        "contexts",
                        CredentialsPatchResource::setContexts,
                        patchedContextResources,
                        HsCredentialsEntity::setLoginContexts,
                        expectedPatchedContextEntities)
                    .notNullable()
        );
    }
}
