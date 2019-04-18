package org.hostsharing.hsadminng.web.rest;

import org.hostsharing.hsadminng.HsadminNgApp;

import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.domain.Share;
import org.hostsharing.hsadminng.domain.Asset;
import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.repository.MembershipRepository;
import org.hostsharing.hsadminng.service.MembershipService;
import org.hostsharing.hsadminng.service.dto.MembershipDTO;
import org.hostsharing.hsadminng.service.mapper.MembershipMapper;
import org.hostsharing.hsadminng.web.rest.errors.ExceptionTranslator;
import org.hostsharing.hsadminng.service.dto.MembershipCriteria;
import org.hostsharing.hsadminng.service.MembershipQueryService;

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

/**
 * Test class for the MembershipResource REST controller.
 *
 * @see MembershipResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = HsadminNgApp.class)
public class MembershipResourceIntTest {

    private static final LocalDate DEFAULT_FROM = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_FROM = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_TO = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_TO = LocalDate.now(ZoneId.systemDefault());

    private static final String DEFAULT_COMMENT = "AAAAAAAAAA";
    private static final String UPDATED_COMMENT = "BBBBBBBBBB";

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
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Membership createEntity(EntityManager em) {
        Membership membership = new Membership()
            .from(DEFAULT_FROM)
            .to(DEFAULT_TO)
            .comment(DEFAULT_COMMENT);
        // Add required entity
        Customer customer = CustomerResourceIntTest.createEntity(em);
        em.persist(customer);
        em.flush();
        membership.setCustomer(customer);
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
        assertThat(testMembership.getFrom()).isEqualTo(DEFAULT_FROM);
        assertThat(testMembership.getTo()).isEqualTo(DEFAULT_TO);
        assertThat(testMembership.getComment()).isEqualTo(DEFAULT_COMMENT);
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
    public void checkFromIsRequired() throws Exception {
        int databaseSizeBeforeTest = membershipRepository.findAll().size();
        // set the field null
        membership.setFrom(null);

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
            .andExpect(jsonPath("$.[*].from").value(hasItem(DEFAULT_FROM.toString())))
            .andExpect(jsonPath("$.[*].to").value(hasItem(DEFAULT_TO.toString())))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT.toString())));
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
            .andExpect(jsonPath("$.from").value(DEFAULT_FROM.toString()))
            .andExpect(jsonPath("$.to").value(DEFAULT_TO.toString()))
            .andExpect(jsonPath("$.comment").value(DEFAULT_COMMENT.toString()));
    }

    @Test
    @Transactional
    public void getAllMembershipsByFromIsEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where from equals to DEFAULT_FROM
        defaultMembershipShouldBeFound("from.equals=" + DEFAULT_FROM);

        // Get all the membershipList where from equals to UPDATED_FROM
        defaultMembershipShouldNotBeFound("from.equals=" + UPDATED_FROM);
    }

    @Test
    @Transactional
    public void getAllMembershipsByFromIsInShouldWork() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where from in DEFAULT_FROM or UPDATED_FROM
        defaultMembershipShouldBeFound("from.in=" + DEFAULT_FROM + "," + UPDATED_FROM);

        // Get all the membershipList where from equals to UPDATED_FROM
        defaultMembershipShouldNotBeFound("from.in=" + UPDATED_FROM);
    }

    @Test
    @Transactional
    public void getAllMembershipsByFromIsNullOrNotNull() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where from is not null
        defaultMembershipShouldBeFound("from.specified=true");

        // Get all the membershipList where from is null
        defaultMembershipShouldNotBeFound("from.specified=false");
    }

    @Test
    @Transactional
    public void getAllMembershipsByFromIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where from greater than or equals to DEFAULT_FROM
        defaultMembershipShouldBeFound("from.greaterOrEqualThan=" + DEFAULT_FROM);

        // Get all the membershipList where from greater than or equals to UPDATED_FROM
        defaultMembershipShouldNotBeFound("from.greaterOrEqualThan=" + UPDATED_FROM);
    }

    @Test
    @Transactional
    public void getAllMembershipsByFromIsLessThanSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where from less than or equals to DEFAULT_FROM
        defaultMembershipShouldNotBeFound("from.lessThan=" + DEFAULT_FROM);

        // Get all the membershipList where from less than or equals to UPDATED_FROM
        defaultMembershipShouldBeFound("from.lessThan=" + UPDATED_FROM);
    }


    @Test
    @Transactional
    public void getAllMembershipsByToIsEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where to equals to DEFAULT_TO
        defaultMembershipShouldBeFound("to.equals=" + DEFAULT_TO);

        // Get all the membershipList where to equals to UPDATED_TO
        defaultMembershipShouldNotBeFound("to.equals=" + UPDATED_TO);
    }

    @Test
    @Transactional
    public void getAllMembershipsByToIsInShouldWork() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where to in DEFAULT_TO or UPDATED_TO
        defaultMembershipShouldBeFound("to.in=" + DEFAULT_TO + "," + UPDATED_TO);

        // Get all the membershipList where to equals to UPDATED_TO
        defaultMembershipShouldNotBeFound("to.in=" + UPDATED_TO);
    }

    @Test
    @Transactional
    public void getAllMembershipsByToIsNullOrNotNull() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where to is not null
        defaultMembershipShouldBeFound("to.specified=true");

        // Get all the membershipList where to is null
        defaultMembershipShouldNotBeFound("to.specified=false");
    }

    @Test
    @Transactional
    public void getAllMembershipsByToIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where to greater than or equals to DEFAULT_TO
        defaultMembershipShouldBeFound("to.greaterOrEqualThan=" + DEFAULT_TO);

        // Get all the membershipList where to greater than or equals to UPDATED_TO
        defaultMembershipShouldNotBeFound("to.greaterOrEqualThan=" + UPDATED_TO);
    }

    @Test
    @Transactional
    public void getAllMembershipsByToIsLessThanSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where to less than or equals to DEFAULT_TO
        defaultMembershipShouldNotBeFound("to.lessThan=" + DEFAULT_TO);

        // Get all the membershipList where to less than or equals to UPDATED_TO
        defaultMembershipShouldBeFound("to.lessThan=" + UPDATED_TO);
    }


    @Test
    @Transactional
    public void getAllMembershipsByCommentIsEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where comment equals to DEFAULT_COMMENT
        defaultMembershipShouldBeFound("comment.equals=" + DEFAULT_COMMENT);

        // Get all the membershipList where comment equals to UPDATED_COMMENT
        defaultMembershipShouldNotBeFound("comment.equals=" + UPDATED_COMMENT);
    }

    @Test
    @Transactional
    public void getAllMembershipsByCommentIsInShouldWork() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where comment in DEFAULT_COMMENT or UPDATED_COMMENT
        defaultMembershipShouldBeFound("comment.in=" + DEFAULT_COMMENT + "," + UPDATED_COMMENT);

        // Get all the membershipList where comment equals to UPDATED_COMMENT
        defaultMembershipShouldNotBeFound("comment.in=" + UPDATED_COMMENT);
    }

    @Test
    @Transactional
    public void getAllMembershipsByCommentIsNullOrNotNull() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where comment is not null
        defaultMembershipShouldBeFound("comment.specified=true");

        // Get all the membershipList where comment is null
        defaultMembershipShouldNotBeFound("comment.specified=false");
    }

    @Test
    @Transactional
    public void getAllMembershipsByShareIsEqualToSomething() throws Exception {
        // Initialize the database
        Share share = ShareResourceIntTest.createEntity(em);
        em.persist(share);
        em.flush();
        membership.addShare(share);
        membershipRepository.saveAndFlush(membership);
        Long shareId = share.getId();

        // Get all the membershipList where share equals to shareId
        defaultMembershipShouldBeFound("shareId.equals=" + shareId);

        // Get all the membershipList where share equals to shareId + 1
        defaultMembershipShouldNotBeFound("shareId.equals=" + (shareId + 1));
    }


    @Test
    @Transactional
    public void getAllMembershipsByAssetIsEqualToSomething() throws Exception {
        // Initialize the database
        Asset asset = AssetResourceIntTest.createEntity(em);
        em.persist(asset);
        em.flush();
        membership.addAsset(asset);
        membershipRepository.saveAndFlush(membership);
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
        Customer customer = CustomerResourceIntTest.createEntity(em);
        em.persist(customer);
        em.flush();
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
            .andExpect(jsonPath("$.[*].from").value(hasItem(DEFAULT_FROM.toString())))
            .andExpect(jsonPath("$.[*].to").value(hasItem(DEFAULT_TO.toString())))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT)));

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
            .from(UPDATED_FROM)
            .to(UPDATED_TO)
            .comment(UPDATED_COMMENT);
        MembershipDTO membershipDTO = membershipMapper.toDto(updatedMembership);

        restMembershipMockMvc.perform(put("/api/memberships")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(membershipDTO)))
            .andExpect(status().isOk());

        // Validate the Membership in the database
        List<Membership> membershipList = membershipRepository.findAll();
        assertThat(membershipList).hasSize(databaseSizeBeforeUpdate);
        Membership testMembership = membershipList.get(membershipList.size() - 1);
        assertThat(testMembership.getFrom()).isEqualTo(UPDATED_FROM);
        assertThat(testMembership.getTo()).isEqualTo(UPDATED_TO);
        assertThat(testMembership.getComment()).isEqualTo(UPDATED_COMMENT);
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
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Membership> membershipList = membershipRepository.findAll();
        assertThat(membershipList).hasSize(databaseSizeBeforeDelete - 1);
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
