package net.hostsharing.hsadminng.hs.office.coopassets;

import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipEntity;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipRepository;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerRealEntity;
import net.hostsharing.hsadminng.config.MessagesResourceConfig;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.test.JsonBuilder;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import net.hostsharing.hsadminng.test.TestUuidGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static net.hostsharing.hsadminng.hs.office.coopassets.HsOfficeCoopAssetsTransactionType.ADOPTION;
import static net.hostsharing.hsadminng.hs.office.coopassets.HsOfficeCoopAssetsTransactionType.DEPOSIT;
import static net.hostsharing.hsadminng.hs.office.coopassets.HsOfficeCoopAssetsTransactionType.DISBURSAL;
import static net.hostsharing.hsadminng.hs.office.coopassets.HsOfficeCoopAssetsTransactionType.REVERSAL;
import static net.hostsharing.hsadminng.hs.office.coopassets.HsOfficeCoopAssetsTransactionType.TRANSFER;
import static net.hostsharing.hsadminng.rbac.test.JsonBuilder.jsonObject;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsOfficeCoopAssetsTransactionController.class)
@Import({ StrictMapper.class,
          MessagesResourceConfig.class,
          MessageTranslator.class,
          JsonObjectMapperConfiguration.class,
          DisableSecurityConfig.class })
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
class HsOfficeCoopAssetsTransactionControllerRestTest {

    // If you need to run just a single test-case in this data-driven test-method, set SINGLE_TEST_CASE_EXECUTION to true!
    // There is a test which fails if single test-case execution active to avoid merging this to master.
    private static final boolean SINGLE_TEST_CASE_EXECUTION = false;

    private static final int DYNAMIC_UUID_START_INDEX = 13;

    private static final UUID UNAVAILABLE_UUID = TestUuidGenerator.use(0);
    private static final String UNAVAILABLE_MEMBER_NUMBER = "M-1234699";

    private static final UUID ORIGIN_MEMBERSHIP_UUID = TestUuidGenerator.use(1);
    private static final String ORIGIN_MEMBER_NUMBER = "M-1111100";
    public final HsOfficeMembershipEntity ORIGIN_TARGET_MEMBER_ENTITY = HsOfficeMembershipEntity.builder()
            .uuid(ORIGIN_MEMBERSHIP_UUID)
            .partner(HsOfficePartnerRealEntity.builder()
                    .partnerNumber(partnerNumberOf(ORIGIN_MEMBER_NUMBER))
                    .build())
            .memberNumberSuffix(suffixOf(ORIGIN_MEMBER_NUMBER))
            .build();

    private static final UUID AVAILABLE_TARGET_MEMBERSHIP_UUID = TestUuidGenerator.use(2);
    private static final String AVAILABLE_TARGET_MEMBER_NUMBER = "M-1234500";
    public final HsOfficeMembershipEntity AVAILABLE_MEMBER_ENTITY = HsOfficeMembershipEntity.builder()
            .uuid(AVAILABLE_TARGET_MEMBERSHIP_UUID)
            .partner(HsOfficePartnerRealEntity.builder()
                    .partnerNumber(partnerNumberOf(AVAILABLE_TARGET_MEMBER_NUMBER))
                    .build())
            .memberNumberSuffix(suffixOf(AVAILABLE_TARGET_MEMBER_NUMBER))
            .build();

    // The following refs depend on the implementation of the respective implementation and might change if it changes.
    // The same TestUuidGenerator.ref(#) does NOT mean the UUIDs refer to the same entity,
    // its rather coincidence because different test-cases have different execution paths in the production code.
    private static final UUID NEW_EXPLICITLY_CREATED_REVERSAL_ASSET_TX_UUID = TestUuidGenerator.ref(DYNAMIC_UUID_START_INDEX);
    private static final UUID NEW_EXPLICITLY_CREATED_TRANSFER_ASSET_TX_UUID = TestUuidGenerator.ref(DYNAMIC_UUID_START_INDEX);

    private static final UUID SOME_EXISTING_LOSS_ASSET_TX_UUID = TestUuidGenerator.use(3);
    public final HsOfficeCoopAssetsTransactionEntity SOME_EXISTING_LOSS_ASSET_TX_ENTITY = HsOfficeCoopAssetsTransactionEntity.builder()
            .uuid(SOME_EXISTING_LOSS_ASSET_TX_UUID)
            .membership(ORIGIN_TARGET_MEMBER_ENTITY)
            .transactionType(HsOfficeCoopAssetsTransactionType.LOSS)
            .assetValue(BigDecimal.valueOf(-64))
            .reference("some loss asset tx ref")
            .comment("some loss asset tx comment")
            .valueDate(LocalDate.parse("2024-10-15"))
            .build();

    private static final UUID SOME_EXISTING_TRANSFER_ASSET_TX_UUID = TestUuidGenerator.use(4);
    public final HsOfficeCoopAssetsTransactionEntity SOME_EXISTING_TRANSFER_ASSET_TX_ENTITY = HsOfficeCoopAssetsTransactionEntity.builder()
            .uuid(SOME_EXISTING_TRANSFER_ASSET_TX_UUID)
            .membership(ORIGIN_TARGET_MEMBER_ENTITY)
            .transactionType(HsOfficeCoopAssetsTransactionType.TRANSFER)
            .assetValue(BigDecimal.valueOf(-256))
            .reference("some transfer asset tx ref")
            .comment("some transfer asset tx comment")
            .valueDate(LocalDate.parse("2024-10-15"))
            .build();

