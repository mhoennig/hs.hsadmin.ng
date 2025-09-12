package net.hostsharing.hsadminng.hs.accounts;

import lombok.val;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.ScopeResource;
import net.hostsharing.hsadminng.accounts.generated.api.v1.model.ProfilePatchResource;
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
class HsProfileEntityPatcherUnitTest extends PatchUnitTestBase<
        ProfilePatchResource,
        HsProfileEntity
        > {

    private static final UUID INITIAL_PROFILE_UUID = UUID.randomUUID();

    private static final Boolean INITIAL_ACTIVE = true;
    private static final String INITIAL_EMAIL_ADDRESS = "initial@example.com";
    private static final List<String> INITIAL_TOTP_SECRETS = List.of("initial_2fa");
    private static final String INITIAL_SMS_NUMBER = "initial_sms";
    private static final String INITIAL_PHONE_PASSWORD = "initial_phone_pw";

    private static final Boolean PATCHED_ACTIVE = false;
    private static final String PATCHED_EMAIL_ADDRESS = "patched@example.com";
    private static final List<String> PATCHED_TOTP_SECRETS = List.of("patched_2fa");
    private static final String PATCHED_SMS_NUMBER = "patched_sms";
    private static final String PATCHED_PHONE_PASSWORD = "patched_phone_pw";

    // Scopes
    private static final UUID SCOPE_UUID_1 = UUID.randomUUID();
    private static final UUID SCOPE_UUID_2 = UUID.randomUUID();
    private static final UUID SCOPE_UUID_3 = UUID.randomUUID();

    private final HsProfileScopeRealEntity initialScopeEntity1 = HsProfileScopeRealEntity.builder()
            .uuid(SCOPE_UUID_1)
            .type("HSADMIN")
            .qualifier("prod")
            .build();
    private final HsProfileScopeRealEntity initialScopeEntity2 = HsProfileScopeRealEntity.builder()
            .uuid(SCOPE_UUID_2)
            .type("SSH")
            .qualifier("dev")
            .build();

    // This is what em.find should return for SCOPE_UUID_3
    private final HsProfileScopeRealEntity newScopeEntity3 = HsProfileScopeRealEntity.builder()
            .uuid(SCOPE_UUID_3)
            .type("HSADMIN")
            .qualifier("test")
            .build();

    private final Set<HsProfileScopeRealEntity> initialScopeEntities = Set.of(initialScopeEntity1, initialScopeEntity2);
    private List<ScopeResource> patchedScopeResources;
    private final Set<HsProfileScopeRealEntity> expectedPatchedScopeEntities = Set.of(initialScopeEntity2,
            newScopeEntity3);

    @Mock
    private EntityManager em;

    @BeforeEach
    void initMocks() {
        // Mock em.find for scopes that are part of the patch and need to be fetched
        lenient().when(em.find(eq(HsProfileScopeRealEntity.class), eq(SCOPE_UUID_1))).thenReturn(initialScopeEntity1);
        lenient().when(em.find(eq(HsProfileScopeRealEntity.class), eq(SCOPE_UUID_2))).thenReturn(initialScopeEntity2);
        lenient().when(em.find(eq(HsProfileScopeRealEntity.class), eq(SCOPE_UUID_3))).thenReturn(newScopeEntity3);

        val patchScopeResource2 = new ScopeResource();
        patchScopeResource2.setUuid(SCOPE_UUID_2);
        patchScopeResource2.setType("SSH");
        patchScopeResource2.setQualifier("dev");

        val patchScopeResource3 = new ScopeResource();
        patchScopeResource3.setUuid(SCOPE_UUID_3);
        patchScopeResource3.setType("HSADMIN");
        patchScopeResource3.setQualifier("test");

        patchedScopeResources = List.of(patchScopeResource2, patchScopeResource3);
    }

    @Override
    protected HsProfileEntity newInitialEntity() {
        final var entity = new HsProfileEntity();
        entity.setUuid(INITIAL_PROFILE_UUID);
        entity.setActive(INITIAL_ACTIVE);
        entity.setEmailAddress(INITIAL_EMAIL_ADDRESS);
        entity.setTotpSecrets(INITIAL_TOTP_SECRETS);
        entity.setSmsNumber(INITIAL_SMS_NUMBER);
        entity.setPhonePassword(INITIAL_PHONE_PASSWORD);
        // Ensure scopes is a mutable set for the patcher
        entity.setScopes(new HashSet<>(initialScopeEntities));
        return entity;
    }

    @Override
    protected ProfilePatchResource newPatchResource() {
        return new ProfilePatchResource();
    }

    @Override
    protected HsProfileEntityPatcher createPatcher(final HsProfileEntity entity) {
        final var scopeMapper = new ScopeResourceToEntityMapper(em, mock(MessageTranslator.class));
        return new HsProfileEntityPatcher(scopeMapper, entity);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new SimpleProperty<>(
                        "active",
                        ProfilePatchResource::setActive,
                        PATCHED_ACTIVE,
                        HsProfileEntity::setActive,
                        PATCHED_ACTIVE)
                    .notNullable(),
                new JsonNullableProperty<>(
                        "emailAddress",
                        ProfilePatchResource::setEmailAddress,
                        PATCHED_EMAIL_ADDRESS,
                        HsProfileEntity::setEmailAddress,
                        PATCHED_EMAIL_ADDRESS),
                new SimpleProperty<>(
                        "totpSecret",
                        ProfilePatchResource::setTotpSecrets,
                        PATCHED_TOTP_SECRETS,
                        HsProfileEntity::setTotpSecrets,
                        PATCHED_TOTP_SECRETS)
                        .notNullable(),
                new JsonNullableProperty<>(
                        "smsNumber",
                        ProfilePatchResource::setSmsNumber,
                        PATCHED_SMS_NUMBER,
                        HsProfileEntity::setSmsNumber,
                        PATCHED_SMS_NUMBER),
                new JsonNullableProperty<>(
                        "phonePassword",
                        ProfilePatchResource::setPhonePassword,
                        PATCHED_PHONE_PASSWORD,
                        HsProfileEntity::setPhonePassword,
                        PATCHED_PHONE_PASSWORD),
                new SimpleProperty<>(
                        "scopes",
                        ProfilePatchResource::setScopes,
                        patchedScopeResources,
                        HsProfileEntity::setScopes,
                        expectedPatchedScopeEntities)
                    .notNullable()
        );
    }
}
