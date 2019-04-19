package org.hostsharing.hsadminng.web.rest;

import org.hostsharing.hsadminng.HsadminNgApp;
import org.hostsharing.hsadminng.domain.Asset;
import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.domain.Share;
import org.hostsharing.hsadminng.repository.MembershipRepository;
import org.hostsharing.hsadminng.service.MembershipQueryService;
import org.hostsharing.hsadminng.service.MembershipService;
import org.hostsharing.hsadminng.service.dto.MembershipDTO;
import org.hostsharing.hsadminng.service.mapper.MembershipMapper;
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
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hostsharing.hsadminng.web.rest.TestUtil.createFormattingConversionService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the MembershipResource REST controller.
 *
 * @see MembershipResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = HsadminNgApp.class)
public class MembershipResourceIntTest {

    private static final LocalDate DEFAULT_DOCUMENT_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate UPDATED_DOCUMENT_DATE = DEFAULT_DOCUMENT_DATE.plusDays(1);

    private static final LocalDate DEFAULT_MEMBER_FROM = DEFAULT_DOCUMENT_DATE.plusDays(2);
    private static final LocalDate UPDATED_MEMBER_FROM = UPDATED_DOCUMENT_DATE.plusDays(8);

    private static final LocalDate DEFAULT_MEMBER_UNTIL = DEFAULT_MEMBER_FROM.plusYears(1).withMonth(12).withDayOfMonth(31);
    private static final LocalDate UPDATED_MEMBER_UNTIL = UPDATED_MEMBER_FROM.plusYears(7).withMonth(12).withDayOfMonth(31);

    private static final String DEFAULT_REMARK = "AAAAAAAAAA";
    private static final String UPDATED_REMARK = "BBBBBBBBBB";

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private MembershipMapper membershipMapper;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private MembershipQueryService membershipQueryService;

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

    private MockMvc restMembershipMockMvc;

    private Membership membership;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final MembershipResource membershipResource = new MembershipResource(membershipService, membershipQueryService);
        this.restMembershipMockMvc = MockMvcBuilders.standaloneSetup(membershipResource)
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
    public static Membership createEntity(EntityManager em) {
        Membership membership = new Membership()
            .documentDate(DEFAULT_DOCUMENT_DATE)
            .memberFrom(DEFAULT_MEMBER_FROM)
            .memberUntil(DEFAULT_MEMBER_UNTIL)
            .remark(DEFAULT_REMARK);
        // Add required entity
        Customer customer = CustomerResourceIntTest.createEntity(em);
        em.persist(customer);
        em.flush();
        membership.setCustomer(customer);
        return membership;
    }

