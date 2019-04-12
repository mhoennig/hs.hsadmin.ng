package org.hostsharing.hsadminng.web.rest;

import org.hostsharing.hsadminng.HsadminNgApp;
import org.hostsharing.hsadminng.domain.Asset;
import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.domain.enumeration.AssetAction;
import org.hostsharing.hsadminng.repository.AssetRepository;
import org.hostsharing.hsadminng.service.AssetQueryService;
import org.hostsharing.hsadminng.service.AssetService;
import org.hostsharing.hsadminng.service.dto.AssetDTO;
import org.hostsharing.hsadminng.service.mapper.AssetMapper;
import org.hostsharing.hsadminng.web.rest.errors.ExceptionTranslator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hostsharing.hsadminng.web.rest.TestUtil.createFormattingConversionService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * Test class for the AssetResource REST controller.
 *
 * @see AssetResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = HsadminNgApp.class)
public class AssetResourceIntTest {

    private static final LocalDate DEFAULT_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final AssetAction DEFAULT_ACTION = AssetAction.PAYMENT;
    private static final AssetAction UPDATED_ACTION = AssetAction.HANDOVER;

    private static final BigDecimal DEFAULT_AMOUNT = new BigDecimal(1);
    private static final BigDecimal UPDATED_AMOUNT = new BigDecimal(2);

    private static final String DEFAULT_COMMENT = "AAAAAAAAAA";
    private static final String UPDATED_COMMENT = "BBBBBBBBBB";

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

    private MockMvc restAssetMockMvc;

    private Asset asset;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final AssetResource assetResource = new AssetResource(assetService, assetQueryService);
        this.restAssetMockMvc = MockMvcBuilders.standaloneSetup(assetResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Asset createEntity(EntityManager em, Membership membership) {
        em.persist(membership);
        Asset asset = new Asset()
            .member(membership)
            .date(DEFAULT_DATE)
            .action(DEFAULT_ACTION)
            .amount(DEFAULT_AMOUNT)
            .comment(DEFAULT_COMMENT);
        // Add required entity
        Membership membership = MembershipResourceIntTest.createEntity(em);
        em.persist(membership);
        em.flush();
        asset.setMember(membership);
        return asset;
    }

    @Before
    public void initTest() {
        asset = createEntity(em, MembershipResourceIntTest.createEntity(em, CustomerResourceIntTest.createEntity(em)));
    }

    @Test
    @Transactional
    public void createAsset() throws Exception {
        int databaseSizeBeforeCreate = assetRepository.findAll().size();

        // Create the Asset
        AssetDTO assetDTO = assetMapper.toDto(asset);
        restAssetMockMvc.perform(post("/api/assets")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(assetDTO)))
            .andExpect(status().isCreated());

        // Validate the Asset in the database
        List<Asset> assetList = assetRepository.findAll();
        assertThat(assetList).hasSize(databaseSizeBeforeCreate + 1);
        Asset testAsset = assetList.get(assetList.size() - 1);
        assertThat(testAsset.getDate()).isEqualTo(DEFAULT_DATE);
        assertThat(testAsset.getAction()).isEqualTo(DEFAULT_ACTION);
        assertThat(testAsset.getAmount()).isEqualTo(DEFAULT_AMOUNT);
        assertThat(testAsset.getComment()).isEqualTo(DEFAULT_COMMENT);
    }

    @Test
    @Transactional
    public void createAssetWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = assetRepository.findAll().size();

        // Create the Asset with an existing ID
        asset.setId(1L);
        AssetDTO assetDTO = assetMapper.toDto(asset);

        // An entity with an existing ID cannot be created, so this API call must fail
        restAssetMockMvc.perform(post("/api/assets")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(assetDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Asset in the database
        List<Asset> assetList = assetRepository.findAll();
        assertThat(assetList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = assetRepository.findAll().size();
        // set the field null
        asset.setDate(null);

        // Create the Asset, which fails.
        AssetDTO assetDTO = assetMapper.toDto(asset);

        restAssetMockMvc.perform(post("/api/assets")
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

        restAssetMockMvc.perform(post("/api/assets")
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

        restAssetMockMvc.perform(post("/api/assets")
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
            .andExpect(jsonPath("$.[*].date").value(hasItem(DEFAULT_DATE.toString())))
            .andExpect(jsonPath("$.[*].action").value(hasItem(DEFAULT_ACTION.toString())))
            .andExpect(jsonPath("$.[*].amount").value(hasItem(DEFAULT_AMOUNT.intValue())))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT.toString())));
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
            .andExpect(jsonPath("$.date").value(DEFAULT_DATE.toString()))
            .andExpect(jsonPath("$.action").value(DEFAULT_ACTION.toString()))
            .andExpect(jsonPath("$.amount").value(DEFAULT_AMOUNT.intValue()))
            .andExpect(jsonPath("$.comment").value(DEFAULT_COMMENT.toString()));
    }

    @Test
    @Transactional
    public void getAllAssetsByDateIsEqualToSomething() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where date equals to DEFAULT_DATE
        defaultAssetShouldBeFound("date.equals=" + DEFAULT_DATE);

        // Get all the assetList where date equals to UPDATED_DATE
        defaultAssetShouldNotBeFound("date.equals=" + UPDATED_DATE);
    }

    @Test
    @Transactional
    public void getAllAssetsByDateIsInShouldWork() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where date in DEFAULT_DATE or UPDATED_DATE
        defaultAssetShouldBeFound("date.in=" + DEFAULT_DATE + "," + UPDATED_DATE);

        // Get all the assetList where date equals to UPDATED_DATE
        defaultAssetShouldNotBeFound("date.in=" + UPDATED_DATE);
    }

