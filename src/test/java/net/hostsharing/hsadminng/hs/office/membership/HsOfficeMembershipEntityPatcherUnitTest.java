package net.hostsharing.hsadminng.hs.office.membership;

import com.vladmihalcea.hibernate.type.range.Range;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeMembershipPatchResource;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeReasonForTerminationResource;
import net.hostsharing.hsadminng.mapper.Mapper;
import net.hostsharing.test.PatchUnitTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

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
    private static final LocalDate GIVEN_VALID_FROM = LocalDate.parse("2020-04-15");
    private static final LocalDate PATCHED_VALID_TO = LocalDate.parse("2022-12-31");

    private static final Boolean GIVEN_MEMBERSHIP_FEE_BILLABLE = true;
    private static final Boolean PATCHED_MEMBERSHIP_FEE_BILLABLE = false;

    @Mock
    private EntityManager em;

    private Mapper mapper = new Mapper();

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
        entity.setPartner(TEST_PARTNER);
        entity.setValidity(Range.closedInfinite(GIVEN_VALID_FROM));
        entity.setMembershipFeeBillable(GIVEN_MEMBERSHIP_FEE_BILLABLE);
        return entity;
    }

    @Override
    protected HsOfficeMembershipPatchResource newPatchResource() {
        return new HsOfficeMembershipPatchResource();
    }

    @Override
    protected HsOfficeMembershipEntityPatcher createPatcher(final HsOfficeMembershipEntity membership) {
        return new HsOfficeMembershipEntityPatcher(mapper, membership);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
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
                        .notNullable(),
                new JsonNullableProperty<>(
                        "membershipFeeBillable",
                        HsOfficeMembershipPatchResource::setMembershipFeeBillable,
                        PATCHED_MEMBERSHIP_FEE_BILLABLE,
                        HsOfficeMembershipEntity::setMembershipFeeBillable)
        );
    }
}