    private static final UUID SOME_EXISTING_ADOPTION_ASSET_TX_UUID = TestUuidGenerator.use(5);
    public final HsOfficeCoopAssetsTransactionEntity SOME_EXISTING_ADOPTION_ASSET_TX_ENTITY = HsOfficeCoopAssetsTransactionEntity.builder()
            .uuid(SOME_EXISTING_ADOPTION_ASSET_TX_UUID)
            .membership(ORIGIN_TARGET_MEMBER_ENTITY)
            .transactionType(HsOfficeCoopAssetsTransactionType.TRANSFER)
            .assetValue(SOME_EXISTING_TRANSFER_ASSET_TX_ENTITY.getAssetValue().negate())
            .reference("some adoption asset tx ref")
            .comment("some adoption asset tx comment")
            .valueDate(LocalDate.parse("2024-10-15"))
            .transferAssetTx(SOME_EXISTING_TRANSFER_ASSET_TX_ENTITY)
            .build();
    {
        SOME_EXISTING_TRANSFER_ASSET_TX_ENTITY.setAdoptionAssetTx(SOME_EXISTING_ADOPTION_ASSET_TX_ENTITY);
    }

    private final static UUID SOME_REVERTED_DISBURSAL_ASSET_TX_UUID = TestUuidGenerator.use(7);
    private final static UUID SOME_DISBURSAL_REVERSAL_ASSET_TX_UUID = TestUuidGenerator.use(8);
    private final HsOfficeCoopAssetsTransactionEntity SOME_REVERTED_DISBURSAL_ASSET_TX_ENTITY = HsOfficeCoopAssetsTransactionEntity.builder()
            .uuid(SOME_REVERTED_DISBURSAL_ASSET_TX_UUID)
            .membership(ORIGIN_TARGET_MEMBER_ENTITY)
            .transactionType(DISBURSAL)
            .assetValue(BigDecimal.valueOf(-128.00))
            .valueDate(LocalDate.parse("2024-10-15"))
            .reference("some disbursal")
            .comment("some disbursal to get reverted")
            .reversalAssetTx(
                    HsOfficeCoopAssetsTransactionEntity.builder()
                            .uuid(SOME_DISBURSAL_REVERSAL_ASSET_TX_UUID)
                            .membership(ORIGIN_TARGET_MEMBER_ENTITY)
                            .transactionType(REVERSAL)
                            .assetValue(BigDecimal.valueOf(128.00))
                            .valueDate(LocalDate.parse("2024-10-20"))
                            .reference("some reversal")
                            .comment("some reversal of a disbursal asset tx")
                            .build()
            )
            .build();
    {
        SOME_REVERTED_DISBURSAL_ASSET_TX_ENTITY.getReversalAssetTx().setRevertedAssetTx(SOME_REVERTED_DISBURSAL_ASSET_TX_ENTITY);
    }

    private final static UUID SOME_REVERTED_TRANSFER_ASSET_TX_UUID = TestUuidGenerator.use(9);
    private final static UUID SOME_TRANSFER_REVERSAL_ASSET_TX_UUID = TestUuidGenerator.use(10);
    private final static UUID SOME_REVERTED_ADOPTION_ASSET_TX_UUID = TestUuidGenerator.use(11);
    private final static UUID SOME_ADOPTION_REVERSAL_ASSET_TX_UUID = TestUuidGenerator.use(12);
    final HsOfficeCoopAssetsTransactionEntity SOME_REVERTED_TRANSFER_ASSET_TX_ENTITY = HsOfficeCoopAssetsTransactionEntity.builder()
            .uuid(SOME_REVERTED_TRANSFER_ASSET_TX_UUID)
            .membership(ORIGIN_TARGET_MEMBER_ENTITY)
            .transactionType(TRANSFER)
            .assetValue(BigDecimal.valueOf(-1024))
            .valueDate(LocalDate.parse("2024-11-10"))
            .reference("some transfer")
            .comment("some transfer to get reverted")
            .adoptionAssetTx(
                    HsOfficeCoopAssetsTransactionEntity.builder()
                            .uuid(SOME_REVERTED_ADOPTION_ASSET_TX_UUID)
                            .membership(AVAILABLE_MEMBER_ENTITY)
                            .transactionType(ADOPTION)
                            .assetValue(BigDecimal.valueOf(1024))
                            .valueDate(LocalDate.parse("2024-11-10"))
                            .reference("related adoption")
                            .comment("some reversal of a transfer asset tx")
                            .reversalAssetTx(
                                    HsOfficeCoopAssetsTransactionEntity.builder()
                                            .uuid(SOME_ADOPTION_REVERSAL_ASSET_TX_UUID)
                                            .membership(AVAILABLE_MEMBER_ENTITY)
                                            .transactionType(REVERSAL)
                                            .assetValue(BigDecimal.valueOf(1024))
                                            .valueDate(LocalDate.parse("2024-11-11"))
                                            .reference("some reversal")
                                            .comment("some adoption asset tx reversal")
                                            .build()
                            )
                            .build()
            )
            .reversalAssetTx(
                    HsOfficeCoopAssetsTransactionEntity.builder()
                            .uuid(SOME_TRANSFER_REVERSAL_ASSET_TX_UUID)
                            .membership(ORIGIN_TARGET_MEMBER_ENTITY)
                            .transactionType(REVERSAL)
                            .assetValue(BigDecimal.valueOf(1024))
                            .valueDate(LocalDate.parse("2024-11-11"))
                            .reference("some transfer")
                            .comment("some transfer asset tx reversal")
                            .build()
            )
            .build();
    {
        SOME_REVERTED_TRANSFER_ASSET_TX_ENTITY.getAdoptionAssetTx()
                .setTransferAssetTx(SOME_REVERTED_DISBURSAL_ASSET_TX_ENTITY);
        SOME_REVERTED_TRANSFER_ASSET_TX_ENTITY.getReversalAssetTx()
                .setRevertedAssetTx(SOME_REVERTED_DISBURSAL_ASSET_TX_ENTITY);
        SOME_REVERTED_TRANSFER_ASSET_TX_ENTITY.getAdoptionAssetTx().getReversalAssetTx()
                .setRevertedAssetTx(SOME_REVERTED_TRANSFER_ASSET_TX_ENTITY.getAdoptionAssetTx());
    }

