// Licensed under Apache-2.0
package org.hostsharing.hsadminng.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hostsharing.hsadminng.web.rest.TestUtil.createFormattingConversionService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.hostsharing.hsadminng.HsadminNgApp;
import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.SepaMandate;
import org.hostsharing.hsadminng.repository.SepaMandateRepository;
import org.hostsharing.hsadminng.security.AuthoritiesConstants;
import org.hostsharing.hsadminng.service.SepaMandateQueryService;
import org.hostsharing.hsadminng.service.SepaMandateService;
import org.hostsharing.hsadminng.service.UserRoleAssignmentService;
import org.hostsharing.hsadminng.service.accessfilter.SecurityContextMock;
import org.hostsharing.hsadminng.service.dto.SepaMandateDTO;
import org.hostsharing.hsadminng.service.mapper.SepaMandateMapper;
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import javax.persistence.EntityManager;

/**
 * Test class for the SepaMandateResource REST controller.
 *
 * @see SepaMandateResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = HsadminNgApp.class)
public class SepaMandateResourceIntTest {

    private static final String DEFAULT_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_REFERENCE = "BBBBBBBBBB";

    private static final String DEFAULT_IBAN = "AAAAAAAAAA";
    private static final String UPDATED_IBAN = "BBBBBBBBBB";

    private static final String DEFAULT_BIC = "AAAAAAAAAA";
    private static final String UPDATED_BIC = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_VALID_FROM_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_VALID_FROM_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_VALID_UNTIL_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_VALID_UNTIL_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_GRANTING_DOCUMENT_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_GRANTING_DOCUMENT_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_REVOKATION_DOCUMENT_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_REVOKATION_DOCUMENT_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_LAST_USED_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_LAST_USED_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final String DEFAULT_REMARK = "AAAAAAAAAA";
    private static final String UPDATED_REMARK = "BBBBBBBBBB";

    @Autowired
    private SepaMandateRepository sepaMandateRepository;

    @Autowired
    private SepaMandateMapper sepaMandateMapper;

    @Autowired
    private SepaMandateService sepaMandateService;

    @Autowired
    private SepaMandateQueryService sepaMandateQueryService;

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

    private MockMvc restSepaMandateMockMvc;

    private SepaMandate sepaMandate;

    @Before
    public void setup() {
        SecurityContextMock.usingMock(userRoleAssignmentService)
                .havingAuthenticatedUser()
                .withAuthority(AuthoritiesConstants.ADMIN);

        MockitoAnnotations.initMocks(this);
        final SepaMandateResource sepaMandateResource = new SepaMandateResource(sepaMandateService, sepaMandateQueryService);
        this.restSepaMandateMockMvc = MockMvcBuilders.standaloneSetup(sepaMandateResource)
                .setCustomArgumentResolvers(pageableArgumentResolver)
                .setControllerAdvice(exceptionTranslator)
                .setConversionService(createFormattingConversionService())
                .setMessageConverters(jacksonMessageConverter)
                .setValidator(validator)
                .build();
    }

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SepaMandate createEntity(EntityManager em) {
        SepaMandate sepaMandate = new SepaMandate()
                .reference(DEFAULT_REFERENCE)
                .iban(DEFAULT_IBAN)
                .bic(DEFAULT_BIC)
                .grantingDocumentDate(DEFAULT_GRANTING_DOCUMENT_DATE)
                .revokationDocumentDate(DEFAULT_REVOKATION_DOCUMENT_DATE)
                .validFromDate(DEFAULT_VALID_FROM_DATE)
                .validUntilDate(DEFAULT_VALID_UNTIL_DATE)
                .lastUsedDate(DEFAULT_LAST_USED_DATE)
                .remark(DEFAULT_REMARK);
        // Add required entity
        Customer customer = CustomerResourceIntTest.createEntity(em);
        em.persist(customer);
        em.flush();
        sepaMandate.setCustomer(customer);
        return sepaMandate;
    }

    /**
     * Create an entity for tests with a specific customer.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static SepaMandate createEntity(EntityManager em, final Customer customer) {
        SepaMandate sepaMandate = new SepaMandate()
                .reference(DEFAULT_REFERENCE)
                .iban(DEFAULT_IBAN)
                .bic(DEFAULT_BIC)
                .grantingDocumentDate(DEFAULT_GRANTING_DOCUMENT_DATE)
                .validFromDate(DEFAULT_VALID_FROM_DATE)
                .validUntilDate(DEFAULT_VALID_UNTIL_DATE)
                .lastUsedDate(DEFAULT_LAST_USED_DATE)
                .revokationDocumentDate(DEFAULT_REVOKATION_DOCUMENT_DATE)
                .remark(DEFAULT_REMARK);
        // Add required entity
        sepaMandate.setCustomer(customer);
        return sepaMandate;
    }

    @Before
    public void initTest() {
        sepaMandate = createEntity(em);
    }

    @Test
    @Transactional
    public void createSepaMandate() throws Exception {
        int databaseSizeBeforeCreate = sepaMandateRepository.findAll().size();

        // Create the SepaMandate
        SepaMandateDTO sepaMandateDTO = sepaMandateMapper.toDto(sepaMandate);
        sepaMandateDTO.setCustomerDisplayLabel(null);
        sepaMandateDTO.setRemark(null);
        sepaMandateDTO.setRevokationDocumentDate(null);
        sepaMandateDTO.setLastUsedDate(null);

        restSepaMandateMockMvc.perform(
                post("/api/sepa-mandates")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(sepaMandateDTO)))
                .andExpect(status().isCreated());

        // Validate the SepaMandate in the database
        List<SepaMandate> sepaMandateList = sepaMandateRepository.findAll();
        assertThat(sepaMandateList).hasSize(databaseSizeBeforeCreate + 1);
        SepaMandate testSepaMandate = sepaMandateList.get(sepaMandateList.size() - 1);
        assertThat(testSepaMandate.getReference()).isEqualTo(DEFAULT_REFERENCE);
        assertThat(testSepaMandate.getIban()).isEqualTo(DEFAULT_IBAN);
        assertThat(testSepaMandate.getBic()).isEqualTo(DEFAULT_BIC);
        assertThat(testSepaMandate.getGrantingDocumentDate()).isEqualTo(DEFAULT_GRANTING_DOCUMENT_DATE);
        assertThat(testSepaMandate.getRevokationDocumentDate()).isNull();
        assertThat(testSepaMandate.getValidFromDate()).isEqualTo(DEFAULT_VALID_FROM_DATE);
        assertThat(testSepaMandate.getValidUntilDate()).isEqualTo(DEFAULT_VALID_UNTIL_DATE);
        assertThat(testSepaMandate.getLastUsedDate()).isNull();
        assertThat(testSepaMandate.getRemark()).isNull();
    }

    @Test
    @Transactional
    public void createSepaMandateWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = sepaMandateRepository.findAll().size();

        // Create the SepaMandate with an existing ID
        sepaMandate.setId(1L);
        SepaMandateDTO sepaMandateDTO = sepaMandateMapper.toDto(sepaMandate);

        // An entity with an existing ID cannot be created, so this API call must fail
        restSepaMandateMockMvc.perform(
                post("/api/sepa-mandates")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(sepaMandateDTO)))
                .andExpect(status().isBadRequest());

        // Validate the SepaMandate in the database
        List<SepaMandate> sepaMandateList = sepaMandateRepository.findAll();
        assertThat(sepaMandateList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkReferenceIsRequired() throws Exception {
        int databaseSizeBeforeTest = sepaMandateRepository.findAll().size();
        // set the field null
        sepaMandate.setReference(null);

        // Create the SepaMandate, which fails.
        SepaMandateDTO sepaMandateDTO = sepaMandateMapper.toDto(sepaMandate);

        restSepaMandateMockMvc.perform(
                post("/api/sepa-mandates")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(sepaMandateDTO)))
                .andExpect(status().isBadRequest());

        List<SepaMandate> sepaMandateList = sepaMandateRepository.findAll();
        assertThat(sepaMandateList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkGrantingDocumentDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = sepaMandateRepository.findAll().size();
        // set the field null
        sepaMandate.setGrantingDocumentDate(null);

        // Create the SepaMandate, which fails.
        SepaMandateDTO sepaMandateDTO = sepaMandateMapper.toDto(sepaMandate);

        restSepaMandateMockMvc.perform(
                post("/api/sepa-mandates")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(sepaMandateDTO)))
                .andExpect(status().isBadRequest());

        List<SepaMandate> sepaMandateList = sepaMandateRepository.findAll();
        assertThat(sepaMandateList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkValidFromDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = sepaMandateRepository.findAll().size();
        // set the field null
        sepaMandate.setValidFromDate(null);

        // Create the SepaMandate, which fails.
        SepaMandateDTO sepaMandateDTO = sepaMandateMapper.toDto(sepaMandate);

        restSepaMandateMockMvc.perform(
                post("/api/sepa-mandates")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(sepaMandateDTO)))
                .andExpect(status().isBadRequest());

        List<SepaMandate> sepaMandateList = sepaMandateRepository.findAll();
        assertThat(sepaMandateList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllSepaMandates() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList
        restSepaMandateMockMvc.perform(get("/api/sepa-mandates?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(sepaMandate.getId().intValue())))
                .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
                .andExpect(jsonPath("$.[*].iban").value(hasItem(DEFAULT_IBAN)))
                .andExpect(jsonPath("$.[*].bic").value(hasItem(DEFAULT_BIC)))
                .andExpect(jsonPath("$.[*].grantingDocumentDate").value(hasItem(DEFAULT_GRANTING_DOCUMENT_DATE.toString())))
                .andExpect(jsonPath("$.[*].revokationDocumentDate").value(hasItem(DEFAULT_REVOKATION_DOCUMENT_DATE.toString())))
                .andExpect(jsonPath("$.[*].validFromDate").value(hasItem(DEFAULT_VALID_FROM_DATE.toString())))
                .andExpect(jsonPath("$.[*].validUntilDate").value(hasItem(DEFAULT_VALID_UNTIL_DATE.toString())))
                .andExpect(jsonPath("$.[*].lastUsedDate").value(hasItem(DEFAULT_LAST_USED_DATE.toString())))
                .andExpect(jsonPath("$.[*].remark").value(hasItem(DEFAULT_REMARK)));
    }

    @Test
    @Transactional
    public void getSepaMandate() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get the sepaMandate
        restSepaMandateMockMvc.perform(get("/api/sepa-mandates/{id}", sepaMandate.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.id").value(sepaMandate.getId().intValue()))
                .andExpect(jsonPath("$.reference").value(DEFAULT_REFERENCE))
                .andExpect(jsonPath("$.iban").value(DEFAULT_IBAN))
                .andExpect(jsonPath("$.bic").value(DEFAULT_BIC))
                .andExpect(jsonPath("$.grantingDocumentDate").value(DEFAULT_GRANTING_DOCUMENT_DATE.toString()))
                .andExpect(jsonPath("$.revokationDocumentDate").value(DEFAULT_REVOKATION_DOCUMENT_DATE.toString()))
                .andExpect(jsonPath("$.validFromDate").value(DEFAULT_VALID_FROM_DATE.toString()))
                .andExpect(jsonPath("$.validUntilDate").value(DEFAULT_VALID_UNTIL_DATE.toString()))
                .andExpect(jsonPath("$.lastUsedDate").value(DEFAULT_LAST_USED_DATE.toString()))
                .andExpect(jsonPath("$.remark").value(DEFAULT_REMARK));
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where reference equals to DEFAULT_REFERENCE
        defaultSepaMandateShouldBeFound("reference.equals=" + DEFAULT_REFERENCE);

        // Get all the sepaMandateList where reference equals to UPDATED_REFERENCE
        defaultSepaMandateShouldNotBeFound("reference.equals=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where reference in DEFAULT_REFERENCE or UPDATED_REFERENCE
        defaultSepaMandateShouldBeFound("reference.in=" + DEFAULT_REFERENCE + "," + UPDATED_REFERENCE);

        // Get all the sepaMandateList where reference equals to UPDATED_REFERENCE
        defaultSepaMandateShouldNotBeFound("reference.in=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where reference is not null
        defaultSepaMandateShouldBeFound("reference.specified=true");

        // Get all the sepaMandateList where reference is null
        defaultSepaMandateShouldNotBeFound("reference.specified=false");
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByIbanIsEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where iban equals to DEFAULT_IBAN
        defaultSepaMandateShouldBeFound("iban.equals=" + DEFAULT_IBAN);

        // Get all the sepaMandateList where iban equals to UPDATED_IBAN
        defaultSepaMandateShouldNotBeFound("iban.equals=" + UPDATED_IBAN);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByIbanIsInShouldWork() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where iban in DEFAULT_IBAN or UPDATED_IBAN
        defaultSepaMandateShouldBeFound("iban.in=" + DEFAULT_IBAN + "," + UPDATED_IBAN);

        // Get all the sepaMandateList where iban equals to UPDATED_IBAN
        defaultSepaMandateShouldNotBeFound("iban.in=" + UPDATED_IBAN);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByIbanIsNullOrNotNull() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where iban is not null
        defaultSepaMandateShouldBeFound("iban.specified=true");

        // Get all the sepaMandateList where iban is null
        defaultSepaMandateShouldNotBeFound("iban.specified=false");
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByBicIsEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where bic equals to DEFAULT_BIC
        defaultSepaMandateShouldBeFound("bic.equals=" + DEFAULT_BIC);

        // Get all the sepaMandateList where bic equals to UPDATED_BIC
        defaultSepaMandateShouldNotBeFound("bic.equals=" + UPDATED_BIC);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByBicIsInShouldWork() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where bic in DEFAULT_BIC or UPDATED_BIC
        defaultSepaMandateShouldBeFound("bic.in=" + DEFAULT_BIC + "," + UPDATED_BIC);

        // Get all the sepaMandateList where bic equals to UPDATED_BIC
        defaultSepaMandateShouldNotBeFound("bic.in=" + UPDATED_BIC);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByBicIsNullOrNotNull() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where bic is not null
        defaultSepaMandateShouldBeFound("bic.specified=true");

        // Get all the sepaMandateList where bic is null
        defaultSepaMandateShouldNotBeFound("bic.specified=false");
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByGrantingDocumentDateIsEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where grantingDocumentDate equals to DEFAULT_GRANTING_DOCUMENT_DATE
        defaultSepaMandateShouldBeFound("grantingDocumentDate.equals=" + DEFAULT_GRANTING_DOCUMENT_DATE);

        // Get all the sepaMandateList where grantingDocumentDate equals to UPDATED_GRANTING_DOCUMENT_DATE
        defaultSepaMandateShouldNotBeFound("grantingDocumentDate.equals=" + UPDATED_GRANTING_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByGrantingDocumentDateIsInShouldWork() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where grantingDocumentDate in DEFAULT_GRANTING_DOCUMENT_DATE or
        // UPDATED_GRANTING_DOCUMENT_DATE
        defaultSepaMandateShouldBeFound(
                "grantingDocumentDate.in=" + DEFAULT_GRANTING_DOCUMENT_DATE + "," + UPDATED_GRANTING_DOCUMENT_DATE);

        // Get all the sepaMandateList where grantingDocumentDate equals to UPDATED_GRANTING_DOCUMENT_DATE
        defaultSepaMandateShouldNotBeFound("grantingDocumentDate.in=" + UPDATED_GRANTING_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByGrantingDocumentDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where grantingDocumentDate is not null
        defaultSepaMandateShouldBeFound("grantingDocumentDate.specified=true");

        // Get all the sepaMandateList where grantingDocumentDate is null
        defaultSepaMandateShouldNotBeFound("grantingDocumentDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByGrantingDocumentDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where grantingDocumentDate greater than or equals to DEFAULT_GRANTING_DOCUMENT_DATE
        defaultSepaMandateShouldBeFound("grantingDocumentDate.greaterOrEqualThan=" + DEFAULT_GRANTING_DOCUMENT_DATE);

        // Get all the sepaMandateList where grantingDocumentDate greater than or equals to UPDATED_GRANTING_DOCUMENT_DATE
        defaultSepaMandateShouldNotBeFound("grantingDocumentDate.greaterOrEqualThan=" + UPDATED_GRANTING_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByGrantingDocumentDateIsLessThanSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where grantingDocumentDate less than or equals to DEFAULT_GRANTING_DOCUMENT_DATE
        defaultSepaMandateShouldNotBeFound("grantingDocumentDate.lessThan=" + DEFAULT_GRANTING_DOCUMENT_DATE);

        // Get all the sepaMandateList where grantingDocumentDate less than or equals to UPDATED_GRANTING_DOCUMENT_DATE
        defaultSepaMandateShouldBeFound("grantingDocumentDate.lessThan=" + UPDATED_GRANTING_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByRevokationDocumentDateIsEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where revokationDocumentDate equals to DEFAULT_REVOKATION_DOCUMENT_DATE
        defaultSepaMandateShouldBeFound("revokationDocumentDate.equals=" + DEFAULT_REVOKATION_DOCUMENT_DATE);

        // Get all the sepaMandateList where revokationDocumentDate equals to UPDATED_REVOKATION_DOCUMENT_DATE
        defaultSepaMandateShouldNotBeFound("revokationDocumentDate.equals=" + UPDATED_REVOKATION_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByRevokationDocumentDateIsInShouldWork() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where revokationDocumentDate in DEFAULT_REVOKATION_DOCUMENT_DATE or
        // UPDATED_REVOKATION_DOCUMENT_DATE
        defaultSepaMandateShouldBeFound(
                "revokationDocumentDate.in=" + DEFAULT_REVOKATION_DOCUMENT_DATE + "," + UPDATED_REVOKATION_DOCUMENT_DATE);

        // Get all the sepaMandateList where revokationDocumentDate equals to UPDATED_REVOKATION_DOCUMENT_DATE
        defaultSepaMandateShouldNotBeFound("revokationDocumentDate.in=" + UPDATED_REVOKATION_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByRevokationDocumentDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where revokationDocumentDate is not null
        defaultSepaMandateShouldBeFound("revokationDocumentDate.specified=true");

        // Get all the sepaMandateList where revokationDocumentDate is null
        defaultSepaMandateShouldNotBeFound("revokationDocumentDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByRevokationDocumentDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where revokationDocumentDate greater than or equals to DEFAULT_REVOKATION_DOCUMENT_DATE
        defaultSepaMandateShouldBeFound("revokationDocumentDate.greaterOrEqualThan=" + DEFAULT_REVOKATION_DOCUMENT_DATE);

        // Get all the sepaMandateList where revokationDocumentDate greater than or equals to UPDATED_REVOKATION_DOCUMENT_DATE
        defaultSepaMandateShouldNotBeFound("revokationDocumentDate.greaterOrEqualThan=" + UPDATED_REVOKATION_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByRevokationDocumentDateIsLessThanSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where revokationDocumentDate less than or equals to DEFAULT_REVOKATION_DOCUMENT_DATE
        defaultSepaMandateShouldNotBeFound("revokationDocumentDate.lessThan=" + DEFAULT_REVOKATION_DOCUMENT_DATE);

        // Get all the sepaMandateList where revokationDocumentDate less than or equals to UPDATED_REVOKATION_DOCUMENT_DATE
        defaultSepaMandateShouldBeFound("revokationDocumentDate.lessThan=" + UPDATED_REVOKATION_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidFromDateIsEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validFromDate equals to DEFAULT_VALID_FROM_DATE
        defaultSepaMandateShouldBeFound("validFromDate.equals=" + DEFAULT_VALID_FROM_DATE);

        // Get all the sepaMandateList where validFromDate equals to UPDATED_VALID_FROM_DATE
        defaultSepaMandateShouldNotBeFound("validFromDate.equals=" + UPDATED_VALID_FROM_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidFromDateIsInShouldWork() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validFromDate in DEFAULT_VALID_FROM_DATE or UPDATED_VALID_FROM_DATE
        defaultSepaMandateShouldBeFound("validFromDate.in=" + DEFAULT_VALID_FROM_DATE + "," + UPDATED_VALID_FROM_DATE);

        // Get all the sepaMandateList where validFromDate equals to UPDATED_VALID_FROM_DATE
        defaultSepaMandateShouldNotBeFound("validFromDate.in=" + UPDATED_VALID_FROM_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidFromDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validFromDate is not null
        defaultSepaMandateShouldBeFound("validFromDate.specified=true");

        // Get all the sepaMandateList where validFromDate is null
        defaultSepaMandateShouldNotBeFound("validFromDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidFromDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validFromDate greater than or equals to DEFAULT_VALID_FROM_DATE
        defaultSepaMandateShouldBeFound("validFromDate.greaterOrEqualThan=" + DEFAULT_VALID_FROM_DATE);

        // Get all the sepaMandateList where validFromDate greater than or equals to UPDATED_VALID_FROM_DATE
        defaultSepaMandateShouldNotBeFound("validFromDate.greaterOrEqualThan=" + UPDATED_VALID_FROM_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidFromDateIsLessThanSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validFromDate less than or equals to DEFAULT_VALID_FROM_DATE
        defaultSepaMandateShouldNotBeFound("validFromDate.lessThan=" + DEFAULT_VALID_FROM_DATE);

        // Get all the sepaMandateList where validFromDate less than or equals to UPDATED_VALID_FROM_DATE
        defaultSepaMandateShouldBeFound("validFromDate.lessThan=" + UPDATED_VALID_FROM_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidUntilDateIsEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validUntilDate equals to DEFAULT_VALID_UNTIL_DATE
        defaultSepaMandateShouldBeFound("validUntilDate.equals=" + DEFAULT_VALID_UNTIL_DATE);

        // Get all the sepaMandateList where validUntilDate equals to UPDATED_VALID_UNTIL_DATE
        defaultSepaMandateShouldNotBeFound("validUntilDate.equals=" + UPDATED_VALID_UNTIL_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidUntilDateIsInShouldWork() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validUntilDate in DEFAULT_VALID_UNTIL_DATE or UPDATED_VALID_UNTIL_DATE
        defaultSepaMandateShouldBeFound("validUntilDate.in=" + DEFAULT_VALID_UNTIL_DATE + "," + UPDATED_VALID_UNTIL_DATE);

        // Get all the sepaMandateList where validUntilDate equals to UPDATED_VALID_UNTIL_DATE
        defaultSepaMandateShouldNotBeFound("validUntilDate.in=" + UPDATED_VALID_UNTIL_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidUntilDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validUntilDate is not null
        defaultSepaMandateShouldBeFound("validUntilDate.specified=true");

        // Get all the sepaMandateList where validUntilDate is null
        defaultSepaMandateShouldNotBeFound("validUntilDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidUntilDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validUntilDate greater than or equals to DEFAULT_VALID_UNTIL_DATE
        defaultSepaMandateShouldBeFound("validUntilDate.greaterOrEqualThan=" + DEFAULT_VALID_UNTIL_DATE);

        // Get all the sepaMandateList where validUntilDate greater than or equals to UPDATED_VALID_UNTIL_DATE
        defaultSepaMandateShouldNotBeFound("validUntilDate.greaterOrEqualThan=" + UPDATED_VALID_UNTIL_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidUntilDateIsLessThanSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validUntilDate less than or equals to DEFAULT_VALID_UNTIL_DATE
        defaultSepaMandateShouldNotBeFound("validUntilDate.lessThan=" + DEFAULT_VALID_UNTIL_DATE);

        // Get all the sepaMandateList where validUntilDate less than or equals to UPDATED_VALID_UNTIL_DATE
        defaultSepaMandateShouldBeFound("validUntilDate.lessThan=" + UPDATED_VALID_UNTIL_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByLastUsedDateIsEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where lastUsedDate equals to DEFAULT_LAST_USED_DATE
        defaultSepaMandateShouldBeFound("lastUsedDate.equals=" + DEFAULT_LAST_USED_DATE);

        // Get all the sepaMandateList where lastUsedDate equals to UPDATED_LAST_USED_DATE
        defaultSepaMandateShouldNotBeFound("lastUsedDate.equals=" + UPDATED_LAST_USED_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByLastUsedDateIsInShouldWork() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where lastUsedDate in DEFAULT_LAST_USED_DATE or UPDATED_LAST_USED_DATE
        defaultSepaMandateShouldBeFound("lastUsedDate.in=" + DEFAULT_LAST_USED_DATE + "," + UPDATED_LAST_USED_DATE);

        // Get all the sepaMandateList where lastUsedDate equals to UPDATED_LAST_USED_DATE
        defaultSepaMandateShouldNotBeFound("lastUsedDate.in=" + UPDATED_LAST_USED_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByLastUsedDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where lastUsedDate is not null
        defaultSepaMandateShouldBeFound("lastUsedDate.specified=true");

        // Get all the sepaMandateList where lastUsedDate is null
        defaultSepaMandateShouldNotBeFound("lastUsedDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByLastUsedDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where lastUsedDate greater than or equals to DEFAULT_LAST_USED_DATE
        defaultSepaMandateShouldBeFound("lastUsedDate.greaterOrEqualThan=" + DEFAULT_LAST_USED_DATE);

        // Get all the sepaMandateList where lastUsedDate greater than or equals to UPDATED_LAST_USED_DATE
        defaultSepaMandateShouldNotBeFound("lastUsedDate.greaterOrEqualThan=" + UPDATED_LAST_USED_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByLastUsedDateIsLessThanSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where lastUsedDate less than or equals to DEFAULT_LAST_USED_DATE
        defaultSepaMandateShouldNotBeFound("lastUsedDate.lessThan=" + DEFAULT_LAST_USED_DATE);

        // Get all the sepaMandateList where lastUsedDate less than or equals to UPDATED_LAST_USED_DATE
        defaultSepaMandateShouldBeFound("lastUsedDate.lessThan=" + UPDATED_LAST_USED_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByRemarkIsEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where remark equals to DEFAULT_REMARK
        defaultSepaMandateShouldBeFound("remark.equals=" + DEFAULT_REMARK);

        // Get all the sepaMandateList where remark equals to UPDATED_REMARK
        defaultSepaMandateShouldNotBeFound("remark.equals=" + UPDATED_REMARK);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByRemarkIsInShouldWork() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where remark in DEFAULT_REMARK or UPDATED_REMARK
        defaultSepaMandateShouldBeFound("remark.in=" + DEFAULT_REMARK + "," + UPDATED_REMARK);

        // Get all the sepaMandateList where remark equals to UPDATED_REMARK
        defaultSepaMandateShouldNotBeFound("remark.in=" + UPDATED_REMARK);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByRemarkIsNullOrNotNull() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where remark is not null
        defaultSepaMandateShouldBeFound("remark.specified=true");

        // Get all the sepaMandateList where remark is null
        defaultSepaMandateShouldNotBeFound("remark.specified=false");
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByCustomerIsEqualToSomething() throws Exception {
        // Initialize the database
        Customer customer = CustomerResourceIntTest.createPersistentEntity(em);
        sepaMandate.setCustomer(customer);
        sepaMandateRepository.saveAndFlush(sepaMandate);
        Long customerId = customer.getId();

        // Get all the sepaMandateList where customer equals to customerId
        defaultSepaMandateShouldBeFound("customerId.equals=" + customerId);

        // Get all the sepaMandateList where customer equals to customerId + 1
        defaultSepaMandateShouldNotBeFound("customerId.equals=" + (customerId + 1));
    }

    /**
     * Executes the search, and checks that the default entity is returned
     */
    private void defaultSepaMandateShouldBeFound(String filter) throws Exception {
        restSepaMandateMockMvc.perform(get("/api/sepa-mandates?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(sepaMandate.getId().intValue())))
                .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
                .andExpect(jsonPath("$.[*].iban").value(hasItem(DEFAULT_IBAN)))
                .andExpect(jsonPath("$.[*].bic").value(hasItem(DEFAULT_BIC)))
                .andExpect(jsonPath("$.[*].grantingDocumentDate").value(hasItem(DEFAULT_GRANTING_DOCUMENT_DATE.toString())))
                .andExpect(jsonPath("$.[*].revokationDocumentDate").value(hasItem(DEFAULT_REVOKATION_DOCUMENT_DATE.toString())))
                .andExpect(jsonPath("$.[*].validFromDate").value(hasItem(DEFAULT_VALID_FROM_DATE.toString())))
                .andExpect(jsonPath("$.[*].validUntilDate").value(hasItem(DEFAULT_VALID_UNTIL_DATE.toString())))
                .andExpect(jsonPath("$.[*].lastUsedDate").value(hasItem(DEFAULT_LAST_USED_DATE.toString())))
                .andExpect(jsonPath("$.[*].remark").value(hasItem(DEFAULT_REMARK)));

        // Check, that the count call also returns 1
        restSepaMandateMockMvc.perform(get("/api/sepa-mandates/count?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned
     */
    private void defaultSepaMandateShouldNotBeFound(String filter) throws Exception {
        restSepaMandateMockMvc.perform(get("/api/sepa-mandates?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restSepaMandateMockMvc.perform(get("/api/sepa-mandates/count?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    public void getNonExistingSepaMandate() throws Exception {
        // Get the sepaMandate
        restSepaMandateMockMvc.perform(get("/api/sepa-mandates/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateSepaMandate() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        int databaseSizeBeforeUpdate = sepaMandateRepository.findAll().size();

        // Update the sepaMandate
        SepaMandate updatedSepaMandate = sepaMandateRepository.findById(sepaMandate.getId()).get();
        // Disconnect from session so that the updates on updatedSepaMandate are not directly saved in db
        em.detach(updatedSepaMandate);
        updatedSepaMandate
                .revokationDocumentDate(UPDATED_REVOKATION_DOCUMENT_DATE)
                .validUntilDate(UPDATED_VALID_UNTIL_DATE)
                .lastUsedDate(UPDATED_LAST_USED_DATE)
                .remark(UPDATED_REMARK);
        SepaMandateDTO sepaMandateDTO = sepaMandateMapper.toDto(updatedSepaMandate);

        restSepaMandateMockMvc.perform(
                put("/api/sepa-mandates")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(sepaMandateDTO)))
                .andExpect(status().isOk());

        // Validate the SepaMandate in the database
        List<SepaMandate> sepaMandateList = sepaMandateRepository.findAll();
        assertThat(sepaMandateList).hasSize(databaseSizeBeforeUpdate);
        SepaMandate testSepaMandate = sepaMandateList.get(sepaMandateList.size() - 1);
        assertThat(testSepaMandate.getReference()).isEqualTo(DEFAULT_REFERENCE);
        assertThat(testSepaMandate.getIban()).isEqualTo(DEFAULT_IBAN);
        assertThat(testSepaMandate.getBic()).isEqualTo(DEFAULT_BIC);
        assertThat(testSepaMandate.getGrantingDocumentDate()).isEqualTo(DEFAULT_GRANTING_DOCUMENT_DATE);
        assertThat(testSepaMandate.getRevokationDocumentDate()).isEqualTo(UPDATED_REVOKATION_DOCUMENT_DATE);
        assertThat(testSepaMandate.getValidFromDate()).isEqualTo(DEFAULT_VALID_FROM_DATE);
        assertThat(testSepaMandate.getValidUntilDate()).isEqualTo(UPDATED_VALID_UNTIL_DATE);
        assertThat(testSepaMandate.getLastUsedDate()).isEqualTo(UPDATED_LAST_USED_DATE);
        assertThat(testSepaMandate.getRemark()).isEqualTo(UPDATED_REMARK);
    }

    @Test
    @Transactional
    public void updateNonExistingSepaMandate() throws Exception {
        int databaseSizeBeforeUpdate = sepaMandateRepository.findAll().size();

        // Create the SepaMandate
        SepaMandateDTO sepaMandateDTO = sepaMandateMapper.toDto(sepaMandate);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSepaMandateMockMvc.perform(
                put("/api/sepa-mandates")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(sepaMandateDTO)))
                .andExpect(status().isBadRequest());

        // Validate the SepaMandate in the database
        List<SepaMandate> sepaMandateList = sepaMandateRepository.findAll();
        assertThat(sepaMandateList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteSepaMandate() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        int databaseSizeBeforeDelete = sepaMandateRepository.findAll().size();

        // Delete the sepaMandate
        restSepaMandateMockMvc.perform(
                delete("/api/sepa-mandates/{id}", sepaMandate.getId())
                        .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<SepaMandate> sepaMandateList = sepaMandateRepository.findAll();
        assertThat(sepaMandateList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(SepaMandate.class);
        SepaMandate sepaMandate1 = new SepaMandate();
        sepaMandate1.setId(1L);
        SepaMandate sepaMandate2 = new SepaMandate();
        sepaMandate2.setId(sepaMandate1.getId());
        assertThat(sepaMandate1).isEqualTo(sepaMandate2);
        sepaMandate2.setId(2L);
        assertThat(sepaMandate1).isNotEqualTo(sepaMandate2);
        sepaMandate1.setId(null);
        assertThat(sepaMandate1).isNotEqualTo(sepaMandate2);
    }

    @Test
    @Transactional
    public void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(SepaMandateDTO.class);
        SepaMandateDTO sepaMandateDTO1 = new SepaMandateDTO();
        sepaMandateDTO1.setId(1L);
        SepaMandateDTO sepaMandateDTO2 = new SepaMandateDTO();
        assertThat(sepaMandateDTO1).isNotEqualTo(sepaMandateDTO2);
        sepaMandateDTO2.setId(sepaMandateDTO1.getId());
        assertThat(sepaMandateDTO1).isEqualTo(sepaMandateDTO2);
        sepaMandateDTO2.setId(2L);
        assertThat(sepaMandateDTO1).isNotEqualTo(sepaMandateDTO2);
        sepaMandateDTO1.setId(null);
        assertThat(sepaMandateDTO1).isNotEqualTo(sepaMandateDTO2);
    }

    @Test
    @Transactional
    public void testEntityFromId() {
        assertThat(sepaMandateMapper.fromId(42L).getId()).isEqualTo(42);
        assertThat(sepaMandateMapper.fromId(null)).isNull();
    }
}
