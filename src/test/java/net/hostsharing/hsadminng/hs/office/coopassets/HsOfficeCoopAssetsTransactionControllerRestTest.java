package net.hostsharing.hsadminng.hs.office.coopassets;

import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipEntity;
import net.hostsharing.hsadminng.hs.office.membership.HsOfficeMembershipRepository;
import net.hostsharing.hsadminng.hs.office.partner.HsOfficePartnerEntity;
import net.hostsharing.hsadminng.mapper.StrictMapper;
import net.hostsharing.hsadminng.persistence.EntityManagerWrapper;
import net.hostsharing.hsadminng.rbac.test.JsonBuilder;
import net.hostsharing.hsadminng.test.TestUuidGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static net.hostsharing.hsadminng.rbac.test.JsonBuilder.jsonObject;
import static net.hostsharing.hsadminng.rbac.test.JsonMatcher.lenientlyEquals;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsOfficeCoopAssetsTransactionController.class)
@Import({ StrictMapper.class, JsonObjectMapperConfiguration.class })
@RunWith(SpringRunner.class)
class HsOfficeCoopAssetsTransactionControllerRestTest {

    private static final UUID UNAVAILABLE_MEMBERSHIP_UUID = TestUuidGenerator.use(0);
    private static final String UNAVAILABLE_MEMBER_NUMBER = "M-1234699";

    private static final UUID ORIGIN_MEMBERSHIP_UUID = TestUuidGenerator.use(1);
    private static final String ORIGIN_MEMBER_NUMBER = "M-1111100";
    public final HsOfficeMembershipEntity ORIGIN_TARGET_MEMBER_ENTITY = HsOfficeMembershipEntity.builder()
            .uuid(ORIGIN_MEMBERSHIP_UUID)
            .partner(HsOfficePartnerEntity.builder()
                    .partnerNumber(partnerNumberOf(ORIGIN_MEMBER_NUMBER))
                    .build())
            .memberNumberSuffix(suffixOf(ORIGIN_MEMBER_NUMBER))
            .build();

    private static final UUID AVAILABLE_TARGET_MEMBERSHIP_UUID = TestUuidGenerator.use(2);
    private static final String AVAILABLE_TARGET_MEMBER_NUMBER = "M-1234500";
    public final HsOfficeMembershipEntity AVAILABLE_MEMBER_ENTITY = HsOfficeMembershipEntity.builder()
            .uuid(AVAILABLE_TARGET_MEMBERSHIP_UUID)
            .partner(HsOfficePartnerEntity.builder()
                    .partnerNumber(partnerNumberOf(AVAILABLE_TARGET_MEMBER_NUMBER))
                    .build())
            .memberNumberSuffix(suffixOf(AVAILABLE_TARGET_MEMBER_NUMBER))
            .build();

    // the following refs might change if impl changes
    private static final UUID NEW_EXPLICITLY_CREATED_REVERSAL_ASSET_TX_UUID = TestUuidGenerator.ref(4);
    private static final UUID NEW_EXPLICITLY_CREATED_TRANSFER_ASSET_TX_UUID = TestUuidGenerator.ref(5);

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

    @Autowired
    MockMvc mockMvc;

    @MockBean
    Context contextMock;

    @Autowired
    @SuppressWarnings("unused") // not used in test, but in controller class
    StrictMapper mapper;

    @MockBean
    EntityManagerWrapper emw; // even if not used in test anymore, it's needed by base-class of StrictMapper

    @MockBean
    HsOfficeCoopAssetsTransactionRepository coopAssetsTransactionRepo;