    /**
     * Create an entity for tests for a specific customer.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Membership createPersistentEntity(EntityManager em, final Customer customer) {
        Membership membership = new Membership()
            .documentDate(DEFAULT_DOCUMENT_DATE)
            .memberFrom(DEFAULT_MEMBER_FROM)
            .memberUntil(DEFAULT_MEMBER_UNTIL)
            .remark(DEFAULT_REMARK);
        // Add required entity
        membership.setCustomer(customer);
        em.persist(membership);
        em.flush();
        return membership;
    }

    @Before
    public void initTest() {
        membership = createEntity(em);
    }

    @Test
    @Transactional
    public void createMembership() throws Exception {
        int databaseSizeBeforeCreate = membershipRepository.findAll().size();

        // Create the Membership
        MembershipDTO membershipDTO = membershipMapper.toDto(membership);
        restMembershipMockMvc.perform(post("/api/memberships")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(membershipDTO)))
            .andExpect(status().isCreated());

        // Validate the Membership in the database
        List<Membership> membershipList = membershipRepository.findAll();
        assertThat(membershipList).hasSize(databaseSizeBeforeCreate + 1);
        Membership testMembership = membershipList.get(membershipList.size() - 1);
        assertThat(testMembership.getDocumentDate()).isEqualTo(DEFAULT_DOCUMENT_DATE);
        assertThat(testMembership.getMemberFrom()).isEqualTo(DEFAULT_MEMBER_FROM);
        assertThat(testMembership.getMemberUntil()).isEqualTo(DEFAULT_MEMBER_UNTIL);
        assertThat(testMembership.getRemark()).isEqualTo(DEFAULT_REMARK);
    }

    @Test
    @Transactional
    public void createMembershipWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = membershipRepository.findAll().size();

        // Create the Membership with an existing ID
        membership.setId(1L);
        MembershipDTO membershipDTO = membershipMapper.toDto(membership);

        // An entity with an existing ID cannot be created, so this API call must fail
        restMembershipMockMvc.perform(post("/api/memberships")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(membershipDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Membership in the database
        List<Membership> membershipList = membershipRepository.findAll();
        assertThat(membershipList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkDocumentDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = membershipRepository.findAll().size();
        // set the field null
        membership.setDocumentDate(null);

        // Create the Membership, which fails.
        MembershipDTO membershipDTO = membershipMapper.toDto(membership);

        restMembershipMockMvc.perform(post("/api/memberships")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(membershipDTO)))
            .andExpect(status().isBadRequest());

        List<Membership> membershipList = membershipRepository.findAll();
        assertThat(membershipList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkMemberFromIsRequired() throws Exception {
        int databaseSizeBeforeTest = membershipRepository.findAll().size();
        // set the field null
        membership.setMemberFrom(null);

        // Create the Membership, which fails.
        MembershipDTO membershipDTO = membershipMapper.toDto(membership);

        restMembershipMockMvc.perform(post("/api/memberships")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(membershipDTO)))
            .andExpect(status().isBadRequest());

        List<Membership> membershipList = membershipRepository.findAll();
        assertThat(membershipList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllMemberships() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList
        restMembershipMockMvc.perform(get("/api/memberships?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(membership.getId().intValue())))
            .andExpect(jsonPath("$.[*].documentDate").value(hasItem(DEFAULT_DOCUMENT_DATE.toString())))
            .andExpect(jsonPath("$.[*].memberFrom").value(hasItem(DEFAULT_MEMBER_FROM.toString())))
            .andExpect(jsonPath("$.[*].memberUntil").value(hasItem(DEFAULT_MEMBER_UNTIL.toString())))
            .andExpect(jsonPath("$.[*].remark").value(hasItem(DEFAULT_REMARK.toString())));
    }

    @Test
    @Transactional
    public void getMembership() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get the membership
        restMembershipMockMvc.perform(get("/api/memberships/{id}", membership.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(membership.getId().intValue()))
            .andExpect(jsonPath("$.documentDate").value(DEFAULT_DOCUMENT_DATE.toString()))
            .andExpect(jsonPath("$.memberFrom").value(DEFAULT_MEMBER_FROM.toString()))
            .andExpect(jsonPath("$.memberUntil").value(DEFAULT_MEMBER_UNTIL.toString()))
            .andExpect(jsonPath("$.remark").value(DEFAULT_REMARK.toString()));
    }

    @Test
    @Transactional
    public void getAllMembershipsByDocumentDateIsEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where documentDate equals to DEFAULT_DOCUMENT_DATE
        defaultMembershipShouldBeFound("documentDate.equals=" + DEFAULT_DOCUMENT_DATE);

        // Get all the membershipList where documentDate equals to UPDATED_DOCUMENT_DATE
        defaultMembershipShouldNotBeFound("documentDate.equals=" + UPDATED_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsByDocumentDateIsInShouldWork() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where documentDate in DEFAULT_DOCUMENT_DATE or UPDATED_DOCUMENT_DATE
        defaultMembershipShouldBeFound("documentDate.in=" + DEFAULT_DOCUMENT_DATE + "," + UPDATED_DOCUMENT_DATE);

        // Get all the membershipList where documentDate equals to UPDATED_DOCUMENT_DATE
        defaultMembershipShouldNotBeFound("documentDate.in=" + UPDATED_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsByDocumentDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where documentDate is not null
        defaultMembershipShouldBeFound("documentDate.specified=true");

        // Get all the membershipList where documentDate is null
        defaultMembershipShouldNotBeFound("documentDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllMembershipsByDocumentDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where documentDate greater than or equals to DEFAULT_DOCUMENT_DATE
        defaultMembershipShouldBeFound("documentDate.greaterOrEqualThan=" + DEFAULT_DOCUMENT_DATE);

        // Get all the membershipList where documentDate greater than or equals to UPDATED_DOCUMENT_DATE
        defaultMembershipShouldNotBeFound("documentDate.greaterOrEqualThan=" + UPDATED_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsByDocumentDateIsLessThanSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where documentDate less than or equals to DEFAULT_DOCUMENT_DATE
        defaultMembershipShouldNotBeFound("documentDate.lessThan=" + DEFAULT_DOCUMENT_DATE);

        // Get all the membershipList where documentDate less than or equals to UPDATED_DOCUMENT_DATE
        defaultMembershipShouldBeFound("documentDate.lessThan=" + UPDATED_DOCUMENT_DATE);
    }


    @Test
    @Transactional
    public void getAllMembershipsByMemberFromIsEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberFrom equals to DEFAULT_MEMBER_FROM
        defaultMembershipShouldBeFound("memberFrom.equals=" + DEFAULT_MEMBER_FROM);

        // Get all the membershipList where memberFrom equals to UPDATED_MEMBER_FROM
        defaultMembershipShouldNotBeFound("memberFrom.equals=" + UPDATED_MEMBER_FROM);
    }

    @Test
    @Transactional
    public void getAllMembershipsByMemberFromIsInShouldWork() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberFrom in DEFAULT_MEMBER_FROM or UPDATED_MEMBER_FROM
        defaultMembershipShouldBeFound("memberFrom.in=" + DEFAULT_MEMBER_FROM + "," + UPDATED_MEMBER_FROM);

        // Get all the membershipList where memberFrom equals to UPDATED_MEMBER_FROM
        defaultMembershipShouldNotBeFound("memberFrom.in=" + UPDATED_MEMBER_FROM);
    }

    @Test
    @Transactional
    public void getAllMembershipsByMemberFromIsNullOrNotNull() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberFrom is not null
        defaultMembershipShouldBeFound("memberFrom.specified=true");

        // Get all the membershipList where memberFrom is null
        defaultMembershipShouldNotBeFound("memberFrom.specified=false");
    }

    @Test
    @Transactional
    public void getAllMembershipsByMemberFromIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberFrom greater than or equals to DEFAULT_MEMBER_FROM
        defaultMembershipShouldBeFound("memberFrom.greaterOrEqualThan=" + DEFAULT_MEMBER_FROM);

        // Get all the membershipList where memberFrom greater than or equals to UPDATED_MEMBER_FROM
        defaultMembershipShouldNotBeFound("memberFrom.greaterOrEqualThan=" + UPDATED_MEMBER_FROM);
    }

    @Test
    @Transactional
    public void getAllMembershipsByMemberFromIsLessThanSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberFrom less than or equals to DEFAULT_MEMBER_FROM
        defaultMembershipShouldNotBeFound("memberFrom.lessThan=" + DEFAULT_MEMBER_FROM);

        // Get all the membershipList where memberFrom less than or equals to UPDATED_MEMBER_FROM
        defaultMembershipShouldBeFound("memberFrom.lessThan=" + UPDATED_MEMBER_FROM);
    }


    @Test
    @Transactional
    public void getAllMembershipsByMemberUntilIsEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberUntil equals to DEFAULT_MEMBER_UNTIL
        defaultMembershipShouldBeFound("memberUntil.equals=" + DEFAULT_MEMBER_UNTIL);

        // Get all the membershipList where memberUntil equals to UPDATED_MEMBER_UNTIL
        defaultMembershipShouldNotBeFound("memberUntil.equals=" + UPDATED_MEMBER_UNTIL);
    }

    @Test
    @Transactional
    public void getAllMembershipsByMemberUntilIsInShouldWork() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberUntil in DEFAULT_MEMBER_UNTIL or UPDATED_MEMBER_UNTIL
        defaultMembershipShouldBeFound("memberUntil.in=" + DEFAULT_MEMBER_UNTIL + "," + UPDATED_MEMBER_UNTIL);

        // Get all the membershipList where memberUntil equals to UPDATED_MEMBER_UNTIL
        defaultMembershipShouldNotBeFound("memberUntil.in=" + UPDATED_MEMBER_UNTIL);
    }

    @Test
    @Transactional
    public void getAllMembershipsByMemberUntilIsNullOrNotNull() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberUntil is not null
        defaultMembershipShouldBeFound("memberUntil.specified=true");

        // Get all the membershipList where memberUntil is null
        defaultMembershipShouldNotBeFound("memberUntil.specified=false");
    }

    @Test
    @Transactional
    public void getAllMembershipsByMemberUntilIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberUntil greater than or equals to DEFAULT_MEMBER_UNTIL
        defaultMembershipShouldBeFound("memberUntil.greaterOrEqualThan=" + DEFAULT_MEMBER_UNTIL);

        // Get all the membershipList where memberUntil greater than or equals to UPDATED_MEMBER_UNTIL
        defaultMembershipShouldNotBeFound("memberUntil.greaterOrEqualThan=" + UPDATED_MEMBER_UNTIL);
    }

    @Test
    @Transactional
    public void getAllMembershipsByMemberUntilIsLessThanSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberUntil less than or equals to DEFAULT_MEMBER_UNTIL
        defaultMembershipShouldNotBeFound("memberUntil.lessThan=" + DEFAULT_MEMBER_UNTIL);

        // Get all the membershipList where memberUntil less than or equals to UPDATED_MEMBER_UNTIL
        defaultMembershipShouldBeFound("memberUntil.lessThan=" + UPDATED_MEMBER_UNTIL);
    }


    @Test
    @Transactional
    public void getAllMembershipsByRemarkIsEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where remark equals to DEFAULT_REMARK
        defaultMembershipShouldBeFound("remark.equals=" + DEFAULT_REMARK);

        // Get all the membershipList where remark equals to UPDATED_REMARK
        defaultMembershipShouldNotBeFound("remark.equals=" + UPDATED_REMARK);
    }

    @Test
    @Transactional
    public void getAllMembershipsByRemarkIsInShouldWork() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where remark in DEFAULT_REMARK or UPDATED_REMARK
        defaultMembershipShouldBeFound("remark.in=" + DEFAULT_REMARK + "," + UPDATED_REMARK);

        // Get all the membershipList where remark equals to UPDATED_REMARK
        defaultMembershipShouldNotBeFound("remark.in=" + UPDATED_REMARK);
    }

    @Test
    @Transactional
    public void getAllMembershipsByRemarkIsNullOrNotNull() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where remark is not null
        defaultMembershipShouldBeFound("remark.specified=true");

        // Get all the membershipList where remark is null
        defaultMembershipShouldNotBeFound("remark.specified=false");
    }

    @Test
    @Transactional
    public void getAllMembershipsByShareIsEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);
        Share share = ShareResourceIntTest.createPersistentEntity(em, membership);
        membership.addShare(share);
        Long shareId = share.getId();
        em.flush();

        // Get all the membershipList where share equals to shareId
        defaultMembershipShouldBeFound("shareId.equals=" + shareId);

        // Get all the membershipList where share equals to shareId + 1
        defaultMembershipShouldNotBeFound("shareId.equals=" + (shareId + 1));
    }

    @Test
    @Transactional
    public void getAllMembershipsByAssetIsEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);
        Asset asset = AssetResourceIntTest.createPersistentEntity(em, membership);
        membership.addAsset(asset);
        em.flush();
        Long assetId = asset.getId();

        // Get all the membershipList where asset equals to assetId
        defaultMembershipShouldBeFound("assetId.equals=" + assetId);

        // Get all the membershipList where asset equals to assetId + 1
        defaultMembershipShouldNotBeFound("assetId.equals=" + (assetId + 1));
    }


    @Test
    @Transactional
    public void getAllMembershipsByCustomerIsEqualToSomething() throws Exception {
        // Initialize the database
        Customer customer = CustomerResourceIntTest.createPersistentEntity(em);
        membership.setCustomer(customer);
        membershipRepository.saveAndFlush(membership);
        Long customerId = customer.getId();

        // Get all the membershipList where customer equals to customerId
        defaultMembershipShouldBeFound("customerId.equals=" + customerId);

        // Get all the membershipList where customer equals to customerId + 1
        defaultMembershipShouldNotBeFound("customerId.equals=" + (customerId + 1));
    }

    /**
     * Executes the search, and checks that the default entity is returned
     */
    private void defaultMembershipShouldBeFound(String filter) throws Exception {
        restMembershipMockMvc.perform(get("/api/memberships?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(membership.getId().intValue())))
            .andExpect(jsonPath("$.[*].documentDate").value(hasItem(DEFAULT_DOCUMENT_DATE.toString())))
            .andExpect(jsonPath("$.[*].memberFrom").value(hasItem(DEFAULT_MEMBER_FROM.toString())))
            //.andExpect(jsonPath("$.[*].memberUntil").value(hasItem(DEFAULT_MEMBER_UNTIL.toString())))
            .andExpect(jsonPath("$.[*].remark").value(hasItem(DEFAULT_REMARK)));

        // Check, that the count call also returns 1
        restMembershipMockMvc.perform(get("/api/memberships/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned
     */
    private void defaultMembershipShouldNotBeFound(String filter) throws Exception {
        restMembershipMockMvc.perform(get("/api/memberships?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restMembershipMockMvc.perform(get("/api/memberships/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(content().string("0"));
    }


    @Test
    @Transactional
    public void getNonExistingMembership() throws Exception {
        // Get the membership
        restMembershipMockMvc.perform(get("/api/memberships/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateMembership() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        int databaseSizeBeforeUpdate = membershipRepository.findAll().size();

        // Update the membership
        Membership updatedMembership = membershipRepository.findById(membership.getId()).get();
        // Disconnect from session so that the updates on updatedMembership are not directly saved in db
        em.detach(updatedMembership);
        updatedMembership
            .documentDate(UPDATED_DOCUMENT_DATE)
            .memberFrom(UPDATED_MEMBER_FROM)
            .memberUntil(UPDATED_MEMBER_UNTIL)
            .remark(UPDATED_REMARK);
        MembershipDTO membershipDTO = membershipMapper.toDto(updatedMembership);

        restMembershipMockMvc.perform(put("/api/memberships")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(membershipDTO)))
            .andExpect(status().isOk());

        // Validate the Membership in the database
        List<Membership> membershipList = membershipRepository.findAll();
        assertThat(membershipList).hasSize(databaseSizeBeforeUpdate);
        Membership testMembership = membershipList.get(membershipList.size() - 1);
        assertThat(testMembership.getDocumentDate()).isEqualTo(UPDATED_DOCUMENT_DATE);
        assertThat(testMembership.getMemberFrom()).isEqualTo(UPDATED_MEMBER_FROM);
        assertThat(testMembership.getMemberUntil()).isEqualTo(UPDATED_MEMBER_UNTIL);
        assertThat(testMembership.getRemark()).isEqualTo(UPDATED_REMARK);
    }

    @Test
    @Transactional
    public void updateNonExistingMembership() throws Exception {
        int databaseSizeBeforeUpdate = membershipRepository.findAll().size();

        // Create the Membership
        MembershipDTO membershipDTO = membershipMapper.toDto(membership);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMembershipMockMvc.perform(put("/api/memberships")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(membershipDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Membership in the database
        List<Membership> membershipList = membershipRepository.findAll();
        assertThat(membershipList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteMembership() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        int databaseSizeBeforeDelete = membershipRepository.findAll().size();

        // Delete the membership
        restMembershipMockMvc.perform(delete("/api/memberships/{id}", membership.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest());

        // Validate the database still contains the same number of memberships
        List<Membership> membershipList = membershipRepository.findAll();
        assertThat(membershipList).hasSize(databaseSizeBeforeDelete);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Membership.class);
        Membership membership1 = new Membership();
        membership1.setId(1L);
        Membership membership2 = new Membership();
        membership2.setId(membership1.getId());
        assertThat(membership1).isEqualTo(membership2);
        membership2.setId(2L);
        assertThat(membership1).isNotEqualTo(membership2);
        membership1.setId(null);
        assertThat(membership1).isNotEqualTo(membership2);
    }

    @Test
    @Transactional
    public void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(MembershipDTO.class);
        MembershipDTO membershipDTO1 = new MembershipDTO();
        membershipDTO1.setId(1L);
        MembershipDTO membershipDTO2 = new MembershipDTO();
        assertThat(membershipDTO1).isNotEqualTo(membershipDTO2);
        membershipDTO2.setId(membershipDTO1.getId());
        assertThat(membershipDTO1).isEqualTo(membershipDTO2);
        membershipDTO2.setId(2L);
        assertThat(membershipDTO1).isNotEqualTo(membershipDTO2);
        membershipDTO1.setId(null);
        assertThat(membershipDTO1).isNotEqualTo(membershipDTO2);
    }

    @Test
    @Transactional
    public void testEntityFromId() {
        assertThat(membershipMapper.fromId(42L).getId()).isEqualTo(42);
        assertThat(membershipMapper.fromId(null)).isNull();
    }
}
