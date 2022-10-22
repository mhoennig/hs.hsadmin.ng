package net.hostsharing.hsadminng.hs.office.membership;

import com.vladmihalcea.hibernate.type.range.Range;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeMembershipPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeReasonForTerminationResource;
import net.hostsharing.test.PatchUnitTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import static net.hostsharing.hsadminng.hs.office.debitor.TestHsOfficeDebitor.TEST_DEBITOR;
import static net.hostsharing.hsadminng.hs.office.partner.TestHsOfficePartner.TEST_PARTNER;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@TestInstance(PER_CLASS)
@ExtendWith(MockitoExtension.class)
class HsOfficeMembershipEntityPatcherUnitTest extends PatchUnitTestBase<
        HsOfficeMembershipPatchResource,
        HsOfficeMembershipEntity
        > {

    private static final UUID INITIAL_MEMBERSHIP_UUID = UUID.randomUUID();
    private static final UUID INITIAL_MAIN_DEBITOR_UUID = UUID.randomUUID();
    private static final UUID PATCHED_MAIN_DEBITOR_UUID = UUID.randomUUID();
    private static final LocalDate GIVEN_VALID_FROM = LocalDate.parse("2020-04-15");
    private static final LocalDate PATCHED_VALID_TO = LocalDate.parse("2022-12-31");

    @Mock
    private EntityManager em;

    @BeforeEach
    void initMocks() {
        lenient().when(em.getReference(eq(HsOfficeDebitorEntity.class), any())).thenAnswer(invocation ->
                HsOfficeDebitorEntity.builder().uuid(invocation.getArgument(1)).build());
        lenient().when(em.getReference(eq(HsOfficeMembershipEntity.class), any())).thenAnswer(invocation ->
                HsOfficeMembershipEntity.builder().uuid(invocation.getArgument(1)).build());
    }

    @Override
    protected HsOfficeMembershipEntity newInitialEntity() {
        final var entity = new HsOfficeMembershipEntity();
        entity.setUuid(INITIAL_MEMBERSHIP_UUID);
        entity.setMainDebitor(TEST_DEBITOR);
        entity.setPartner(TEST_PARTNER);
        entity.setValidity(Range.closedInfinite(GIVEN_VALID_FROM));
        return entity;
    }

    @Override
    protected HsOfficeMembershipPatchResource newPatchResource() {
        return new HsOfficeMembershipPatchResource();
    }

    @Override
    protected HsOfficeMembershipEntityPatcher createPatcher(final HsOfficeMembershipEntity Membership) {
        return new HsOfficeMembershipEntityPatcher(em, Membership);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new JsonNullableProperty<>(
                        "debitor",
                        HsOfficeMembershipPatchResource::setMainDebitorUuid,
                        PATCHED_MAIN_DEBITOR_UUID,
                        HsOfficeMembershipEntity::setMainDebitor,
                        newDebitor(PATCHED_MAIN_DEBITOR_UUID))
                        .notNullable(),
                new JsonNullableProperty<>(
                        "valid",
                        HsOfficeMembershipPatchResource::setValidTo,
                        PATCHED_VALID_TO,
                        HsOfficeMembershipEntity::setValidTo),
                new SimpleProperty<>(
                        "reasonForTermination",
                        HsOfficeMembershipPatchResource::setReasonForTermination,
                        HsOfficeReasonForTerminationResource.CANCELLATION,
                        HsOfficeMembershipEntity::setReasonForTermination,
                        HsOfficeReasonForTermination.CANCELLATION)
                        .notNullable()
        );
    }

    private static HsOfficeDebitorEntity newDebitor(final UUID uuid) {
        final var newDebitor = new HsOfficeDebitorEntity();
        newDebitor.setUuid(uuid);
        return newDebitor;
    }

    private HsOfficeMembershipEntity newMembership(final UUID uuid) {
        final var newMembership = new HsOfficeMembershipEntity();
        newMembership.setUuid(uuid);
        return newMembership;
    }
}