    @Test
    @Transactional
    public void getAllAssetsByDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where date is not null
        defaultAssetShouldBeFound("date.specified=true");

        // Get all the assetList where date is null
        defaultAssetShouldNotBeFound("date.specified=false");
    }

    @Test
    @Transactional
    public void getAllAssetsByDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where date greater than or equals to DEFAULT_DATE
        defaultAssetShouldBeFound("date.greaterOrEqualThan=" + DEFAULT_DATE);

        // Get all the assetList where date greater than or equals to UPDATED_DATE
        defaultAssetShouldNotBeFound("date.greaterOrEqualThan=" + UPDATED_DATE);
    }

    @Test
    @Transactional
    public void getAllAssetsByDateIsLessThanSomething() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where date less than or equals to DEFAULT_DATE
        defaultAssetShouldNotBeFound("date.lessThan=" + DEFAULT_DATE);

        // Get all the assetList where date less than or equals to UPDATED_DATE
        defaultAssetShouldBeFound("date.lessThan=" + UPDATED_DATE);
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
    public void getAllAssetsByCommentIsEqualToSomething() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where comment equals to DEFAULT_COMMENT
        defaultAssetShouldBeFound("comment.equals=" + DEFAULT_COMMENT);

        // Get all the assetList where comment equals to UPDATED_COMMENT
        defaultAssetShouldNotBeFound("comment.equals=" + UPDATED_COMMENT);
    }

    @Test
    @Transactional
    public void getAllAssetsByCommentIsInShouldWork() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where comment in DEFAULT_COMMENT or UPDATED_COMMENT
        defaultAssetShouldBeFound("comment.in=" + DEFAULT_COMMENT + "," + UPDATED_COMMENT);

        // Get all the assetList where comment equals to UPDATED_COMMENT
        defaultAssetShouldNotBeFound("comment.in=" + UPDATED_COMMENT);
    }

    @Test
    @Transactional
    public void getAllAssetsByCommentIsNullOrNotNull() throws Exception {
        // Initialize the database
        assetRepository.saveAndFlush(asset);

        // Get all the assetList where comment is not null
        defaultAssetShouldBeFound("comment.specified=true");

        // Get all the assetList where comment is null
        defaultAssetShouldNotBeFound("comment.specified=false");
    }

    @Test
    @Transactional
    public void getAllAssetsByMemberIsEqualToSomething() throws Exception {
        // Initialize the database
        Membership member = MembershipResourceIntTest.createEntity(em, CustomerResourceIntTest.createAnotherEntity(em));
        em.persist(member);
        em.flush();
        asset.setMember(member);
        assetRepository.saveAndFlush(asset);
        Long memberId = member.getId();

        // Get all the assetList where member equals to memberId
        defaultAssetShouldBeFound("memberId.equals=" + memberId);

        // Get all the assetList where member equals to memberId + 1
        defaultAssetShouldNotBeFound("memberId.equals=" + (memberId + 1));
    }

    /**
     * Executes the search, and checks that the default entity is returned
     */
    private void defaultAssetShouldBeFound(String filter) throws Exception {
        restAssetMockMvc.perform(get("/api/assets?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(asset.getId().intValue())))
            .andExpect(jsonPath("$.[*].date").value(hasItem(DEFAULT_DATE.toString())))
            .andExpect(jsonPath("$.[*].action").value(hasItem(DEFAULT_ACTION.toString())))
            .andExpect(jsonPath("$.[*].amount").value(hasItem(DEFAULT_AMOUNT.intValue())))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT)));

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
            .date(UPDATED_DATE)
            .action(UPDATED_ACTION)
            .amount(UPDATED_AMOUNT)
            .comment(UPDATED_COMMENT);
        AssetDTO assetDTO = assetMapper.toDto(updatedAsset);

        restAssetMockMvc.perform(put("/api/assets")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(assetDTO)))
            .andExpect(status().isOk());

        // Validate the Asset in the database
        List<Asset> assetList = assetRepository.findAll();
        assertThat(assetList).hasSize(databaseSizeBeforeUpdate);
        Asset testAsset = assetList.get(assetList.size() - 1);
        assertThat(testAsset.getDate()).isEqualTo(UPDATED_DATE);
        assertThat(testAsset.getAction()).isEqualTo(UPDATED_ACTION);
        assertThat(testAsset.getAmount()).isEqualTo(UPDATED_AMOUNT);
        assertThat(testAsset.getComment()).isEqualTo(UPDATED_COMMENT);
    }

    @Test
    @Transactional
    public void updateNonExistingAsset() throws Exception {
        int databaseSizeBeforeUpdate = assetRepository.findAll().size();

        // Create the Asset
        AssetDTO assetDTO = assetMapper.toDto(asset);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAssetMockMvc.perform(put("/api/assets")
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
        restAssetMockMvc.perform(delete("/api/assets/{id}", asset.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Asset> assetList = assetRepository.findAll();
        assertThat(assetList).hasSize(databaseSizeBeforeDelete - 1);
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
