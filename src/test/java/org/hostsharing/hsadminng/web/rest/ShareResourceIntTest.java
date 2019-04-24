package org.hostsharing.hsadminng.web.rest;

import org.hostsharing.hsadminng.HsadminNgApp;

import org.hostsharing.hsadminng.domain.Share;
import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.repository.ShareRepository;
import org.hostsharing.hsadminng.service.ShareService;
import org.hostsharing.hsadminng.service.dto.ShareDTO;
import org.hostsharing.hsadminng.service.mapper.ShareMapper;
import org.hostsharing.hsadminng.web.rest.errors.ExceptionTranslator;
import org.hostsharing.hsadminng.service.dto.ShareCriteria;
import org.hostsharing.hsadminng.service.ShareQueryService;

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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;


import static org.hostsharing.hsadminng.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.hostsharing.hsadminng.domain.enumeration.ShareAction;
/**
 * Test class for the ShareResource REST controller.
 *
 * @see ShareResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = HsadminNgApp.class)
public class ShareResourceIntTest {

    private static final LocalDate DEFAULT_DOCUMENT_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DOCUMENT_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_VALUE_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_VALUE_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final ShareAction DEFAULT_ACTION = ShareAction.SUBSCRIPTION;
    private static final ShareAction UPDATED_ACTION = ShareAction.CANCELLATION;

    private static final Integer DEFAULT_QUANTITY = 1;
    private static final Integer UPDATED_QUANTITY = 2;

    private static final String DEFAULT_REMARK = "AAAAAAAAAA";
    private static final String UPDATED_REMARK = "BBBBBBBBBB";

    @Autowired
    private ShareRepository shareRepository;

    @Autowired
    private ShareMapper shareMapper;

    @Autowired
    private ShareService shareService;

    @Autowired
    private ShareQueryService shareQueryService;

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

    private MockMvc restShareMockMvc;

    private Share share;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final ShareResource shareResource = new ShareResource(shareService, shareQueryService);
        this.restShareMockMvc = MockMvcBuilders.standaloneSetup(shareResource)
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
    public static Share createEntity(EntityManager em) {
        Share share = new Share()
            .documentDate(DEFAULT_DOCUMENT_DATE)
            .valueDate(DEFAULT_VALUE_DATE)
            .action(DEFAULT_ACTION)
            .quantity(DEFAULT_QUANTITY)
            .remark(DEFAULT_REMARK);
        // Add required entity
        Membership membership = MembershipResourceIntTest.createEntity(em);
        em.persist(membership);
        em.flush();
        share.setMembership(membership);
        return share;
    }

    /**
     * Create a persistent entity related to the given persistent membership for testing purposes.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Share createPersistentEntity(EntityManager em, final Membership membership) {
        Share share = new Share()
            .documentDate(DEFAULT_DOCUMENT_DATE)
            .valueDate(DEFAULT_VALUE_DATE)
            .action(DEFAULT_ACTION)
            .quantity(DEFAULT_QUANTITY)
            .remark(DEFAULT_REMARK);
        // Add required entity
        share.setMembership(membership);
        membership.addShare(share);
        em.persist(share);
        em.flush();
        return share;
    }

    @Before
    public void initTest() {
        share = createEntity(em);
    }

    @Test
    @Transactional
    public void createShare() throws Exception {
        int databaseSizeBeforeCreate = shareRepository.findAll().size();

        // Create the Share
        ShareDTO shareDTO = shareMapper.toDto(share);
        restShareMockMvc.perform(post("/api/shares")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(shareDTO)))
            .andExpect(status().isCreated());

        // Validate the Share in the database
        List<Share> shareList = shareRepository.findAll();
        assertThat(shareList).hasSize(databaseSizeBeforeCreate + 1);
        Share testShare = shareList.get(shareList.size() - 1);
        assertThat(testShare.getDocumentDate()).isEqualTo(DEFAULT_DOCUMENT_DATE);
        assertThat(testShare.getValueDate()).isEqualTo(DEFAULT_VALUE_DATE);
        assertThat(testShare.getAction()).isEqualTo(DEFAULT_ACTION);
        assertThat(testShare.getQuantity()).isEqualTo(DEFAULT_QUANTITY);
        assertThat(testShare.getRemark()).isEqualTo(DEFAULT_REMARK);
    }

    @Test
    @Transactional
    public void createShareWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = shareRepository.findAll().size();

        // Create the Share with an existing ID
        share.setId(1L);
        ShareDTO shareDTO = shareMapper.toDto(share);

        // An entity with an existing ID cannot be created, so this API call must fail
        restShareMockMvc.perform(post("/api/shares")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(shareDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Share in the database
        List<Share> shareList = shareRepository.findAll();
        assertThat(shareList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkDocumentDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = shareRepository.findAll().size();
        // set the field null
        share.setDocumentDate(null);

        // Create the Share, which fails.
        ShareDTO shareDTO = shareMapper.toDto(share);

        restShareMockMvc.perform(post("/api/shares")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(shareDTO)))
            .andExpect(status().isBadRequest());

        List<Share> shareList = shareRepository.findAll();
        assertThat(shareList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkValueDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = shareRepository.findAll().size();
        // set the field null
        share.setValueDate(null);

        // Create the Share, which fails.
        ShareDTO shareDTO = shareMapper.toDto(share);

        restShareMockMvc.perform(post("/api/shares")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(shareDTO)))
            .andExpect(status().isBadRequest());

        List<Share> shareList = shareRepository.findAll();
        assertThat(shareList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkActionIsRequired() throws Exception {
        int databaseSizeBeforeTest = shareRepository.findAll().size();
        // set the field null
        share.setAction(null);

        // Create the Share, which fails.
        ShareDTO shareDTO = shareMapper.toDto(share);

        restShareMockMvc.perform(post("/api/shares")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(shareDTO)))
            .andExpect(status().isBadRequest());

        List<Share> shareList = shareRepository.findAll();
        assertThat(shareList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkQuantityIsRequired() throws Exception {
        int databaseSizeBeforeTest = shareRepository.findAll().size();
        // set the field null
        share.setQuantity(null);

        // Create the Share, which fails.
        ShareDTO shareDTO = shareMapper.toDto(share);

        restShareMockMvc.perform(post("/api/shares")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(shareDTO)))
            .andExpect(status().isBadRequest());

        List<Share> shareList = shareRepository.findAll();
        assertThat(shareList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllShares() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList
        restShareMockMvc.perform(get("/api/shares?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(share.getId().intValue())))
            .andExpect(jsonPath("$.[*].documentDate").value(hasItem(DEFAULT_DOCUMENT_DATE.toString())))
            .andExpect(jsonPath("$.[*].valueDate").value(hasItem(DEFAULT_VALUE_DATE.toString())))
            .andExpect(jsonPath("$.[*].action").value(hasItem(DEFAULT_ACTION.toString())))
            .andExpect(jsonPath("$.[*].quantity").value(hasItem(DEFAULT_QUANTITY)))
            .andExpect(jsonPath("$.[*].remark").value(hasItem(DEFAULT_REMARK.toString())));
    }
    
    @Test
    @Transactional
    public void getShare() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get the share
        restShareMockMvc.perform(get("/api/shares/{id}", share.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(share.getId().intValue()))
            .andExpect(jsonPath("$.documentDate").value(DEFAULT_DOCUMENT_DATE.toString()))
            .andExpect(jsonPath("$.valueDate").value(DEFAULT_VALUE_DATE.toString()))
            .andExpect(jsonPath("$.action").value(DEFAULT_ACTION.toString()))
            .andExpect(jsonPath("$.quantity").value(DEFAULT_QUANTITY))
            .andExpect(jsonPath("$.remark").value(DEFAULT_REMARK.toString()));
    }

    @Test
    @Transactional
    public void getAllSharesByDocumentDateIsEqualToSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where documentDate equals to DEFAULT_DOCUMENT_DATE
        defaultShareShouldBeFound("documentDate.equals=" + DEFAULT_DOCUMENT_DATE);

        // Get all the shareList where documentDate equals to UPDATED_DOCUMENT_DATE
        defaultShareShouldNotBeFound("documentDate.equals=" + UPDATED_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllSharesByDocumentDateIsInShouldWork() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where documentDate in DEFAULT_DOCUMENT_DATE or UPDATED_DOCUMENT_DATE
        defaultShareShouldBeFound("documentDate.in=" + DEFAULT_DOCUMENT_DATE + "," + UPDATED_DOCUMENT_DATE);

        // Get all the shareList where documentDate equals to UPDATED_DOCUMENT_DATE
        defaultShareShouldNotBeFound("documentDate.in=" + UPDATED_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllSharesByDocumentDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where documentDate is not null
        defaultShareShouldBeFound("documentDate.specified=true");

        // Get all the shareList where documentDate is null
        defaultShareShouldNotBeFound("documentDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllSharesByDocumentDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where documentDate greater than or equals to DEFAULT_DOCUMENT_DATE
        defaultShareShouldBeFound("documentDate.greaterOrEqualThan=" + DEFAULT_DOCUMENT_DATE);

        // Get all the shareList where documentDate greater than or equals to UPDATED_DOCUMENT_DATE
        defaultShareShouldNotBeFound("documentDate.greaterOrEqualThan=" + UPDATED_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllSharesByDocumentDateIsLessThanSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where documentDate less than or equals to DEFAULT_DOCUMENT_DATE
        defaultShareShouldNotBeFound("documentDate.lessThan=" + DEFAULT_DOCUMENT_DATE);

        // Get all the shareList where documentDate less than or equals to UPDATED_DOCUMENT_DATE
        defaultShareShouldBeFound("documentDate.lessThan=" + UPDATED_DOCUMENT_DATE);
    }


    @Test
    @Transactional
    public void getAllSharesByValueDateIsEqualToSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where valueDate equals to DEFAULT_VALUE_DATE
        defaultShareShouldBeFound("valueDate.equals=" + DEFAULT_VALUE_DATE);

        // Get all the shareList where valueDate equals to UPDATED_VALUE_DATE
        defaultShareShouldNotBeFound("valueDate.equals=" + UPDATED_VALUE_DATE);
    }

    @Test
    @Transactional
    public void getAllSharesByValueDateIsInShouldWork() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where valueDate in DEFAULT_VALUE_DATE or UPDATED_VALUE_DATE
        defaultShareShouldBeFound("valueDate.in=" + DEFAULT_VALUE_DATE + "," + UPDATED_VALUE_DATE);

        // Get all the shareList where valueDate equals to UPDATED_VALUE_DATE
        defaultShareShouldNotBeFound("valueDate.in=" + UPDATED_VALUE_DATE);
    }

    @Test
    @Transactional
    public void getAllSharesByValueDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where valueDate is not null
        defaultShareShouldBeFound("valueDate.specified=true");

        // Get all the shareList where valueDate is null
        defaultShareShouldNotBeFound("valueDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllSharesByValueDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where valueDate greater than or equals to DEFAULT_VALUE_DATE
        defaultShareShouldBeFound("valueDate.greaterOrEqualThan=" + DEFAULT_VALUE_DATE);

        // Get all the shareList where valueDate greater than or equals to UPDATED_VALUE_DATE
        defaultShareShouldNotBeFound("valueDate.greaterOrEqualThan=" + UPDATED_VALUE_DATE);
    }

    @Test
    @Transactional
    public void getAllSharesByValueDateIsLessThanSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where valueDate less than or equals to DEFAULT_VALUE_DATE
        defaultShareShouldNotBeFound("valueDate.lessThan=" + DEFAULT_VALUE_DATE);

        // Get all the shareList where valueDate less than or equals to UPDATED_VALUE_DATE
        defaultShareShouldBeFound("valueDate.lessThan=" + UPDATED_VALUE_DATE);
    }


    @Test
    @Transactional
    public void getAllSharesByActionIsEqualToSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where action equals to DEFAULT_ACTION
        defaultShareShouldBeFound("action.equals=" + DEFAULT_ACTION);

        // Get all the shareList where action equals to UPDATED_ACTION
        defaultShareShouldNotBeFound("action.equals=" + UPDATED_ACTION);
    }

    @Test
    @Transactional
    public void getAllSharesByActionIsInShouldWork() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where action in DEFAULT_ACTION or UPDATED_ACTION
        defaultShareShouldBeFound("action.in=" + DEFAULT_ACTION + "," + UPDATED_ACTION);

        // Get all the shareList where action equals to UPDATED_ACTION
        defaultShareShouldNotBeFound("action.in=" + UPDATED_ACTION);
    }

    @Test
    @Transactional
    public void getAllSharesByActionIsNullOrNotNull() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where action is not null
        defaultShareShouldBeFound("action.specified=true");

        // Get all the shareList where action is null
        defaultShareShouldNotBeFound("action.specified=false");
    }

    @Test
    @Transactional
    public void getAllSharesByQuantityIsEqualToSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where quantity equals to DEFAULT_QUANTITY
        defaultShareShouldBeFound("quantity.equals=" + DEFAULT_QUANTITY);

        // Get all the shareList where quantity equals to UPDATED_QUANTITY
        defaultShareShouldNotBeFound("quantity.equals=" + UPDATED_QUANTITY);
    }

    @Test
    @Transactional
    public void getAllSharesByQuantityIsInShouldWork() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where quantity in DEFAULT_QUANTITY or UPDATED_QUANTITY
        defaultShareShouldBeFound("quantity.in=" + DEFAULT_QUANTITY + "," + UPDATED_QUANTITY);

        // Get all the shareList where quantity equals to UPDATED_QUANTITY
        defaultShareShouldNotBeFound("quantity.in=" + UPDATED_QUANTITY);
    }

    @Test
    @Transactional
    public void getAllSharesByQuantityIsNullOrNotNull() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where quantity is not null
        defaultShareShouldBeFound("quantity.specified=true");

        // Get all the shareList where quantity is null
        defaultShareShouldNotBeFound("quantity.specified=false");
    }

    @Test
    @Transactional
    public void getAllSharesByQuantityIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where quantity greater than or equals to DEFAULT_QUANTITY
        defaultShareShouldBeFound("quantity.greaterOrEqualThan=" + DEFAULT_QUANTITY);

        // Get all the shareList where quantity greater than or equals to UPDATED_QUANTITY
        defaultShareShouldNotBeFound("quantity.greaterOrEqualThan=" + UPDATED_QUANTITY);
    }

    @Test
    @Transactional
    public void getAllSharesByQuantityIsLessThanSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where quantity less than or equals to DEFAULT_QUANTITY
        defaultShareShouldNotBeFound("quantity.lessThan=" + DEFAULT_QUANTITY);

        // Get all the shareList where quantity less than or equals to UPDATED_QUANTITY
        defaultShareShouldBeFound("quantity.lessThan=" + UPDATED_QUANTITY);
    }


    @Test
    @Transactional
    public void getAllSharesByRemarkIsEqualToSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where remark equals to DEFAULT_REMARK
        defaultShareShouldBeFound("remark.equals=" + DEFAULT_REMARK);

        // Get all the shareList where remark equals to UPDATED_REMARK
        defaultShareShouldNotBeFound("remark.equals=" + UPDATED_REMARK);
    }

    @Test
    @Transactional
    public void getAllSharesByRemarkIsInShouldWork() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where remark in DEFAULT_REMARK or UPDATED_REMARK
        defaultShareShouldBeFound("remark.in=" + DEFAULT_REMARK + "," + UPDATED_REMARK);

        // Get all the shareList where remark equals to UPDATED_REMARK
        defaultShareShouldNotBeFound("remark.in=" + UPDATED_REMARK);
    }

    @Test
    @Transactional
    public void getAllSharesByRemarkIsNullOrNotNull() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where remark is not null
        defaultShareShouldBeFound("remark.specified=true");

        // Get all the shareList where remark is null
        defaultShareShouldNotBeFound("remark.specified=false");
    }

    @Test
    @Transactional
    public void getAllSharesByMembershipIsEqualToSomething() throws Exception {
        // Initialize the database
        Membership membership = MembershipResourceIntTest.createPersistentEntity(em, CustomerResourceIntTest.createPersistentEntity(em));
        share.setMembership(membership);
        shareRepository.saveAndFlush(share);
        Long membershipId = membership.getId();

        // Get all the shareList where membership equals to membershipId
        defaultShareShouldBeFound("membershipId.equals=" + membershipId);

        // Get all the shareList where membership equals to membershipId + 1
        defaultShareShouldNotBeFound("membershipId.equals=" + (membershipId + 1));
    }

    /**
     * Executes the search, and checks that the default entity is returned
     */
    private void defaultShareShouldBeFound(String filter) throws Exception {
        restShareMockMvc.perform(get("/api/shares?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(share.getId().intValue())))
            .andExpect(jsonPath("$.[*].documentDate").value(hasItem(DEFAULT_DOCUMENT_DATE.toString())))
            .andExpect(jsonPath("$.[*].valueDate").value(hasItem(DEFAULT_VALUE_DATE.toString())))
            .andExpect(jsonPath("$.[*].action").value(hasItem(DEFAULT_ACTION.toString())))
            .andExpect(jsonPath("$.[*].quantity").value(hasItem(DEFAULT_QUANTITY)))
            .andExpect(jsonPath("$.[*].remark").value(hasItem(DEFAULT_REMARK)));

        // Check, that the count call also returns 1
        restShareMockMvc.perform(get("/api/shares/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned
     */
    private void defaultShareShouldNotBeFound(String filter) throws Exception {
        restShareMockMvc.perform(get("/api/shares?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restShareMockMvc.perform(get("/api/shares/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(content().string("0"));
    }


    @Test
    @Transactional
    public void getNonExistingShare() throws Exception {
        // Get the share
        restShareMockMvc.perform(get("/api/shares/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateShare() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        int databaseSizeBeforeUpdate = shareRepository.findAll().size();

        // Update the share
        Share updatedShare = shareRepository.findById(share.getId()).get();
        // Disconnect from session so that the updates on updatedShare are not directly saved in db
        em.detach(updatedShare);
        updatedShare
            .documentDate(UPDATED_DOCUMENT_DATE)
            .valueDate(UPDATED_VALUE_DATE)
            .action(UPDATED_ACTION)
            .quantity(UPDATED_QUANTITY)
            .remark(UPDATED_REMARK);
        ShareDTO shareDTO = shareMapper.toDto(updatedShare);

        restShareMockMvc.perform(put("/api/shares")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(shareDTO)))
            .andExpect(status().isBadRequest());

        // Validate the database is unchanged
        List<Share> shareList = shareRepository.findAll();
        assertThat(shareList).hasSize(databaseSizeBeforeUpdate);

        Share testShare = shareList.get(shareList.size() - 1);
        assertThat(testShare.getDocumentDate()).isEqualTo(DEFAULT_DOCUMENT_DATE);
        assertThat(testShare.getValueDate()).isEqualTo(DEFAULT_VALUE_DATE);
        assertThat(testShare.getAction()).isEqualTo(DEFAULT_ACTION);
        assertThat(testShare.getQuantity()).isEqualTo(DEFAULT_QUANTITY);
        assertThat(testShare.getRemark()).isEqualTo(DEFAULT_REMARK);
    }

    @Test
    @Transactional
    public void updateNonExistingShare() throws Exception {
        int databaseSizeBeforeUpdate = shareRepository.findAll().size();

        // Create the Share
        ShareDTO shareDTO = shareMapper.toDto(share);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restShareMockMvc.perform(put("/api/shares")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(shareDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Share in the database
        List<Share> shareList = shareRepository.findAll();
        assertThat(shareList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteShare() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        int databaseSizeBeforeDelete = shareRepository.findAll().size();

        // Delete the share
        restShareMockMvc.perform(delete("/api/shares/{id}", share.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest());

        // Validate the database is unchanged
        List<Share> shareList = shareRepository.findAll();
        assertThat(shareList).hasSize(databaseSizeBeforeDelete);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Share.class);
        Share share1 = new Share();
        share1.setId(1L);
        Share share2 = new Share();
        share2.setId(share1.getId());
        assertThat(share1).isEqualTo(share2);
        share2.setId(2L);
        assertThat(share1).isNotEqualTo(share2);
        share1.setId(null);
        assertThat(share1).isNotEqualTo(share2);
    }

    @Test
    @Transactional
    public void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ShareDTO.class);
        ShareDTO shareDTO1 = new ShareDTO();
        shareDTO1.setId(1L);
        ShareDTO shareDTO2 = new ShareDTO();
        assertThat(shareDTO1).isNotEqualTo(shareDTO2);
        shareDTO2.setId(shareDTO1.getId());
        assertThat(shareDTO1).isEqualTo(shareDTO2);
        shareDTO2.setId(2L);
        assertThat(shareDTO1).isNotEqualTo(shareDTO2);
        shareDTO1.setId(null);
        assertThat(shareDTO1).isNotEqualTo(shareDTO2);
    }

    @Test
    @Transactional
    public void testEntityFromId() {
        assertThat(shareMapper.fromId(42L).getId()).isEqualTo(42);
        assertThat(shareMapper.fromId(null)).isNull();
    }
}
