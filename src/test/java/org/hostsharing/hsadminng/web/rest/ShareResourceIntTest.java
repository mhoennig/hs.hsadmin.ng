package org.hostsharing.hsadminng.web.rest;

import org.hostsharing.hsadminng.HsadminNgApp;
import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.domain.Share;
import org.hostsharing.hsadminng.domain.enumeration.ShareAction;
import org.hostsharing.hsadminng.repository.ShareRepository;
import org.hostsharing.hsadminng.service.ShareQueryService;
import org.hostsharing.hsadminng.service.ShareService;
import org.hostsharing.hsadminng.service.dto.ShareDTO;
import org.hostsharing.hsadminng.service.mapper.ShareMapper;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hostsharing.hsadminng.web.rest.TestUtil.createFormattingConversionService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the ShareResource REST controller.
 *
 * @see ShareResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = HsadminNgApp.class)
public class ShareResourceIntTest {

    private static final LocalDate DEFAULT_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final ShareAction DEFAULT_ACTION = ShareAction.SUBSCRIPTION;
    private static final ShareAction UPDATED_ACTION = ShareAction.CANCELLATION;

    private static final Integer DEFAULT_QUANTITY = 2;
    private static final Integer UPDATED_QUANTITY = 3;

    private static final String DEFAULT_COMMENT = "Some Comment";
    private static final String UPDATED_COMMENT = "Updated Comment";

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
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Share createEntity(final EntityManager em, final Membership membership) {
        em.persist(membership);
        Share share = new Share()
            .member(membership)
            .date(DEFAULT_DATE)
            .action(DEFAULT_ACTION)
            .quantity(DEFAULT_QUANTITY)
            .comment(DEFAULT_COMMENT);
        return share;
    }