    private static final String EXPECTED_RESULT_FROM_GET_SINGLE = """
            {
                 "uuid": "99999999-9999-9999-9999-999999999999",
                 "membership.uuid": "11111111-1111-1111-1111-111111111111",
                 "membership.memberNumber": "M-1111100",
                 "transactionType": "TRANSFER",
                 "assetValue": -1024,
                 "valueDate": "2024-11-10",
                 "reference": "some transfer",
                 "comment": "some transfer to get reverted",
                 "adoptionAssetTx": {
                   "uuid": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                   "membership.uuid": "22222222-2222-2222-2222-222222222222",
                   "membership.memberNumber": "M-1234500",
                   "transactionType": "ADOPTION",
                   "assetValue": 1024,
                   "valueDate": "2024-11-10",
                   "reference": "related adoption",
                   "comment": "some reversal of a transfer asset tx",
                   "adoptionAssetTx.uuid": null,
                   "transferAssetTx.uuid": "99999999-9999-9999-9999-999999999999",
                   "revertedAssetTx.uuid": null,
                   "reversalAssetTx.uuid": "cccccccc-cccc-cccc-cccc-cccccccccccc"
                }
            }
            """;


    private static final String EXPECTED_RESULT_FROM_GET_LIST = """
            [
               {
                 "uuid": "33333333-3333-3333-3333-333333333333",
                 "membership.uuid": "11111111-1111-1111-1111-111111111111",
                 "membership.memberNumber": "M-1111100",
                 "transactionType": "LOSS",
                 "assetValue": -64,
                 "valueDate": "2024-10-15",
                 "reference": "some loss asset tx ref",
                 "comment": "some loss asset tx comment",
                 "adoptionAssetTx": null,
                 "transferAssetTx": null,
                 "revertedAssetTx": null,
                 "reversalAssetTx": null
               },
               {
                 "uuid": "44444444-4444-4444-4444-444444444444",
                 "membership.uuid": "11111111-1111-1111-1111-111111111111",
                 "membership.memberNumber": "M-1111100",
                 "transactionType": "TRANSFER",
                 "assetValue": -256,
                 "valueDate": "2024-10-15",
                 "reference": "some transfer asset tx ref",
                 "comment": "some transfer asset tx comment",
                 "adoptionAssetTx": {
                   "uuid": "55555555-5555-5555-5555-555555555555",
                   "membership.uuid": "11111111-1111-1111-1111-111111111111",
                   "membership.memberNumber": "M-1111100",
                   "transactionType": "TRANSFER",
                   "assetValue": 256,
                   "valueDate": "2024-10-15",
                   "reference": "some adoption asset tx ref",
                   "comment": "some adoption asset tx comment",
                   "adoptionAssetTx.uuid": null,
                   "transferAssetTx.uuid": "44444444-4444-4444-4444-444444444444",
                   "revertedAssetTx.uuid": null,
                   "reversalAssetTx.uuid": null
                 },
                 "transferAssetTx": null,
                 "revertedAssetTx": null,
                 "reversalAssetTx": null
               },
               {
                 "uuid": "55555555-5555-5555-5555-555555555555",
                 "membership.uuid": "11111111-1111-1111-1111-111111111111",
                 "membership.memberNumber": "M-1111100",
                 "transactionType": "TRANSFER",
                 "assetValue": 256,
                 "valueDate": "2024-10-15",
                 "reference": "some adoption asset tx ref",
                 "comment": "some adoption asset tx comment",
                 "adoptionAssetTx": null,
                 "transferAssetTx": {
                   "uuid": "44444444-4444-4444-4444-444444444444",
                   "membership.uuid": "11111111-1111-1111-1111-111111111111",
                   "membership.memberNumber": "M-1111100",
                   "transactionType": "TRANSFER",
                   "assetValue": -256,
                   "valueDate": "2024-10-15",
                   "reference": "some transfer asset tx ref",
                   "comment": "some transfer asset tx comment",
                   "adoptionAssetTx.uuid": "55555555-5555-5555-5555-555555555555",
                   "transferAssetTx.uuid": null,
                   "revertedAssetTx.uuid": null,
                   "reversalAssetTx.uuid": null
                 },
                 "revertedAssetTx": null,
                 "reversalAssetTx": null
               },
               {
                 "uuid": "77777777-7777-7777-7777-777777777777",
                 "membership.uuid": "11111111-1111-1111-1111-111111111111",
                 "membership.memberNumber": "M-1111100",
                 "transactionType": "DISBURSAL",
                 "assetValue": -128.0,
                 "valueDate": "2024-10-15",
                 "reference": "some disbursal",
                 "comment": "some disbursal to get reverted",
                 "adoptionAssetTx": null,
                 "transferAssetTx": null,
                 "revertedAssetTx": null,
                 "reversalAssetTx": {
                   "uuid": "88888888-8888-8888-8888-888888888888",
                   "membership.uuid": "11111111-1111-1111-1111-111111111111",
                   "membership.memberNumber": "M-1111100",
                   "transactionType": "REVERSAL",
                   "assetValue": 128.0,
                   "valueDate": "2024-10-20",
                   "reference": "some reversal",
                   "comment": "some reversal of a disbursal asset tx",
                   "adoptionAssetTx.uuid": null,
                   "transferAssetTx.uuid": null,
                   "revertedAssetTx.uuid": "77777777-7777-7777-7777-777777777777",
                   "reversalAssetTx.uuid": null
                 }
               },
               {
                 "uuid": "88888888-8888-8888-8888-888888888888",
                 "membership.uuid": "11111111-1111-1111-1111-111111111111",
                 "membership.memberNumber": "M-1111100",
                 "transactionType": "REVERSAL",
                 "assetValue": 128.0,
                 "valueDate": "2024-10-20",
                 "reference": "some reversal",
                 "comment": "some reversal of a disbursal asset tx",
                 "adoptionAssetTx": null,
                 "transferAssetTx": null,
                 "revertedAssetTx": {
                   "uuid": "77777777-7777-7777-7777-777777777777",
                   "membership.uuid": "11111111-1111-1111-1111-111111111111",
                   "membership.memberNumber": "M-1111100",
                   "transactionType": "DISBURSAL",
                   "assetValue": -128.0,
                   "valueDate": "2024-10-15",
                   "reference": "some disbursal",
                   "comment": "some disbursal to get reverted",
                   "adoptionAssetTx.uuid": null,
                   "transferAssetTx.uuid": null,
                   "revertedAssetTx.uuid": null,
                   "reversalAssetTx.uuid": "88888888-8888-8888-8888-888888888888"
                 },
                 "reversalAssetTx": null
               },
               {
                 "uuid": "99999999-9999-9999-9999-999999999999",
                 "membership.uuid": "11111111-1111-1111-1111-111111111111",
                 "membership.memberNumber": "M-1111100",
                 "transactionType": "TRANSFER",
                 "assetValue": -1024,
                 "valueDate": "2024-11-10",
                 "reference": "some transfer",
                 "comment": "some transfer to get reverted",
                 "adoptionAssetTx": {
                   "uuid": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                   "membership.uuid": "22222222-2222-2222-2222-222222222222",
                   "membership.memberNumber": "M-1234500",
                   "transactionType": "ADOPTION",
                   "assetValue": 1024,
                   "valueDate": "2024-11-10",
                   "reference": "related adoption",
                   "comment": "some reversal of a transfer asset tx",
                   "adoptionAssetTx.uuid": null,
                   "transferAssetTx.uuid": "99999999-9999-9999-9999-999999999999",
                   "revertedAssetTx.uuid": null,
                   "reversalAssetTx.uuid": "cccccccc-cccc-cccc-cccc-cccccccccccc"
                 },
                 "transferAssetTx": null,
                 "revertedAssetTx": null,
                 "reversalAssetTx": {
                   "uuid": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                   "membership.uuid": "11111111-1111-1111-1111-111111111111",
                   "membership.memberNumber": "M-1111100",
                   "transactionType": "REVERSAL",
                   "assetValue": 1024,
                   "valueDate": "2024-11-11",
                   "reference": "some transfer",
                   "comment": "some transfer asset tx reversal",
                   "adoptionAssetTx.uuid": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                   "transferAssetTx.uuid": null,
                   "revertedAssetTx.uuid": "99999999-9999-9999-9999-999999999999",
                   "reversalAssetTx.uuid": null
                 }
               },
               {
                 "uuid": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                 "membership.uuid": "22222222-2222-2222-2222-222222222222",
                 "membership.memberNumber": "M-1234500",
                 "transactionType": "ADOPTION",
                 "assetValue": 1024,
                 "valueDate": "2024-11-10",
                 "reference": "related adoption",
                 "comment": "some reversal of a transfer asset tx",
                 "adoptionAssetTx": null,
                 "transferAssetTx": {
                   "uuid": "77777777-7777-7777-7777-777777777777",
                   "membership.uuid": "11111111-1111-1111-1111-111111111111",
                   "membership.memberNumber": "M-1111100",
                   "transactionType": "DISBURSAL",
                   "assetValue": -128.0,
                   "valueDate": "2024-10-15",
                   "reference": "some disbursal",
                   "comment": "some disbursal to get reverted",
                   "adoptionAssetTx.uuid": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                   "transferAssetTx.uuid": null,
                   "revertedAssetTx.uuid": null,
                   "reversalAssetTx.uuid": "88888888-8888-8888-8888-888888888888"
                 },
                 "revertedAssetTx": null,
                 "reversalAssetTx": {
                   "uuid": "cccccccc-cccc-cccc-cccc-cccccccccccc",
                   "membership.uuid": "22222222-2222-2222-2222-222222222222",
                   "membership.memberNumber": "M-1234500",
                   "transactionType": "REVERSAL",
                   "assetValue": 1024,
                   "valueDate": "2024-11-11",
                   "reference": "some reversal",
                   "comment": "some adoption asset tx reversal",
                   "adoptionAssetTx.uuid": null,
                   "transferAssetTx.uuid": "77777777-7777-7777-7777-777777777777",
                   "revertedAssetTx.uuid": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                   "reversalAssetTx.uuid": null
                 }
               },
               {
                 "uuid": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                 "membership.uuid": "11111111-1111-1111-1111-111111111111",
                 "membership.memberNumber": "M-1111100",
                 "transactionType": "REVERSAL",
                 "assetValue": 1024,
                 "valueDate": "2024-11-11",
                 "reference": "some transfer",
                 "comment": "some transfer asset tx reversal",
                 "adoptionAssetTx": null,
                 "transferAssetTx": null,
                 "revertedAssetTx": {
                   "uuid": "77777777-7777-7777-7777-777777777777",
                   "membership.uuid": "11111111-1111-1111-1111-111111111111",
                   "membership.memberNumber": "M-1111100",
                   "transactionType": "DISBURSAL",
                   "assetValue": -128.0,
                   "valueDate": "2024-10-15",
                   "reference": "some disbursal",
                   "comment": "some disbursal to get reverted",
                   "adoptionAssetTx.uuid": null,
                   "transferAssetTx.uuid": null,
                   "revertedAssetTx.uuid": null,
                   "reversalAssetTx.uuid": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
                 },
                 "reversalAssetTx": null
               },
               {
                 "uuid": "cccccccc-cccc-cccc-cccc-cccccccccccc",
                 "membership.uuid": "22222222-2222-2222-2222-222222222222",
                 "membership.memberNumber": "M-1234500",
                 "transactionType": "REVERSAL",
                 "assetValue": 1024,
                 "valueDate": "2024-11-11",
                 "reference": "some reversal",
                 "comment": "some adoption asset tx reversal",
                 "adoptionAssetTx": null,
                 "transferAssetTx": null,
                 "revertedAssetTx": {
                   "uuid": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                   "membership.uuid": "22222222-2222-2222-2222-222222222222",
                   "membership.memberNumber": "M-1234500",
                   "transactionType": "ADOPTION",
                   "assetValue": 1024,
                   "valueDate": "2024-11-10",
                   "reference": "related adoption",
                   "comment": "some reversal of a transfer asset tx",
                   "adoptionAssetTx.uuid": null,
                   "transferAssetTx.uuid": "77777777-7777-7777-7777-777777777777",
                   "revertedAssetTx.uuid": null,
                   "reversalAssetTx.uuid": "cccccccc-cccc-cccc-cccc-cccccccccccc"
                 },
                 "reversalAssetTx": null
               }
            ]
            """;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    Context contextMock;

