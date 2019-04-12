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

    private static final LocalDate DEFAULT_SINCE_DATE = LocalDate.now(ZoneId.systemDefault());
    private static final LocalDate UPDATED_SINCE_DATE = DEFAULT_SINCE_DATE.plusMonths(1);

    private static final LocalDate DEFAULT_UNTIL_DATE = UPDATED_SINCE_DATE.plusDays(600).withMonth(12).withDayOfMonth(31);
    private static final LocalDate UPDATED_UNTIL_DATE = DEFAULT_UNTIL_DATE.plusYears(1);
    private static final LocalDate ANOTHER_QUERY_DATE = DEFAULT_UNTIL_DATE.plusMonths(2);

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
    public static Membership createEntity(final EntityManager em, final Customer customer) {
        em.persist(customer);
        Membership membership = new Membership()
            .customer(customer)
            .sinceDate(DEFAULT_SINCE_DATE)
            .untilDate(DEFAULT_UNTIL_DATE);
        return membership;
    }

    @Before
    public void initTest() {
        membership = createEntity(em, CustomerResourceIntTest.createEntity(em));
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
        assertThat(testMembership.getSinceDate()).isEqualTo(DEFAULT_SINCE_DATE);
        assertThat(testMembership.getUntilDate()).isEqualTo(DEFAULT_UNTIL_DATE);
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
    public void checkSinceDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = membershipRepository.findAll().size();
        // set the field null
        membership.setSinceDate(null);

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
            .andExpect(jsonPath("$.[*].sinceDate").value(hasItem(DEFAULT_SINCE_DATE.toString())))
            .andExpect(jsonPath("$.[*].untilDate").value(hasItem(DEFAULT_UNTIL_DATE.toString())));
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
            .andExpect(jsonPath("$.sinceDate").value(DEFAULT_SINCE_DATE.toString()))
            .andExpect(jsonPath("$.untilDate").value(DEFAULT_UNTIL_DATE.toString()));
    }

    @Test
    @Transactional
    public void getAllMembershipsBySinceDateIsEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where sinceDate equals to DEFAULT_SINCE_DATE
        defaultMembershipShouldBeFound("sinceDate.equals=" + DEFAULT_SINCE_DATE);

        // Get all the membershipList where sinceDate equals to UPDATED_SINCE_DATE
        defaultMembershipShouldNotBeFound("sinceDate.equals=" + UPDATED_SINCE_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsBySinceDateIsInShouldWork() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where sinceDate in DEFAULT_SINCE_DATE or UPDATED_SINCE_DATE
        defaultMembershipShouldBeFound("sinceDate.in=" + DEFAULT_SINCE_DATE + "," + UPDATED_SINCE_DATE);

        // Get all the membershipList where sinceDate equals to UPDATED_SINCE_DATE
        defaultMembershipShouldNotBeFound("sinceDate.in=" + UPDATED_SINCE_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsBySinceDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where sinceDate is not null
        defaultMembershipShouldBeFound("sinceDate.specified=true");

        // Get all the membershipList where sinceDate is null
        defaultMembershipShouldNotBeFound("sinceDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllMembershipsBySinceDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where sinceDate greater than or equals to DEFAULT_SINCE_DATE
        defaultMembershipShouldBeFound("sinceDate.greaterOrEqualThan=" + DEFAULT_SINCE_DATE);

        // Get all the membershipList where sinceDate greater than or equals to UPDATED_SINCE_DATE
        defaultMembershipShouldNotBeFound("sinceDate.greaterOrEqualThan=" + UPDATED_SINCE_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsBySinceDateIsLessThanSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where sinceDate less than or equals to DEFAULT_SINCE_DATE
        defaultMembershipShouldNotBeFound("sinceDate.lessThan=" + DEFAULT_SINCE_DATE);

        // Get all the membershipList where sinceDate less than or equals to UPDATED_SINCE_DATE
        defaultMembershipShouldBeFound("sinceDate.lessThan=" + UPDATED_SINCE_DATE);
    }


    @Test
    @Transactional
    public void getAllMembershipsByUntilDateIsEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where untilDate equals to DEFAULT_UNTIL_DATE
        defaultMembershipShouldBeFound("untilDate.equals=" + DEFAULT_UNTIL_DATE);

        // Get all the membershipList where untilDate equals to UPDATED_UNTIL_DATE
        defaultMembershipShouldNotBeFound("untilDate.equals=" + asString(UPDATED_UNTIL_DATE));
    }

    @Test
    @Transactional
    public void getAllMembershipsByUntilDateIsInShouldWork() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where untilDate in DEFAULT_UNTIL_DATE or UPDATED_UNTIL_DATE
        defaultMembershipShouldBeFound("untilDate.in=" + DEFAULT_UNTIL_DATE + "," + ANOTHER_QUERY_DATE);

        // Get all the membershipList where untilDate equals to UPDATED_UNTIL_DATE
        defaultMembershipShouldNotBeFound("untilDate.in=" + asString(UPDATED_UNTIL_DATE));
    }

    @Test
    @Transactional
    public void getAllMembershipsByUntilDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where untilDate is not null
        defaultMembershipShouldBeFound("untilDate.specified=true");

        // Get all the membershipList where untilDate is null
        defaultMembershipShouldNotBeFound("untilDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllMembershipsByUntilDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where untilDate greater than or equals to DEFAULT_UNTIL_DATE
        defaultMembershipShouldBeFound("untilDate.greaterOrEqualThan=" + DEFAULT_UNTIL_DATE);

        // Get all the membershipList where untilDate greater than or equals to UPDATED_UNTIL_DATE
        defaultMembershipShouldNotBeFound("untilDate.greaterOrEqualThan=" + asString(UPDATED_UNTIL_DATE));
    }

    @Test
    @Transactional
    public void getAllMembershipsByUntilDateIsLessThanSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where untilDate less than or equals to DEFAULT_UNTIL_DATE
        defaultMembershipShouldNotBeFound("untilDate.lessThan=" + DEFAULT_UNTIL_DATE);

        // Get all the membershipList where untilDate less than or equals to UPDATED_UNTIL_DATE
        defaultMembershipShouldBeFound("untilDate.lessThan=" + asString(UPDATED_UNTIL_DATE));
    }


    @Test
    @Transactional
    public void getAllMembershipsByShareIsEqualToSomething() throws Exception {
        // Initialize the database
        Share share = ShareResourceIntTest.createEntity(em, membership);
        em.persist(share);
        em.flush();
        membership.addShare(share);
        membershipRepository.flush();
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
        Asset asset = AssetResourceIntTest.createEntity(em, membership);
        em.persist(asset);
        em.flush();
        membership.addAsset(asset);
        membershipRepository.flush();
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
        Customer customer = CustomerResourceIntTest.createAnotherEntity(em);
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
            .andExpect(jsonPath("$.[*].sinceDate").value(hasItem(DEFAULT_SINCE_DATE.toString())))
            .andExpect(jsonPath("$.[*].untilDate").value(hasItem(DEFAULT_UNTIL_DATE.toString())));

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
            .sinceDate(UPDATED_SINCE_DATE)
            .untilDate(UPDATED_UNTIL_DATE);
        MembershipDTO membershipDTO = membershipMapper.toDto(updatedMembership);

        restMembershipMockMvc.perform(put("/api/memberships")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(membershipDTO)))
            .andExpect(status().isOk());

        // Validate the Membership in the database
        List<Membership> membershipList = membershipRepository.findAll();
        assertThat(membershipList).hasSize(databaseSizeBeforeUpdate);
        Membership testMembership = membershipList.get(membershipList.size() - 1);
        assertThat(testMembership.getSinceDate()).isEqualTo(UPDATED_SINCE_DATE);
        assertThat(testMembership.getUntilDate()).isEqualTo(UPDATED_UNTIL_DATE);
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

        // Validate the database is unchanged empty
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

    private String asString(LocalDate updatedUntilDate) {
        return updatedUntilDate == null ? "" : updatedUntilDate.toString();
    }
}
