// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service.dto;

import static org.apache.commons.lang3.tuple.ImmutablePair.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.domain.User;
import org.hostsharing.hsadminng.domain.UserRoleAssignment;
import org.hostsharing.hsadminng.repository.UserRepository;
import org.hostsharing.hsadminng.repository.UserRoleAssignmentRepository;
import org.hostsharing.hsadminng.service.UserRoleAssignmentService;
import org.hostsharing.hsadminng.service.accessfilter.JSonBuilder;
import org.hostsharing.hsadminng.service.accessfilter.Role;
import org.hostsharing.hsadminng.service.accessfilter.SecurityContextMock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Optional;

@JsonTest
@SpringBootTest(
        classes = {
                UserRoleAssignmentRepository.class,
                UserRoleAssignmentService.class,
                UserRoleAssignment.UserRoleAssignmentJsonSerializer.class,
                UserRoleAssignment.UserRoleAssignmentJsonDeserializer.class })
@RunWith(SpringRunner.class)
public class UserRoleAssignmentUnitTest {

    private static final long USER_ROLE_ASSIGNMENT_ID = 1234L;
    private static final long CUSTOMER_ID = 888L;
    private static final long USER_ID = 42L;

    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserRoleAssignmentRepository userRoleAssignmentRepository;

    @MockBean
    private UserRoleAssignmentService userRoleAssignmentService;

    private SecurityContextMock securityContext;

    @Before
    public void init() {
        securityContext = SecurityContextMock.usingMock(userRoleAssignmentService);
    }

    @Test
    public void testSerializationAsContractualCustomerContact() throws JsonProcessingException {

        // given
        securityContext.havingAuthenticatedUser()
                .withRole(
                        CustomerDTO.class,
                        CUSTOMER_ID,
                        Role.CustomerContractualContact.ROLE);
        UserRoleAssignment given = createSomeUserRoleAssignment(USER_ROLE_ASSIGNMENT_ID);

        // when
        String actual = objectMapper.writeValueAsString(given);

        // then
        assertEquals("{}", actual); // dependent rights not yet implemented for UserRoleAssignments
    }

    @Test
    public void testSerializationAsSupporter() throws JsonProcessingException {

        // given
        securityContext.havingAuthenticatedUser().withAuthority(Role.Supporter.ROLE.authority());
        UserRoleAssignment given = createSomeUserRoleAssignment(USER_ROLE_ASSIGNMENT_ID);

        // when
        String actual = objectMapper.writeValueAsString(given);

        // then
        assertThat(actual).isEqualTo(createExpectedJSon(given));
    }

    @Test
    public void testDeserializeAsAdmin() throws IOException {
        // given
        securityContext.havingAuthenticatedUser().withAuthority(Role.Admin.ROLE.authority());
        given(userRoleAssignmentRepository.findById(USER_ROLE_ASSIGNMENT_ID))
                .willReturn(Optional.of(new UserRoleAssignment().id(USER_ROLE_ASSIGNMENT_ID)));
        final User expectedUser = new User().id(USER_ID);
        given(userRepository.getOne(USER_ID)).willReturn(expectedUser);
        String json = JSonBuilder.asJSon(
                of("id", USER_ROLE_ASSIGNMENT_ID),
                of("entityTypeId", Customer.ENTITY_TYPE_ID),
                of("entityObjectId", CUSTOMER_ID),
                of(
                        "user",
                        JSonBuilder.asJSon(
                                of("id", USER_ID))),
                of("assignedRole", Role.CustomerTechnicalContact.ROLE.name()));

        // when
        UserRoleAssignment actual = objectMapper.readValue(json, UserRoleAssignment.class);

        // then
        UserRoleAssignment expected = new UserRoleAssignment();
        expected.setId(USER_ROLE_ASSIGNMENT_ID);
        expected.setEntityTypeId(Customer.ENTITY_TYPE_ID);
        expected.setEntityObjectId(CUSTOMER_ID);
        expected.setAssignedRole(Role.CustomerTechnicalContact.ROLE);
        expected.setUser(expectedUser);
        assertThat(actual).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void getAssignedRoleHandlesNullValue() {
        assertThat(new UserRoleAssignment().assignedRole(null).getAssignedRole()).isNull();
        assertThat(new UserRoleAssignment().assignedRole(Role.Admin.ROLE).getAssignedRole()).isEqualTo(Role.Admin.ROLE);
    }

    // --- only test fixture below ---

    public static String createExpectedJSon(UserRoleAssignment dto) {
        return new JSonBuilder()
                .withFieldValueIfPresent("id", dto.getId())
                .withFieldValueIfPresent("entityTypeId", dto.getEntityTypeId())
                .withFieldValueIfPresent("entityObjectId", dto.getEntityObjectId())
                .withFieldValueIfPresent("assignedRole", dto.getAssignedRole())
                .withFieldValueIfPresent("user", dto.getUser().getId())
                .toString();
    }

    public static UserRoleAssignment createSomeUserRoleAssignment(final Long id) {
        final UserRoleAssignment given = new UserRoleAssignment();
        given.setId(id);
        given.setEntityTypeId(Customer.ENTITY_TYPE_ID);
        given.setEntityObjectId(CUSTOMER_ID);
        given.setUser(new User().id(USER_ID));
        given.setAssignedRole(Role.CustomerTechnicalContact.ROLE);
        return given;
    }
}