    @MockBean
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
                "[membershipUuid must not be null but is \"null\"]"), // TODO.impl: should be membership.uuid, Spring validation-problem?

        TRANSACTION_TYPE_MISSING(
                requestBody -> requestBody.without("transactionType"),
                "[transactionType must not be null but is \"null\"]"),

        VALUE_DATE_MISSING(
                requestBody -> requestBody.without("valueDate"),
                "[valueDate must not be null but is \"null\"]"),

        ASSETS_VALUE_FOR_DEPOSIT_MUST_BE_POSITIVE(
                requestBody -> requestBody
                        .with("transactionType", "DEPOSIT")
                        .with("assetValue", -64.00),
                "[for DEPOSIT, assetValue must be positive but is \"-64.00\"]"),

        ASSETS_VALUE_FOR_DISBURSAL_MUST_BE_NEGATIVE(
                requestBody -> requestBody
                        .with("transactionType", "DISBURSAL")
                        .with("assetValue", 64.00),
                "[for DISBURSAL, assetValue must be negative but is \"64.00\"]"),

        //TODO: other transaction types

        ADOPTING_MEMBERSHIP_NUMBER_FOR_TRANSFER_MUST_BE_GIVEN_AND_AVAILABLE(
                requestBody -> requestBody
                        .with("transactionType", "TRANSFER")
                        .with("assetValue", -64.00)
                        .with("adoptingMembership.memberNumber", UNAVAILABLE_MEMBER_NUMBER),
                "adoptingMembership.memberNumber='M-1234699' not found or not accessible"),

        ADOPTING_MEMBERSHIP_UUID_FOR_TRANSFER_MUST_BE_GIVEN_AND_AVAILABLE(
                requestBody -> requestBody
                        .with("transactionType", "TRANSFER")
                        .with("assetValue", -64.00)
                        .with("adoptingMembership.uuid", UNAVAILABLE_MEMBERSHIP_UUID.toString()),
                "adoptingMembership.uuid='" + UNAVAILABLE_MEMBERSHIP_UUID + "' not found or not accessible"),

        ASSETS_VALUE_MUST_NOT_BE_NULL(
                requestBody -> requestBody
                        .with("transactionType", "REVERSAL")
                        .with("assetValue", 0.00),
                "[assetValue must not be 0 but is \"0.00\"]"),

        REFERENCE_MISSING(
                requestBody -> requestBody.without("reference"),
                "[reference must not be null but is \"null\"]"),

        REFERENCE_TOO_SHORT(
                requestBody -> requestBody.with("reference", "12345"),
                "[reference size must be between 6 and 48 but is \"12345\"]"),

        REFERENCE_TOO_LONG(
                requestBody -> requestBody.with("reference", "0123456789012345678901234567890123456789012345678"),
                "[reference size must be between 6 and 48 but is \"0123456789012345678901234567890123456789012345678\"]");

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
        // org.assertj.core.api.Assumptions.assumeThat(
        //      testCase == ADOPTING_MEMBERSHIP_NUMBER_FOR_TRANSFER_MUST_BE_GIVEN_AND_AVAILABLE).isTrue();

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/coopassetstransactions")
                        .header("current-subject", "superuser-alex@hostsharing.net")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testCase.givenRequestBody())
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(jsonPath("message", is("ERROR: [400] " + testCase.expectedErrorMessage)))
                .andExpect(jsonPath("statusCode", is(400)))
                .andExpect(jsonPath("statusPhrase", is("Bad Request")))
                .andExpect(status().is4xxClientError());
    }

    enum SuccessfullyCreatedTestCases {

        REVERTING_SIMPLE_ASSET_TRANSACTION(
                requestBody -> requestBody
                        .with("transactionType", "REVERSAL")
                        .with("assetValue", "64.00")
                        .with("valueDate", "2024-10-15")
                        .with("reference", "reversal ref")
                        .with("comment", "reversal comment")
                        .with("revertedAssetTx.uuid", SOME_EXISTING_LOSS_ASSET_TX_UUID.toString()),
                Expected.REVERT_RESPONSE),

        TRANSFER_TO_GIVEN_AVAILABLE_MEMBERSHIP_NUMBER(
                requestBody -> requestBody
                        .with("transactionType", "TRANSFER")
                        .with("assetValue", -64.00)
                        .with("adoptingMembership.memberNumber", AVAILABLE_TARGET_MEMBER_NUMBER),
                Expected.TRANSFER_RESPONSE),

        TRANSFER_TO_GIVEN_AVAILABLE_MEMBERSHIP_UUID(
                requestBody -> requestBody
                        .with("transactionType", "TRANSFER")
                        .with("assetValue", -64.00)
                        .with("membership.uuid", ORIGIN_MEMBERSHIP_UUID.toString())
                        .with("adoptingMembership.uuid", AVAILABLE_TARGET_MEMBERSHIP_UUID.toString()),
                Expected.TRANSFER_RESPONSE);

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

            public static final String REVERT_RESPONSE = """
                    {
                         "uuid": "%{NEW_EXPLICITLY_CREATED_REVERSAL_ASSET_TX_UUID}",
                         "membership.uuid": "%{ORIGIN_MEMBERSHIP_UUID}",
                         "membership.memberNumber": "%{ORIGIN_MEMBER_NUMBER}",
                         "transactionType": "REVERSAL",
                         "assetValue": 64.00,
                         "valueDate": "2024-10-15",
                         "reference": "reversal ref",
                         "comment": "reversal comment",
                         "adoptionAssetTx": null,
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
                    .replace("%{NEW_EXPLICITLY_CREATED_REVERSAL_ASSET_TX_UUID}", NEW_EXPLICITLY_CREATED_REVERSAL_ASSET_TX_UUID.toString())
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
                    .replace("%{NEW_EXPLICITLY_CREATED_TRANSFER_ASSET_TX_UUID}", NEW_EXPLICITLY_CREATED_TRANSFER_ASSET_TX_UUID.toString())
                    .replace("%{ORIGIN_MEMBERSHIP_UUID}", ORIGIN_MEMBERSHIP_UUID.toString())
                    .replace("%{ORIGIN_MEMBER_NUMBER}", ORIGIN_MEMBER_NUMBER)
                    .replace("%{AVAILABLE_MEMBERSHIP_UUID}", AVAILABLE_TARGET_MEMBERSHIP_UUID.toString())
                    .replace("%{AVAILABLE_TARGET_MEMBER_NUMBER}", AVAILABLE_TARGET_MEMBER_NUMBER);
        }
    }

    @ParameterizedTest
    @EnumSource(SuccessfullyCreatedTestCases.class)
    void respondWithSuccessfullyCreated(final SuccessfullyCreatedTestCases testCase) throws Exception {
        // uncomment, if you need to run just a single test-case in this data-driven test-method
        // org.assertj.core.api.Assumptions.assumeThat(
        //        testCase == ADOPTING_MEMBERSHIP_UUID_FOR_TRANSFER_MUST_BE_GIVEN_AND_AVAILABLE).isTrue();

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/hs/office/coopassetstransactions")
                        .header("current-subject", "superuser-alex@hostsharing.net")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testCase.givenRequestBody())
                        .accept(MediaType.APPLICATION_JSON))

                // then
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$", lenientlyEquals(testCase.expectedResponseBody)));
    }

    @BeforeEach
    void initMocks() {
        TestUuidGenerator.start(4);

        when(emw.find(eq(HsOfficeMembershipEntity.class), eq(ORIGIN_MEMBERSHIP_UUID))).thenReturn(ORIGIN_TARGET_MEMBER_ENTITY);
        when(emw.find(eq(HsOfficeMembershipEntity.class), eq(AVAILABLE_TARGET_MEMBERSHIP_UUID))).thenReturn(AVAILABLE_MEMBER_ENTITY);

        final var availableMemberNumber = Integer.valueOf(AVAILABLE_TARGET_MEMBER_NUMBER.substring("M-".length()));
        when(membershipRepo.findMembershipByMemberNumber(eq(availableMemberNumber))).thenReturn(AVAILABLE_MEMBER_ENTITY);

        when(membershipRepo.findByUuid(eq(ORIGIN_MEMBERSHIP_UUID))).thenReturn(Optional.of(ORIGIN_TARGET_MEMBER_ENTITY));
        when(membershipRepo.findByUuid(eq(AVAILABLE_TARGET_MEMBERSHIP_UUID))).thenReturn(Optional.of(AVAILABLE_MEMBER_ENTITY));

        when(coopAssetsTransactionRepo.findByUuid(SOME_EXISTING_LOSS_ASSET_TX_UUID))
                .thenReturn(Optional.of(SOME_EXISTING_LOSS_ASSET_TX_ENTITY));
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
        return Integer.parseInt(memberNumber.substring("M-".length(), memberNumber.length()-2));
    }

    private String suffixOf(final String memberNumber) {
        return memberNumber.substring("M-".length()+5);
    }
}
