package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.hs.hosting.generated.api.v1.model.HsHostingAssetPatchResource;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
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

import static net.hostsharing.hsadminng.hs.booking.item.TestHsBookingItem.TEST_BOOKING_ITEM;
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
        HsHostingAssetEntity
        > {

    private static final UUID INITIAL_BOOKING_ITEM_UUID = UUID.randomUUID();

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

    private static final String INITIAL_CAPTION = "initial caption";
    private static final String PATCHED_CAPTION = "patched caption";

    @Mock
    private EntityManager em;

    @BeforeEach
    void initMocks() {
        lenient().when(em.getReference(eq(HsOfficeDebitorEntity.class), any())).thenAnswer(invocation ->
                HsOfficeDebitorEntity.builder().uuid(invocation.getArgument(1)).build());
        lenient().when(em.getReference(eq(HsHostingAssetEntity.class), any())).thenAnswer(invocation ->
                HsHostingAssetEntity.builder().uuid(invocation.getArgument(1)).build());
    }

    @Override
    protected HsHostingAssetEntity newInitialEntity() {
        final var entity = new HsHostingAssetEntity();
        entity.setUuid(INITIAL_BOOKING_ITEM_UUID);
        entity.setBookingItem(TEST_BOOKING_ITEM);
        entity.getConfig().putAll(KeyValueMap.from(INITIAL_CONFIG));
        entity.setCaption(INITIAL_CAPTION);
        return entity;
    }

    @Override
    protected HsHostingAssetPatchResource newPatchResource() {
        return new HsHostingAssetPatchResource();
    }

    @Override
    protected HsHostingAssetEntityPatcher createPatcher(final HsHostingAssetEntity server) {
        return new HsHostingAssetEntityPatcher(server);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new JsonNullableProperty<>(
                        "caption",
                        HsHostingAssetPatchResource::setCaption,
                        PATCHED_CAPTION,
                        HsHostingAssetEntity::setCaption),
                new SimpleProperty<>(
                        "config",
                        HsHostingAssetPatchResource::setConfig,
                        PATCH_CONFIG,
                        HsHostingAssetEntity::putConfig,
                        PATCHED_CONFIG)
                        .notNullable()
        );
    }
}