    @Autowired
    @SuppressWarnings("unused") // not used in test, but in controller class
    StrictMapper mapper;

    @MockitoBean
    EntityManagerWrapper emw; // even if not used in test anymore, it's needed by base-class of StrictMapper

    @MockitoBean
    HsOfficeCoopAssetsTransactionRepository coopAssetsTransactionRepo;

    @MockitoBean
    HsOfficeMembershipRepository membershipRepo;

    static final String INSERT_REQUEST_BODY_TEMPLATE = """
            {
               "membership.uuid": "%s",
               "transactionType": "DEPOSIT",
               "assetValue": 128.00,
               "valueDate": "2022-10-13",
               "reference": "valid reference",
               "comment": "valid comment",
               "adoptingMembership.uuid": null,
               "adoptingMembership.memberNumber": null
            }
            """.formatted(ORIGIN_MEMBERSHIP_UUID);

    enum BadRequestTestCases {
        MEMBERSHIP_UUID_MISSING(
                requestBody -> requestBody.without("membership.uuid"),
                // TODO.impl: should be membership.uuid, but the Hibernate validator does not use the name from @JsonProperty
                "[membershipUuid darf nicht null sein"), // bracket because it's from a list of violations

        MEMBERSHIP_UUID_NOT_FOUND_OR_NOT_ACCESSIBLE(
                requestBody -> requestBody.with("membership.uuid", UNAVAILABLE_UUID),
                "membership.uuid \"" + UNAVAILABLE_UUID + "\" nicht gefunden"),

