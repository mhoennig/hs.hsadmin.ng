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

    private static final LocalDate DEFAULT_CREATED = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_CREATED = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_VALID_FROM = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_VALID_FROM = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_VALID_TO = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_VALID_TO = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_LAST_USED = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_LAST_USED = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_CANCELLED = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_CANCELLED = LocalDate.now(ZoneId.systemDefault());

    private static final String DEFAULT_COMMENT = "AAAAAAAAAA";
    private static final String UPDATED_COMMENT = "BBBBBBBBBB";

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
            .created(DEFAULT_CREATED)
            .validFrom(DEFAULT_VALID_FROM)
            .validTo(DEFAULT_VALID_TO)
            .lastUsed(DEFAULT_LAST_USED)
            .cancelled(DEFAULT_CANCELLED)
            .comment(DEFAULT_COMMENT);
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
        assertThat(testSepaMandate.getCreated()).isEqualTo(DEFAULT_CREATED);
        assertThat(testSepaMandate.getValidFrom()).isEqualTo(DEFAULT_VALID_FROM);
        assertThat(testSepaMandate.getValidTo()).isEqualTo(DEFAULT_VALID_TO);
        assertThat(testSepaMandate.getLastUsed()).isEqualTo(DEFAULT_LAST_USED);
        assertThat(testSepaMandate.getCancelled()).isEqualTo(DEFAULT_CANCELLED);
        assertThat(testSepaMandate.getComment()).isEqualTo(DEFAULT_COMMENT);
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
    public void checkCreatedIsRequired() throws Exception {
        int databaseSizeBeforeTest = sepaMandateRepository.findAll().size();
        // set the field null
        sepaMandate.setCreated(null);

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
            .andExpect(jsonPath("$.[*].created").value(hasItem(DEFAULT_CREATED.toString())))
            .andExpect(jsonPath("$.[*].validFrom").value(hasItem(DEFAULT_VALID_FROM.toString())))
            .andExpect(jsonPath("$.[*].validTo").value(hasItem(DEFAULT_VALID_TO.toString())))
            .andExpect(jsonPath("$.[*].lastUsed").value(hasItem(DEFAULT_LAST_USED.toString())))
            .andExpect(jsonPath("$.[*].cancelled").value(hasItem(DEFAULT_CANCELLED.toString())))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT.toString())));
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
            .andExpect(jsonPath("$.created").value(DEFAULT_CREATED.toString()))
            .andExpect(jsonPath("$.validFrom").value(DEFAULT_VALID_FROM.toString()))
            .andExpect(jsonPath("$.validTo").value(DEFAULT_VALID_TO.toString()))
            .andExpect(jsonPath("$.lastUsed").value(DEFAULT_LAST_USED.toString()))
            .andExpect(jsonPath("$.cancelled").value(DEFAULT_CANCELLED.toString()))
            .andExpect(jsonPath("$.comment").value(DEFAULT_COMMENT.toString()));
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
    public void getAllSepaMandatesByCreatedIsEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where created equals to DEFAULT_CREATED
        defaultSepaMandateShouldBeFound("created.equals=" + DEFAULT_CREATED);

        // Get all the sepaMandateList where created equals to UPDATED_CREATED
        defaultSepaMandateShouldNotBeFound("created.equals=" + UPDATED_CREATED);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByCreatedIsInShouldWork() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where created in DEFAULT_CREATED or UPDATED_CREATED
        defaultSepaMandateShouldBeFound("created.in=" + DEFAULT_CREATED + "," + UPDATED_CREATED);

        // Get all the sepaMandateList where created equals to UPDATED_CREATED
        defaultSepaMandateShouldNotBeFound("created.in=" + UPDATED_CREATED);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByCreatedIsNullOrNotNull() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where created is not null
        defaultSepaMandateShouldBeFound("created.specified=true");

        // Get all the sepaMandateList where created is null
        defaultSepaMandateShouldNotBeFound("created.specified=false");
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByCreatedIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where created greater than or equals to DEFAULT_CREATED
        defaultSepaMandateShouldBeFound("created.greaterOrEqualThan=" + DEFAULT_CREATED);

        // Get all the sepaMandateList where created greater than or equals to UPDATED_CREATED
        defaultSepaMandateShouldNotBeFound("created.greaterOrEqualThan=" + UPDATED_CREATED);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByCreatedIsLessThanSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where created less than or equals to DEFAULT_CREATED
        defaultSepaMandateShouldNotBeFound("created.lessThan=" + DEFAULT_CREATED);

        // Get all the sepaMandateList where created less than or equals to UPDATED_CREATED
        defaultSepaMandateShouldBeFound("created.lessThan=" + UPDATED_CREATED);
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
    public void getAllSepaMandatesByValidToIsEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validTo equals to DEFAULT_VALID_TO
        defaultSepaMandateShouldBeFound("validTo.equals=" + DEFAULT_VALID_TO);

        // Get all the sepaMandateList where validTo equals to UPDATED_VALID_TO
        defaultSepaMandateShouldNotBeFound("validTo.equals=" + UPDATED_VALID_TO);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidToIsInShouldWork() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validTo in DEFAULT_VALID_TO or UPDATED_VALID_TO
        defaultSepaMandateShouldBeFound("validTo.in=" + DEFAULT_VALID_TO + "," + UPDATED_VALID_TO);

        // Get all the sepaMandateList where validTo equals to UPDATED_VALID_TO
        defaultSepaMandateShouldNotBeFound("validTo.in=" + UPDATED_VALID_TO);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidToIsNullOrNotNull() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validTo is not null
        defaultSepaMandateShouldBeFound("validTo.specified=true");

        // Get all the sepaMandateList where validTo is null
        defaultSepaMandateShouldNotBeFound("validTo.specified=false");
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidToIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validTo greater than or equals to DEFAULT_VALID_TO
        defaultSepaMandateShouldBeFound("validTo.greaterOrEqualThan=" + DEFAULT_VALID_TO);

        // Get all the sepaMandateList where validTo greater than or equals to UPDATED_VALID_TO
        defaultSepaMandateShouldNotBeFound("validTo.greaterOrEqualThan=" + UPDATED_VALID_TO);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByValidToIsLessThanSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where validTo less than or equals to DEFAULT_VALID_TO
        defaultSepaMandateShouldNotBeFound("validTo.lessThan=" + DEFAULT_VALID_TO);

        // Get all the sepaMandateList where validTo less than or equals to UPDATED_VALID_TO
        defaultSepaMandateShouldBeFound("validTo.lessThan=" + UPDATED_VALID_TO);
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
    public void getAllSepaMandatesByCancelledIsEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where cancelled equals to DEFAULT_CANCELLED
        defaultSepaMandateShouldBeFound("cancelled.equals=" + DEFAULT_CANCELLED);

        // Get all the sepaMandateList where cancelled equals to UPDATED_CANCELLED
        defaultSepaMandateShouldNotBeFound("cancelled.equals=" + UPDATED_CANCELLED);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByCancelledIsInShouldWork() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where cancelled in DEFAULT_CANCELLED or UPDATED_CANCELLED
        defaultSepaMandateShouldBeFound("cancelled.in=" + DEFAULT_CANCELLED + "," + UPDATED_CANCELLED);

        // Get all the sepaMandateList where cancelled equals to UPDATED_CANCELLED
        defaultSepaMandateShouldNotBeFound("cancelled.in=" + UPDATED_CANCELLED);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByCancelledIsNullOrNotNull() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where cancelled is not null
        defaultSepaMandateShouldBeFound("cancelled.specified=true");

        // Get all the sepaMandateList where cancelled is null
        defaultSepaMandateShouldNotBeFound("cancelled.specified=false");
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByCancelledIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where cancelled greater than or equals to DEFAULT_CANCELLED
        defaultSepaMandateShouldBeFound("cancelled.greaterOrEqualThan=" + DEFAULT_CANCELLED);

        // Get all the sepaMandateList where cancelled greater than or equals to UPDATED_CANCELLED
        defaultSepaMandateShouldNotBeFound("cancelled.greaterOrEqualThan=" + UPDATED_CANCELLED);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByCancelledIsLessThanSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where cancelled less than or equals to DEFAULT_CANCELLED
        defaultSepaMandateShouldNotBeFound("cancelled.lessThan=" + DEFAULT_CANCELLED);

        // Get all the sepaMandateList where cancelled less than or equals to UPDATED_CANCELLED
        defaultSepaMandateShouldBeFound("cancelled.lessThan=" + UPDATED_CANCELLED);
    }


    @Test
    @Transactional
    public void getAllSepaMandatesByCommentIsEqualToSomething() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where comment equals to DEFAULT_COMMENT
        defaultSepaMandateShouldBeFound("comment.equals=" + DEFAULT_COMMENT);

        // Get all the sepaMandateList where comment equals to UPDATED_COMMENT
        defaultSepaMandateShouldNotBeFound("comment.equals=" + UPDATED_COMMENT);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByCommentIsInShouldWork() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where comment in DEFAULT_COMMENT or UPDATED_COMMENT
        defaultSepaMandateShouldBeFound("comment.in=" + DEFAULT_COMMENT + "," + UPDATED_COMMENT);

        // Get all the sepaMandateList where comment equals to UPDATED_COMMENT
        defaultSepaMandateShouldNotBeFound("comment.in=" + UPDATED_COMMENT);
    }

    @Test
    @Transactional
    public void getAllSepaMandatesByCommentIsNullOrNotNull() throws Exception {
        // Initialize the database
        sepaMandateRepository.saveAndFlush(sepaMandate);

        // Get all the sepaMandateList where comment is not null
        defaultSepaMandateShouldBeFound("comment.specified=true");

        // Get all the sepaMandateList where comment is null
        defaultSepaMandateShouldNotBeFound("comment.specified=false");
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
            .andExpect(jsonPath("$.[*].created").value(hasItem(DEFAULT_CREATED.toString())))
            .andExpect(jsonPath("$.[*].validFrom").value(hasItem(DEFAULT_VALID_FROM.toString())))
            .andExpect(jsonPath("$.[*].validTo").value(hasItem(DEFAULT_VALID_TO.toString())))
            .andExpect(jsonPath("$.[*].lastUsed").value(hasItem(DEFAULT_LAST_USED.toString())))
            .andExpect(jsonPath("$.[*].cancelled").value(hasItem(DEFAULT_CANCELLED.toString())))
            .andExpect(jsonPath("$.[*].comment").value(hasItem(DEFAULT_COMMENT)));

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
            .created(UPDATED_CREATED)
            .validFrom(UPDATED_VALID_FROM)
            .validTo(UPDATED_VALID_TO)
            .lastUsed(UPDATED_LAST_USED)
            .cancelled(UPDATED_CANCELLED)
            .comment(UPDATED_COMMENT);
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
        assertThat(testSepaMandate.getCreated()).isEqualTo(UPDATED_CREATED);
        assertThat(testSepaMandate.getValidFrom()).isEqualTo(UPDATED_VALID_FROM);
        assertThat(testSepaMandate.getValidTo()).isEqualTo(UPDATED_VALID_TO);
        assertThat(testSepaMandate.getLastUsed()).isEqualTo(UPDATED_LAST_USED);
        assertThat(testSepaMandate.getCancelled()).isEqualTo(UPDATED_CANCELLED);
        assertThat(testSepaMandate.getComment()).isEqualTo(UPDATED_COMMENT);
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
