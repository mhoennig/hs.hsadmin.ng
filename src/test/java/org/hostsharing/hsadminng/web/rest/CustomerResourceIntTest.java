package org.hostsharing.hsadminng.web.rest;

import org.hostsharing.hsadminng.HsadminNgApp;
import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.CustomerContact;
import org.hostsharing.hsadminng.domain.Membership;
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

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hostsharing.hsadminng.web.rest.TestUtil.createFormattingConversionService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the CustomerResource REST controller.
 *
 * @see CustomerResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = HsadminNgApp.class)
public class CustomerResourceIntTest {

    private static final Integer DEFAULT_NUMBER = 10000;
    private static final Integer ANOTHER_NUMBER = 10001;
    private static final Integer UPDATED_NUMBER = 10002;

    private static final String DEFAULT_PREFIX = "def";
    private static final String ANOTHER_PREFIX = "old";
    private static final String UPDATED_PREFIX = "new";

    private static final String DEFAULT_NAME = "Default GmbH";
    private static final String UPDATED_NAME = "Updated Default GmbH";
    private static final String ANOTHER_NAME = "Another Corp.";

    private static final String DEFAULT_CONTRACTUAL_ADDRESS = "Default Address";
    private static final String UPDATED_CONTRACTUAL_ADDRESS = "Updated Address";
    private static final String ANOTHER_CONTRACTUAL_ADDRESS = "Another Address";

    private static final String DEFAULT_CONTRACTUAL_SALUTATION = "AAAAAAAAAA";
    private static final String UPDATED_CONTRACTUAL_SALUTATION = "BBBBBBBBBB";

    private static final String DEFAULT_BILLING_ADDRESS = "AAAAAAAAAA";
    private static final String UPDATED_BILLING_ADDRESS = "BBBBBBBBBB";

