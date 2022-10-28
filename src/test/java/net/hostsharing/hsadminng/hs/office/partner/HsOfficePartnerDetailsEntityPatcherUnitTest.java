package net.hostsharing.hsadminng.hs.office.partner;

import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficePartnerDetailsPatchResource;
import net.hostsharing.hsadminng.hs.office.person.HsOfficePersonEntity;
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

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@TestInstance(PER_CLASS)
@ExtendWith(MockitoExtension.class)
class HsOfficePartnerDetailsEntityPatcherUnitTest extends PatchUnitTestBase<
        HsOfficePartnerDetailsPatchResource,
        HsOfficePartnerDetailsEntity
        > {

    private static final UUID INITIAL_PARTNER_UUID = UUID.randomUUID();
    private static final UUID INITIAL_CONTACT_UUID = UUID.randomUUID();
    private static final UUID INITIAL_PERSON_UUID = UUID.randomUUID();

    private static final LocalDate INITIAL_BIRTHDAY = LocalDate.parse("1900-01-01");
    private static final LocalDate PATCHED_BIRTHDAY = LocalDate.parse("1990-12-31");

    private static final LocalDate INITIAL_DAY_OF_DEATH = LocalDate.parse("2000-01-01");
    private static final LocalDate PATCHED_DATE_OF_DEATH = LocalDate.parse("2022-08-31");

    @Mock
    private EntityManager em;

    @BeforeEach
    void initMocks() {
        lenient().when(em.getReference(eq(HsOfficeContactEntity.class), any())).thenAnswer(invocation ->
                HsOfficeContactEntity.builder().uuid(invocation.getArgument(1)).build());
        lenient().when(em.getReference(eq(HsOfficePersonEntity.class), any())).thenAnswer(invocation ->
                HsOfficePersonEntity.builder().uuid(invocation.getArgument(1)).build());
    }

    @Override
    protected HsOfficePartnerDetailsEntity newInitialEntity() {
        final var entity = new HsOfficePartnerDetailsEntity();
        entity.setUuid(INITIAL_PARTNER_UUID);
        entity.setRegistrationOffice("initial Reg-Office");
        entity.setRegistrationNumber("initial Reg-Number");
        entity.setBirthday(INITIAL_BIRTHDAY);
        entity.setBirthName("initial birth name");
        entity.setDateOfDeath(INITIAL_DAY_OF_DEATH);
        return entity;
    }

    @Override
    protected HsOfficePartnerDetailsPatchResource newPatchResource() {
        return new HsOfficePartnerDetailsPatchResource();
    }

    @Override
    protected HsOfficePartnerDetailsEntityPatcher createPatcher(final HsOfficePartnerDetailsEntity details) {
        return new HsOfficePartnerDetailsEntityPatcher(em, details);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new JsonNullableProperty<>(
                        "registrationOffice",
                        HsOfficePartnerDetailsPatchResource::setRegistrationOffice,
                        "patched Reg-Office",
                        HsOfficePartnerDetailsEntity::setRegistrationOffice),
                new JsonNullableProperty<>(
                        "birthday",
                        HsOfficePartnerDetailsPatchResource::setBirthday,
                        PATCHED_BIRTHDAY,
                        HsOfficePartnerDetailsEntity::setBirthday),
                new JsonNullableProperty<>(
                        "dayOfDeath",
                        HsOfficePartnerDetailsPatchResource::setDateOfDeath,
                        PATCHED_DATE_OF_DEATH,
                        HsOfficePartnerDetailsEntity::setDateOfDeath)
        );
    }
}
