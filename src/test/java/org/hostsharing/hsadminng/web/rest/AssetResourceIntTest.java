// Licensed under Apache-2.0
package org.hostsharing.hsadminng.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hostsharing.hsadminng.web.rest.TestUtil.createFormattingConversionService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.hostsharing.hsadminng.HsadminNgApp;
import org.hostsharing.hsadminng.domain.Asset;
import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.domain.enumeration.AssetAction;
import org.hostsharing.hsadminng.repository.AssetRepository;
import org.hostsharing.hsadminng.service.AssetQueryService;
import org.hostsharing.hsadminng.service.AssetService;
import org.hostsharing.hsadminng.service.UserRoleAssignmentService;
import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.hostsharing.hsadminng.service.accessfilter.SecurityContextMock;
import org.hostsharing.hsadminng.service.dto.AssetDTO;
import org.hostsharing.hsadminng.service.mapper.AssetMapper;
import org.hostsharing.hsadminng.web.rest.errors.ExceptionTranslator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import javax.persistence.EntityManager;

/**
 * Test class for the AssetResource REST controller.
 *
 * @see AssetResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = HsadminNgApp.class)
public class AssetResourceIntTest {

    private static final LocalDate DEFAULT_DOCUMENT_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DOCUMENT_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_VALUE_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_VALUE_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final AssetAction DEFAULT_ACTION = AssetAction.PAYMENT;
    private static final AssetAction UPDATED_ACTION = AssetAction.HANDOVER;

    private static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("1");
    private static final BigDecimal UPDATED_AMOUNT = new BigDecimal("2");

    private static final String DEFAULT_REMARK = "AAAAAAAAAA";
    private static final String UPDATED_REMARK = "BBBBBBBBBB";

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private AssetMapper assetMapper;

    @Autowired
    private AssetService assetService;

    @Autowired
    private AssetQueryService assetQueryService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    @MockBean
    private UserRoleAssignmentService userRoleAssignmentService;

    private MockMvc restAssetMockMvc;

    private Asset asset;

    @Before
    public void setup() {
        SecurityContextMock.usingMock(userRoleAssignmentService)
                .havingAuthenticatedUser()
                .withAuthority(Role.Admin.ROLE.authority());

        MockitoAnnotations.initMocks(this);
        final AssetResource assetResource = new AssetResource(assetService, assetQueryService);
        this.restAssetMockMvc = MockMvcBuilders.standaloneSetup(assetResource)
                .setCustomArgumentResolvers(pageableArgumentResolver)
                .setControllerAdvice(exceptionTranslator)
                .setConversionService(createFormattingConversionService())
                .setMessageConverters(jacksonMessageConverter)
                .setValidator(validator)
                .build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Asset createEntity(EntityManager em) {
        Asset asset = new Asset()
                .documentDate(DEFAULT_DOCUMENT_DATE)
                .valueDate(DEFAULT_VALUE_DATE)
                .action(DEFAULT_ACTION)
                .amount(DEFAULT_AMOUNT)
                .remark(DEFAULT_REMARK);
        // Add required entity
        Membership membership = MembershipResourceIntTest.createEntity(em);
        em.persist(membership);
        em.flush();
        asset.setMembership(membership);
        return asset;
    }

    /**
     * Create a persistent entity related to the given persistent membership for testing purposes.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Asset createPersistentEntity(EntityManager em, final Membership membership) {
        Asset asset = new Asset()
                .documentDate(DEFAULT_DOCUMENT_DATE)
                .valueDate(DEFAULT_VALUE_DATE)
                .action(DEFAULT_ACTION)
                .amount(DEFAULT_AMOUNT)
                .remark(DEFAULT_REMARK);
        // Add required entity
        asset.setMembership(membership);
        membership.addAsset(asset);
        em.persist(asset);
        em.flush();
        return asset;
    }

    @Before
    public void initTest() {
        asset = createEntity(em);
    }

    @Test
    @Transactional
    public void createAsset() throws Exception {
        int databaseSizeBeforeCreate = assetRepository.findAll().size();

        // Create the Asset
        AssetDTO assetDTO = assetMapper.toDto(asset);
        assetDTO.setMembershipDisplayLabel(null);
        restAssetMockMvc.perform(
                post("/api/assets")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(assetDTO)))
                .andExpect(status().isCreated());

        // Validate the Asset in the database
        List<Asset> assetList = assetRepository.findAll();
        assertThat(assetList).hasSize(databaseSizeBeforeCreate + 1);
        Asset testAsset = assetList.get(assetList.size() - 1);
        assertThat(testAsset.getDocumentDate()).isEqualTo(DEFAULT_DOCUMENT_DATE);
        assertThat(testAsset.getValueDate()).isEqualTo(DEFAULT_VALUE_DATE);
        assertThat(testAsset.getAction()).isEqualTo(DEFAULT_ACTION);
        assertThat(testAsset.getAmount()).isEqualTo(DEFAULT_AMOUNT.setScale(2, RoundingMode.HALF_DOWN));
        assertThat(testAsset.getRemark()).isEqualTo(DEFAULT_REMARK);
    }

    @Test
    @Transactional
    public void createAssetWithIdForNonExistingEntity() throws Exception {
        int databaseSizeBeforeCreate = assetRepository.findAll().size();

        // Create the Asset with an ID
        asset.setId(1L);
        AssetDTO assetDTO = assetMapper.toDto(asset);

        // An entity with an existing ID cannot be created, so this API call must fail
        restAssetMockMvc.perform(
                post("/api/assets")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(assetDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Asset in the database
        List<Asset> assetList = assetRepository.findAll();
        assertThat(assetList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void createAssetWithExistingExistingEntity() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);
        int databaseSizeBeforeCreate = assetRepository.findAll().size();

        // Create the Asset with the ID of an existing ID
        AssetDTO assetDTO = assetMapper.toDto(asset);

        // An entity with an existing ID cannot be created, so this API call must fail
        restAssetMockMvc.perform(
                post("/api/assets")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(assetDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Asset in the database
        List<Asset> assetList = assetRepository.findAll();
        assertThat(assetList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkDocumentDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = assetRepository.findAll().size();
        // set the field null
        asset.setDocumentDate(null);

        // Create the Asset, which fails.
        AssetDTO assetDTO = assetMapper.toDto(asset);

        restAssetMockMvc.perform(
                post("/api/assets")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(assetDTO)))
                .andExpect(status().isBadRequest());

        List<Asset> assetList = assetRepository.findAll();
        assertThat(assetList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkValueDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = assetRepository.findAll().size();
        // set the field null
        asset.setValueDate(null);

        // Create the Asset, which fails.
        AssetDTO assetDTO = assetMapper.toDto(asset);

        restAssetMockMvc.perform(
                post("/api/assets")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(assetDTO)))
                .andExpect(status().isBadRequest());

        List<Asset> assetList = assetRepository.findAll();
        assertThat(assetList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkActionIsRequired() throws Exception {
        int databaseSizeBeforeTest = assetRepository.findAll().size();
        // set the field null
        asset.setAction(null);

        // Create the Asset, which fails.
        AssetDTO assetDTO = assetMapper.toDto(asset);

        restAssetMockMvc.perform(
                post("/api/assets")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(assetDTO)))
                .andExpect(status().isBadRequest());

        List<Asset> assetList = assetRepository.findAll();
        assertThat(assetList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkAmountIsRequired() throws Exception {
        int databaseSizeBeforeTest = assetRepository.findAll().size();
        // set the field null
        asset.setAmount(null);

        // Create the Asset, which fails.
        AssetDTO assetDTO = assetMapper.toDto(asset);

        restAssetMockMvc.perform(
                post("/api/assets")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(assetDTO)))
                .andExpect(status().isBadRequest());

        List<Asset> assetList = assetRepository.findAll();
        assertThat(assetList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllAssets() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList
        restAssetMockMvc.perform(get("/api/assets?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(asset.getId().intValue())))
                .andExpect(jsonPath("$.[*].documentDate").value(hasItem(DEFAULT_DOCUMENT_DATE.toString())))
                .andExpect(jsonPath("$.[*].valueDate").value(hasItem(DEFAULT_VALUE_DATE.toString())))
                .andExpect(jsonPath("$.[*].action").value(hasItem(DEFAULT_ACTION.toString())))
                .andExpect(jsonPath("$.[*].amount").value(hasItem(DEFAULT_AMOUNT.intValue())))
                .andExpect(jsonPath("$.[*].remark").value(hasItem(DEFAULT_REMARK)));
    }

    @Test
    @Transactional
    public void getAsset() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get the asset
        restAssetMockMvc.perform(get("/api/assets/{id}", asset.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.id").value(asset.getId().intValue()))
                .andExpect(jsonPath("$.documentDate").value(DEFAULT_DOCUMENT_DATE.toString()))
                .andExpect(jsonPath("$.valueDate").value(DEFAULT_VALUE_DATE.toString()))
                .andExpect(jsonPath("$.action").value(DEFAULT_ACTION.toString()))
                .andExpect(jsonPath("$.amount").value(DEFAULT_AMOUNT.intValue()))
                .andExpect(jsonPath("$.remark").value(DEFAULT_REMARK));
    }

    @Test
    @Transactional
    public void getAllAssetsByDocumentDateIsEqualToSomething() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where documentDate equals to DEFAULT_DOCUMENT_DATE
        defaultAssetShouldBeFound("documentDate.equals=" + DEFAULT_DOCUMENT_DATE);

        // Get all the assetList where documentDate equals to UPDATED_DOCUMENT_DATE
        defaultAssetShouldNotBeFound("documentDate.equals=" + UPDATED_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllAssetsByDocumentDateIsInShouldWork() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where documentDate in DEFAULT_DOCUMENT_DATE or UPDATED_DOCUMENT_DATE
        defaultAssetShouldBeFound("documentDate.in=" + DEFAULT_DOCUMENT_DATE + "," + UPDATED_DOCUMENT_DATE);

        // Get all the assetList where documentDate equals to UPDATED_DOCUMENT_DATE
        defaultAssetShouldNotBeFound("documentDate.in=" + UPDATED_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllAssetsByDocumentDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where documentDate is not null
        defaultAssetShouldBeFound("documentDate.specified=true");

        // Get all the assetList where documentDate is null
        defaultAssetShouldNotBeFound("documentDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllAssetsByDocumentDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where documentDate greater than or equals to DEFAULT_DOCUMENT_DATE
        defaultAssetShouldBeFound("documentDate.greaterOrEqualThan=" + DEFAULT_DOCUMENT_DATE);

        // Get all the assetList where documentDate greater than or equals to UPDATED_DOCUMENT_DATE
        defaultAssetShouldNotBeFound("documentDate.greaterOrEqualThan=" + UPDATED_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllAssetsByDocumentDateIsLessThanSomething() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where documentDate less than or equals to DEFAULT_DOCUMENT_DATE
        defaultAssetShouldNotBeFound("documentDate.lessThan=" + DEFAULT_DOCUMENT_DATE);

        // Get all the assetList where documentDate less than or equals to UPDATED_DOCUMENT_DATE
        defaultAssetShouldBeFound("documentDate.lessThan=" + UPDATED_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllAssetsByValueDateIsEqualToSomething() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where valueDate equals to DEFAULT_VALUE_DATE
        defaultAssetShouldBeFound("valueDate.equals=" + DEFAULT_VALUE_DATE);

        // Get all the assetList where valueDate equals to UPDATED_VALUE_DATE
        defaultAssetShouldNotBeFound("valueDate.equals=" + UPDATED_VALUE_DATE);
    }

    @Test
    @Transactional
    public void getAllAssetsByValueDateIsInShouldWork() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where valueDate in DEFAULT_VALUE_DATE or UPDATED_VALUE_DATE
        defaultAssetShouldBeFound("valueDate.in=" + DEFAULT_VALUE_DATE + "," + UPDATED_VALUE_DATE);

        // Get all the assetList where valueDate equals to UPDATED_VALUE_DATE
        defaultAssetShouldNotBeFound("valueDate.in=" + UPDATED_VALUE_DATE);
    }

    @Test
    @Transactional
    public void getAllAssetsByValueDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where valueDate is not null
        defaultAssetShouldBeFound("valueDate.specified=true");

        // Get all the assetList where valueDate is null
        defaultAssetShouldNotBeFound("valueDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllAssetsByValueDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where valueDate greater than or equals to DEFAULT_VALUE_DATE
        defaultAssetShouldBeFound("valueDate.greaterOrEqualThan=" + DEFAULT_VALUE_DATE);

        // Get all the assetList where valueDate greater than or equals to UPDATED_VALUE_DATE
        defaultAssetShouldNotBeFound("valueDate.greaterOrEqualThan=" + UPDATED_VALUE_DATE);
    }

    @Test
    @Transactional
    public void getAllAssetsByValueDateIsLessThanSomething() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where valueDate less than or equals to DEFAULT_VALUE_DATE
        defaultAssetShouldNotBeFound("valueDate.lessThan=" + DEFAULT_VALUE_DATE);

        // Get all the assetList where valueDate less than or equals to UPDATED_VALUE_DATE
        defaultAssetShouldBeFound("valueDate.lessThan=" + UPDATED_VALUE_DATE);
    }

    @Test
    @Transactional
    public void getAllAssetsByActionIsEqualToSomething() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where action equals to DEFAULT_ACTION
        defaultAssetShouldBeFound("action.equals=" + DEFAULT_ACTION);

        // Get all the assetList where action equals to UPDATED_ACTION
        defaultAssetShouldNotBeFound("action.equals=" + UPDATED_ACTION);
    }

    @Test
    @Transactional
    public void getAllAssetsByActionIsInShouldWork() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where action in DEFAULT_ACTION or UPDATED_ACTION
        defaultAssetShouldBeFound("action.in=" + DEFAULT_ACTION + "," + UPDATED_ACTION);

        // Get all the assetList where action equals to UPDATED_ACTION
        defaultAssetShouldNotBeFound("action.in=" + UPDATED_ACTION);
    }

    @Test
    @Transactional
    public void getAllAssetsByActionIsNullOrNotNull() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where action is not null
        defaultAssetShouldBeFound("action.specified=true");

        // Get all the assetList where action is null
        defaultAssetShouldNotBeFound("action.specified=false");
    }

    @Test
    @Transactional
    public void getAllAssetsByAmountIsEqualToSomething() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where amount equals to DEFAULT_AMOUNT
        defaultAssetShouldBeFound("amount.equals=" + DEFAULT_AMOUNT);

        // Get all the assetList where amount equals to UPDATED_AMOUNT
        defaultAssetShouldNotBeFound("amount.equals=" + UPDATED_AMOUNT);
    }

    @Test
    @Transactional
    public void getAllAssetsByAmountIsInShouldWork() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where amount in DEFAULT_AMOUNT or UPDATED_AMOUNT
        defaultAssetShouldBeFound("amount.in=" + DEFAULT_AMOUNT + "," + UPDATED_AMOUNT);

        // Get all the assetList where amount equals to UPDATED_AMOUNT
        defaultAssetShouldNotBeFound("amount.in=" + UPDATED_AMOUNT);
    }

    @Test
    @Transactional
    public void getAllAssetsByAmountIsNullOrNotNull() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where amount is not null
        defaultAssetShouldBeFound("amount.specified=true");

        // Get all the assetList where amount is null
        defaultAssetShouldNotBeFound("amount.specified=false");
    }

    @Test
    @Transactional
    public void getAllAssetsByRemarkIsEqualToSomething() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where remark equals to DEFAULT_REMARK
        defaultAssetShouldBeFound("remark.equals=" + DEFAULT_REMARK);

        // Get all the assetList where remark equals to UPDATED_REMARK
        defaultAssetShouldNotBeFound("remark.equals=" + UPDATED_REMARK);
    }

    @Test
    @Transactional
    public void getAllAssetsByRemarkIsInShouldWork() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where remark in DEFAULT_REMARK or UPDATED_REMARK
        defaultAssetShouldBeFound("remark.in=" + DEFAULT_REMARK + "," + UPDATED_REMARK);

        // Get all the assetList where remark equals to UPDATED_REMARK
        defaultAssetShouldNotBeFound("remark.in=" + UPDATED_REMARK);
    }

    @Test
    @Transactional
    public void getAllAssetsByRemarkIsNullOrNotNull() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where remark is not null
        defaultAssetShouldBeFound("remark.specified=true");

        // Get all the assetList where remark is null
        defaultAssetShouldNotBeFound("remark.specified=false");
    }

    @Test
    @Transactional
    public void getAllAssetsByMembershipIsEqualToSomething() throws Exception {
        // Initialize the database
        Membership membership = MembershipResourceIntTest
                .createPersistentEntity(em, CustomerResourceIntTest.createPersistentEntity(em));
        asset.setMembership(membership);
        assetRepository.saveAndFlush(asset);
        Long membershipId = membership.getId();

        // Get all the assetList where membership equals to membershipId
        defaultAssetShouldBeFound("membershipId.equals=" + membershipId);

        // Get all the assetList where membership equals to membershipId + 1
        defaultAssetShouldNotBeFound("membershipId.equals=" + (membershipId + 1));
    }

    /**
     * Executes the search, and checks that the default entity is returned
     */
    private void defaultAssetShouldBeFound(String filter) throws Exception {
        restAssetMockMvc.perform(get("/api/assets?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(asset.getId().intValue())))
                .andExpect(jsonPath("$.[*].documentDate").value(hasItem(DEFAULT_DOCUMENT_DATE.toString())))
                .andExpect(jsonPath("$.[*].valueDate").value(hasItem(DEFAULT_VALUE_DATE.toString())))
                .andExpect(jsonPath("$.[*].action").value(hasItem(DEFAULT_ACTION.toString())))
                .andExpect(jsonPath("$.[*].amount").value(hasItem(DEFAULT_AMOUNT.intValue())))
                .andExpect(jsonPath("$.[*].remark").value(hasItem(DEFAULT_REMARK)));

        // Check, that the count call also returns 1
        restAssetMockMvc.perform(get("/api/assets/count?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned
     */
    private void defaultAssetShouldNotBeFound(String filter) throws Exception {
        restAssetMockMvc.perform(get("/api/assets?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restAssetMockMvc.perform(get("/api/assets/count?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    public void getNonExistingAsset() throws Exception {
        // Get the asset
        restAssetMockMvc.perform(get("/api/assets/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateAsset() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        int databaseSizeBeforeUpdate = assetRepository.findAll().size();

        // Update the asset
        Asset updatedAsset = assetRepository.findById(asset.getId()).get();
        // Disconnect from session so that the updates on updatedAsset are not directly saved in db
        em.detach(updatedAsset);
        updatedAsset
                .documentDate(UPDATED_DOCUMENT_DATE)
                .valueDate(UPDATED_VALUE_DATE)
                .action(UPDATED_ACTION)
                .amount(UPDATED_AMOUNT)
                .remark(UPDATED_REMARK);
        AssetDTO assetDTO = assetMapper.toDto(updatedAsset);

        restAssetMockMvc.perform(
                put("/api/assets")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(assetDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Asset in the database
        List<Asset> assetList = assetRepository.findAll();
        assertThat(assetList).hasSize(databaseSizeBeforeUpdate);
        Asset testAsset = assetList.get(assetList.size() - 1);
        assertThat(testAsset.getDocumentDate()).isEqualTo(DEFAULT_DOCUMENT_DATE);
        assertThat(testAsset.getValueDate()).isEqualTo(DEFAULT_VALUE_DATE);
        assertThat(testAsset.getAction()).isEqualByComparingTo(DEFAULT_ACTION);
        assertThat(testAsset.getAmount()).isEqualByComparingTo(DEFAULT_AMOUNT);
        assertThat(testAsset.getRemark()).isEqualTo(DEFAULT_REMARK);
    }

    @Test
    @Transactional
    public void updateNonExistingAsset() throws Exception {
        int databaseSizeBeforeUpdate = assetRepository.findAll().size();

        // Create the Asset
        AssetDTO assetDTO = assetMapper.toDto(asset);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAssetMockMvc.perform(
                put("/api/assets")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(assetDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Asset in the database
        List<Asset> assetList = assetRepository.findAll();
        assertThat(assetList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteAsset() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        int databaseSizeBeforeDelete = assetRepository.findAll().size();

        // Delete the asset
        restAssetMockMvc.perform(
                delete("/api/assets/{id}", asset.getId())
                        .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());

        // Validate the database still contains the same number of assets
        List<Asset> assetList = assetRepository.findAll();
        assertThat(assetList).hasSize(databaseSizeBeforeDelete);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Asset.class);
        Asset asset1 = new Asset();
        asset1.setId(1L);
        Asset asset2 = new Asset();
        asset2.setId(asset1.getId());
        assertThat(asset1).isEqualTo(asset2);
        asset2.setId(2L);
        assertThat(asset1).isNotEqualTo(asset2);
        asset1.setId(null);
        assertThat(asset1).isNotEqualTo(asset2);
    }

    @Test
    @Transactional
    public void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AssetDTO.class);
        AssetDTO assetDTO1 = new AssetDTO();
        assetDTO1.setId(1L);
        AssetDTO assetDTO2 = new AssetDTO();
        assertThat(assetDTO1).isNotEqualTo(assetDTO2);
        assetDTO2.setId(assetDTO1.getId());
        assertThat(assetDTO1).isEqualTo(assetDTO2);
        assetDTO2.setId(2L);
        assertThat(assetDTO1).isNotEqualTo(assetDTO2);
        assetDTO1.setId(null);
        assertThat(assetDTO1).isNotEqualTo(assetDTO2);
    }

    @Test
    @Transactional
    public void testEntityFromId() {
        assertThat(assetMapper.fromId(42L).getId()).isEqualTo(42);
        assertThat(assetMapper.fromId(null)).isNull();
    }
}
