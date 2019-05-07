// Licensed under Apache-2.0
package org.hostsharing.hsadminng.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hostsharing.hsadminng.web.rest.TestUtil.createFormattingConversionService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.hostsharing.hsadminng.HsadminNgApp;
import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.domain.SepaMandate;
import org.hostsharing.hsadminng.domain.enumeration.CustomerKind;
import org.hostsharing.hsadminng.domain.enumeration.VatRegion;
import org.hostsharing.hsadminng.repository.CustomerRepository;
import org.hostsharing.hsadminng.service.CustomerQueryService;
import org.hostsharing.hsadminng.service.CustomerService;
import org.hostsharing.hsadminng.service.dto.CustomerDTO;
import org.hostsharing.hsadminng.service.mapper.CustomerMapper;
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
 * Test class for the CustomerResource REST controller.
 *
 * @see CustomerResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = HsadminNgApp.class)
public class CustomerResourceIntTest {

    private static final Integer DEFAULT_REFERENCE = 10000;
    private static final Integer UPDATED_REFERENCE = 10001;

    private static final String DEFAULT_PREFIX = "y5j";
    private static final String UPDATED_PREFIX = "f0u";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final CustomerKind DEFAULT_KIND = CustomerKind.NATURAL;
    private static final CustomerKind UPDATED_KIND = CustomerKind.LEGAL;

    private static final LocalDate DEFAULT_BIRTH_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_BIRTH_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final String DEFAULT_BIRTH_PLACE = "AAAAAAAAAA";
    private static final String UPDATED_BIRTH_PLACE = "BBBBBBBBBB";

    private static final String DEFAULT_REGISTRATION_COURT = "AAAAAAAAAA";
    private static final String UPDATED_REGISTRATION_COURT = "BBBBBBBBBB";

    private static final String DEFAULT_REGISTRATION_NUMBER = "AAAAAAAAAA";
    private static final String UPDATED_REGISTRATION_NUMBER = "BBBBBBBBBB";

    private static final VatRegion DEFAULT_VAT_REGION = VatRegion.DOMESTIC;
    private static final VatRegion UPDATED_VAT_REGION = VatRegion.EU;

    private static final String DEFAULT_VAT_NUMBER = "AAAAAAAAAA";
    private static final String UPDATED_VAT_NUMBER = "BBBBBBBBBB";

    private static final String DEFAULT_CONTRACTUAL_SALUTATION = "AAAAAAAAAA";
    private static final String UPDATED_CONTRACTUAL_SALUTATION = "BBBBBBBBBB";

    private static final String DEFAULT_CONTRACTUAL_ADDRESS = "AAAAAAAAAA";
    private static final String UPDATED_CONTRACTUAL_ADDRESS = "BBBBBBBBBB";

    private static final String DEFAULT_BILLING_SALUTATION = "AAAAAAAAAA";
    private static final String UPDATED_BILLING_SALUTATION = "BBBBBBBBBB";

    private static final String DEFAULT_BILLING_ADDRESS = "AAAAAAAAAA";
    private static final String UPDATED_BILLING_ADDRESS = "BBBBBBBBBB";

    private static final String DEFAULT_REMARK = "AAAAAAAAAA";
    private static final String UPDATED_REMARK = "BBBBBBBBBB";

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerQueryService customerQueryService;

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

    private MockMvc restCustomerMockMvc;

    private Customer customer;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final CustomerResource customerResource = new CustomerResource(customerService, customerQueryService);
        this.restCustomerMockMvc = MockMvcBuilders.standaloneSetup(customerResource)
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
    public static Customer createEntity(EntityManager em) {
        Customer customer = new Customer()
                .reference(DEFAULT_REFERENCE)
                .prefix(DEFAULT_PREFIX)
                .name(DEFAULT_NAME)
                .kind(DEFAULT_KIND)
                .birthDate(DEFAULT_BIRTH_DATE)
                .birthPlace(DEFAULT_BIRTH_PLACE)
                .registrationCourt(DEFAULT_REGISTRATION_COURT)
                .registrationNumber(DEFAULT_REGISTRATION_NUMBER)
                .vatRegion(DEFAULT_VAT_REGION)
                .vatNumber(DEFAULT_VAT_NUMBER)
                .contractualSalutation(DEFAULT_CONTRACTUAL_SALUTATION)
                .contractualAddress(DEFAULT_CONTRACTUAL_ADDRESS)
                .billingSalutation(DEFAULT_BILLING_SALUTATION)
                .billingAddress(DEFAULT_BILLING_ADDRESS)
                .remark(DEFAULT_REMARK);
        return customer;
    }

    @Before
    public void initTest() {
        customer = createEntity(em);
    }

    @Test
    @Transactional
    public void createCustomer() throws Exception {
        int databaseSizeBeforeCreate = customerRepository.findAll().size();

        // Create the Customer
        CustomerDTO customerDTO = customerMapper.toDto(customer);
        restCustomerMockMvc.perform(
                post("/api/customers")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(customerDTO)))
                .andExpect(status().isCreated());

