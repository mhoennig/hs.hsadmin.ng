package net.hostsharing.hsadminng.hs.office.debitor;

import net.hostsharing.hsadminng.hs.office.bankaccount.HsOfficeBankAccountEntity;
import net.hostsharing.hsadminng.hs.office.contact.HsOfficeContactEntity;
import net.hostsharing.hsadminng.hs.office.generated.api.v1.model.HsOfficeDebitorPatchResource;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.test.PatchUnitTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
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

    private static final String PATCHED_DEFAULT_PREFIX = "xyz";
    private static final String PATCHED_VAT_COUNTRY_CODE = "ZZ";

    private static final boolean PATCHED_VAT_BUSINESS = false;

    private static final boolean INITIAL_BILLABLE = false;
    private static final boolean PATCHED_BILLABLE = true;

    private static final boolean INITIAL_VAT_REVERSE_CHARGE = true;
    private static final boolean PATCHED_VAT_REVERSE_CHARGE = false;

    private static final UUID INITIAL_REFUND_BANK_ACCOUNT_UUID = UUID.randomUUID();
    private static final UUID PATCHED_REFUND_BANK_ACCOUNT_UUID = UUID.randomUUID();

    private final HsOfficePartnerEntity givenInitialPartner = HsOfficePartnerEntity.builder()
            .uuid(INITIAL_PARTNER_UUID)
            .build();

    private final HsOfficeContactEntity givenInitialContact = HsOfficeContactEntity.builder()
            .uuid(INITIAL_CONTACT_UUID)
            .build();

    private final HsOfficeBankAccountEntity givenInitialBankAccount = HsOfficeBankAccountEntity.builder()
            .uuid(INITIAL_REFUND_BANK_ACCOUNT_UUID)
            .build();
    @Mock
    private EntityManager em;

    @BeforeEach
    void initMocks() {
        lenient().when(em.getReference(eq(HsOfficeContactEntity.class), any())).thenAnswer(invocation ->
                HsOfficeContactEntity.builder().uuid(invocation.getArgument(1)).build());
        lenient().when(em.getReference(eq(HsOfficeBankAccountEntity.class), any())).thenAnswer(invocation ->
                HsOfficeBankAccountEntity.builder().uuid(invocation.getArgument(1)).build());
    }

    @Override
    protected HsOfficeDebitorEntity newInitialEntity() {
        final var entity = new HsOfficeDebitorEntity();
        entity.setUuid(INITIAL_DEBITOR_UUID);
        entity.setPartner(givenInitialPartner);
        entity.setBillingContact(givenInitialContact);
        entity.setBillable(INITIAL_BILLABLE);
        entity.setVatId("initial VAT-ID");
        entity.setVatCountryCode("AA");
        entity.setVatBusiness(true);
        entity.setVatReverseCharge(INITIAL_VAT_REVERSE_CHARGE);
        entity.setDefaultPrefix("abc");
        entity.setRefundBankAccount(givenInitialBankAccount);
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
                new SimpleProperty<>(
                        "billable",
                        HsOfficeDebitorPatchResource::setBillable,
                        PATCHED_BILLABLE,
                        HsOfficeDebitorEntity::setBillable)
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
                new SimpleProperty<>(
                        "vatBusiness",
                        HsOfficeDebitorPatchResource::setVatBusiness,
                        PATCHED_VAT_BUSINESS,
                        HsOfficeDebitorEntity::setVatBusiness)
                        .notNullable(),
                new SimpleProperty<>(
                        "vatReverseCharge",
                        HsOfficeDebitorPatchResource::setVatReverseCharge,
                        PATCHED_BILLABLE,
                        HsOfficeDebitorEntity::setVatReverseCharge)
                        .notNullable(),
                new JsonNullableProperty<>(
                        "defaultPrefix",
                        HsOfficeDebitorPatchResource::setDefaultPrefix,
                        PATCHED_DEFAULT_PREFIX,
                        HsOfficeDebitorEntity::setDefaultPrefix)
                        .notNullable(),
                new JsonNullableProperty<>(
                        "refundBankAccount",
                        HsOfficeDebitorPatchResource::setRefundBankAccountUuid,
                        PATCHED_REFUND_BANK_ACCOUNT_UUID,
                        HsOfficeDebitorEntity::setRefundBankAccount,
                        newBankAccount(PATCHED_REFUND_BANK_ACCOUNT_UUID))
                        .notNullable()
        );
    }

    private HsOfficeContactEntity newBillingContact(final UUID uuid) {
        final var newContact = new HsOfficeContactEntity();
        newContact.setUuid(uuid);
        return newContact;
    }

    private HsOfficeBankAccountEntity newBankAccount(final UUID uuid) {
        final var newBankAccount = new HsOfficeBankAccountEntity();
        newBankAccount.setUuid(uuid);
        return newBankAccount;
    }
}
