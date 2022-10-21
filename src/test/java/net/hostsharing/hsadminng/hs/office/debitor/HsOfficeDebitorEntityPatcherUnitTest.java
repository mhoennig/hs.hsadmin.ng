package net.hostsharing.hsadminng.hs.office.debitor;

import net.hostsharing.test.PatchUnitTestBase;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeDebitorPatchResource;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@TestInstance(PER_CLASS)
@ExtendWith(MockitoExtension.class)
class HsOfficeDebitorEntityPatcherUnitTest extends PatchUnitTestBase<
        HsOfficeDebitorPatchResource,
        HsOfficeDebitorEntity
        > {

    private static final UUID INITIAL_DEBITOR_UUID = UUID.randomUUID();
    private static final UUID INITIAL_PARTNER_UUID = UUID.randomUUID();
    private static final UUID INITIAL_CONTACT_UUID = UUID.randomUUID();
    private static final UUID PATCHED_CONTACT_UUID = UUID.randomUUID();

    private static final String PATCHED_VAT_COUNTRY_CODE = "ZZ";

    private static final boolean PATCHED_VAT_BUSINESS = false;

    private final HsOfficePartnerEntity givenInitialPartner = HsOfficePartnerEntity.builder()
            .uuid(INITIAL_PARTNER_UUID)
            .build();

    private final HsOfficeContactEntity givenInitialContact = HsOfficeContactEntity.builder()
            .uuid(INITIAL_CONTACT_UUID)
            .build();
    @Mock
    private EntityManager em;

    @BeforeEach
    void initMocks() {
        lenient().when(em.getReference(eq(HsOfficeContactEntity.class), any())).thenAnswer(invocation ->
                HsOfficeContactEntity.builder().uuid(invocation.getArgument(1)).build());
        lenient().when(em.getReference(eq(HsOfficeContactEntity.class), any())).thenAnswer(invocation ->
                HsOfficeContactEntity.builder().uuid(invocation.getArgument(1)).build());
    }

    @Override
    protected HsOfficeDebitorEntity newInitialEntity() {
        final var entity = new HsOfficeDebitorEntity();
        entity.setUuid(INITIAL_DEBITOR_UUID);
        entity.setPartner(givenInitialPartner);
        entity.setBillingContact(givenInitialContact);
        entity.setVatId("initial VAT-ID");
        entity.setVatCountryCode("AA");
        entity.setVatBusiness(true);
        return entity;
    }

    @Override
    protected HsOfficeDebitorPatchResource newPatchResource() {
        return new HsOfficeDebitorPatchResource();
    }

    @Override
    protected HsOfficeDebitorEntityPatcher createPatcher(final HsOfficeDebitorEntity debitor) {
        return new HsOfficeDebitorEntityPatcher(em, debitor);
    }

    @Override
    protected Stream<Property> propertyTestDescriptors() {
        return Stream.of(
                new JsonNullableProperty<>(
                        "billingContact",
                        HsOfficeDebitorPatchResource::setBillingContactUuid,
                        PATCHED_CONTACT_UUID,
                        HsOfficeDebitorEntity::setBillingContact,
                        newBillingContact(PATCHED_CONTACT_UUID))
                        .notNullable(),
                new JsonNullableProperty<>(
                        "vatId",
                        HsOfficeDebitorPatchResource::setVatId,
                        "patched VAT-ID",
                        HsOfficeDebitorEntity::setVatId),
                new JsonNullableProperty<>(
                        "vatCountryCode",
                        HsOfficeDebitorPatchResource::setVatCountryCode,
                        PATCHED_VAT_COUNTRY_CODE,
                        HsOfficeDebitorEntity::setVatCountryCode),
                new JsonNullableProperty<>(
                        "vatBusiness",
                        HsOfficeDebitorPatchResource::setVatBusiness,
                        PATCHED_VAT_BUSINESS,
                        HsOfficeDebitorEntity::setVatBusiness)
                        .notNullable()
        );
    }

    private HsOfficeContactEntity newBillingContact(final UUID uuid) {
        final var newContact = new HsOfficeContactEntity();
        newContact.setUuid(uuid);
        return newContact;
    }
}