        MEMBERSHIP_UUID_AND_MEMBER_NUMBER_MUST_NOT_BE_GIVEN_BOTH(
                requestBody -> requestBody
                        .with("transactionType", TRANSFER.name())
                        .with("assetValue", "-128.00")
                        .with("adoptingMembership.uuid", UNAVAILABLE_UUID)
                        .with("adoptingMembership.memberNumber", UNAVAILABLE_MEMBER_NUMBER),
                "entweder adoptingMembership.uuid oder adoptingMembership.memberNumber muss angegeben werden, nicht beide"),

        MEMBERSHIP_UUID_OR_MEMBER_NUMBER_MUST_BE_GIVEN(
                requestBody -> requestBody
                        .with("transactionType", TRANSFER)
                        .with("assetValue", "-128.00"),
                "für transactionType=TRANSFER muss entweder adoptingMembership.uuid oder adoptingMembership.memberNumber angegeben werden"),

        REVERSAL_ASSET_TRANSACTION_REQUIRES_REVERTED_ASSET_TX_UUID(
                requestBody -> requestBody
                            .with("transactionType", REVERSAL)
                            .with("assetValue", "-128.00"),
                "eine REVERSAL Geschäftsguthaben-Transaktion erfordert die Angabe einer revertedAssetTx.uuid"),

        REVERSAL_ASSET_TRANSACTION_REQUIRES_AVAILABLE_REVERTED_ASSET_TX_UUID(
                requestBody -> requestBody
                        .with("transactionType", REVERSAL)
                        .with("assetValue", "-128.00")
                        .with("revertedAssetTx.uuid", UNAVAILABLE_UUID),
                "revertedAssetTx.uuid \"" + UNAVAILABLE_UUID + "\" nicht gefunden"),

        REVERSAL_ASSET_TRANSACTION_MUST_NEGATE_VALUE_OF_REVERTED_ASSET_TX(
                requestBody -> requestBody
                        .with("transactionType", REVERSAL)
                        .with("assetValue", "128.00")
                        .with("revertedAssetTx.uuid", SOME_EXISTING_LOSS_ASSET_TX_UUID),
                "assetValue=128,00 muss dem negativen Wert des Wertes der stornierten Geschäftsguthaben-Transaktion entsprechen: -64,00"),

        TRANSACTION_TYPE_MISSING(
                requestBody -> requestBody.without("transactionType"),
                "[transactionType darf nicht null sein"),

        VALUE_DATE_MISSING(
                requestBody -> requestBody.without("valueDate"),
                "[valueDate darf nicht null sein"),

        ASSETS_VALUE_FOR_DEPOSIT_MUST_BE_POSITIVE(
                requestBody -> requestBody
                        .with("transactionType", DEPOSIT)
                        .with("assetValue", -64.00),
                "[für transactionType=DEPOSIT, muss assetValue positiv sein, ist aber -64,00]"),

        ASSETS_VALUE_FOR_DISBURSAL_MUST_BE_NEGATIVE(
                requestBody -> requestBody
                        .with("transactionType", DISBURSAL)
                        .with("assetValue", 64.00),
                "[für transactionType=DISBURSAL, muss assetValue negativ sein, ist aber 64,00]"),

        ADOPTING_MEMBERSHIP_MUST_NOT_BE_THE_SAME(
                requestBody -> requestBody
                        .with("transactionType", TRANSFER)
                        .with("assetValue", -64.00)
                        .with("adoptingMembership.uuid", ORIGIN_MEMBERSHIP_UUID),
                "übertragende und annehmende Mitgliedschaft müssen unterschiedlich sein, aber beide sind M-1111100"),

        ADOPTING_MEMBERSHIP_NUMBER_FOR_TRANSFER_MUST_BE_GIVEN_AND_AVAILABLE(
                requestBody -> requestBody
                        .with("transactionType", TRANSFER)
                        .with("assetValue", -64.00)
                        .with("adoptingMembership.memberNumber", UNAVAILABLE_MEMBER_NUMBER),
                "adoptingMembership.memberNumber \"M-1234699\" nicht gefunden oder nicht zugänglich"),

