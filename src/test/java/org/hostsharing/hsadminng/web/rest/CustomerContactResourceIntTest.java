package org.hostsharing.hsadminng.web.rest;

import org.hostsharing.hsadminng.HsadminNgApp;

import org.hostsharing.hsadminng.domain.CustomerContact;
import org.hostsharing.hsadminng.domain.Contact;
import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.repository.CustomerContactRepository;
import org.hostsharing.hsadminng.service.CustomerContactService;
import org.hostsharing.hsadminng.service.dto.CustomerContactDTO;
import org.hostsharing.hsadminng.service.mapper.CustomerContactMapper;
import org.hostsharing.hsadminng.web.rest.errors.ExceptionTranslator;
import org.hostsharing.hsadminng.service.CustomerContactQueryService;

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


import static org.hostsharing.hsadminng.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.hostsharing.hsadminng.domain.enumeration.CustomerContactRole;
/**
 * Test class for the CustomerContactResource REST controller.
 *
 * @see CustomerContactResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = HsadminNgApp.class)
public class CustomerContactResourceIntTest {

    private static final CustomerContactRole DEFAULT_ROLE = CustomerContactRole.CONTRACTUAL;
    private static final CustomerContactRole UPDATED_ROLE = CustomerContactRole.TECHNICAL;

    @Autowired
    private CustomerContactRepository customerContactRepository;

    @Autowired
    private CustomerContactMapper customerContactMapper;

    @Autowired
    private CustomerContactService customerContactService;

    @Autowired
    private CustomerContactQueryService customerContactQueryService;

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

    private MockMvc restCustomerContactMockMvc;

    private CustomerContact customerContact;

    @Before
    public void setup() {

        MockitoAnnotations.initMocks(this);
        final CustomerContactResource customerContactResource = new CustomerContactResource(customerContactService, customerContactQueryService);
        this.restCustomerContactMockMvc = MockMvcBuilders.standaloneSetup(customerContactResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create a CustomerContaact entity for the given Customer for testing purposes.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CustomerContact crateEnitity(final EntityManager em, Customer customer) {
        CustomerContact customerContact = new CustomerContact()
            .role(DEFAULT_ROLE);
        // Add required entity
        Contact contact = ContactResourceIntTest.createEntity(em);
        em.persist(contact);
        em.flush();
        customerContact.setContact(contact);
        // Add required entity
        em.persist(customer);
        em.flush();
        customerContact.setCustomer(customer);
        return customerContact;
    }

    /**
     * Create an arbitrary CustomerContact entity for tests.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CustomerContact createDefaultEntity(EntityManager em) {
        return crateEnitity(em, CustomerResourceIntTest.createEntity(em));
    }

    /**
     * Create another arbitrary CustomerContact entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CustomerContact createAnotherEntity(EntityManager em) {
        return crateEnitity(em, CustomerResourceIntTest.createAnotherEntity(em));
    }

    @Before
    public void initTest() {
        customerContact = createDefaultEntity(em);
    }

    @Test
    @Transactional
    public void createCustomerContact() throws Exception {
        int databaseSizeBeforeCreate = customerContactRepository.findAll().size();

        // Create the CustomerContact
        CustomerContactDTO customerContactDTO = customerContactMapper.toDto(customerContact);
        restCustomerContactMockMvc.perform(post("/api/customer-contacts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(customerContactDTO)))
            .andExpect(status().isCreated());

        // Validate the CustomerContact in the database
        List<CustomerContact> customerContactList = customerContactRepository.findAll();
        assertThat(customerContactList).hasSize(databaseSizeBeforeCreate + 1);
        CustomerContact testCustomerContact = customerContactList.get(customerContactList.size() - 1);
        assertThat(testCustomerContact.getRole()).isEqualTo(DEFAULT_ROLE);
    }

    @Test
    @Transactional
    public void createCustomerContactWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = customerContactRepository.findAll().size();

        // Create the CustomerContact with an existing ID
        customerContact.setId(1L);
        CustomerContactDTO customerContactDTO = customerContactMapper.toDto(customerContact);

        // An entity with an existing ID cannot be created, so this API call must fail
        restCustomerContactMockMvc.perform(post("/api/customer-contacts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(customerContactDTO)))
            .andExpect(status().isBadRequest());

        // Validate the CustomerContact in the database
        List<CustomerContact> customerContactList = customerContactRepository.findAll();
        assertThat(customerContactList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkRoleIsRequired() throws Exception {
        int databaseSizeBeforeTest = customerContactRepository.findAll().size();
        // set the field null
        customerContact.setRole(null);

        // Create the CustomerContact, which fails.
        CustomerContactDTO customerContactDTO = customerContactMapper.toDto(customerContact);

        restCustomerContactMockMvc.perform(post("/api/customer-contacts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(customerContactDTO)))
            .andExpect(status().isBadRequest());

        List<CustomerContact> customerContactList = customerContactRepository.findAll();
        assertThat(customerContactList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllCustomerContacts() throws Exception {
        // Initialize the database
        customerContactRepository.saveAndFlush(customerContact);

        // Get all the customerContactList
        restCustomerContactMockMvc.perform(get("/api/customer-contacts?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(customerContact.getId().intValue())))
            .andExpect(jsonPath("$.[*].role").value(hasItem(DEFAULT_ROLE.toString())));
    }
    
    @Test
    @Transactional
    public void getCustomerContact() throws Exception {
        // Initialize the database
        customerContactRepository.saveAndFlush(customerContact);

        // Get the customerContact
        restCustomerContactMockMvc.perform(get("/api/customer-contacts/{id}", customerContact.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(customerContact.getId().intValue()))
            .andExpect(jsonPath("$.role").value(DEFAULT_ROLE.toString()));
    }

    @Test
    @Transactional
    public void getAllCustomerContactsByRoleIsEqualToSomething() throws Exception {
        // Initialize the database
        customerContactRepository.saveAndFlush(customerContact);

        // Get all the customerContactList where role equals to DEFAULT_ROLE
        defaultCustomerContactShouldBeFound("role.equals=" + DEFAULT_ROLE);

        // Get all the customerContactList where role equals to UPDATED_ROLE
        defaultCustomerContactShouldNotBeFound("role.equals=" + UPDATED_ROLE);
    }

    @Test
    @Transactional
    public void getAllCustomerContactsByRoleIsInShouldWork() throws Exception {
        // Initialize the database
        customerContactRepository.saveAndFlush(customerContact);

        // Get all the customerContactList where role in DEFAULT_ROLE or UPDATED_ROLE
        defaultCustomerContactShouldBeFound("role.in=" + DEFAULT_ROLE + "," + UPDATED_ROLE);

        // Get all the customerContactList where role equals to UPDATED_ROLE
        defaultCustomerContactShouldNotBeFound("role.in=" + UPDATED_ROLE);
    }

    @Test
    @Transactional
    public void getAllCustomerContactsByRoleIsNullOrNotNull() throws Exception {
        // Initialize the database
        customerContactRepository.saveAndFlush(customerContact);

        // Get all the customerContactList where role is not null
        defaultCustomerContactShouldBeFound("role.specified=true");

        // Get all the customerContactList where role is null
        defaultCustomerContactShouldNotBeFound("role.specified=false");
    }

    @Test
    @Transactional
    public void getAllCustomerContactsByContactIsEqualToSomething() throws Exception {
        // Initialize the database
        Contact contact = ContactResourceIntTest.createEntity(em);
        em.persist(contact);
        em.flush();
        customerContact.setContact(contact);
        customerContactRepository.saveAndFlush(customerContact);
        Long contactId = contact.getId();

        // Get all the customerContactList where contact equals to contactId
        defaultCustomerContactShouldBeFound("contactId.equals=" + contactId);

        // Get all the customerContactList where contact equals to contactId + 1
        defaultCustomerContactShouldNotBeFound("contactId.equals=" + (contactId + 1));
    }

    @Test
    @Transactional
    public void getAllCustomerContactsByCustomerIsEqualToSomething() throws Exception {
        // Initialize the database
        Customer customer = CustomerResourceIntTest.createAnotherEntity(em);
        em.persist(customer);
        em.flush();
        customerContact.setCustomer(customer);
        customerContactRepository.saveAndFlush(customerContact);
        Long customerId = customer.getId();

        // Get all the customerContactList where customer equals to customerId
        defaultCustomerContactShouldBeFound("customerId.equals=" + customerId);

        // Get all the customerContactList where customer equals to customerId + 1
        defaultCustomerContactShouldNotBeFound("customerId.equals=" + (customerId + 1));
    }

    /**
     * Executes the search, and checks that the default entity is returned
     */
    private void defaultCustomerContactShouldBeFound(String filter) throws Exception {
        restCustomerContactMockMvc.perform(get("/api/customer-contacts?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(customerContact.getId().intValue())))
            .andExpect(jsonPath("$.[*].role").value(hasItem(DEFAULT_ROLE.toString())));

        // Check, that the count call also returns 1
        restCustomerContactMockMvc.perform(get("/api/customer-contacts/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned
     */
    private void defaultCustomerContactShouldNotBeFound(String filter) throws Exception {
        restCustomerContactMockMvc.perform(get("/api/customer-contacts?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restCustomerContactMockMvc.perform(get("/api/customer-contacts/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(content().string("0"));
    }


    @Test
    @Transactional
    public void getNonExistingCustomerContact() throws Exception {
        // Get the customerContact
        restCustomerContactMockMvc.perform(get("/api/customer-contacts/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateCustomerContact() throws Exception {
        // Initialize the database
        customerContactRepository.saveAndFlush(customerContact);

        int databaseSizeBeforeUpdate = customerContactRepository.findAll().size();

        // Update the customerContact
        CustomerContact updatedCustomerContact = customerContactRepository.findById(customerContact.getId()).get();
        // Disconnect from session so that the updates on updatedCustomerContact are not directly saved in db
        em.detach(updatedCustomerContact);
        updatedCustomerContact
            .role(UPDATED_ROLE);
        CustomerContactDTO customerContactDTO = customerContactMapper.toDto(updatedCustomerContact);

        restCustomerContactMockMvc.perform(put("/api/customer-contacts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(customerContactDTO)))
            .andExpect(status().isOk());

        // Validate the CustomerContact in the database
        List<CustomerContact> customerContactList = customerContactRepository.findAll();
        assertThat(customerContactList).hasSize(databaseSizeBeforeUpdate);
        CustomerContact testCustomerContact = customerContactList.get(customerContactList.size() - 1);
        assertThat(testCustomerContact.getRole()).isEqualTo(UPDATED_ROLE);
    }

    @Test
    @Transactional
    public void updateNonExistingCustomerContact() throws Exception {
        int databaseSizeBeforeUpdate = customerContactRepository.findAll().size();

        // Create the CustomerContact
        CustomerContactDTO customerContactDTO = customerContactMapper.toDto(customerContact);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCustomerContactMockMvc.perform(put("/api/customer-contacts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(customerContactDTO)))
            .andExpect(status().isBadRequest());

        // Validate the CustomerContact in the database
        List<CustomerContact> customerContactList = customerContactRepository.findAll();
        assertThat(customerContactList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteCustomerContact() throws Exception {
        // Initialize the database
        customerContactRepository.saveAndFlush(customerContact);

        int databaseSizeBeforeDelete = customerContactRepository.findAll().size();

        // Delete the customerContact
        restCustomerContactMockMvc.perform(delete("/api/customer-contacts/{id}", customerContact.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<CustomerContact> customerContactList = customerContactRepository.findAll();
        assertThat(customerContactList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CustomerContact.class);
        CustomerContact customerContact1 = new CustomerContact();
        customerContact1.setId(1L);
        CustomerContact customerContact2 = new CustomerContact();
        customerContact2.setId(customerContact1.getId());
        assertThat(customerContact1).isEqualTo(customerContact2);
        customerContact2.setId(2L);
        assertThat(customerContact1).isNotEqualTo(customerContact2);
        customerContact1.setId(null);
        assertThat(customerContact1).isNotEqualTo(customerContact2);
    }

    @Test
    @Transactional
    public void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CustomerContactDTO.class);
        CustomerContactDTO customerContactDTO1 = new CustomerContactDTO();
        customerContactDTO1.setId(1L);
        CustomerContactDTO customerContactDTO2 = new CustomerContactDTO();
        assertThat(customerContactDTO1).isNotEqualTo(customerContactDTO2);
        customerContactDTO2.setId(customerContactDTO1.getId());
        assertThat(customerContactDTO1).isEqualTo(customerContactDTO2);
        customerContactDTO2.setId(2L);
        assertThat(customerContactDTO1).isNotEqualTo(customerContactDTO2);
        customerContactDTO1.setId(null);
        assertThat(customerContactDTO1).isNotEqualTo(customerContactDTO2);
    }

    @Test
    @Transactional
    public void testEntityFromId() {
        assertThat(customerContactMapper.fromId(42L).getId()).isEqualTo(42);
        assertThat(customerContactMapper.fromId(null)).isNull();
    }
}
