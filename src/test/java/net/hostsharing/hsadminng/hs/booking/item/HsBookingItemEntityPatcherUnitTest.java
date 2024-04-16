package net.hostsharing.hsadminng.hs.booking.item;

import io.hypersistence.utils.hibernate.type.range.Range;
import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingItemPatchResource;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.mapper.KeyValueMap;
import net.hostsharing.hsadminng.rbac.test.PatchUnitTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static net.hostsharing.hsadminng.hs.office.debitor.TestHsOfficeDebitor.TEST_DEBITOR;
import static net.hostsharing.hsadminng.mapper.PatchMap.entry;
import static net.hostsharing.hsadminng.mapper.PatchMap.patchMap;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@TestInstance(PER_CLASS)
@ExtendWith(MockitoExtension.class)
class HsBookingItemEntityPatcherUnitTest extends PatchUnitTestBase<
        HsBookingItemPatchResource,
        HsBookingItemEntity
        > {

    private static final UUID INITIAL_BOOKING_ITEM_UUID = UUID.randomUUID();
    private static final LocalDate GIVEN_VALID_FROM = LocalDate.parse("2020-04-15");
    private static final LocalDate PATCHED_VALID_TO = LocalDate.parse("2022-12-31");

    private static final Map<String, Object> INITIAL_RESOURCES = patchMap(
            entry("CPU", 1),
            entry("HDD", 1024),
            entry("MEM", 64)
    );
    private static final  Map<String, Object> PATCH_RESOURCES = patchMap(
            entry("CPU", 2),
            entry("HDD", null),
            entry("SDD", 256)
    );
    private static final  Map<String, Object> PATCHED_RESOURCES = patchMap(
            entry("CPU", 2),
            entry("SDD", 256),
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
        lenient().when(em.getReference(eq(HsBookingItemEntity.class), any())).thenAnswer(invocation ->
                HsBookingItemEntity.builder().uuid(invocation.getArgument(1)).build());
    }

    @Override
    protected HsBookingItemEntity newInitialEntity() {
        final var entity = new HsBookingItemEntity();
        entity.setUuid(INITIAL_BOOKING_ITEM_UUID);
        entity.setDebitor(TEST_DEBITOR);
        entity.getResources().putAll(KeyValueMap.from(INITIAL_RESOURCES));
        entity.setCaption(INITIAL_CAPTION);
        entity.setValidity(Range.closedInfinite(GIVEN_VALID_FROM));
        return entity;
    }

    @Override
    protected HsBookingItemPatchResource newPatchResource() {
        return new HsBookingItemPatchResource();
    }

    @Override
    protected HsBookingItemEntityPatcher createPatcher(final HsBookingItemEntity bookingItem) {
        return new HsBookingItemEntityPatcher(bookingItem);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new JsonNullableProperty<>(
                        "caption",
                        HsBookingItemPatchResource::setCaption,
                        PATCHED_CAPTION,
                        HsBookingItemEntity::setCaption),
                new SimpleProperty<>(
                        "resources",
                        HsBookingItemPatchResource::setResources,
                        PATCH_RESOURCES,
                        HsBookingItemEntity::putResources,
                        PATCHED_RESOURCES)
                        .notNullable(),
                new JsonNullableProperty<>(
                        "validto",
                        HsBookingItemPatchResource::setValidTo,
                        PATCHED_VALID_TO,
                        HsBookingItemEntity::setValidTo)
        );
    }
}