        ADOPTING_MEMBERSHIP_UUID_FOR_TRANSFER_MUST_BE_GIVEN_AND_AVAILABLE(
                requestBody -> requestBody
                        .with("transactionType", TRANSFER)
                        .with("assetValue", -64.00)
                        .with("adoptingMembership.uuid", UNAVAILABLE_UUID),
                "adoptingMembership.uuid \"" + UNAVAILABLE_UUID + "\" nicht gefunden oder nicht zugänglich"),

        ASSETS_VALUE_MUST_NOT_BE_NULL(
                requestBody -> requestBody
                        .with("transactionType", REVERSAL)
                        .with("assetValue", 0.00),
                "[assetValue darf nicht 0 sein]"),

        REFERENCE_MISSING(
                requestBody -> requestBody.without("reference"),
                "[reference darf nicht null sein"),

        REFERENCE_TOO_SHORT(
                requestBody -> requestBody.with("reference", "12345"),
                "[reference Größe muss zwischen 6 und 48 sein"), // OpenAPI Spring templates uses @Size, but should use @Length

        REFERENCE_TOO_LONG(
                requestBody -> requestBody.with("reference", "0123456789012345678901234567890123456789012345678"),
                "[reference Größe muss zwischen 6 und 48 sein"); // OpenAPI Spring templates uses @Size, but should use @Length

        private final Function<JsonBuilder, JsonBuilder> givenBodyTransformation;
        private final String expectedErrorMessage;

        BadRequestTestCases(
                final Function<JsonBuilder, JsonBuilder> givenBodyTransformation,
                final String expectedErrorMessage) {
            this.givenBodyTransformation = givenBodyTransformation;
            this.expectedErrorMessage = expectedErrorMessage;
        }

        String givenRequestBody() {
            return givenBodyTransformation.apply(jsonObject(INSERT_REQUEST_BODY_TEMPLATE)).toString();
        }
    }

