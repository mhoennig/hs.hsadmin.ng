// Licensed under Apache-2.0
package org.hostsharing.hsadminng.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hostsharing.hsadminng.web.rest.TestUtil.createFormattingConversionService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.hostsharing.hsadminng.HsadminNgApp;
import org.hostsharing.hsadminng.domain.Asset;
import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.domain.Share;
import org.hostsharing.hsadminng.repository.MembershipRepository;
import org.hostsharing.hsadminng.service.MembershipQueryService;
import org.hostsharing.hsadminng.service.MembershipService;
import org.hostsharing.hsadminng.service.accessfilter.MockSecurityContext;
import org.hostsharing.hsadminng.service.accessfilter.Role;
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import javax.persistence.EntityManager;

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

    private static final LocalDate DEFAULT_MEMBER_FROM_DATE = DEFAULT_DOCUMENT_DATE.plusDays(2);
    private static final LocalDate UPDATED_MEMBER_FROM_DATE = UPDATED_DOCUMENT_DATE.plusDays(8);

    private static final LocalDate DEFAULT_MEMBER_UNTIL_DATE = DEFAULT_MEMBER_FROM_DATE.plusYears(1)
            .withMonth(12)
            .withDayOfMonth(31);
    private static final LocalDate UPDATED_MEMBER_UNTIL_DATE = UPDATED_MEMBER_FROM_DATE.plusYears(7)
            .withMonth(12)
            .withDayOfMonth(31);

    private static final LocalDate DEFAULT_ADMISSION_DOCUMENT_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_ADMISSION_DOCUMENT_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_CANCELLATION_DOCUMENT_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_CANCELLATION_DOCUMENT_DATE = LocalDate.now(ZoneId.systemDefault());

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
        MockSecurityContext.givenAuthenticatedUser();
        MockSecurityContext.givenUserHavingRole(Role.ADMIN);

        MockitoAnnotations.initMocks(this);
        final MembershipResource membershipResource = new MembershipResource(membershipService, membershipQueryService);
        this.restMembershipMockMvc = MockMvcBuilders.standaloneSetup(membershipResource)
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
    public static Membership createEntity(EntityManager em) {
        Membership membership = new Membership()
                .admissionDocumentDate(DEFAULT_ADMISSION_DOCUMENT_DATE)
                .cancellationDocumentDate(DEFAULT_CANCELLATION_DOCUMENT_DATE)
                .memberFromDate(DEFAULT_MEMBER_FROM_DATE)
                .memberUntilDate(DEFAULT_MEMBER_UNTIL_DATE)
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
                .admissionDocumentDate(DEFAULT_ADMISSION_DOCUMENT_DATE)
                .memberFromDate(DEFAULT_MEMBER_FROM_DATE)
                .memberUntilDate(DEFAULT_MEMBER_UNTIL_DATE)
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
        restMembershipMockMvc.perform(
                post("/api/memberships")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(membershipDTO)))
                .andExpect(status().isCreated());

        // Validate the Membership in the database
        List<Membership> membershipList = membershipRepository.findAll();
        assertThat(membershipList).hasSize(databaseSizeBeforeCreate + 1);
        Membership testMembership = membershipList.get(membershipList.size() - 1);
        assertThat(testMembership.getAdmissionDocumentDate()).isEqualTo(DEFAULT_ADMISSION_DOCUMENT_DATE);
        assertThat(testMembership.getCancellationDocumentDate()).isEqualTo(DEFAULT_CANCELLATION_DOCUMENT_DATE);
        assertThat(testMembership.getMemberFromDate()).isEqualTo(DEFAULT_MEMBER_FROM_DATE);
        assertThat(testMembership.getMemberUntilDate()).isEqualTo(DEFAULT_MEMBER_UNTIL_DATE);
        assertThat(testMembership.getRemark()).isEqualTo(DEFAULT_REMARK);
    }

    @Test
    @Transactional
    public void createCustomerWithExistingIdIsRejected() throws Exception {
        // Initialize the database
        final long existingCustomerId = membershipRepository.saveAndFlush(membership).getId();
        int databaseSizeBeforeCreate = membershipRepository.findAll().size();

        // Create the Customer with an existing ID
        membership.setId(existingCustomerId);
        MembershipDTO membershipDTO = membershipMapper.toDto(membership);

        // An entity with an existing ID cannot be created, so this API call must fail
        restMembershipMockMvc.perform(
                post("/api/memberships")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(membershipDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Customer in the database
        List<Membership> membershipList = membershipRepository.findAll();
        assertThat(membershipList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void createCustomerWithNonExistingIdIsRejected() throws Exception {
        int databaseSizeBeforeCreate = membershipRepository.findAll().size();

        // Create the Membership with an ID for which no entity exists
        membership.setId(1L);
        MembershipDTO membershipDTO = membershipMapper.toDto(membership);

        // An entity with an existing ID cannot be created, so this API call must fail
        restMembershipMockMvc.perform(
                post("/api/memberships")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(membershipDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Membership in the database
        List<Membership> membershipList = membershipRepository.findAll();
        assertThat(membershipList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkAdmissionDocumentDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = membershipRepository.findAll().size();
        // set the field null
        membership.setAdmissionDocumentDate(null);

        // Create the Membership, which fails.
        MembershipDTO membershipDTO = membershipMapper.toDto(membership);

        restMembershipMockMvc.perform(
                post("/api/memberships")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(membershipDTO)))
                .andExpect(status().isBadRequest());

        List<Membership> membershipList = membershipRepository.findAll();
        assertThat(membershipList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkMemberFromDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = membershipRepository.findAll().size();
        // set the field null
        membership.setMemberFromDate(null);

        // Create the Membership, which fails.
        MembershipDTO membershipDTO = membershipMapper.toDto(membership);

        restMembershipMockMvc.perform(
                post("/api/memberships")
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
                .andExpect(jsonPath("$.[*].admissionDocumentDate").value(hasItem(DEFAULT_ADMISSION_DOCUMENT_DATE.toString())))
                .andExpect(
                        jsonPath("$.[*].cancellationDocumentDate")
                                .value(hasItem(DEFAULT_CANCELLATION_DOCUMENT_DATE.toString())))
                .andExpect(jsonPath("$.[*].memberFromDate").value(hasItem(DEFAULT_MEMBER_FROM_DATE.toString())))
                .andExpect(jsonPath("$.[*].memberUntilDate").value(hasItem(DEFAULT_MEMBER_UNTIL_DATE.toString())))
                .andExpect(jsonPath("$.[*].remark").value(hasItem(DEFAULT_REMARK)));
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
                .andExpect(jsonPath("$.admissionDocumentDate").value(DEFAULT_ADMISSION_DOCUMENT_DATE.toString()))
                .andExpect(jsonPath("$.cancellationDocumentDate").value(DEFAULT_CANCELLATION_DOCUMENT_DATE.toString()))
                .andExpect(jsonPath("$.memberFromDate").value(DEFAULT_MEMBER_FROM_DATE.toString()))
                .andExpect(jsonPath("$.memberUntilDate").value(DEFAULT_MEMBER_UNTIL_DATE.toString()))
                .andExpect(jsonPath("$.remark").value(DEFAULT_REMARK));
    }

    @Test
    @Transactional
    public void getAllMembershipsByAdmissionDocumentDateIsEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where admissionDocumentDate equals to DEFAULT_ADMISSION_DOCUMENT_DATE
        defaultMembershipShouldBeFound("admissionDocumentDate.equals=" + DEFAULT_ADMISSION_DOCUMENT_DATE);

        // Get all the membershipList where admissionDocumentDate equals to UPDATED_ADMISSION_DOCUMENT_DATE
        defaultMembershipShouldNotBeFound("admissionDocumentDate.equals=" + UPDATED_ADMISSION_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsByAdmissionDocumentDateIsInShouldWork() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where admissionDocumentDate in DEFAULT_ADMISSION_DOCUMENT_DATE or
        // UPDATED_ADMISSION_DOCUMENT_DATE
        defaultMembershipShouldBeFound(
                "admissionDocumentDate.in=" + DEFAULT_ADMISSION_DOCUMENT_DATE + "," + UPDATED_ADMISSION_DOCUMENT_DATE);

        // Get all the membershipList where admissionDocumentDate equals to UPDATED_ADMISSION_DOCUMENT_DATE
        defaultMembershipShouldNotBeFound("admissionDocumentDate.in=" + UPDATED_ADMISSION_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsByAdmissionDocumentDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where admissionDocumentDate is not null
        defaultMembershipShouldBeFound("admissionDocumentDate.specified=true");

        // Get all the membershipList where admissionDocumentDate is null
        defaultMembershipShouldNotBeFound("admissionDocumentDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllMembershipsByAdmissionDocumentDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where admissionDocumentDate greater than or equals to DEFAULT_ADMISSION_DOCUMENT_DATE
        defaultMembershipShouldBeFound("admissionDocumentDate.greaterOrEqualThan=" + DEFAULT_ADMISSION_DOCUMENT_DATE);

        // Get all the membershipList where admissionDocumentDate greater than or equals to UPDATED_ADMISSION_DOCUMENT_DATE
        defaultMembershipShouldNotBeFound("admissionDocumentDate.greaterOrEqualThan=" + UPDATED_ADMISSION_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsByAdmissionDocumentDateIsLessThanSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where admissionDocumentDate less than or equals to DEFAULT_ADMISSION_DOCUMENT_DATE
        defaultMembershipShouldNotBeFound("admissionDocumentDate.lessThan=" + DEFAULT_ADMISSION_DOCUMENT_DATE);

        // Get all the membershipList where admissionDocumentDate less than or equals to UPDATED_ADMISSION_DOCUMENT_DATE
        defaultMembershipShouldBeFound("admissionDocumentDate.lessThan=" + UPDATED_ADMISSION_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsByCancellationDocumentDateIsEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where cancellationDocumentDate equals to DEFAULT_CANCELLATION_DOCUMENT_DATE
        defaultMembershipShouldBeFound("cancellationDocumentDate.equals=" + DEFAULT_CANCELLATION_DOCUMENT_DATE);

        // Get all the membershipList where cancellationDocumentDate equals to UPDATED_CANCELLATION_DOCUMENT_DATE
        defaultMembershipShouldNotBeFound("cancellationDocumentDate.equals=" + UPDATED_CANCELLATION_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsByCancellationDocumentDateIsInShouldWork() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where cancellationDocumentDate in DEFAULT_CANCELLATION_DOCUMENT_DATE or
        // UPDATED_CANCELLATION_DOCUMENT_DATE
        defaultMembershipShouldBeFound(
                "cancellationDocumentDate.in=" + DEFAULT_CANCELLATION_DOCUMENT_DATE + "," + UPDATED_CANCELLATION_DOCUMENT_DATE);

        // Get all the membershipList where cancellationDocumentDate equals to UPDATED_CANCELLATION_DOCUMENT_DATE
        defaultMembershipShouldNotBeFound("cancellationDocumentDate.in=" + UPDATED_CANCELLATION_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsByCancellationDocumentDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where cancellationDocumentDate is not null
        defaultMembershipShouldBeFound("cancellationDocumentDate.specified=true");

        // Get all the membershipList where cancellationDocumentDate is null
        defaultMembershipShouldNotBeFound("cancellationDocumentDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllMembershipsByCancellationDocumentDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where cancellationDocumentDate greater than or equals to
        // DEFAULT_CANCELLATION_DOCUMENT_DATE
        defaultMembershipShouldBeFound("cancellationDocumentDate.greaterOrEqualThan=" + DEFAULT_CANCELLATION_DOCUMENT_DATE);

        // Get all the membershipList where cancellationDocumentDate greater than or equals to
        // UPDATED_CANCELLATION_DOCUMENT_DATE
        defaultMembershipShouldNotBeFound("cancellationDocumentDate.greaterOrEqualThan=" + UPDATED_CANCELLATION_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsByCancellationDocumentDateIsLessThanSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where cancellationDocumentDate less than or equals to DEFAULT_CANCELLATION_DOCUMENT_DATE
        defaultMembershipShouldNotBeFound("cancellationDocumentDate.lessThan=" + DEFAULT_CANCELLATION_DOCUMENT_DATE);

        // Get all the membershipList where cancellationDocumentDate less than or equals to UPDATED_CANCELLATION_DOCUMENT_DATE
        defaultMembershipShouldBeFound("cancellationDocumentDate.lessThan=" + UPDATED_CANCELLATION_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsByMemberFromDateIsEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberFromDate equals to DEFAULT_MEMBER_FROM_DATE
        defaultMembershipShouldBeFound("memberFromDate.equals=" + DEFAULT_MEMBER_FROM_DATE);

        // Get all the membershipList where memberFromDate equals to UPDATED_MEMBER_FROM_DATE
        defaultMembershipShouldNotBeFound("memberFromDate.equals=" + UPDATED_MEMBER_FROM_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsByMemberFromDateIsInShouldWork() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberFromDate in DEFAULT_MEMBER_FROM_DATE or UPDATED_MEMBER_FROM_DATE
        defaultMembershipShouldBeFound("memberFromDate.in=" + DEFAULT_MEMBER_FROM_DATE + "," + UPDATED_MEMBER_FROM_DATE);

        // Get all the membershipList where memberFromDate equals to UPDATED_MEMBER_FROM_DATE
        defaultMembershipShouldNotBeFound("memberFromDate.in=" + UPDATED_MEMBER_FROM_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsByMemberFromDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberFromDate is not null
        defaultMembershipShouldBeFound("memberFromDate.specified=true");

        // Get all the membershipList where memberFromDate is null
        defaultMembershipShouldNotBeFound("memberFromDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllMembershipsByMemberFromDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberFromDate greater than or equals to DEFAULT_MEMBER_FROM_DATE
        defaultMembershipShouldBeFound("memberFromDate.greaterOrEqualThan=" + DEFAULT_MEMBER_FROM_DATE);

        // Get all the membershipList where memberFromDate greater than or equals to UPDATED_MEMBER_FROM_DATE
        defaultMembershipShouldNotBeFound("memberFromDate.greaterOrEqualThan=" + UPDATED_MEMBER_FROM_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsByMemberFromDateIsLessThanSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberFromDate less than or equals to DEFAULT_MEMBER_FROM_DATE
        defaultMembershipShouldNotBeFound("memberFromDate.lessThan=" + DEFAULT_MEMBER_FROM_DATE);

        // Get all the membershipList where memberFromDate less than or equals to UPDATED_MEMBER_FROM_DATE
        defaultMembershipShouldBeFound("memberFromDate.lessThan=" + UPDATED_MEMBER_FROM_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsByMemberUntilDateIsEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberUntilDate equals to DEFAULT_MEMBER_UNTIL_DATE
        defaultMembershipShouldBeFound("memberUntilDate.equals=" + DEFAULT_MEMBER_UNTIL_DATE);

        // Get all the membershipList where memberUntilDate equals to UPDATED_MEMBER_UNTIL_DATE
        defaultMembershipShouldNotBeFound("memberUntilDate.equals=" + UPDATED_MEMBER_UNTIL_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsByMemberUntilDateIsInShouldWork() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberUntilDate in DEFAULT_MEMBER_UNTIL_DATE or UPDATED_MEMBER_UNTIL_DATE
        defaultMembershipShouldBeFound("memberUntilDate.in=" + DEFAULT_MEMBER_UNTIL_DATE + "," + UPDATED_MEMBER_UNTIL_DATE);

        // Get all the membershipList where memberUntilDate equals to UPDATED_MEMBER_UNTIL_DATE
        defaultMembershipShouldNotBeFound("memberUntilDate.in=" + UPDATED_MEMBER_UNTIL_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsByMemberUntilDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberUntilDate is not null
        defaultMembershipShouldBeFound("memberUntilDate.specified=true");

        // Get all the membershipList where memberUntilDate is null
        defaultMembershipShouldNotBeFound("memberUntilDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllMembershipsByMemberUntilDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberUntilDate greater than or equals to DEFAULT_MEMBER_UNTIL_DATE
        defaultMembershipShouldBeFound("memberUntilDate.greaterOrEqualThan=" + DEFAULT_MEMBER_UNTIL_DATE);

        // Get all the membershipList where memberUntilDate greater than or equals to UPDATED_MEMBER_UNTIL_DATE
        defaultMembershipShouldNotBeFound("memberUntilDate.greaterOrEqualThan=" + UPDATED_MEMBER_UNTIL_DATE);
    }

    @Test
    @Transactional
    public void getAllMembershipsByMemberUntilDateIsLessThanSomething() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the membershipList where memberUntilDate less than or equals to DEFAULT_MEMBER_UNTIL_DATE
        defaultMembershipShouldNotBeFound("memberUntilDate.lessThan=" + DEFAULT_MEMBER_UNTIL_DATE);

        // Get all the membershipList where memberUntilDate less than or equals to UPDATED_MEMBER_UNTIL_DATE
        defaultMembershipShouldBeFound("memberUntilDate.lessThan=" + UPDATED_MEMBER_UNTIL_DATE);
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
        membershipRepository.saveAndFlush(membership);
        Asset asset = AssetResourceIntTest.createPersistentEntity(em, membership);

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
                .andExpect(jsonPath("$.[*].admissionDocumentDate").value(hasItem(DEFAULT_ADMISSION_DOCUMENT_DATE.toString())))
                .andExpect(
                        jsonPath("$.[*].cancellationDocumentDate")
                                .value(hasItem(DEFAULT_CANCELLATION_DOCUMENT_DATE.toString())))
                .andExpect(jsonPath("$.[*].memberFromDate").value(hasItem(DEFAULT_MEMBER_FROM_DATE.toString())))
                .andExpect(jsonPath("$.[*].memberUntilDate").value(hasItem(DEFAULT_MEMBER_UNTIL_DATE.toString())))
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
                .cancellationDocumentDate(UPDATED_CANCELLATION_DOCUMENT_DATE)
                .memberUntilDate(UPDATED_MEMBER_UNTIL_DATE)
                .remark(UPDATED_REMARK);
        MembershipDTO membershipDTO = membershipMapper.toDto(updatedMembership);

        restMembershipMockMvc.perform(
                put("/api/memberships")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(membershipDTO)))
                .andExpect(status().isOk());

        // Validate the Membership in the database
        List<Membership> membershipList = membershipRepository.findAll();
        assertThat(membershipList).hasSize(databaseSizeBeforeUpdate);
        Membership testMembership = membershipList.get(membershipList.size() - 1);
        assertThat(testMembership.getAdmissionDocumentDate()).isEqualTo(DEFAULT_ADMISSION_DOCUMENT_DATE);
        assertThat(testMembership.getCancellationDocumentDate()).isEqualTo(UPDATED_CANCELLATION_DOCUMENT_DATE);
        assertThat(testMembership.getMemberFromDate()).isEqualTo(DEFAULT_MEMBER_FROM_DATE);
        assertThat(testMembership.getMemberUntilDate()).isEqualTo(UPDATED_MEMBER_UNTIL_DATE);
        assertThat(testMembership.getRemark()).isEqualTo(UPDATED_REMARK);
    }

    @Test
    @Transactional
    public void updateNonExistingMembership() throws Exception {
        int databaseSizeBeforeUpdate = membershipRepository.findAll().size();

        // Create the Membership
        MembershipDTO membershipDTO = membershipMapper.toDto(membership);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMembershipMockMvc.perform(
                put("/api/memberships")
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
        restMembershipMockMvc.perform(
                delete("/api/memberships/{id}", membership.getId())
                        .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());

        // Validate the database is unchanged
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