        // Validate the Customer in the database
        List<Customer> customerList = customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeCreate + 1);
        Customer testCustomer = customerList.get(customerList.size() - 1);
        assertThat(testCustomer.getReference()).isEqualTo(DEFAULT_REFERENCE);
        assertThat(testCustomer.getPrefix()).isEqualTo(DEFAULT_PREFIX);
        assertThat(testCustomer.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testCustomer.getKind()).isEqualTo(DEFAULT_KIND);
        assertThat(testCustomer.getBirthDate()).isEqualTo(DEFAULT_BIRTH_DATE);
        assertThat(testCustomer.getBirthPlace()).isEqualTo(DEFAULT_BIRTH_PLACE);
        assertThat(testCustomer.getRegistrationCourt()).isEqualTo(DEFAULT_REGISTRATION_COURT);
        assertThat(testCustomer.getRegistrationNumber()).isEqualTo(DEFAULT_REGISTRATION_NUMBER);
        assertThat(testCustomer.getVatRegion()).isEqualTo(DEFAULT_VAT_REGION);
        assertThat(testCustomer.getVatNumber()).isEqualTo(DEFAULT_VAT_NUMBER);
        assertThat(testCustomer.getContractualSalutation()).isEqualTo(DEFAULT_CONTRACTUAL_SALUTATION);
        assertThat(testCustomer.getContractualAddress()).isEqualTo(DEFAULT_CONTRACTUAL_ADDRESS);
        assertThat(testCustomer.getBillingSalutation()).isEqualTo(DEFAULT_BILLING_SALUTATION);
        assertThat(testCustomer.getBillingAddress()).isEqualTo(DEFAULT_BILLING_ADDRESS);
        assertThat(testCustomer.getRemark()).isEqualTo(DEFAULT_REMARK);
    }

    @Test
    @Transactional
    public void createCustomerWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = customerRepository.findAll().size();

        // Create the Customer with an existing ID
        customer.setId(1L);
        CustomerDTO customerDTO = customerMapper.toDto(customer);

        // An entity with an existing ID cannot be created, so this API call must fail
        restCustomerMockMvc.perform(
                post("/api/customers")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(customerDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Customer in the database
        List<Customer> customerList = customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkReferenceIsRequired() throws Exception {
        int databaseSizeBeforeTest = customerRepository.findAll().size();
        // set the field null
        customer.setReference(null);

        // Create the Customer, which fails.
        CustomerDTO customerDTO = customerMapper.toDto(customer);

        restCustomerMockMvc.perform(
                post("/api/customers")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(customerDTO)))
                .andExpect(status().isBadRequest());

        List<Customer> customerList = customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkPrefixIsRequired() throws Exception {
        int databaseSizeBeforeTest = customerRepository.findAll().size();
        // set the field null
        customer.setPrefix(null);

        // Create the Customer, which fails.
        CustomerDTO customerDTO = customerMapper.toDto(customer);

        restCustomerMockMvc.perform(
                post("/api/customers")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(customerDTO)))
                .andExpect(status().isBadRequest());

        List<Customer> customerList = customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = customerRepository.findAll().size();
        // set the field null
        customer.setName(null);

        // Create the Customer, which fails.
        CustomerDTO customerDTO = customerMapper.toDto(customer);

        restCustomerMockMvc.perform(
                post("/api/customers")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(customerDTO)))
                .andExpect(status().isBadRequest());

        List<Customer> customerList = customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkKindIsRequired() throws Exception {
        int databaseSizeBeforeTest = customerRepository.findAll().size();
        // set the field null
        customer.setKind(null);

        // Create the Customer, which fails.
        CustomerDTO customerDTO = customerMapper.toDto(customer);

        restCustomerMockMvc.perform(
                post("/api/customers")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(customerDTO)))
                .andExpect(status().isBadRequest());

        List<Customer> customerList = customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkVatRegionIsRequired() throws Exception {
        int databaseSizeBeforeTest = customerRepository.findAll().size();
        // set the field null
        customer.setVatRegion(null);

        // Create the Customer, which fails.
        CustomerDTO customerDTO = customerMapper.toDto(customer);

        restCustomerMockMvc.perform(
                post("/api/customers")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(customerDTO)))
                .andExpect(status().isBadRequest());

        List<Customer> customerList = customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkContractualAddressIsRequired() throws Exception {
        int databaseSizeBeforeTest = customerRepository.findAll().size();
        // set the field null
        customer.setContractualAddress(null);

        // Create the Customer, which fails.
        CustomerDTO customerDTO = customerMapper.toDto(customer);

        restCustomerMockMvc.perform(
                post("/api/customers")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(customerDTO)))
                .andExpect(status().isBadRequest());

        List<Customer> customerList = customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllCustomers() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList
        restCustomerMockMvc.perform(get("/api/customers?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(customer.getId().intValue())))
                .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
                .andExpect(jsonPath("$.[*].prefix").value(hasItem(DEFAULT_PREFIX.toString())))
                .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
                .andExpect(jsonPath("$.[*].kind").value(hasItem(DEFAULT_KIND.toString())))
                .andExpect(jsonPath("$.[*].birthDate").value(hasItem(DEFAULT_BIRTH_DATE.toString())))
                .andExpect(jsonPath("$.[*].birthPlace").value(hasItem(DEFAULT_BIRTH_PLACE.toString())))
                .andExpect(jsonPath("$.[*].registrationCourt").value(hasItem(DEFAULT_REGISTRATION_COURT.toString())))
                .andExpect(jsonPath("$.[*].registrationNumber").value(hasItem(DEFAULT_REGISTRATION_NUMBER.toString())))
                .andExpect(jsonPath("$.[*].vatRegion").value(hasItem(DEFAULT_VAT_REGION.toString())))
                .andExpect(jsonPath("$.[*].vatNumber").value(hasItem(DEFAULT_VAT_NUMBER.toString())))
                .andExpect(jsonPath("$.[*].contractualSalutation").value(hasItem(DEFAULT_CONTRACTUAL_SALUTATION.toString())))
                .andExpect(jsonPath("$.[*].contractualAddress").value(hasItem(DEFAULT_CONTRACTUAL_ADDRESS.toString())))
                .andExpect(jsonPath("$.[*].billingSalutation").value(hasItem(DEFAULT_BILLING_SALUTATION.toString())))
                .andExpect(jsonPath("$.[*].billingAddress").value(hasItem(DEFAULT_BILLING_ADDRESS.toString())))
                .andExpect(jsonPath("$.[*].remark").value(hasItem(DEFAULT_REMARK.toString())));
    }

    @Test
    @Transactional
    public void getCustomer() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get the customer
        restCustomerMockMvc.perform(get("/api/customers/{id}", customer.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.id").value(customer.getId().intValue()))
                .andExpect(jsonPath("$.reference").value(DEFAULT_REFERENCE))
                .andExpect(jsonPath("$.prefix").value(DEFAULT_PREFIX.toString()))
                .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
                .andExpect(jsonPath("$.kind").value(DEFAULT_KIND.toString()))
                .andExpect(jsonPath("$.birthDate").value(DEFAULT_BIRTH_DATE.toString()))
                .andExpect(jsonPath("$.birthPlace").value(DEFAULT_BIRTH_PLACE.toString()))
                .andExpect(jsonPath("$.registrationCourt").value(DEFAULT_REGISTRATION_COURT.toString()))
                .andExpect(jsonPath("$.registrationNumber").value(DEFAULT_REGISTRATION_NUMBER.toString()))
                .andExpect(jsonPath("$.vatRegion").value(DEFAULT_VAT_REGION.toString()))
                .andExpect(jsonPath("$.vatNumber").value(DEFAULT_VAT_NUMBER.toString()))
                .andExpect(jsonPath("$.contractualSalutation").value(DEFAULT_CONTRACTUAL_SALUTATION.toString()))
                .andExpect(jsonPath("$.contractualAddress").value(DEFAULT_CONTRACTUAL_ADDRESS.toString()))
                .andExpect(jsonPath("$.billingSalutation").value(DEFAULT_BILLING_SALUTATION.toString()))
                .andExpect(jsonPath("$.billingAddress").value(DEFAULT_BILLING_ADDRESS.toString()))
                .andExpect(jsonPath("$.remark").value(DEFAULT_REMARK.toString()));
    }

    @Test
    @Transactional
    public void getAllCustomersByReferenceIsEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where reference equals to DEFAULT_REFERENCE
        defaultCustomerShouldBeFound("reference.equals=" + DEFAULT_REFERENCE);

        // Get all the customerList where reference equals to UPDATED_REFERENCE
        defaultCustomerShouldNotBeFound("reference.equals=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    public void getAllCustomersByReferenceIsInShouldWork() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where reference in DEFAULT_REFERENCE or UPDATED_REFERENCE
        defaultCustomerShouldBeFound("reference.in=" + DEFAULT_REFERENCE + "," + UPDATED_REFERENCE);

        // Get all the customerList where reference equals to UPDATED_REFERENCE
        defaultCustomerShouldNotBeFound("reference.in=" + UPDATED_REFERENCE);
    }

    @Test
    @Transactional
    public void getAllCustomersByReferenceIsNullOrNotNull() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where reference is not null
        defaultCustomerShouldBeFound("reference.specified=true");

        // Get all the customerList where reference is null
        defaultCustomerShouldNotBeFound("reference.specified=false");
    }

    @Test
    @Transactional
    public void getAllCustomersByReferenceIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where reference greater than or equals to DEFAULT_REFERENCE
        defaultCustomerShouldBeFound("reference.greaterOrEqualThan=" + DEFAULT_REFERENCE);

        // Get all the customerList where reference greater than or equals to (DEFAULT_REFERENCE + 1)
        defaultCustomerShouldNotBeFound("reference.greaterOrEqualThan=" + (DEFAULT_REFERENCE + 1));
    }

    @Test
    @Transactional
    public void getAllCustomersByReferenceIsLessThanSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where reference less than or equals to DEFAULT_REFERENCE
        defaultCustomerShouldNotBeFound("reference.lessThan=" + DEFAULT_REFERENCE);

        // Get all the customerList where reference less than or equals to (DEFAULT_REFERENCE + 1)
        defaultCustomerShouldBeFound("reference.lessThan=" + (DEFAULT_REFERENCE + 1));
    }

    @Test
    @Transactional
    public void getAllCustomersByPrefixIsEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where prefix equals to DEFAULT_PREFIX
        defaultCustomerShouldBeFound("prefix.equals=" + DEFAULT_PREFIX);

        // Get all the customerList where prefix equals to UPDATED_PREFIX
        defaultCustomerShouldNotBeFound("prefix.equals=" + UPDATED_PREFIX);
    }

    @Test
    @Transactional
    public void getAllCustomersByPrefixIsInShouldWork() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where prefix in DEFAULT_PREFIX or UPDATED_PREFIX
        defaultCustomerShouldBeFound("prefix.in=" + DEFAULT_PREFIX + "," + UPDATED_PREFIX);

        // Get all the customerList where prefix equals to UPDATED_PREFIX
        defaultCustomerShouldNotBeFound("prefix.in=" + UPDATED_PREFIX);
    }

    @Test
    @Transactional
    public void getAllCustomersByPrefixIsNullOrNotNull() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where prefix is not null
        defaultCustomerShouldBeFound("prefix.specified=true");

        // Get all the customerList where prefix is null
        defaultCustomerShouldNotBeFound("prefix.specified=false");
    }

    @Test
    @Transactional
    public void getAllCustomersByNameIsEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where name equals to DEFAULT_NAME
        defaultCustomerShouldBeFound("name.equals=" + DEFAULT_NAME);

        // Get all the customerList where name equals to UPDATED_NAME
        defaultCustomerShouldNotBeFound("name.equals=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    public void getAllCustomersByNameIsInShouldWork() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where name in DEFAULT_NAME or UPDATED_NAME
        defaultCustomerShouldBeFound("name.in=" + DEFAULT_NAME + "," + UPDATED_NAME);

        // Get all the customerList where name equals to UPDATED_NAME
        defaultCustomerShouldNotBeFound("name.in=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    public void getAllCustomersByNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where name is not null
        defaultCustomerShouldBeFound("name.specified=true");

        // Get all the customerList where name is null
        defaultCustomerShouldNotBeFound("name.specified=false");
    }

    @Test
    @Transactional
    public void getAllCustomersByKindIsEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where kind equals to DEFAULT_KIND
        defaultCustomerShouldBeFound("kind.equals=" + DEFAULT_KIND);

        // Get all the customerList where kind equals to UPDATED_KIND
        defaultCustomerShouldNotBeFound("kind.equals=" + UPDATED_KIND);
    }

    @Test
    @Transactional
    public void getAllCustomersByKindIsInShouldWork() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where kind in DEFAULT_KIND or UPDATED_KIND
        defaultCustomerShouldBeFound("kind.in=" + DEFAULT_KIND + "," + UPDATED_KIND);

        // Get all the customerList where kind equals to UPDATED_KIND
        defaultCustomerShouldNotBeFound("kind.in=" + UPDATED_KIND);
    }

    @Test
    @Transactional
    public void getAllCustomersByKindIsNullOrNotNull() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where kind is not null
        defaultCustomerShouldBeFound("kind.specified=true");

        // Get all the customerList where kind is null
        defaultCustomerShouldNotBeFound("kind.specified=false");
    }

    @Test
    @Transactional
    public void getAllCustomersByBirthDateIsEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where birthDate equals to DEFAULT_BIRTH_DATE
        defaultCustomerShouldBeFound("birthDate.equals=" + DEFAULT_BIRTH_DATE);

        // Get all the customerList where birthDate equals to UPDATED_BIRTH_DATE
        defaultCustomerShouldNotBeFound("birthDate.equals=" + UPDATED_BIRTH_DATE);
    }

    @Test
    @Transactional
    public void getAllCustomersByBirthDateIsInShouldWork() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where birthDate in DEFAULT_BIRTH_DATE or UPDATED_BIRTH_DATE
        defaultCustomerShouldBeFound("birthDate.in=" + DEFAULT_BIRTH_DATE + "," + UPDATED_BIRTH_DATE);

        // Get all the customerList where birthDate equals to UPDATED_BIRTH_DATE
        defaultCustomerShouldNotBeFound("birthDate.in=" + UPDATED_BIRTH_DATE);
    }

    @Test
    @Transactional
    public void getAllCustomersByBirthDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where birthDate is not null
        defaultCustomerShouldBeFound("birthDate.specified=true");

        // Get all the customerList where birthDate is null
        defaultCustomerShouldNotBeFound("birthDate.specified=false");
    }

    @Test
    @Transactional
    public void getAllCustomersByBirthDateIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where birthDate greater than or equals to DEFAULT_BIRTH_DATE
        defaultCustomerShouldBeFound("birthDate.greaterOrEqualThan=" + DEFAULT_BIRTH_DATE);

        // Get all the customerList where birthDate greater than or equals to UPDATED_BIRTH_DATE
        defaultCustomerShouldNotBeFound("birthDate.greaterOrEqualThan=" + UPDATED_BIRTH_DATE);
    }

    @Test
    @Transactional
    public void getAllCustomersByBirthDateIsLessThanSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where birthDate less than or equals to DEFAULT_BIRTH_DATE
        defaultCustomerShouldNotBeFound("birthDate.lessThan=" + DEFAULT_BIRTH_DATE);

        // Get all the customerList where birthDate less than or equals to UPDATED_BIRTH_DATE
        defaultCustomerShouldBeFound("birthDate.lessThan=" + UPDATED_BIRTH_DATE);
    }

    @Test
    @Transactional
    public void getAllCustomersByBirthPlaceIsEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where birthPlace equals to DEFAULT_BIRTH_PLACE
        defaultCustomerShouldBeFound("birthPlace.equals=" + DEFAULT_BIRTH_PLACE);

        // Get all the customerList where birthPlace equals to UPDATED_BIRTH_PLACE
        defaultCustomerShouldNotBeFound("birthPlace.equals=" + UPDATED_BIRTH_PLACE);
    }

    @Test
    @Transactional
    public void getAllCustomersByBirthPlaceIsInShouldWork() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where birthPlace in DEFAULT_BIRTH_PLACE or UPDATED_BIRTH_PLACE
        defaultCustomerShouldBeFound("birthPlace.in=" + DEFAULT_BIRTH_PLACE + "," + UPDATED_BIRTH_PLACE);

        // Get all the customerList where birthPlace equals to UPDATED_BIRTH_PLACE
        defaultCustomerShouldNotBeFound("birthPlace.in=" + UPDATED_BIRTH_PLACE);
    }

    @Test
    @Transactional
    public void getAllCustomersByBirthPlaceIsNullOrNotNull() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where birthPlace is not null
        defaultCustomerShouldBeFound("birthPlace.specified=true");

        // Get all the customerList where birthPlace is null
        defaultCustomerShouldNotBeFound("birthPlace.specified=false");
    }

    @Test
    @Transactional
    public void getAllCustomersByRegistrationCourtIsEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where registrationCourt equals to DEFAULT_REGISTRATION_COURT
        defaultCustomerShouldBeFound("registrationCourt.equals=" + DEFAULT_REGISTRATION_COURT);

        // Get all the customerList where registrationCourt equals to UPDATED_REGISTRATION_COURT
        defaultCustomerShouldNotBeFound("registrationCourt.equals=" + UPDATED_REGISTRATION_COURT);
    }

    @Test
    @Transactional
    public void getAllCustomersByRegistrationCourtIsInShouldWork() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where registrationCourt in DEFAULT_REGISTRATION_COURT or UPDATED_REGISTRATION_COURT
        defaultCustomerShouldBeFound("registrationCourt.in=" + DEFAULT_REGISTRATION_COURT + "," + UPDATED_REGISTRATION_COURT);

        // Get all the customerList where registrationCourt equals to UPDATED_REGISTRATION_COURT
        defaultCustomerShouldNotBeFound("registrationCourt.in=" + UPDATED_REGISTRATION_COURT);
    }

    @Test
    @Transactional
    public void getAllCustomersByRegistrationCourtIsNullOrNotNull() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where registrationCourt is not null
        defaultCustomerShouldBeFound("registrationCourt.specified=true");

        // Get all the customerList where registrationCourt is null
        defaultCustomerShouldNotBeFound("registrationCourt.specified=false");
    }

    @Test
    @Transactional
    public void getAllCustomersByRegistrationNumberIsEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where registrationNumber equals to DEFAULT_REGISTRATION_NUMBER
        defaultCustomerShouldBeFound("registrationNumber.equals=" + DEFAULT_REGISTRATION_NUMBER);

        // Get all the customerList where registrationNumber equals to UPDATED_REGISTRATION_NUMBER
        defaultCustomerShouldNotBeFound("registrationNumber.equals=" + UPDATED_REGISTRATION_NUMBER);
    }

    @Test
    @Transactional
    public void getAllCustomersByRegistrationNumberIsInShouldWork() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where registrationNumber in DEFAULT_REGISTRATION_NUMBER or UPDATED_REGISTRATION_NUMBER
        defaultCustomerShouldBeFound(
                "registrationNumber.in=" + DEFAULT_REGISTRATION_NUMBER + "," + UPDATED_REGISTRATION_NUMBER);

        // Get all the customerList where registrationNumber equals to UPDATED_REGISTRATION_NUMBER
        defaultCustomerShouldNotBeFound("registrationNumber.in=" + UPDATED_REGISTRATION_NUMBER);
    }

    @Test
    @Transactional
    public void getAllCustomersByRegistrationNumberIsNullOrNotNull() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where registrationNumber is not null
        defaultCustomerShouldBeFound("registrationNumber.specified=true");

        // Get all the customerList where registrationNumber is null
        defaultCustomerShouldNotBeFound("registrationNumber.specified=false");
    }

    @Test
    @Transactional
    public void getAllCustomersByVatRegionIsEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where vatRegion equals to DEFAULT_VAT_REGION
        defaultCustomerShouldBeFound("vatRegion.equals=" + DEFAULT_VAT_REGION);

        // Get all the customerList where vatRegion equals to UPDATED_VAT_REGION
        defaultCustomerShouldNotBeFound("vatRegion.equals=" + UPDATED_VAT_REGION);
    }

    @Test
    @Transactional
    public void getAllCustomersByVatRegionIsInShouldWork() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where vatRegion in DEFAULT_VAT_REGION or UPDATED_VAT_REGION
        defaultCustomerShouldBeFound("vatRegion.in=" + DEFAULT_VAT_REGION + "," + UPDATED_VAT_REGION);

        // Get all the customerList where vatRegion equals to UPDATED_VAT_REGION
        defaultCustomerShouldNotBeFound("vatRegion.in=" + UPDATED_VAT_REGION);
    }

    @Test
    @Transactional
    public void getAllCustomersByVatRegionIsNullOrNotNull() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where vatRegion is not null
        defaultCustomerShouldBeFound("vatRegion.specified=true");

        // Get all the customerList where vatRegion is null
        defaultCustomerShouldNotBeFound("vatRegion.specified=false");
    }

    @Test
    @Transactional
    public void getAllCustomersByVatNumberIsEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where vatNumber equals to DEFAULT_VAT_NUMBER
        defaultCustomerShouldBeFound("vatNumber.equals=" + DEFAULT_VAT_NUMBER);

        // Get all the customerList where vatNumber equals to UPDATED_VAT_NUMBER
        defaultCustomerShouldNotBeFound("vatNumber.equals=" + UPDATED_VAT_NUMBER);
    }

    @Test
    @Transactional
    public void getAllCustomersByVatNumberIsInShouldWork() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where vatNumber in DEFAULT_VAT_NUMBER or UPDATED_VAT_NUMBER
        defaultCustomerShouldBeFound("vatNumber.in=" + DEFAULT_VAT_NUMBER + "," + UPDATED_VAT_NUMBER);

        // Get all the customerList where vatNumber equals to UPDATED_VAT_NUMBER
        defaultCustomerShouldNotBeFound("vatNumber.in=" + UPDATED_VAT_NUMBER);
    }

    @Test
    @Transactional
    public void getAllCustomersByVatNumberIsNullOrNotNull() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where vatNumber is not null
        defaultCustomerShouldBeFound("vatNumber.specified=true");

        // Get all the customerList where vatNumber is null
        defaultCustomerShouldNotBeFound("vatNumber.specified=false");
    }

    @Test
    @Transactional
    public void getAllCustomersByContractualSalutationIsEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where contractualSalutation equals to DEFAULT_CONTRACTUAL_SALUTATION
        defaultCustomerShouldBeFound("contractualSalutation.equals=" + DEFAULT_CONTRACTUAL_SALUTATION);

        // Get all the customerList where contractualSalutation equals to UPDATED_CONTRACTUAL_SALUTATION
        defaultCustomerShouldNotBeFound("contractualSalutation.equals=" + UPDATED_CONTRACTUAL_SALUTATION);
    }

    @Test
    @Transactional
    public void getAllCustomersByContractualSalutationIsInShouldWork() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where contractualSalutation in DEFAULT_CONTRACTUAL_SALUTATION or
        // UPDATED_CONTRACTUAL_SALUTATION
        defaultCustomerShouldBeFound(
                "contractualSalutation.in=" + DEFAULT_CONTRACTUAL_SALUTATION + "," + UPDATED_CONTRACTUAL_SALUTATION);

        // Get all the customerList where contractualSalutation equals to UPDATED_CONTRACTUAL_SALUTATION
        defaultCustomerShouldNotBeFound("contractualSalutation.in=" + UPDATED_CONTRACTUAL_SALUTATION);
    }

    @Test
    @Transactional
    public void getAllCustomersByContractualSalutationIsNullOrNotNull() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where contractualSalutation is not null
        defaultCustomerShouldBeFound("contractualSalutation.specified=true");

        // Get all the customerList where contractualSalutation is null
        defaultCustomerShouldNotBeFound("contractualSalutation.specified=false");
    }

    @Test
    @Transactional
    public void getAllCustomersByContractualAddressIsEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where contractualAddress equals to DEFAULT_CONTRACTUAL_ADDRESS
        defaultCustomerShouldBeFound("contractualAddress.equals=" + DEFAULT_CONTRACTUAL_ADDRESS);

        // Get all the customerList where contractualAddress equals to UPDATED_CONTRACTUAL_ADDRESS
        defaultCustomerShouldNotBeFound("contractualAddress.equals=" + UPDATED_CONTRACTUAL_ADDRESS);
    }

    @Test
    @Transactional
    public void getAllCustomersByContractualAddressIsInShouldWork() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where contractualAddress in DEFAULT_CONTRACTUAL_ADDRESS or UPDATED_CONTRACTUAL_ADDRESS
        defaultCustomerShouldBeFound(
                "contractualAddress.in=" + DEFAULT_CONTRACTUAL_ADDRESS + "," + UPDATED_CONTRACTUAL_ADDRESS);

        // Get all the customerList where contractualAddress equals to UPDATED_CONTRACTUAL_ADDRESS
        defaultCustomerShouldNotBeFound("contractualAddress.in=" + UPDATED_CONTRACTUAL_ADDRESS);
    }

    @Test
    @Transactional
    public void getAllCustomersByContractualAddressIsNullOrNotNull() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where contractualAddress is not null
        defaultCustomerShouldBeFound("contractualAddress.specified=true");

        // Get all the customerList where contractualAddress is null
        defaultCustomerShouldNotBeFound("contractualAddress.specified=false");
    }

    @Test
    @Transactional
    public void getAllCustomersByBillingSalutationIsEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where billingSalutation equals to DEFAULT_BILLING_SALUTATION
        defaultCustomerShouldBeFound("billingSalutation.equals=" + DEFAULT_BILLING_SALUTATION);

        // Get all the customerList where billingSalutation equals to UPDATED_BILLING_SALUTATION
        defaultCustomerShouldNotBeFound("billingSalutation.equals=" + UPDATED_BILLING_SALUTATION);
    }

    @Test
    @Transactional
    public void getAllCustomersByBillingSalutationIsInShouldWork() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where billingSalutation in DEFAULT_BILLING_SALUTATION or UPDATED_BILLING_SALUTATION
        defaultCustomerShouldBeFound("billingSalutation.in=" + DEFAULT_BILLING_SALUTATION + "," + UPDATED_BILLING_SALUTATION);

        // Get all the customerList where billingSalutation equals to UPDATED_BILLING_SALUTATION
        defaultCustomerShouldNotBeFound("billingSalutation.in=" + UPDATED_BILLING_SALUTATION);
    }

    @Test
    @Transactional
    public void getAllCustomersByBillingSalutationIsNullOrNotNull() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where billingSalutation is not null
        defaultCustomerShouldBeFound("billingSalutation.specified=true");

        // Get all the customerList where billingSalutation is null
        defaultCustomerShouldNotBeFound("billingSalutation.specified=false");
    }

    @Test
    @Transactional
    public void getAllCustomersByBillingAddressIsEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where billingAddress equals to DEFAULT_BILLING_ADDRESS
        defaultCustomerShouldBeFound("billingAddress.equals=" + DEFAULT_BILLING_ADDRESS);

        // Get all the customerList where billingAddress equals to UPDATED_BILLING_ADDRESS
        defaultCustomerShouldNotBeFound("billingAddress.equals=" + UPDATED_BILLING_ADDRESS);
    }

    @Test
    @Transactional
    public void getAllCustomersByBillingAddressIsInShouldWork() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where billingAddress in DEFAULT_BILLING_ADDRESS or UPDATED_BILLING_ADDRESS
        defaultCustomerShouldBeFound("billingAddress.in=" + DEFAULT_BILLING_ADDRESS + "," + UPDATED_BILLING_ADDRESS);

        // Get all the customerList where billingAddress equals to UPDATED_BILLING_ADDRESS
        defaultCustomerShouldNotBeFound("billingAddress.in=" + UPDATED_BILLING_ADDRESS);
    }

    @Test
    @Transactional
    public void getAllCustomersByBillingAddressIsNullOrNotNull() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where billingAddress is not null
        defaultCustomerShouldBeFound("billingAddress.specified=true");

        // Get all the customerList where billingAddress is null
        defaultCustomerShouldNotBeFound("billingAddress.specified=false");
    }

    @Test
    @Transactional
    public void getAllCustomersByRemarkIsEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where remark equals to DEFAULT_REMARK
        defaultCustomerShouldBeFound("remark.equals=" + DEFAULT_REMARK);

        // Get all the customerList where remark equals to UPDATED_REMARK
        defaultCustomerShouldNotBeFound("remark.equals=" + UPDATED_REMARK);
    }

    @Test
    @Transactional
    public void getAllCustomersByRemarkIsInShouldWork() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where remark in DEFAULT_REMARK or UPDATED_REMARK
        defaultCustomerShouldBeFound("remark.in=" + DEFAULT_REMARK + "," + UPDATED_REMARK);

        // Get all the customerList where remark equals to UPDATED_REMARK
        defaultCustomerShouldNotBeFound("remark.in=" + UPDATED_REMARK);
    }

    @Test
    @Transactional
    public void getAllCustomersByRemarkIsNullOrNotNull() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where remark is not null
        defaultCustomerShouldBeFound("remark.specified=true");

        // Get all the customerList where remark is null
        defaultCustomerShouldNotBeFound("remark.specified=false");
    }

    @Test
    @Transactional
    public void getAllCustomersByMembershipIsEqualToSomething() throws Exception {
        // Initialize the database
        Membership membership = MembershipResourceIntTest.createEntity(em);
        em.persist(membership);
        em.flush();
        customer.addMembership(membership);
        customerRepository.saveAndFlush(customer);
        Long membershipId = membership.getId();

        // Get all the customerList where membership equals to membershipId
        defaultCustomerShouldBeFound("membershipId.equals=" + membershipId);

        // Get all the customerList where membership equals to membershipId + 1
        defaultCustomerShouldNotBeFound("membershipId.equals=" + (membershipId + 1));
    }

    @Test
    @Transactional
    public void getAllCustomersBySepamandateIsEqualToSomething() throws Exception {
        // Initialize the database
        SepaMandate sepamandate = SepaMandateResourceIntTest.createEntity(em);
        em.persist(sepamandate);
        em.flush();
        customer.addSepamandate(sepamandate);
        customerRepository.saveAndFlush(customer);
        Long sepamandateId = sepamandate.getId();

        // Get all the customerList where sepamandate equals to sepamandateId
        defaultCustomerShouldBeFound("sepamandateId.equals=" + sepamandateId);

        // Get all the customerList where sepamandate equals to sepamandateId + 1
        defaultCustomerShouldNotBeFound("sepamandateId.equals=" + (sepamandateId + 1));
    }

    /**
     * Executes the search, and checks that the default entity is returned
     */
    private void defaultCustomerShouldBeFound(String filter) throws Exception {
        restCustomerMockMvc.perform(get("/api/customers?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(customer.getId().intValue())))
                .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
                .andExpect(jsonPath("$.[*].prefix").value(hasItem(DEFAULT_PREFIX)))
                .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
                .andExpect(jsonPath("$.[*].kind").value(hasItem(DEFAULT_KIND.toString())))
                .andExpect(jsonPath("$.[*].birthDate").value(hasItem(DEFAULT_BIRTH_DATE.toString())))
                .andExpect(jsonPath("$.[*].birthPlace").value(hasItem(DEFAULT_BIRTH_PLACE)))
                .andExpect(jsonPath("$.[*].registrationCourt").value(hasItem(DEFAULT_REGISTRATION_COURT)))
                .andExpect(jsonPath("$.[*].registrationNumber").value(hasItem(DEFAULT_REGISTRATION_NUMBER)))
                .andExpect(jsonPath("$.[*].vatRegion").value(hasItem(DEFAULT_VAT_REGION.toString())))
                .andExpect(jsonPath("$.[*].vatNumber").value(hasItem(DEFAULT_VAT_NUMBER)))
                .andExpect(jsonPath("$.[*].contractualSalutation").value(hasItem(DEFAULT_CONTRACTUAL_SALUTATION)))
                .andExpect(jsonPath("$.[*].contractualAddress").value(hasItem(DEFAULT_CONTRACTUAL_ADDRESS)))
                .andExpect(jsonPath("$.[*].billingSalutation").value(hasItem(DEFAULT_BILLING_SALUTATION)))
                .andExpect(jsonPath("$.[*].billingAddress").value(hasItem(DEFAULT_BILLING_ADDRESS)))
                .andExpect(jsonPath("$.[*].remark").value(hasItem(DEFAULT_REMARK)));

        // Check, that the count call also returns 1
        restCustomerMockMvc.perform(get("/api/customers/count?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned
     */
    private void defaultCustomerShouldNotBeFound(String filter) throws Exception {
        restCustomerMockMvc.perform(get("/api/customers?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restCustomerMockMvc.perform(get("/api/customers/count?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    public void getNonExistingCustomer() throws Exception {
        // Get the customer
        restCustomerMockMvc.perform(get("/api/customers/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateCustomer() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        int databaseSizeBeforeUpdate = customerRepository.findAll().size();

        // Update the customer
        Customer updatedCustomer = customerRepository.findById(customer.getId()).get();
        // Disconnect from session so that the updates on updatedCustomer are not directly saved in db
        em.detach(updatedCustomer);
        updatedCustomer
                .reference(UPDATED_REFERENCE)
                .prefix(UPDATED_PREFIX)
                .name(UPDATED_NAME)
                .kind(UPDATED_KIND)
                .birthDate(UPDATED_BIRTH_DATE)
                .birthPlace(UPDATED_BIRTH_PLACE)
                .registrationCourt(UPDATED_REGISTRATION_COURT)
                .registrationNumber(UPDATED_REGISTRATION_NUMBER)
                .vatRegion(UPDATED_VAT_REGION)
                .vatNumber(UPDATED_VAT_NUMBER)
                .contractualSalutation(UPDATED_CONTRACTUAL_SALUTATION)
                .contractualAddress(UPDATED_CONTRACTUAL_ADDRESS)
                .billingSalutation(UPDATED_BILLING_SALUTATION)
                .billingAddress(UPDATED_BILLING_ADDRESS)
                .remark(UPDATED_REMARK);
        CustomerDTO customerDTO = customerMapper.toDto(updatedCustomer);

        restCustomerMockMvc.perform(
                put("/api/customers")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(customerDTO)))
                .andExpect(status().isOk());

        // Validate the Customer in the database
        List<Customer> customerList = customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeUpdate);
        Customer testCustomer = customerList.get(customerList.size() - 1);
        assertThat(testCustomer.getReference()).isEqualTo(UPDATED_REFERENCE);
        assertThat(testCustomer.getPrefix()).isEqualTo(UPDATED_PREFIX);
        assertThat(testCustomer.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testCustomer.getKind()).isEqualTo(UPDATED_KIND);
        assertThat(testCustomer.getBirthDate()).isEqualTo(UPDATED_BIRTH_DATE);
        assertThat(testCustomer.getBirthPlace()).isEqualTo(UPDATED_BIRTH_PLACE);
        assertThat(testCustomer.getRegistrationCourt()).isEqualTo(UPDATED_REGISTRATION_COURT);
        assertThat(testCustomer.getRegistrationNumber()).isEqualTo(UPDATED_REGISTRATION_NUMBER);
        assertThat(testCustomer.getVatRegion()).isEqualTo(UPDATED_VAT_REGION);
        assertThat(testCustomer.getVatNumber()).isEqualTo(UPDATED_VAT_NUMBER);
        assertThat(testCustomer.getContractualSalutation()).isEqualTo(UPDATED_CONTRACTUAL_SALUTATION);
        assertThat(testCustomer.getContractualAddress()).isEqualTo(UPDATED_CONTRACTUAL_ADDRESS);
        assertThat(testCustomer.getBillingSalutation()).isEqualTo(UPDATED_BILLING_SALUTATION);
        assertThat(testCustomer.getBillingAddress()).isEqualTo(UPDATED_BILLING_ADDRESS);
        assertThat(testCustomer.getRemark()).isEqualTo(UPDATED_REMARK);
    }

    @Test
    @Transactional
    public void updateNonExistingCustomer() throws Exception {
        int databaseSizeBeforeUpdate = customerRepository.findAll().size();

        // Create the Customer
        CustomerDTO customerDTO = customerMapper.toDto(customer);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCustomerMockMvc.perform(
                put("/api/customers")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(customerDTO)))
                .andExpect(status().isBadRequest());

        // Validate the Customer in the database
        List<Customer> customerList = customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteCustomer() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        int databaseSizeBeforeDelete = customerRepository.findAll().size();

        // Delete the customer
        restCustomerMockMvc.perform(
                delete("/api/customers/{id}", customer.getId())
                        .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<Customer> customerList = customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Customer.class);
        Customer customer1 = new Customer();
        customer1.setId(1L);
        Customer customer2 = new Customer();
        customer2.setId(customer1.getId());
        assertThat(customer1).isEqualTo(customer2);
        customer2.setId(2L);
        assertThat(customer1).isNotEqualTo(customer2);
        customer1.setId(null);
        assertThat(customer1).isNotEqualTo(customer2);
    }

    @Test
    @Transactional
    public void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CustomerDTO.class);
        CustomerDTO customerDTO1 = new CustomerDTO();
        customerDTO1.setId(1L);
        CustomerDTO customerDTO2 = new CustomerDTO();
        assertThat(customerDTO1).isNotEqualTo(customerDTO2);
        customerDTO2.setId(customerDTO1.getId());
        assertThat(customerDTO1).isEqualTo(customerDTO2);
        customerDTO2.setId(2L);
        assertThat(customerDTO1).isNotEqualTo(customerDTO2);
        customerDTO1.setId(null);
        assertThat(customerDTO1).isNotEqualTo(customerDTO2);
    }

    @Test
    @Transactional
    public void testEntityFromId() {
        assertThat(customerMapper.fromId(42L).getId()).isEqualTo(42);
        assertThat(customerMapper.fromId(null)).isNull();
    }
}