    private static final String DEFAULT_BILLING_SALUTATION = "AAAAAAAAAA";
    private static final String UPDATED_BILLING_SALUTATION = "BBBBBBBBBB";

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
            .setValidator(validator).build();
    }

    /**
     * Create an entity for tests.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Customer createEntity(EntityManager em) {
        Customer customer = new Customer()
            .number(DEFAULT_NUMBER)
            .prefix(DEFAULT_PREFIX)
            .name(DEFAULT_NAME)
            .contractualAddress(DEFAULT_CONTRACTUAL_ADDRESS)
            .contractualSalutation(DEFAULT_CONTRACTUAL_SALUTATION)
            .billingAddress(DEFAULT_BILLING_ADDRESS)
            .billingSalutation(DEFAULT_BILLING_SALUTATION);
        return customer;
    }

    /**
     * Create another entity for tests.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Customer createAnotherEntity(EntityManager em) {
        Customer customer = new Customer()
            .number(ANOTHER_NUMBER)
            .prefix(ANOTHER_PREFIX)
            .name(ANOTHER_NAME)
            .contractualAddress(ANOTHER_CONTRACTUAL_ADDRESS);
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
        restCustomerMockMvc.perform(post("/api/customers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(customerDTO)))
            .andExpect(status().isCreated());

        // Validate the Customer in the database
        List<Customer> customerList = customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeCreate + 1);
        Customer testCustomer = customerList.get(customerList.size() - 1);
        assertThat(testCustomer.getNumber()).isEqualTo(DEFAULT_NUMBER);
        assertThat(testCustomer.getPrefix()).isEqualTo(DEFAULT_PREFIX);
        assertThat(testCustomer.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testCustomer.getContractualAddress()).isEqualTo(DEFAULT_CONTRACTUAL_ADDRESS);
        assertThat(testCustomer.getContractualSalutation()).isEqualTo(DEFAULT_CONTRACTUAL_SALUTATION);
        assertThat(testCustomer.getBillingAddress()).isEqualTo(DEFAULT_BILLING_ADDRESS);
        assertThat(testCustomer.getBillingSalutation()).isEqualTo(DEFAULT_BILLING_SALUTATION);
    }

    @Test
    @Transactional
    public void createCustomerWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = customerRepository.findAll().size();

        // Create the Customer with an existing ID
        customer.setId(1L);
        CustomerDTO customerDTO = customerMapper.toDto(customer);

        // An entity with an existing ID cannot be created, so this API call must fail
        restCustomerMockMvc.perform(post("/api/customers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(customerDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Customer in the database
        List<Customer> customerList = customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkNumberIsRequired() throws Exception {
        int databaseSizeBeforeTest = customerRepository.findAll().size();
        // set the field null
        customer.setNumber(null);

        // Create the Customer, which fails.
        CustomerDTO customerDTO = customerMapper.toDto(customer);

        restCustomerMockMvc.perform(post("/api/customers")
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

        restCustomerMockMvc.perform(post("/api/customers")
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

        restCustomerMockMvc.perform(post("/api/customers")
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

        restCustomerMockMvc.perform(post("/api/customers")
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
            .andExpect(jsonPath("$.[*].number").value(hasItem(DEFAULT_NUMBER)))
            .andExpect(jsonPath("$.[*].prefix").value(hasItem(DEFAULT_PREFIX.toString())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].contractualAddress").value(hasItem(DEFAULT_CONTRACTUAL_ADDRESS.toString())))
            .andExpect(jsonPath("$.[*].contractualSalutation").value(hasItem(DEFAULT_CONTRACTUAL_SALUTATION.toString())))
            .andExpect(jsonPath("$.[*].billingAddress").value(hasItem(DEFAULT_BILLING_ADDRESS.toString())))
            .andExpect(jsonPath("$.[*].billingSalutation").value(hasItem(DEFAULT_BILLING_SALUTATION.toString())));
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
            .andExpect(jsonPath("$.number").value(DEFAULT_NUMBER))
            .andExpect(jsonPath("$.prefix").value(DEFAULT_PREFIX.toString()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
            .andExpect(jsonPath("$.contractualAddress").value(DEFAULT_CONTRACTUAL_ADDRESS.toString()))
            .andExpect(jsonPath("$.contractualSalutation").value(DEFAULT_CONTRACTUAL_SALUTATION.toString()))
            .andExpect(jsonPath("$.billingAddress").value(DEFAULT_BILLING_ADDRESS.toString()))
            .andExpect(jsonPath("$.billingSalutation").value(DEFAULT_BILLING_SALUTATION.toString()));
    }

    @Test
    @Transactional
    public void getAllCustomersByNumberIsEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where number equals to DEFAULT_NUMBER
        defaultCustomerShouldBeFound("number.equals=" + DEFAULT_NUMBER);

        // Get all the customerList where number equals to UPDATED_NUMBER
        defaultCustomerShouldNotBeFound("number.equals=" + UPDATED_NUMBER);
    }

    @Test
    @Transactional
    public void getAllCustomersByNumberIsInShouldWork() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where number in DEFAULT_NUMBER or UPDATED_NUMBER
        defaultCustomerShouldBeFound("number.in=" + DEFAULT_NUMBER + "," + UPDATED_NUMBER);

        // Get all the customerList where number equals to UPDATED_NUMBER
        defaultCustomerShouldNotBeFound("number.in=" + UPDATED_NUMBER);
    }

    @Test
    @Transactional
    public void getAllCustomersByNumberIsNullOrNotNull() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where number is not null
        defaultCustomerShouldBeFound("number.specified=true");

        // Get all the customerList where number is null
        defaultCustomerShouldNotBeFound("number.specified=false");
    }

    @Test
    @Transactional
    public void getAllCustomersByNumberIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where number greater than or equals to DEFAULT_NUMBER
        defaultCustomerShouldBeFound("number.greaterOrEqualThan=" + DEFAULT_NUMBER);

        // Get all the customerList where number greater than or equals to (DEFAULT_NUMBER + 1)
        defaultCustomerShouldNotBeFound("number.greaterOrEqualThan=" + (DEFAULT_NUMBER + 1));
    }

    @Test
    @Transactional
    public void getAllCustomersByNumberIsLessThanSomething() throws Exception {
        // Initialize the database
        customerRepository.saveAndFlush(customer);

        // Get all the customerList where number less than or equals to DEFAULT_NUMBER
        defaultCustomerShouldNotBeFound("number.lessThan=" + DEFAULT_NUMBER);

        // Get all the customerList where number less than or equals to (DEFAULT_NUMBER + 1)
        defaultCustomerShouldBeFound("number.lessThan=" + (DEFAULT_NUMBER + 1));
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
        defaultCustomerShouldBeFound("contractualAddress.in=" + DEFAULT_CONTRACTUAL_ADDRESS + "," + UPDATED_CONTRACTUAL_ADDRESS);

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

        // Get all the customerList where contractualSalutation in DEFAULT_CONTRACTUAL_SALUTATION or UPDATED_CONTRACTUAL_SALUTATION
        defaultCustomerShouldBeFound("contractualSalutation.in=" + DEFAULT_CONTRACTUAL_SALUTATION + "," + UPDATED_CONTRACTUAL_SALUTATION);

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
    public void getAllCustomersByRoleIsEqualToSomething() throws Exception {
        // Initialize the database
        CustomerContact role = CustomerContactResourceIntTest.createAnotherEntity(em);
        em.persist(role);
        em.flush();
        customer.addRole(role);
        customerRepository.saveAndFlush(customer);
        Long roleId = role.getId();

        // Get all the customerList where role equals to roleId
        defaultCustomerShouldBeFound("roleId.equals=" + roleId);

        // Get all the customerList where role equals to roleId + 1
        defaultCustomerShouldNotBeFound("roleId.equals=" + (roleId + 1));
    }

    /**
     * Executes the search, and checks that the default entity is returned
     */
    private void defaultCustomerShouldBeFound(String filter) throws Exception {
        restCustomerMockMvc.perform(get("/api/customers?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(customer.getId().intValue())))
            .andExpect(jsonPath("$.[*].number").value(hasItem(DEFAULT_NUMBER)))
            .andExpect(jsonPath("$.[*].prefix").value(hasItem(DEFAULT_PREFIX)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].contractualAddress").value(hasItem(DEFAULT_CONTRACTUAL_ADDRESS)))
            .andExpect(jsonPath("$.[*].contractualSalutation").value(hasItem(DEFAULT_CONTRACTUAL_SALUTATION)))
            .andExpect(jsonPath("$.[*].billingAddress").value(hasItem(DEFAULT_BILLING_ADDRESS)))
            .andExpect(jsonPath("$.[*].billingSalutation").value(hasItem(DEFAULT_BILLING_SALUTATION)));

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
            .number(UPDATED_NUMBER)
            .prefix(UPDATED_PREFIX)
            .name(UPDATED_NAME)
            .contractualAddress(UPDATED_CONTRACTUAL_ADDRESS)
            .contractualSalutation(UPDATED_CONTRACTUAL_SALUTATION)
            .billingAddress(UPDATED_BILLING_ADDRESS)
            .billingSalutation(UPDATED_BILLING_SALUTATION);
        CustomerDTO customerDTO = customerMapper.toDto(updatedCustomer);

        restCustomerMockMvc.perform(put("/api/customers")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(customerDTO)))
            .andExpect(status().isOk());

        // Validate the Customer in the database
        List<Customer> customerList = customerRepository.findAll();
        assertThat(customerList).hasSize(databaseSizeBeforeUpdate);
        Customer testCustomer = customerList.get(customerList.size() - 1);
        assertThat(testCustomer.getNumber()).isEqualTo(UPDATED_NUMBER);
        assertThat(testCustomer.getPrefix()).isEqualTo(UPDATED_PREFIX);
        assertThat(testCustomer.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testCustomer.getContractualAddress()).isEqualTo(UPDATED_CONTRACTUAL_ADDRESS);
        assertThat(testCustomer.getContractualSalutation()).isEqualTo(UPDATED_CONTRACTUAL_SALUTATION);
        assertThat(testCustomer.getBillingAddress()).isEqualTo(UPDATED_BILLING_ADDRESS);
        assertThat(testCustomer.getBillingSalutation()).isEqualTo(UPDATED_BILLING_SALUTATION);
    }

    @Test
    @Transactional
    public void updateNonExistingCustomer() throws Exception {
        int databaseSizeBeforeUpdate = customerRepository.findAll().size();

        // Create the Customer
        CustomerDTO customerDTO = customerMapper.toDto(customer);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCustomerMockMvc.perform(put("/api/customers")
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
        restCustomerMockMvc.perform(delete("/api/customers/{id}", customer.getId())
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
