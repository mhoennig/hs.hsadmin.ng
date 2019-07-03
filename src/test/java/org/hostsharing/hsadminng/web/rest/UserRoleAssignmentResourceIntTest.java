// Licensed under Apache-2.0
package org.hostsharing.hsadminng.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hostsharing.hsadminng.web.rest.TestUtil.createFormattingConversionService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.hostsharing.hsadminng.HsadminNgApp;
import org.hostsharing.hsadminng.domain.User;
import org.hostsharing.hsadminng.domain.UserRoleAssignment;
import org.hostsharing.hsadminng.repository.UserRoleAssignmentRepository;
import org.hostsharing.hsadminng.service.UserRoleAssignmentQueryService;
import org.hostsharing.hsadminng.service.UserRoleAssignmentService;
import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.hostsharing.hsadminng.service.accessfilter.Role.Admin;
import org.hostsharing.hsadminng.service.accessfilter.Role.CustomerContractualContact;
import org.hostsharing.hsadminng.service.accessfilter.Role.CustomerTechnicalContact;
import org.hostsharing.hsadminng.service.accessfilter.SecurityContextFake;
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

import java.util.List;

import javax.persistence.EntityManager;

/**
 * Test class for the UserRoleAssignmentResource REST controller.
 *
 * @see UserRoleAssignmentResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { HsadminNgApp.class })
public class UserRoleAssignmentResourceIntTest {

    private static final String DEFAULT_ENTITY_TYPE_ID = "AAAAAAAAAA";
    private static final String UPDATED_ENTITY_TYPE_ID = "BBBBBBBBBB";

    private static final Long DEFAULT_ENTITY_OBJECT_ID = 1L;
    private static final Long UPDATED_ENTITY_OBJECT_ID = 2L;

    private static final Role DEFAULT_ASSIGNED_ROLE = CustomerTechnicalContact.ROLE;
    private static final Role UPDATED_ASSIGNED_ROLE = CustomerContractualContact.ROLE;

    @Autowired
    private UserRoleAssignmentRepository userRoleAssignmentRepository;

    @Autowired
    private UserRoleAssignmentService userRoleAssignmentService;

    @Autowired
    private UserRoleAssignmentQueryService userRoleAssignmentQueryService;

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

    private MockMvc restUserRoleAssignmentMockMvc;

    private UserRoleAssignment userRoleAssignment;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final UserRoleAssignmentResource userRoleAssignmentResource = new UserRoleAssignmentResource(
                userRoleAssignmentService,
                userRoleAssignmentQueryService);
        this.restUserRoleAssignmentMockMvc = MockMvcBuilders.standaloneSetup(userRoleAssignmentResource)
                .setCustomArgumentResolvers(pageableArgumentResolver)
                .setControllerAdvice(exceptionTranslator)
                .setConversionService(createFormattingConversionService())
                .setMessageConverters(jacksonMessageConverter)
                .setValidator(validator)
                .build();

        SecurityContextFake.havingAuthenticatedUser().withAuthority(Role.Supporter.ROLE.authority());
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UserRoleAssignment createEntity(EntityManager em) {
        User user = UserResourceIntTest.createEntity(em);
        em.persist(user);
        em.flush();
        return new UserRoleAssignment()
                .entityTypeId(DEFAULT_ENTITY_TYPE_ID)
                .entityObjectId(DEFAULT_ENTITY_OBJECT_ID)
                .user(user)
                .assignedRole(DEFAULT_ASSIGNED_ROLE);
    }

    @Before
    public void initTest() {
        userRoleAssignment = createEntity(em);
    }

    @Test
    @Transactional
    public void createUserRoleAssignment() throws Exception {
        int databaseSizeBeforeCreate = userRoleAssignmentRepository.findAll().size();

        // Create the UserRoleAssignment
        SecurityContextFake.havingAuthenticatedUser().withAuthority(Admin.ROLE.authority());
        restUserRoleAssignmentMockMvc.perform(
                post("/api/user-role-assignments")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(userRoleAssignment)))
                .andExpect(status().isCreated());

        // Validate the UserRoleAssignment in the database
        List<UserRoleAssignment> userRoleAssignmentList = userRoleAssignmentRepository.findAll();
        assertThat(userRoleAssignmentList).hasSize(databaseSizeBeforeCreate + 1);
        UserRoleAssignment testUserRoleAssignment = userRoleAssignmentList.get(userRoleAssignmentList.size() - 1);
        assertThat(testUserRoleAssignment.getEntityTypeId()).isEqualTo(DEFAULT_ENTITY_TYPE_ID);
        assertThat(testUserRoleAssignment.getEntityObjectId()).isEqualTo(DEFAULT_ENTITY_OBJECT_ID);
        assertThat(testUserRoleAssignment.getAssignedRole().name()).isEqualTo(DEFAULT_ASSIGNED_ROLE.name());
        assertThat(testUserRoleAssignment.getAssignedRole()).isEqualTo(DEFAULT_ASSIGNED_ROLE);
    }

    @Test
    @Transactional
    public void createUserRoleAssignmentWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = userRoleAssignmentRepository.findAll().size();

        // Create the UserRoleAssignment with an existing ID
        userRoleAssignment.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restUserRoleAssignmentMockMvc.perform(
                post("/api/user-role-assignments")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(userRoleAssignment)))
                .andExpect(status().isBadRequest());

        // Validate the UserRoleAssignment in the database
        List<UserRoleAssignment> userRoleAssignmentList = userRoleAssignmentRepository.findAll();
        assertThat(userRoleAssignmentList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkEntityTypeIdIsRequired() throws Exception {
        int databaseSizeBeforeTest = userRoleAssignmentRepository.findAll().size();
        // set the field null
        userRoleAssignment.setEntityTypeId(null);

        // Create the UserRoleAssignment, which fails.

        restUserRoleAssignmentMockMvc.perform(
                post("/api/user-role-assignments")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(userRoleAssignment)))
                .andExpect(status().isBadRequest());

        List<UserRoleAssignment> userRoleAssignmentList = userRoleAssignmentRepository.findAll();
        assertThat(userRoleAssignmentList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkEntityObjectIdIsRequired() throws Exception {
        int databaseSizeBeforeTest = userRoleAssignmentRepository.findAll().size();
        // set the field null
        userRoleAssignment.setEntityObjectId(null);

        // Create the UserRoleAssignment, which fails.

        restUserRoleAssignmentMockMvc.perform(
                post("/api/user-role-assignments")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(userRoleAssignment)))
                .andExpect(status().isBadRequest());

        List<UserRoleAssignment> userRoleAssignmentList = userRoleAssignmentRepository.findAll();
        assertThat(userRoleAssignmentList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkAssignedRoleIsRequired() throws Exception {
        int databaseSizeBeforeTest = userRoleAssignmentRepository.findAll().size();
        // set the field null
        userRoleAssignment.setAssignedRole(null);

        // Create the UserRoleAssignment, which fails.

        restUserRoleAssignmentMockMvc.perform(
                post("/api/user-role-assignments")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(userRoleAssignment)))
                .andExpect(status().isBadRequest());

        List<UserRoleAssignment> userRoleAssignmentList = userRoleAssignmentRepository.findAll();
        assertThat(userRoleAssignmentList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllUserRoleAssignments() throws Exception {
        // Initialize the database
        userRoleAssignmentRepository.saveAndFlush(userRoleAssignment);

        // Get all the userRoleAssignmentList
        restUserRoleAssignmentMockMvc.perform(get("/api/user-role-assignments?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(userRoleAssignment.getId().intValue())))
                .andExpect(jsonPath("$.[*].entityTypeId").value(hasItem(DEFAULT_ENTITY_TYPE_ID)))
                .andExpect(jsonPath("$.[*].entityObjectId").value(hasItem(DEFAULT_ENTITY_OBJECT_ID.intValue())))
                .andExpect(jsonPath("$.[*].assignedRole").value(hasItem(DEFAULT_ASSIGNED_ROLE.name())));
    }

    @Test
    @Transactional
    public void getUserRoleAssignment() throws Exception {
        // Initialize the database
        userRoleAssignmentRepository.saveAndFlush(userRoleAssignment);

        // Get the userRoleAssignment
        restUserRoleAssignmentMockMvc.perform(get("/api/user-role-assignments/{id}", userRoleAssignment.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.id").value(userRoleAssignment.getId().intValue()))
                .andExpect(jsonPath("$.entityTypeId").value(DEFAULT_ENTITY_TYPE_ID))
                .andExpect(jsonPath("$.entityObjectId").value(DEFAULT_ENTITY_OBJECT_ID.intValue()))
                .andExpect(jsonPath("$.assignedRole").value(DEFAULT_ASSIGNED_ROLE.name()));
    }

    @Test
    @Transactional
    public void getAllUserRoleAssignmentsByEntityTypeIdIsEqualToSomething() throws Exception {
        // Initialize the database
        userRoleAssignmentRepository.saveAndFlush(userRoleAssignment);

        // Get all the userRoleAssignmentList where entityTypeId equals to DEFAULT_ENTITY_TYPE_ID
        defaultUserRoleAssignmentShouldBeFound("entityTypeId.equals=" + DEFAULT_ENTITY_TYPE_ID);

        // Get all the userRoleAssignmentList where entityTypeId equals to UPDATED_ENTITY_TYPE_ID
        defaultUserRoleAssignmentShouldNotBeFound("entityTypeId.equals=" + UPDATED_ENTITY_TYPE_ID);
    }

    @Test
    @Transactional
    public void getAllUserRoleAssignmentsByEntityTypeIdIsInShouldWork() throws Exception {
        // Initialize the database
        userRoleAssignmentRepository.saveAndFlush(userRoleAssignment);

        // Get all the userRoleAssignmentList where entityTypeId in DEFAULT_ENTITY_TYPE_ID or UPDATED_ENTITY_TYPE_ID
        defaultUserRoleAssignmentShouldBeFound("entityTypeId.in=" + DEFAULT_ENTITY_TYPE_ID + "," + UPDATED_ENTITY_TYPE_ID);

        // Get all the userRoleAssignmentList where entityTypeId equals to UPDATED_ENTITY_TYPE_ID
        defaultUserRoleAssignmentShouldNotBeFound("entityTypeId.in=" + UPDATED_ENTITY_TYPE_ID);
    }

    @Test
    @Transactional
    public void getAllUserRoleAssignmentsByEntityTypeIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        userRoleAssignmentRepository.saveAndFlush(userRoleAssignment);

        // Get all the userRoleAssignmentList where entityTypeId is not null
        defaultUserRoleAssignmentShouldBeFound("entityTypeId.specified=true");

        // Get all the userRoleAssignmentList where entityTypeId is null
        defaultUserRoleAssignmentShouldNotBeFound("entityTypeId.specified=false");
    }

    @Test
    @Transactional
    public void getAllUserRoleAssignmentsByEntityObjectIdIsEqualToSomething() throws Exception {
        // Initialize the database
        userRoleAssignmentRepository.saveAndFlush(userRoleAssignment);

        // Get all the userRoleAssignmentList where entityObjectId equals to DEFAULT_ENTITY_OBJECT_ID
        defaultUserRoleAssignmentShouldBeFound("entityObjectId.equals=" + DEFAULT_ENTITY_OBJECT_ID);

        // Get all the userRoleAssignmentList where entityObjectId equals to UPDATED_ENTITY_OBJECT_ID
        defaultUserRoleAssignmentShouldNotBeFound("entityObjectId.equals=" + UPDATED_ENTITY_OBJECT_ID);
    }

    @Test
    @Transactional
    public void getAllUserRoleAssignmentsByEntityObjectIdIsInShouldWork() throws Exception {
        // Initialize the database
        userRoleAssignmentRepository.saveAndFlush(userRoleAssignment);

        // Get all the userRoleAssignmentList where entityObjectId in DEFAULT_ENTITY_OBJECT_ID or UPDATED_ENTITY_OBJECT_ID
        defaultUserRoleAssignmentShouldBeFound(
                "entityObjectId.in=" + DEFAULT_ENTITY_OBJECT_ID + "," + UPDATED_ENTITY_OBJECT_ID);

        // Get all the userRoleAssignmentList where entityObjectId equals to UPDATED_ENTITY_OBJECT_ID
        defaultUserRoleAssignmentShouldNotBeFound("entityObjectId.in=" + UPDATED_ENTITY_OBJECT_ID);
    }

    @Test
    @Transactional
    public void getAllUserRoleAssignmentsByEntityObjectIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        userRoleAssignmentRepository.saveAndFlush(userRoleAssignment);

        // Get all the userRoleAssignmentList where entityObjectId is not null
        defaultUserRoleAssignmentShouldBeFound("entityObjectId.specified=true");

        // Get all the userRoleAssignmentList where entityObjectId is null
        defaultUserRoleAssignmentShouldNotBeFound("entityObjectId.specified=false");
    }

    @Test
    @Transactional
    public void getAllUserRoleAssignmentsByEntityObjectIdIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        userRoleAssignmentRepository.saveAndFlush(userRoleAssignment);

        // Get all the userRoleAssignmentList where entityObjectId greater than or equals to DEFAULT_ENTITY_OBJECT_ID
        defaultUserRoleAssignmentShouldBeFound("entityObjectId.greaterOrEqualThan=" + DEFAULT_ENTITY_OBJECT_ID);

        // Get all the userRoleAssignmentList where entityObjectId greater than or equals to UPDATED_ENTITY_OBJECT_ID
        defaultUserRoleAssignmentShouldNotBeFound("entityObjectId.greaterOrEqualThan=" + UPDATED_ENTITY_OBJECT_ID);
    }

    @Test
    @Transactional
    public void getAllUserRoleAssignmentsByEntityObjectIdIsLessThanSomething() throws Exception {
        // Initialize the database
        userRoleAssignmentRepository.saveAndFlush(userRoleAssignment);

        // Get all the userRoleAssignmentList where entityObjectId less than or equals to DEFAULT_ENTITY_OBJECT_ID
        defaultUserRoleAssignmentShouldNotBeFound("entityObjectId.lessThan=" + DEFAULT_ENTITY_OBJECT_ID);

        // Get all the userRoleAssignmentList where entityObjectId less than or equals to UPDATED_ENTITY_OBJECT_ID
        defaultUserRoleAssignmentShouldBeFound("entityObjectId.lessThan=" + UPDATED_ENTITY_OBJECT_ID);
    }

    @Test
    @Transactional
    public void getAllUserRoleAssignmentsByAssignedRoleIsEqualToSomething() throws Exception {
        // Initialize the database
        userRoleAssignmentRepository.saveAndFlush(userRoleAssignment);

        // Get all the userRoleAssignmentList where assignedRole equals to DEFAULT_ASSIGNED_ROLE
        defaultUserRoleAssignmentShouldBeFound("assignedRole.equals=" + DEFAULT_ASSIGNED_ROLE.name());

        // Get all the userRoleAssignmentList where assignedRole equals to UPDATED_ASSIGNED_ROLE
        defaultUserRoleAssignmentShouldNotBeFound("assignedRole.equals=" + UPDATED_ASSIGNED_ROLE.name());
    }

    @Test
    @Transactional
    public void getAllUserRoleAssignmentsByAssignedRoleIsInShouldWork() throws Exception {
        // Initialize the database
        userRoleAssignmentRepository.saveAndFlush(userRoleAssignment);

        // Get all the userRoleAssignmentList where assignedRole in DEFAULT_ASSIGNED_ROLE or UPDATED_ASSIGNED_ROLE
        defaultUserRoleAssignmentShouldBeFound(
                "assignedRole.in=" + DEFAULT_ASSIGNED_ROLE.name() + "," + UPDATED_ASSIGNED_ROLE.name());

        // Get all the userRoleAssignmentList where assignedRole equals to UPDATED_ASSIGNED_ROLE
        defaultUserRoleAssignmentShouldNotBeFound("assignedRole.in=" + UPDATED_ASSIGNED_ROLE.name());
    }

    @Test
    @Transactional
    public void getAllUserRoleAssignmentsByAssignedRoleIsNullOrNotNull() throws Exception {
        // Initialize the database
        userRoleAssignmentRepository.saveAndFlush(userRoleAssignment);

        // Get all the userRoleAssignmentList where assignedRole is not null
        defaultUserRoleAssignmentShouldBeFound("assignedRole.specified=true");

        // Get all the userRoleAssignmentList where assignedRole is null
        defaultUserRoleAssignmentShouldNotBeFound("assignedRole.specified=false");
    }

    @Test
    @Transactional
    public void getAllUserRoleAssignmentsByUserIsEqualToSomething() throws Exception {
        // Initialize the database
        User user = UserResourceIntTest.createEntity(em);
        em.persist(user);
        em.flush();
        userRoleAssignment.setUser(user);
        userRoleAssignmentRepository.saveAndFlush(userRoleAssignment);
        Long userId = user.getId();

        // Get all the userRoleAssignmentList where user equals to userId
        defaultUserRoleAssignmentShouldBeFound("userId.equals=" + userId);

        // Get all the userRoleAssignmentList where user equals to userId + 1
        defaultUserRoleAssignmentShouldNotBeFound("userId.equals=" + (userId + 1));
    }

    /**
     * Executes the search, and checks that the default entity is returned
     */
    private void defaultUserRoleAssignmentShouldBeFound(String filter) throws Exception {
        restUserRoleAssignmentMockMvc.perform(get("/api/user-role-assignments?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(userRoleAssignment.getId().intValue())))
                .andExpect(jsonPath("$.[*].entityTypeId").value(hasItem(DEFAULT_ENTITY_TYPE_ID)))
                .andExpect(jsonPath("$.[*].entityObjectId").value(hasItem(DEFAULT_ENTITY_OBJECT_ID.intValue())))
                .andExpect(jsonPath("$.[*].assignedRole").value(hasItem(DEFAULT_ASSIGNED_ROLE.name())));

        // Check, that the count call also returns 1
        restUserRoleAssignmentMockMvc.perform(get("/api/user-role-assignments/count?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned
     */
    private void defaultUserRoleAssignmentShouldNotBeFound(String filter) throws Exception {
        restUserRoleAssignmentMockMvc.perform(get("/api/user-role-assignments?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restUserRoleAssignmentMockMvc.perform(get("/api/user-role-assignments/count?sort=id,desc&" + filter))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    public void getNonExistingUserRoleAssignment() throws Exception {
        // Get the userRoleAssignment
        restUserRoleAssignmentMockMvc.perform(get("/api/user-role-assignments/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateUserRoleAssignment() throws Exception {
        // Initialize the database
        userRoleAssignmentService.save(userRoleAssignment);

        int databaseSizeBeforeUpdate = userRoleAssignmentRepository.findAll().size();

        // Update the userRoleAssignment
        SecurityContextFake.havingAuthenticatedUser().withAuthority(Admin.ROLE.authority());
        UserRoleAssignment updatedUserRoleAssignment = userRoleAssignmentRepository.findById(userRoleAssignment.getId()).get();
        // Disconnect from session so that the updates on updatedUserRoleAssignment are not directly saved in db
        em.detach(updatedUserRoleAssignment);
        updatedUserRoleAssignment
                .entityTypeId(UPDATED_ENTITY_TYPE_ID)
                .entityObjectId(UPDATED_ENTITY_OBJECT_ID)
                .assignedRole(UPDATED_ASSIGNED_ROLE);

        restUserRoleAssignmentMockMvc.perform(
                put("/api/user-role-assignments")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(updatedUserRoleAssignment)))
                .andExpect(status().isOk());

        // Validate the UserRoleAssignment in the database
        List<UserRoleAssignment> userRoleAssignmentList = userRoleAssignmentRepository.findAll();
        assertThat(userRoleAssignmentList).hasSize(databaseSizeBeforeUpdate);
        UserRoleAssignment testUserRoleAssignment = userRoleAssignmentList.get(userRoleAssignmentList.size() - 1);
        assertThat(testUserRoleAssignment.getEntityTypeId()).isEqualTo(UPDATED_ENTITY_TYPE_ID);
        assertThat(testUserRoleAssignment.getEntityObjectId()).isEqualTo(UPDATED_ENTITY_OBJECT_ID);
        assertThat(testUserRoleAssignment.getAssignedRole().name()).isEqualTo(UPDATED_ASSIGNED_ROLE.name());
        assertThat(testUserRoleAssignment.getAssignedRole()).isEqualTo(UPDATED_ASSIGNED_ROLE);
    }

    @Test
    @Transactional
    public void updateNonExistingUserRoleAssignment() throws Exception {
        int databaseSizeBeforeUpdate = userRoleAssignmentRepository.findAll().size();

        // Create the UserRoleAssignment

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restUserRoleAssignmentMockMvc.perform(
                put("/api/user-role-assignments")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(userRoleAssignment)))
                .andExpect(status().isBadRequest());

        // Validate the UserRoleAssignment in the database
        List<UserRoleAssignment> userRoleAssignmentList = userRoleAssignmentRepository.findAll();
        assertThat(userRoleAssignmentList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteUserRoleAssignment() throws Exception {
        // Initialize the database
        userRoleAssignmentService.save(userRoleAssignment);

        int databaseSizeBeforeDelete = userRoleAssignmentRepository.findAll().size();

        // Delete the userRoleAssignment
        restUserRoleAssignmentMockMvc.perform(
                delete("/api/user-role-assignments/{id}", userRoleAssignment.getId())
                        .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<UserRoleAssignment> userRoleAssignmentList = userRoleAssignmentRepository.findAll();
        assertThat(userRoleAssignmentList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(UserRoleAssignment.class);
        UserRoleAssignment userRoleAssignment1 = new UserRoleAssignment();
        userRoleAssignment1.setId(1L);
        UserRoleAssignment userRoleAssignment2 = new UserRoleAssignment();
        userRoleAssignment2.setId(userRoleAssignment1.getId());
        assertThat(userRoleAssignment1).isEqualTo(userRoleAssignment2);
        userRoleAssignment2.setId(2L);
        assertThat(userRoleAssignment1).isNotEqualTo(userRoleAssignment2);
        userRoleAssignment1.setId(null);
        assertThat(userRoleAssignment1).isNotEqualTo(userRoleAssignment2);
    }
}