    @ParameterizedTest
    @EnumSource(BadRequestTestCases.class)
    void respondWithBadRequest(final BadRequestTestCases testCase) throws Exception {
        // HOWTO: run just a single test-case in a data-driven test-method
        //  - set SINGLE_TEST_CASE_EXECUTION to true - see above
        //  - select the test case enum value you want to run
        assumeThat(!SINGLE_TEST_CASE_EXECUTION ||
                testCase == BadRequestTestCases.MEMBERSHIP_UUID_OR_MEMBER_NUMBER_MUST_BE_GIVEN).isTrue();

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/coopassetstransactions")
                        .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                        .header("Accept-Language", "de")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testCase.givenRequestBody())
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(jsonPath("statusCode", is(400)))
                .andExpect(jsonPath("statusPhrase", is("Bad Request")))
                .andExpect(jsonPath("message", startsWith("ERROR: [400] " + testCase.expectedErrorMessage)))
                .andExpect(status().is4xxClientError());
    }

    enum SuccessfullyCreatedTestCases {

        REVERTING_SIMPLE_ASSET_TRANSACTION(
                requestBody -> requestBody
                        .with("transactionType", REVERSAL)
                        .with("assetValue", "64.00")
                        .with("valueDate", "2024-10-15")
                        .with("reference", "reversal of loss ref")
                        .with("comment", "reversal of loss asset tx comment")
                        .with("revertedAssetTx.uuid", SOME_EXISTING_LOSS_ASSET_TX_UUID),
                Expected.REVERT_LOSS_RESPONSE),

        TRANSFER_TO_GIVEN_AVAILABLE_MEMBERSHIP_NUMBER(
                requestBody -> requestBody
                        .with("transactionType", TRANSFER)
                        .with("assetValue", -64.00)
                        .with("adoptingMembership.memberNumber", AVAILABLE_TARGET_MEMBER_NUMBER),
                Expected.TRANSFER_RESPONSE),

        TRANSFER_TO_GIVEN_AVAILABLE_MEMBERSHIP_UUID(
                requestBody -> requestBody
                        .with("transactionType", TRANSFER)
                        .with("assetValue", -64.00)
                        .with("membership.uuid", ORIGIN_MEMBERSHIP_UUID)
                        .with("adoptingMembership.uuid", AVAILABLE_TARGET_MEMBERSHIP_UUID),
                Expected.TRANSFER_RESPONSE),

        REVERTING_TRANSFER_ASSET_TRANSACTION_IMPLICITLY_REVERTS_ADOPTING_ASSET_TRANSACTION(
                requestBody -> requestBody
                        .with("transactionType", REVERSAL)
                        .with("assetValue", "256.00")
                        .with("valueDate", "2024-10-15")
                        .with("reference", "reversal of transfer ref")
                        .with("comment", "reversal of transfer asset tx comment")
                        .with("revertedAssetTx.uuid", SOME_EXISTING_TRANSFER_ASSET_TX_UUID),
                Expected.REVERT_TRANSFER_RESPONSE);

        private final Function<JsonBuilder, JsonBuilder> givenBodyTransformation;
        private final String expectedResponseBody;

        SuccessfullyCreatedTestCases(
                final Function<JsonBuilder, JsonBuilder> givenBodyTransformation,
                final String expectedResponseBody) {
            this.givenBodyTransformation = givenBodyTransformation;
            this.expectedResponseBody = expectedResponseBody;
        }

        String givenRequestBody() {
            return givenBodyTransformation.apply(jsonObject(INSERT_REQUEST_BODY_TEMPLATE)).toString();
        }

        private static class Expected {

            public static final String REVERT_LOSS_RESPONSE = """
                    {
                         "uuid": "%{NEW_EXPLICITLY_CREATED_REVERSAL_ASSET_TX_UUID}",
                         "membership.uuid": "%{ORIGIN_MEMBERSHIP_UUID}",
                         "membership.memberNumber": "%{ORIGIN_MEMBER_NUMBER}",
                         "transactionType": "REVERSAL",
                         "assetValue": 64.00,
                         "valueDate": "2024-10-15",
                         "reference": "reversal of loss ref",
                         "comment": "reversal of loss asset tx comment",
                         "adoptionAssetTx": null,
                         "reversalAssetTx": null,
                         "transferAssetTx": null,
                         "revertedAssetTx": {
                           "uuid": "%{SOME_EXISTING_LOSS_ASSET_TX_UUID}",
                           "membership.uuid": "%{ORIGIN_MEMBERSHIP_UUID}",
                           "membership.memberNumber": "%{ORIGIN_MEMBER_NUMBER}",
                           "transactionType": "LOSS",
                           "assetValue": -64.00,
                           "valueDate": "2024-10-15",
                           "reference": "some loss asset tx ref",
                           "comment": "some loss asset tx comment",
                           "adoptionAssetTx.uuid": null,
                           "transferAssetTx.uuid": null,
                           "revertedAssetTx.uuid": null,
                           "reversalAssetTx.uuid": "%{NEW_EXPLICITLY_CREATED_REVERSAL_ASSET_TX_UUID}"
                         }
                    }
                    """
                    .replace(
                            "%{NEW_EXPLICITLY_CREATED_REVERSAL_ASSET_TX_UUID}",
                            NEW_EXPLICITLY_CREATED_REVERSAL_ASSET_TX_UUID.toString())
                    .replace("%{ORIGIN_MEMBERSHIP_UUID}", ORIGIN_MEMBERSHIP_UUID.toString())
                    .replace("%{ORIGIN_MEMBER_NUMBER}", ORIGIN_MEMBER_NUMBER)
                    .replace("%{SOME_EXISTING_LOSS_ASSET_TX_UUID}", SOME_EXISTING_LOSS_ASSET_TX_UUID.toString());

            public static final String TRANSFER_RESPONSE = """
                    {
                        "uuid": "%{NEW_EXPLICITLY_CREATED_TRANSFER_ASSET_TX_UUID}",
                        "membership.uuid": "%{ORIGIN_MEMBERSHIP_UUID}",
                        "membership.memberNumber": "%{ORIGIN_MEMBER_NUMBER}",
                        "transactionType": "TRANSFER",
                        "assetValue": -64.00,
                        "adoptionAssetTx": {
                            "membership.uuid": "%{AVAILABLE_MEMBERSHIP_UUID}",
                            "membership.memberNumber": "%{AVAILABLE_TARGET_MEMBER_NUMBER}",
                            "transactionType": "ADOPTION",
                            "assetValue": 64.00,
                            "transferAssetTx.uuid": "%{NEW_EXPLICITLY_CREATED_TRANSFER_ASSET_TX_UUID}"
                        },
                        "transferAssetTx": null,
                        "revertedAssetTx": null,
                        "reversalAssetTx": null
                    }
                    """
                    .replace(
                            "%{NEW_EXPLICITLY_CREATED_TRANSFER_ASSET_TX_UUID}",
                            NEW_EXPLICITLY_CREATED_TRANSFER_ASSET_TX_UUID.toString())
                    .replace("%{ORIGIN_MEMBERSHIP_UUID}", ORIGIN_MEMBERSHIP_UUID.toString())
                    .replace("%{ORIGIN_MEMBER_NUMBER}", ORIGIN_MEMBER_NUMBER)
                    .replace("%{AVAILABLE_MEMBERSHIP_UUID}", AVAILABLE_TARGET_MEMBERSHIP_UUID.toString())
                    .replace("%{AVAILABLE_TARGET_MEMBER_NUMBER}", AVAILABLE_TARGET_MEMBER_NUMBER);

            public static final String REVERT_TRANSFER_RESPONSE = """
                    {
                         "uuid": "%{NEW_EXPLICITLY_CREATED_REVERSAL_ASSET_TX_UUID}",
                         "membership.uuid": "%{ORIGIN_MEMBERSHIP_UUID}",
                         "membership.memberNumber": "%{ORIGIN_MEMBER_NUMBER}",
                         "transactionType": "REVERSAL",
                         "assetValue": 256.00,
                         "valueDate": "2024-10-15",
                         "reference": "reversal of transfer ref",
                         "comment": "reversal of transfer asset tx comment",
                         "adoptionAssetTx": null,
                         "transferAssetTx": null,
                         "revertedAssetTx": {
                           "uuid": "%{SOME_EXISTING_TRANSFER_ASSET_TX_UUID}",
                           "membership.uuid": "%{ORIGIN_MEMBERSHIP_UUID}",
                           "membership.memberNumber": "%{ORIGIN_MEMBER_NUMBER}",
                           "transactionType": "TRANSFER",
                           "assetValue": -256.00,
                           "valueDate": "2024-10-15",
                           "reference": "some transfer asset tx ref",
                           "comment": "some transfer asset tx comment",
                           "adoptionAssetTx.uuid": "%{SOME_EXISTING_ADOPTION_ASSET_TX_UUID}",
                           "transferAssetTx.uuid": null,
                           "revertedAssetTx.uuid": null,
                           "reversalAssetTx.uuid": "%{NEW_EXPLICITLY_CREATED_REVERSAL_ASSET_TX_UUID}"
                         }
                    }
                    """
                    .replace(
                            "%{NEW_EXPLICITLY_CREATED_REVERSAL_ASSET_TX_UUID}",
                            NEW_EXPLICITLY_CREATED_REVERSAL_ASSET_TX_UUID.toString())
                    .replace("%{ORIGIN_MEMBERSHIP_UUID}", ORIGIN_MEMBERSHIP_UUID.toString())
                    .replace("%{ORIGIN_MEMBER_NUMBER}", ORIGIN_MEMBER_NUMBER)
                    .replace("%{SOME_EXISTING_TRANSFER_ASSET_TX_UUID}", SOME_EXISTING_TRANSFER_ASSET_TX_UUID.toString())
                    .replace("%{SOME_EXISTING_ADOPTION_ASSET_TX_UUID}", SOME_EXISTING_ADOPTION_ASSET_TX_UUID.toString());
        }
    }

    @ParameterizedTest
    @EnumSource(SuccessfullyCreatedTestCases.class)
    void respondWithSuccessfullyCreated(final SuccessfullyCreatedTestCases testCase) throws Exception {
        assumeThat(!SINGLE_TEST_CASE_EXECUTION ||
                testCase == SuccessfullyCreatedTestCases.REVERTING_TRANSFER_ASSET_TRANSACTION_IMPLICITLY_REVERTS_ADOPTING_ASSET_TRANSACTION).isTrue();

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/coopassetstransactions")
                        .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testCase.givenRequestBody())
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$", lenientlyEquals(testCase.expectedResponseBody)));
    }

    @Test
    void getSingleGeneratesProperJsonForAvailableUuid() throws Exception {
        // given
        when(coopAssetsTransactionRepo.findByUuid(SOME_REVERTED_TRANSFER_ASSET_TX_ENTITY.getUuid()))
                .thenReturn(Optional.of(SOME_REVERTED_TRANSFER_ASSET_TX_ENTITY));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/hs/office/coopassetstransactions/" + SOME_REVERTED_TRANSFER_ASSET_TX_ENTITY.getUuid())
                        .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                        .contentType(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$", lenientlyEquals(EXPECTED_RESULT_FROM_GET_SINGLE)));
    }

    @Test
    void getSingleGeneratesNotFoundForUnavailableUuid() throws Exception {
        // given
        when(coopAssetsTransactionRepo.findByUuid(UNAVAILABLE_UUID)).thenReturn(Optional.empty());

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/hs/office/coopassetstransactions/" + UNAVAILABLE_UUID)
                        .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                        .contentType(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().isNotFound());
    }

    @Test
    void getListGeneratesProperJson() throws Exception {
        // given
        when(coopAssetsTransactionRepo.findCoopAssetsTransactionByOptionalMembershipUuidAndDateRange(null, null, null))
                .thenReturn(List.of(
                        SOME_EXISTING_LOSS_ASSET_TX_ENTITY,
                        SOME_EXISTING_TRANSFER_ASSET_TX_ENTITY,
                        SOME_EXISTING_ADOPTION_ASSET_TX_ENTITY,
                        SOME_REVERTED_DISBURSAL_ASSET_TX_ENTITY,
                        SOME_REVERTED_DISBURSAL_ASSET_TX_ENTITY.getReversalAssetTx(),
                        SOME_REVERTED_TRANSFER_ASSET_TX_ENTITY,
                        SOME_REVERTED_TRANSFER_ASSET_TX_ENTITY.getAdoptionAssetTx(),
                        SOME_REVERTED_TRANSFER_ASSET_TX_ENTITY.getReversalAssetTx(),
                        SOME_REVERTED_TRANSFER_ASSET_TX_ENTITY.getAdoptionAssetTx().getReversalAssetTx()
                ));

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/hs/office/coopassetstransactions")
                        .header("Authorization", "Bearer superuser-alex@hostsharing.net")
                        .contentType(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$", lenientlyEquals(EXPECTED_RESULT_FROM_GET_LIST)));
    }

    @Test
    void singleTestCaseExecutionMustBeDisabled() {
        assertThat(SINGLE_TEST_CASE_EXECUTION).isFalse();
    }

    @BeforeEach
    void initMocks() {
        TestUuidGenerator.start(DYNAMIC_UUID_START_INDEX);

        when(emw.find(eq(HsOfficeMembershipEntity.class), eq(ORIGIN_MEMBERSHIP_UUID))).thenReturn(ORIGIN_TARGET_MEMBER_ENTITY);
        when(emw.find(eq(HsOfficeMembershipEntity.class), eq(AVAILABLE_TARGET_MEMBERSHIP_UUID))).thenReturn(
                AVAILABLE_MEMBER_ENTITY);

        final var availableMemberNumber = Integer.valueOf(AVAILABLE_TARGET_MEMBER_NUMBER.substring("M-".length()));
        when(membershipRepo.findMembershipByMemberNumber(eq(availableMemberNumber))).thenReturn(Optional.of(AVAILABLE_MEMBER_ENTITY));

        when(membershipRepo.findByUuid(eq(ORIGIN_MEMBERSHIP_UUID))).thenReturn(Optional.of(ORIGIN_TARGET_MEMBER_ENTITY));
        when(membershipRepo.findByUuid(eq(AVAILABLE_TARGET_MEMBERSHIP_UUID))).thenReturn(Optional.of(AVAILABLE_MEMBER_ENTITY));

        when(coopAssetsTransactionRepo.findByUuid(SOME_EXISTING_LOSS_ASSET_TX_UUID))
                .thenReturn(Optional.of(SOME_EXISTING_LOSS_ASSET_TX_ENTITY));
        when(coopAssetsTransactionRepo.findByUuid(SOME_EXISTING_TRANSFER_ASSET_TX_UUID))
                .thenReturn(Optional.of(SOME_EXISTING_TRANSFER_ASSET_TX_ENTITY));
        when(coopAssetsTransactionRepo.findByUuid(SOME_EXISTING_ADOPTION_ASSET_TX_UUID))
                .thenReturn(Optional.of(SOME_EXISTING_ADOPTION_ASSET_TX_ENTITY));
        when(coopAssetsTransactionRepo.save(any(HsOfficeCoopAssetsTransactionEntity.class)))
                .thenAnswer(invocation -> {
                            final var entity = (HsOfficeCoopAssetsTransactionEntity) invocation.getArgument(0);
                            if (entity.getUuid() == null) {
                                entity.setUuid(TestUuidGenerator.next());
                            }
                            return entity;
                        }
                );
    }

    private int partnerNumberOf(final String memberNumber) {
        return Integer.parseInt(memberNumber.substring("M-".length(), memberNumber.length() - 2));
    }

    private String suffixOf(final String memberNumber) {
        return memberNumber.substring("M-".length() + 5);
    }

}
