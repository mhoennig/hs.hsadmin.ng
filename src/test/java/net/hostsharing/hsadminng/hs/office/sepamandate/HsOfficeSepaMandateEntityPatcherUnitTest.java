package net.hostsharing.hsadminng.hs.office.sepamandate;

import com.vladmihalcea.hibernate.type.range.Range;
import net.hostsharing.hsadminng.hs.office.debitor.HsOfficeDebitorEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeSepaMandatePatchResource;
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

import static net.hostsharing.hsadminng.hs.office.bankaccount.TestHsOfficeBankAccount.TEST_BANK_ACCOUNT;
import static net.hostsharing.hsadminng.hs.office.debitor.TestHsOfficeDebitor.TEST_DEBITOR;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@TestInstance(PER_CLASS)
@ExtendWith(MockitoExtension.class)
class HsOfficeSepaMandateEntityPatcherUnitTest extends PatchUnitTestBase<
        HsOfficeSepaMandatePatchResource,
        HsOfficeSepaMandateEntity
        > {

    private static final UUID INITIAL_SepaMandate_UUID = UUID.randomUUID();
    private static final LocalDate GIVEN_VALID_FROM = LocalDate.parse("2020-04-15");
    private static final LocalDate PATCHED_VALID_FROM = LocalDate.parse("2022-10-30");
    private static final LocalDate PATCHED_VALID_TO = LocalDate.parse("2022-12-31");
    private static final LocalDate PATCHED_AGREEMENT = LocalDate.parse("2022-11-01");
    private static final String PATCHED_REFERENCE = "ref sepamandate-patched";

    @Mock
    private EntityManager em;

    @BeforeEach
    void initMocks() {
        lenient().when(em.getReference(eq(HsOfficeDebitorEntity.class), any())).thenAnswer(invocation ->
                HsOfficeDebitorEntity.builder().uuid(invocation.getArgument(1)).build());
        lenient().when(em.getReference(eq(HsOfficeSepaMandateEntity.class), any())).thenAnswer(invocation ->
                HsOfficeSepaMandateEntity.builder().uuid(invocation.getArgument(1)).build());
    }

    @Override
    protected HsOfficeSepaMandateEntity newInitialEntity() {
        final var entity = new HsOfficeSepaMandateEntity();
        entity.setUuid(INITIAL_SepaMandate_UUID);
        entity.setDebitor(TEST_DEBITOR);
        entity.setBankAccount(TEST_BANK_ACCOUNT);
        entity.setReference("ref sepamandate");
        entity.setAgreement(LocalDate.parse("2022-10-28"));
        entity.setValidity(Range.closedInfinite(GIVEN_VALID_FROM));
        return entity;
    }

    @Override
    protected HsOfficeSepaMandatePatchResource newPatchResource() {
        return new HsOfficeSepaMandatePatchResource();
    }

    @Override
    protected HsOfficeSepaMandateEntityPatcher createPatcher(final HsOfficeSepaMandateEntity sepaMandate) {
        return new HsOfficeSepaMandateEntityPatcher(sepaMandate);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new JsonNullableProperty<>(
                        "reference",
                        HsOfficeSepaMandatePatchResource::setReference,
                        PATCHED_REFERENCE,
                        HsOfficeSepaMandateEntity::setReference),

                new JsonNullableProperty<>(
                        "agreement",
                        HsOfficeSepaMandatePatchResource::setAgreement,
                        PATCHED_AGREEMENT,
                        HsOfficeSepaMandateEntity::setAgreement),
                new JsonNullableProperty<>(
                        "validfrom",
                        HsOfficeSepaMandatePatchResource::setValidFrom,
                        PATCHED_VALID_FROM,
                        HsOfficeSepaMandateEntity::setValidFrom),
                new JsonNullableProperty<>(
                        "validto",
                        HsOfficeSepaMandatePatchResource::setValidTo,
                        PATCHED_VALID_TO,
                        HsOfficeSepaMandateEntity::setValidTo)
        );
    }
}