    @Before
    public void initTest() {
        share = createEntity(em, MembershipResourceIntTest.createEntity(em));
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
        assertThat(testShare.getDate()).isEqualTo(DEFAULT_DATE);
        assertThat(testShare.getAction()).isEqualTo(DEFAULT_ACTION);
        assertThat(testShare.getQuantity()).isEqualTo(DEFAULT_QUANTITY);
        assertThat(testShare.getComment()).isEqualTo(DEFAULT_COMMENT);
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
    public void checkDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = shareRepository.findAll().size();
        // set the field null
        share.setDate(null);

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
            .andExpect(jsonPath("$.[*].date").value(hasItem(DEFAULT_DATE.toString())))
            .andExpect(jsonPath("$.[*].action").value(hasItem(DEFAULT_ACTION.toString())))
            .andExpect(jsonPath("$.[*].quantity").value(hasItem(DEFAULT_QUANTITY)))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT.toString())));
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
            .andExpect(jsonPath("$.date").value(DEFAULT_DATE.toString()))
            .andExpect(jsonPath("$.action").value(DEFAULT_ACTION.toString()))
            .andExpect(jsonPath("$.quantity").value(DEFAULT_QUANTITY))
            .andExpect(jsonPath("$.comment").value(DEFAULT_COMMENT.toString()));
    }

    @Test
    @Transactional
    public void getAllSharesByDateIsEqualToSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where date equals to DEFAULT_DATE
        shouldFindDefaultShare("date.equals=" + DEFAULT_DATE);

        // Get all the shareList where date equals to UPDATED_DATE
        shouldNotFindAnyShare("date.equals=" + UPDATED_DATE);
    }

    @Test
    @Transactional
    public void getAllSharesByDateIsInShouldWork() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where date in DEFAULT_DATE or UPDATED_DATE
        shouldFindDefaultShare("date.in=" + DEFAULT_DATE + "," + UPDATED_DATE);

        // Get all the shareList where date equals to UPDATED_DATE
        shouldNotFindAnyShare("date.in=" + UPDATED_DATE);
    }

    @Test
    @Transactional
    public void getAllSharesByDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where date is not null
        shouldFindDefaultShare("date.specified=true");

        // Get all the shareList where date is null
        shouldNotFindAnyShare("date.specified=false");
    }

    @Test
    @Transactional
    public void getAllSharesByDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where date greater than or equals to DEFAULT_DATE
        shouldFindDefaultShare("date.greaterOrEqualThan=" + DEFAULT_DATE);

        // Get all the shareList where date greater than or equals to UPDATED_DATE
        shouldNotFindAnyShare("date.greaterOrEqualThan=" + UPDATED_DATE);
    }

    @Test
    @Transactional
    public void getAllSharesByDateIsLessThanSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where date less than or equals to DEFAULT_DATE
        shouldNotFindAnyShare("date.lessThan=" + DEFAULT_DATE);

        // Get all the shareList where date less than or equals to UPDATED_DATE
        shouldFindDefaultShare("date.lessThan=" + UPDATED_DATE);
    }


    @Test
    @Transactional
    public void getAllSharesByActionIsEqualToSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where action equals to DEFAULT_ACTION
        shouldFindDefaultShare("action.equals=" + DEFAULT_ACTION);

        // Get all the shareList where action equals to UPDATED_ACTION
        shouldNotFindAnyShare("action.equals=" + UPDATED_ACTION);
    }

    @Test
    @Transactional
    public void getAllSharesByActionIsInShouldWork() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where action in DEFAULT_ACTION or UPDATED_ACTION
        shouldFindDefaultShare("action.in=" + DEFAULT_ACTION + "," + UPDATED_ACTION);

        // Get all the shareList where action equals to UPDATED_ACTION
        shouldNotFindAnyShare("action.in=" + UPDATED_ACTION);
    }

    @Test
    @Transactional
    public void getAllSharesByActionIsNullOrNotNull() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where action is not null
        shouldFindDefaultShare("action.specified=true");

        // Get all the shareList where action is null
        shouldNotFindAnyShare("action.specified=false");
    }

    @Test
    @Transactional
    public void getAllSharesByQuantityIsEqualToSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where quantity equals to DEFAULT_QUANTITY
        shouldFindDefaultShare("quantity.equals=" + DEFAULT_QUANTITY);

        // Get all the shareList where quantity is not in database
        shouldNotFindAnyShare("quantity.equals=" + (DEFAULT_QUANTITY + 1));
        shouldNotFindAnyShare("quantity.equals=" + (-DEFAULT_QUANTITY));
    }

    @Test
    @Transactional
    public void getAllSharesByQuantityIsInShouldWork() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where quantity in DEFAULT_QUANTITY or UPDATED_QUANTITY
        shouldFindDefaultShare("quantity.in=" + DEFAULT_QUANTITY + "," + (-DEFAULT_QUANTITY));

        // Get all the shareList where quantity equals to UPDATED_QUANTITY
        shouldNotFindAnyShare("quantity.in=" + (DEFAULT_QUANTITY + 1));
    }

    @Test
    @Transactional
    public void getAllSharesByQuantityIsNullOrNotNull() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where quantity is not null
        shouldFindDefaultShare("quantity.specified=true");

        // Get all the shareList where quantity is null
        shouldNotFindAnyShare("quantity.specified=false");
    }

    @Test
    @Transactional
    public void getAllSharesByQuantityIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where quantity greater than or equals to DEFAULT_QUANTITY
        shouldFindDefaultShare("quantity.greaterOrEqualThan=" + DEFAULT_QUANTITY);

        // Get all the shareList where quantity greater than or equals to DEFAULT_QUANTITY+1
        shouldNotFindAnyShare("quantity.greaterOrEqualThan=" + (DEFAULT_QUANTITY + 1));
    }

    @Test
    @Transactional
    public void getAllSharesByQuantityIsLessThanSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where quantity less than or equals to DEFAULT_QUANTITY
        shouldNotFindAnyShare("quantity.lessThan=" + DEFAULT_QUANTITY);

        // Get all the shareList where quantity less than or equals to DEFAULT_QUANTITY-1
        shouldFindDefaultShare("quantity.lessThan=" + (DEFAULT_QUANTITY + 1));
    }


    @Test
    @Transactional
    public void getAllSharesByCommentIsEqualToSomething() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where comment equals to DEFAULT_COMMENT
        shouldFindDefaultShare("comment.equals=" + DEFAULT_COMMENT);

        // Get all the shareList where comment equals to UPDATED_COMMENT
        shouldNotFindAnyShare("comment.equals=" + UPDATED_COMMENT);
    }

    @Test
    @Transactional
    public void getAllSharesByCommentIsInShouldWork() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where comment in DEFAULT_COMMENT or UPDATED_COMMENT
        shouldFindDefaultShare("comment.in=" + DEFAULT_COMMENT + "," + UPDATED_COMMENT);

        // Get all the shareList where comment equals to UPDATED_COMMENT
        shouldNotFindAnyShare("comment.in=" + UPDATED_COMMENT);
    }

    @Test
    @Transactional
    public void getAllSharesByCommentIsNullOrNotNull() throws Exception {
        // Initialize the database
        shareRepository.saveAndFlush(share);

        // Get all the shareList where comment is not null
        shouldFindDefaultShare("comment.specified=true");

        // Get all the shareList where comment is null
        shouldNotFindAnyShare("comment.specified=false");
    }

    @Test
    @Transactional
    public void getAllSharesByMemberIsEqualToSomething() throws Exception {
        // Initialize the database
        Membership member = MembershipResourceIntTest.createEntity(em);
        em.persist(member);
        em.flush();
        share.setMember(member);
        shareRepository.saveAndFlush(share);
        Long memberId = member.getId();

        // Get all the shareList where member equals to memberId
        shouldFindDefaultShare("memberId.equals=" + memberId);

        // Get all the shareList where member equals to memberId + 1
        shouldNotFindAnyShare("memberId.equals=" + (memberId + 1));
    }

    /**
     * Executes the search, and checks that the default entity is returned
     */
    private void shouldFindDefaultShare(String filter) throws Exception {
        restShareMockMvc.perform(get("/api/shares?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(share.getId().intValue())))
            .andExpect(jsonPath("$.[*].date").value(hasItem(DEFAULT_DATE.toString())))
            .andExpect(jsonPath("$.[*].action").value(hasItem(DEFAULT_ACTION.toString())))
            .andExpect(jsonPath("$.[*].quantity").value(hasItem(DEFAULT_QUANTITY)))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT)));

        // Check, that the count call also returns 1
        restShareMockMvc.perform(get("/api/shares/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned
     */
    private void shouldNotFindAnyShare(String filter) throws Exception {
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
            .date(UPDATED_DATE)
            .action(UPDATED_ACTION)
            .quantity(UPDATED_QUANTITY)
            .comment(UPDATED_COMMENT);
        ShareDTO shareDTO = shareMapper.toDto(updatedShare);

        restShareMockMvc.perform(put("/api/shares")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(shareDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Share in the database
        List<Share> shareList = shareRepository.findAll();
        assertThat(shareList).hasSize(databaseSizeBeforeUpdate);
        Share testShare = shareList.get(shareList.size() - 1);
        assertThat(testShare.getDate()).isEqualTo(DEFAULT_DATE);
        assertThat(testShare.getAction()).isEqualTo(DEFAULT_ACTION);
        assertThat(testShare.getQuantity()).isEqualTo(DEFAULT_QUANTITY);
        assertThat(testShare.getComment()).isEqualTo(DEFAULT_COMMENT);
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

        // Validate the share is still in the database
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
