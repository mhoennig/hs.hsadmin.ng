package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetPatchResource;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactRealEntity;
import net.hostsharing.hsadminng.mapper.KeyValueMap;
import net.hostsharing.hsadminng.rbac.test.PatchUnitTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.CLOUD_SERVER_BOOKING_ITEM_REAL_ENTITY;
import static net.hostsharing.hsadminng.mapper.PatchMap.entry;
import static net.hostsharing.hsadminng.mapper.PatchMap.patchMap;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@TestInstance(PER_CLASS)
@ExtendWith(MockitoExtension.class)
class HsHostingAssetEntityPatcherUnitTest extends PatchUnitTestBase<
        HsHostingAssetPatchResource,
        HsHostingAssetRbacEntity
        > {

    private static final UUID INITIAL_BOOKING_ITEM_UUID = UUID.randomUUID();
    private static final UUID PATCHED_CONTACT_UUID = UUID.randomUUID();

    private static final Map<String, Object> INITIAL_CONFIG = patchMap(
            entry("CPU", 1),
            entry("HDD", 1024),
            entry("MEM", 64)
    );
    private static final  Map<String, Object> PATCH_CONFIG = patchMap(
            entry("CPU", 2),
            entry("HDD", null),
            entry("SSD", 256)
    );
    private static final  Map<String, Object> PATCHED_CONFIG = patchMap(
            entry("CPU", 2),
            entry("SSD", 256),
            entry("MEM", 64)
    );
    final HsOfficeContactRealEntity givenInitialContact = HsOfficeContactRealEntity.builder()
            .uuid(UUID.randomUUID())
            .build();

    private static final String INITIAL_CAPTION = "initial caption";
    private static final String PATCHED_CAPTION = "patched caption";

    @Mock
    private EntityManager em;

    @BeforeEach
    void initMocks() {
        lenient().when(em.getReference(eq(HsHostingAssetRbacEntity.class), any())).thenAnswer(invocation ->
                HsHostingAssetRbacEntity.builder().uuid(invocation.getArgument(1)).build());
        lenient().when(em.getReference(eq(HsOfficeContactRealEntity.class), any())).thenAnswer(invocation ->
                HsOfficeContactRealEntity.builder().uuid(invocation.getArgument(1)).build());
    }

    @Override
    protected HsHostingAssetRbacEntity newInitialEntity() {
        final var entity = new HsHostingAssetRbacEntity();
        entity.setUuid(INITIAL_BOOKING_ITEM_UUID);
        entity.setBookingItem(CLOUD_SERVER_BOOKING_ITEM_REAL_ENTITY);
        entity.getConfig().putAll(KeyValueMap.from(INITIAL_CONFIG));
        entity.setCaption(INITIAL_CAPTION);
        entity.setAlarmContact(givenInitialContact);
        return entity;
    }

    @Override
    protected HsHostingAssetPatchResource newPatchResource() {
        return new HsHostingAssetPatchResource();
    }

    @Override
    protected HsHostingAssetEntityPatcher createPatcher(final HsHostingAssetRbacEntity server) {
        return new HsHostingAssetEntityPatcher(em, server);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new JsonNullableProperty<>(
                        "caption",
                        HsHostingAssetPatchResource::setCaption,
                        PATCHED_CAPTION,
                        HsHostingAssetRbacEntity::setCaption),
                new SimpleProperty<>(
                        "config",
                        HsHostingAssetPatchResource::setConfig,
                        PATCH_CONFIG,
                        HsHostingAssetRbacEntity::putConfig,
                        PATCHED_CONFIG)
                        .notNullable(),
                new JsonNullableProperty<>(
                        "alarmContact",
                        HsHostingAssetPatchResource::setAlarmContactUuid,
                        PATCHED_CONTACT_UUID,
                        HsHostingAssetRbacEntity::setAlarmContact,
                        newContact(PATCHED_CONTACT_UUID))
        );
    }

    static HsOfficeContactRealEntity newContact(final UUID uuid) {
        return HsOfficeContactRealEntity.builder().uuid(uuid).build();
    }
}
