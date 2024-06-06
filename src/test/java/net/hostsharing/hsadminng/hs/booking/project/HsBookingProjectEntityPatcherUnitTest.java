package net.hostsharing.hsadminng.hs.booking.project;

import net.hostsharing.hsadminng.hs.booking.generated.api.v1.model.HsBookingProjectPatchResource;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.rbac.test.PatchUnitTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import java.util.stream.Stream;

import static net.hostsharing.hsadminng.hs.booking.debitor.TestHsBookingDebitor.TEST_BOOKING_DEBITOR;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@TestInstance(PER_CLASS)
@ExtendWith(MockitoExtension.class)
class HsBookingProjectEntityPatcherUnitTest extends PatchUnitTestBase<
        HsBookingProjectPatchResource,
        HsBookingProjectEntity
        > {

    private static final UUID INITIAL_BOOKING_PROJECT_UUID = UUID.randomUUID();

    private static final String INITIAL_CAPTION = "initial caption";
    private static final String PATCHED_CAPTION = "patched caption";

    @Mock
    private EntityManager em;

    @BeforeEach
    void initMocks() {
        lenient().when(em.getReference(eq(HsOfficeDebitorEntity.class), any())).thenAnswer(invocation ->
                HsOfficeDebitorEntity.builder().uuid(invocation.getArgument(1)).build());
        lenient().when(em.getReference(eq(HsBookingProjectEntity.class), any())).thenAnswer(invocation ->
                HsBookingProjectEntity.builder().uuid(invocation.getArgument(1)).build());
    }

    @Override
    protected HsBookingProjectEntity newInitialEntity() {
        final var entity = new HsBookingProjectEntity();
        entity.setUuid(INITIAL_BOOKING_PROJECT_UUID);
        entity.setDebitor(TEST_BOOKING_DEBITOR);
        entity.setCaption(INITIAL_CAPTION);
        return entity;
    }

    @Override
    protected HsBookingProjectPatchResource newPatchResource() {
        return new HsBookingProjectPatchResource();
    }

    @Override
    protected HsBookingProjectEntityPatcher createPatcher(final HsBookingProjectEntity bookingProject) {
        return new HsBookingProjectEntityPatcher(bookingProject);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new JsonNullableProperty<>(
                        "caption",
                        HsBookingProjectPatchResource::setCaption,
                        PATCHED_CAPTION,
                        HsBookingProjectEntity::setCaption)
        );
    }
}
