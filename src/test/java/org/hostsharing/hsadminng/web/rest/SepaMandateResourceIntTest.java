package org.hostsharing.hsadminng.web.rest;

import org.hostsharing.hsadminng.HsadminNgApp;

import org.hostsharing.hsadminng.domain.SepaMandate;
import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.repository.SepaMandateRepository;
import org.hostsharing.hsadminng.service.SepaMandateService;
import org.hostsharing.hsadminng.service.dto.SepaMandateDTO;
import org.hostsharing.hsadminng.service.mapper.SepaMandateMapper;
import org.hostsharing.hsadminng.web.rest.errors.ExceptionTranslator;
import org.hostsharing.hsadminng.service.dto.SepaMandateCriteria;
import org.hostsharing.hsadminng.service.SepaMandateQueryService;

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

    private static final LocalDate DEFAULT_DOCUMENT_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DOCUMENT_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_VALID_FROM = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_VALID_FROM = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_VALID_UNTIL = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_VALID_UNTIL = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_LAST_USED = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_LAST_USED = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_CANCELLATION_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_CANCELLATION_DATE = LocalDate.now(ZoneId.systemDefault());

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

    private MockMvc restSepaMandateMockMvc;

    private SepaMandate sepaMandate;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final SepaMandateResource sepaMandateResource = new SepaMandateResource(sepaMandateService, sepaMandateQueryService);
        this.restSepaMandateMockMvc = MockMvcBuilders.standaloneSetup(sepaMandateResource)
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
    public static SepaMandate createEntity(EntityManager em) {
        SepaMandate sepaMandate = new SepaMandate()
            .reference(DEFAULT_REFERENCE)
            .iban(DEFAULT_IBAN)
            .bic(DEFAULT_BIC)
            .documentDate(DEFAULT_DOCUMENT_DATE)
            .validFrom(DEFAULT_VALID_FROM)
            .validUntil(DEFAULT_VALID_UNTIL)
            .lastUsed(DEFAULT_LAST_USED)
            .cancellationDate(DEFAULT_CANCELLATION_DATE)
            .remark(DEFAULT_REMARK);
        // Add required entity
        Customer customer = CustomerResourceIntTest.createEntity(em);
        em.persist(customer);
        em.flush();
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
        restSepaMandateMockMvc.perform(post("/api/sepa-mandates")
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
        assertThat(testSepaMandate.getDocumentDate()).isEqualTo(DEFAULT_DOCUMENT_DATE);
        assertThat(testSepaMandate.getValidFrom()).isEqualTo(DEFAULT_VALID_FROM);
        assertThat(testSepaMandate.getValidUntil()).isEqualTo(DEFAULT_VALID_UNTIL);
        assertThat(testSepaMandate.getLastUsed()).isEqualTo(DEFAULT_LAST_USED);
        assertThat(testSepaMandate.getCancellationDate()).isEqualTo(DEFAULT_CANCELLATION_DATE);
        assertThat(testSepaMandate.getRemark()).isEqualTo(DEFAULT_REMARK);
    }

    @Test
    @Transactional
    public void createSepaMandateWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = sepaMandateRepository.findAll().size();

        // Create the SepaMandate with an existing ID
        sepaMandate.setId(1L);
        SepaMandateDTO sepaMandateDTO = sepaMandateMapper.toDto(sepaMandate);

        // An entity with an existing ID cannot be created, so this API call must fail
        restSepaMandateMockMvc.perform(post("/api/sepa-mandates")
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

        restSepaMandateMockMvc.perform(post("/api/sepa-mandates")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(sepaMandateDTO)))
            .andExpect(status().isBadRequest());

        List<SepaMandate> sepaMandateList = sepaMandateRepository.findAll();
        assertThat(sepaMandateList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkDocumentDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = sepaMandateRepository.findAll().size();
        // set the field null
        sepaMandate.setDocumentDate(null);

        // Create the SepaMandate, which fails.
        SepaMandateDTO sepaMandateDTO = sepaMandateMapper.toDto(sepaMandate);

        restSepaMandateMockMvc.perform(post("/api/sepa-mandates")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(sepaMandateDTO)))
            .andExpect(status().isBadRequest());

        List<SepaMandate> sepaMandateList = sepaMandateRepository.findAll();
        assertThat(sepaMandateList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkValidFromIsRequired() throws Exception {
        int databaseSizeBeforeTest = sepaMandateRepository.findAll().size();
        // set the field null
        sepaMandate.setValidFrom(null);

        // Create the SepaMandate, which fails.
        SepaMandateDTO sepaMandateDTO = sepaMandateMapper.toDto(sepaMandate);

        restSepaMandateMockMvc.perform(post("/api/sepa-mandates")
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
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE.toString())))
            .andExpect(jsonPath("$.[*].iban").value(hasItem(DEFAULT_IBAN.toString())))
            .andExpect(jsonPath("$.[*].bic").value(hasItem(DEFAULT_BIC.toString())))
            .andExpect(jsonPath("$.[*].documentDate").value(hasItem(DEFAULT_DOCUMENT_DATE.toString())))
            .andExpect(jsonPath("$.[*].validFrom").value(hasItem(DEFAULT_VALID_FROM.toString())))
            .andExpect(jsonPath("$.[*].validUntil").value(hasItem(DEFAULT_VALID_UNTIL.toString())))
            .andExpect(jsonPath("$.[*].lastUsed").value(hasItem(DEFAULT_LAST_USED.toString())))
            .andExpect(jsonPath("$.[*].cancellationDate").value(hasItem(DEFAULT_CANCELLATION_DATE.toString())))
            .andExpect(jsonPath("$.[*].remark").value(hasItem(DEFAULT_REMARK.toString())));
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
            .andExpect(jsonPath("$.reference").value(DEFAULT_REFERENCE.toString()))
            .andExpect(jsonPath("$.iban").value(DEFAULT_IBAN.toString()))
            .andExpect(jsonPath("$.bic").value(DEFAULT_BIC.toString()))
            .andExpect(jsonPath("$.documentDate").value(DEFAULT_DOCUMENT_DATE.toString()))
            .andExpect(jsonPath("$.validFrom").value(DEFAULT_VALID_FROM.toString()))
            .andExpect(jsonPath("$.validUntil").value(DEFAULT_VALID_UNTIL.toString()))
            .andExpect(jsonPath("$.lastUsed").value(DEFAULT_LAST_USED.toString()))
            .andExpect(jsonPath("$.cancellationDate").value(DEFAULT_CANCELLATION_DATE.toString()))
            .andExpect(jsonPath("$.remark").value(DEFAULT_REMARK.toString()));
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
    public void getAllSepaMandatesByDocumentDateIsEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where documentDate equals to DEFAULT_DOCUMENT_DATE
        defaultSepaMandateShouldBeFound("documentDate.equals=" + DEFAULT_DOCUMENT_DATE);

        // Get all the sepaMandateList where documentDate equals to UPDATED_DOCUMENT_DATE
        defaultSepaMandateShouldNotBeFound("documentDate.equals=" + UPDATED_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByDocumentDateIsInShouldWork() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where documentDate in DEFAULT_DOCUMENT_DATE or UPDATED_DOCUMENT_DATE
        defaultSepaMandateShouldBeFound("documentDate.in=" + DEFAULT_DOCUMENT_DATE + "," + UPDATED_DOCUMENT_DATE);

        // Get all the sepaMandateList where documentDate equals to UPDATED_DOCUMENT_DATE
        defaultSepaMandateShouldNotBeFound("documentDate.in=" + UPDATED_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByDocumentDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where documentDate is not null
        defaultSepaMandateShouldBeFound("documentDate.specified=true");

        // Get all the sepaMandateList where documentDate is null
        defaultSepaMandateShouldNotBeFound("documentDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByDocumentDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where documentDate greater than or equals to DEFAULT_DOCUMENT_DATE
        defaultSepaMandateShouldBeFound("documentDate.greaterOrEqualThan=" + DEFAULT_DOCUMENT_DATE);

        // Get all the sepaMandateList where documentDate greater than or equals to UPDATED_DOCUMENT_DATE
        defaultSepaMandateShouldNotBeFound("documentDate.greaterOrEqualThan=" + UPDATED_DOCUMENT_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByDocumentDateIsLessThanSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where documentDate less than or equals to DEFAULT_DOCUMENT_DATE
        defaultSepaMandateShouldNotBeFound("documentDate.lessThan=" + DEFAULT_DOCUMENT_DATE);

        // Get all the sepaMandateList where documentDate less than or equals to UPDATED_DOCUMENT_DATE
        defaultSepaMandateShouldBeFound("documentDate.lessThan=" + UPDATED_DOCUMENT_DATE);
    }


    @Test
    @Transactional
    public void getAllSepaMandatesByValidFromIsEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validFrom equals to DEFAULT_VALID_FROM
        defaultSepaMandateShouldBeFound("validFrom.equals=" + DEFAULT_VALID_FROM);

        // Get all the sepaMandateList where validFrom equals to UPDATED_VALID_FROM
        defaultSepaMandateShouldNotBeFound("validFrom.equals=" + UPDATED_VALID_FROM);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidFromIsInShouldWork() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validFrom in DEFAULT_VALID_FROM or UPDATED_VALID_FROM
        defaultSepaMandateShouldBeFound("validFrom.in=" + DEFAULT_VALID_FROM + "," + UPDATED_VALID_FROM);

        // Get all the sepaMandateList where validFrom equals to UPDATED_VALID_FROM
        defaultSepaMandateShouldNotBeFound("validFrom.in=" + UPDATED_VALID_FROM);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidFromIsNullOrNotNull() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validFrom is not null
        defaultSepaMandateShouldBeFound("validFrom.specified=true");

        // Get all the sepaMandateList where validFrom is null
        defaultSepaMandateShouldNotBeFound("validFrom.specified=false");
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidFromIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validFrom greater than or equals to DEFAULT_VALID_FROM
        defaultSepaMandateShouldBeFound("validFrom.greaterOrEqualThan=" + DEFAULT_VALID_FROM);

        // Get all the sepaMandateList where validFrom greater than or equals to UPDATED_VALID_FROM
        defaultSepaMandateShouldNotBeFound("validFrom.greaterOrEqualThan=" + UPDATED_VALID_FROM);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidFromIsLessThanSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validFrom less than or equals to DEFAULT_VALID_FROM
        defaultSepaMandateShouldNotBeFound("validFrom.lessThan=" + DEFAULT_VALID_FROM);

        // Get all the sepaMandateList where validFrom less than or equals to UPDATED_VALID_FROM
        defaultSepaMandateShouldBeFound("validFrom.lessThan=" + UPDATED_VALID_FROM);
    }


    @Test
    @Transactional
    public void getAllSepaMandatesByValidUntilIsEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validUntil equals to DEFAULT_VALID_UNTIL
        defaultSepaMandateShouldBeFound("validUntil.equals=" + DEFAULT_VALID_UNTIL);

        // Get all the sepaMandateList where validUntil equals to UPDATED_VALID_UNTIL
        defaultSepaMandateShouldNotBeFound("validUntil.equals=" + UPDATED_VALID_UNTIL);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidUntilIsInShouldWork() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validUntil in DEFAULT_VALID_UNTIL or UPDATED_VALID_UNTIL
        defaultSepaMandateShouldBeFound("validUntil.in=" + DEFAULT_VALID_UNTIL + "," + UPDATED_VALID_UNTIL);

        // Get all the sepaMandateList where validUntil equals to UPDATED_VALID_UNTIL
        defaultSepaMandateShouldNotBeFound("validUntil.in=" + UPDATED_VALID_UNTIL);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidUntilIsNullOrNotNull() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validUntil is not null
        defaultSepaMandateShouldBeFound("validUntil.specified=true");

        // Get all the sepaMandateList where validUntil is null
        defaultSepaMandateShouldNotBeFound("validUntil.specified=false");
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidUntilIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validUntil greater than or equals to DEFAULT_VALID_UNTIL
        defaultSepaMandateShouldBeFound("validUntil.greaterOrEqualThan=" + DEFAULT_VALID_UNTIL);

        // Get all the sepaMandateList where validUntil greater than or equals to UPDATED_VALID_UNTIL
        defaultSepaMandateShouldNotBeFound("validUntil.greaterOrEqualThan=" + UPDATED_VALID_UNTIL);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidUntilIsLessThanSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validUntil less than or equals to DEFAULT_VALID_UNTIL
        defaultSepaMandateShouldNotBeFound("validUntil.lessThan=" + DEFAULT_VALID_UNTIL);

        // Get all the sepaMandateList where validUntil less than or equals to UPDATED_VALID_UNTIL
        defaultSepaMandateShouldBeFound("validUntil.lessThan=" + UPDATED_VALID_UNTIL);
    }


    @Test
    @Transactional
    public void getAllSepaMandatesByLastUsedIsEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where lastUsed equals to DEFAULT_LAST_USED
        defaultSepaMandateShouldBeFound("lastUsed.equals=" + DEFAULT_LAST_USED);

        // Get all the sepaMandateList where lastUsed equals to UPDATED_LAST_USED
        defaultSepaMandateShouldNotBeFound("lastUsed.equals=" + UPDATED_LAST_USED);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByLastUsedIsInShouldWork() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where lastUsed in DEFAULT_LAST_USED or UPDATED_LAST_USED
        defaultSepaMandateShouldBeFound("lastUsed.in=" + DEFAULT_LAST_USED + "," + UPDATED_LAST_USED);

        // Get all the sepaMandateList where lastUsed equals to UPDATED_LAST_USED
        defaultSepaMandateShouldNotBeFound("lastUsed.in=" + UPDATED_LAST_USED);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByLastUsedIsNullOrNotNull() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where lastUsed is not null
        defaultSepaMandateShouldBeFound("lastUsed.specified=true");

        // Get all the sepaMandateList where lastUsed is null
        defaultSepaMandateShouldNotBeFound("lastUsed.specified=false");
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByLastUsedIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where lastUsed greater than or equals to DEFAULT_LAST_USED
        defaultSepaMandateShouldBeFound("lastUsed.greaterOrEqualThan=" + DEFAULT_LAST_USED);

        // Get all the sepaMandateList where lastUsed greater than or equals to UPDATED_LAST_USED
        defaultSepaMandateShouldNotBeFound("lastUsed.greaterOrEqualThan=" + UPDATED_LAST_USED);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByLastUsedIsLessThanSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where lastUsed less than or equals to DEFAULT_LAST_USED
        defaultSepaMandateShouldNotBeFound("lastUsed.lessThan=" + DEFAULT_LAST_USED);

        // Get all the sepaMandateList where lastUsed less than or equals to UPDATED_LAST_USED
        defaultSepaMandateShouldBeFound("lastUsed.lessThan=" + UPDATED_LAST_USED);
    }


    @Test
    @Transactional
    public void getAllSepaMandatesByCancellationDateIsEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where cancellationDate equals to DEFAULT_CANCELLATION_DATE
        defaultSepaMandateShouldBeFound("cancellationDate.equals=" + DEFAULT_CANCELLATION_DATE);

        // Get all the sepaMandateList where cancellationDate equals to UPDATED_CANCELLATION_DATE
        defaultSepaMandateShouldNotBeFound("cancellationDate.equals=" + UPDATED_CANCELLATION_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByCancellationDateIsInShouldWork() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where cancellationDate in DEFAULT_CANCELLATION_DATE or UPDATED_CANCELLATION_DATE
        defaultSepaMandateShouldBeFound("cancellationDate.in=" + DEFAULT_CANCELLATION_DATE + "," + UPDATED_CANCELLATION_DATE);

        // Get all the sepaMandateList where cancellationDate equals to UPDATED_CANCELLATION_DATE
        defaultSepaMandateShouldNotBeFound("cancellationDate.in=" + UPDATED_CANCELLATION_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByCancellationDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where cancellationDate is not null
        defaultSepaMandateShouldBeFound("cancellationDate.specified=true");

        // Get all the sepaMandateList where cancellationDate is null
        defaultSepaMandateShouldNotBeFound("cancellationDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByCancellationDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where cancellationDate greater than or equals to DEFAULT_CANCELLATION_DATE
        defaultSepaMandateShouldBeFound("cancellationDate.greaterOrEqualThan=" + DEFAULT_CANCELLATION_DATE);

        // Get all the sepaMandateList where cancellationDate greater than or equals to UPDATED_CANCELLATION_DATE
        defaultSepaMandateShouldNotBeFound("cancellationDate.greaterOrEqualThan=" + UPDATED_CANCELLATION_DATE);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByCancellationDateIsLessThanSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where cancellationDate less than or equals to DEFAULT_CANCELLATION_DATE
        defaultSepaMandateShouldNotBeFound("cancellationDate.lessThan=" + DEFAULT_CANCELLATION_DATE);

        // Get all the sepaMandateList where cancellationDate less than or equals to UPDATED_CANCELLATION_DATE
        defaultSepaMandateShouldBeFound("cancellationDate.lessThan=" + UPDATED_CANCELLATION_DATE);
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
        Customer customer = CustomerResourceIntTest.createEntity(em);
        em.persist(customer);
        em.flush();
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
            .andExpect(jsonPath("$.[*].documentDate").value(hasItem(DEFAULT_DOCUMENT_DATE.toString())))
            .andExpect(jsonPath("$.[*].validFrom").value(hasItem(DEFAULT_VALID_FROM.toString())))
            .andExpect(jsonPath("$.[*].validUntil").value(hasItem(DEFAULT_VALID_UNTIL.toString())))
            .andExpect(jsonPath("$.[*].lastUsed").value(hasItem(DEFAULT_LAST_USED.toString())))
            .andExpect(jsonPath("$.[*].cancellationDate").value(hasItem(DEFAULT_CANCELLATION_DATE.toString())))
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
            .reference(UPDATED_REFERENCE)
            .iban(UPDATED_IBAN)
            .bic(UPDATED_BIC)
            .documentDate(UPDATED_DOCUMENT_DATE)
            .validFrom(UPDATED_VALID_FROM)
            .validUntil(UPDATED_VALID_UNTIL)
            .lastUsed(UPDATED_LAST_USED)
            .cancellationDate(UPDATED_CANCELLATION_DATE)
            .remark(UPDATED_REMARK);
        SepaMandateDTO sepaMandateDTO = sepaMandateMapper.toDto(updatedSepaMandate);

        restSepaMandateMockMvc.perform(put("/api/sepa-mandates")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(sepaMandateDTO)))
            .andExpect(status().isOk());

        // Validate the SepaMandate in the database
        List<SepaMandate> sepaMandateList = sepaMandateRepository.findAll();
        assertThat(sepaMandateList).hasSize(databaseSizeBeforeUpdate);
        SepaMandate testSepaMandate = sepaMandateList.get(sepaMandateList.size() - 1);
        assertThat(testSepaMandate.getReference()).isEqualTo(UPDATED_REFERENCE);
        assertThat(testSepaMandate.getIban()).isEqualTo(UPDATED_IBAN);
        assertThat(testSepaMandate.getBic()).isEqualTo(UPDATED_BIC);
        assertThat(testSepaMandate.getDocumentDate()).isEqualTo(UPDATED_DOCUMENT_DATE);
        assertThat(testSepaMandate.getValidFrom()).isEqualTo(UPDATED_VALID_FROM);
        assertThat(testSepaMandate.getValidUntil()).isEqualTo(UPDATED_VALID_UNTIL);
        assertThat(testSepaMandate.getLastUsed()).isEqualTo(UPDATED_LAST_USED);
        assertThat(testSepaMandate.getCancellationDate()).isEqualTo(UPDATED_CANCELLATION_DATE);
        assertThat(testSepaMandate.getRemark()).isEqualTo(UPDATED_REMARK);
    }

    @Test
    @Transactional
    public void updateNonExistingSepaMandate() throws Exception {
        int databaseSizeBeforeUpdate = sepaMandateRepository.findAll().size();

        // Create the SepaMandate
        SepaMandateDTO sepaMandateDTO = sepaMandateMapper.toDto(sepaMandate);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSepaMandateMockMvc.perform(put("/api/sepa-mandates")
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
        restSepaMandateMockMvc.perform(delete("/api/sepa-mandates/{id}", sepaMandate.getId())
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
