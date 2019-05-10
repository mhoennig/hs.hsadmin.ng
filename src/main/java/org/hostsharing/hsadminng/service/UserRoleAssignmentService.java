// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service;

import static com.google.common.base.Verify.verify;

import org.hostsharing.hsadminng.domain.UserRoleAssignment;
import org.hostsharing.hsadminng.repository.UserRepository;
import org.hostsharing.hsadminng.repository.UserRoleAssignmentRepository;
import org.hostsharing.hsadminng.security.SecurityUtils;
import org.hostsharing.hsadminng.service.accessfilter.Role;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing UserRoleAssignment.
 */
@Service
@Transactional
public class UserRoleAssignmentService {

    private final Logger log = LoggerFactory.getLogger(UserRoleAssignmentService.class);

    private final UserRoleAssignmentRepository userRoleAssignmentRepository;

    public UserRoleAssignmentService(
            final UserRepository userRepository,
            final UserRoleAssignmentRepository userRoleAssignmentRepository) {
        this.userRoleAssignmentRepository = userRoleAssignmentRepository;
    }

    /**
     * Save a userRoleAssignment.
     *
     * @param userRoleAssignment the entity to save
     * @return the persisted entity
     */
    public UserRoleAssignment save(UserRoleAssignment userRoleAssignment) {
        log.debug("Request to save UserRoleAssignment : {}", userRoleAssignment);
        return userRoleAssignmentRepository.save(userRoleAssignment);
    }

    /**
     * Get all the userRoleAssignments.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<UserRoleAssignment> findAll(Pageable pageable) {
        log.debug("Request to get all UserRoleAssignments");
        return userRoleAssignmentRepository.findAll(pageable);
    }

    /**
     * Get one userRoleAssignment by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Optional<UserRoleAssignment> findOne(Long id) {
        log.debug("Request to get UserRoleAssignment : {}", id);
        return userRoleAssignmentRepository.findById(id);
    }

    /**
     * Delete the userRoleAssignment by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete UserRoleAssignment : {}", id);
        userRoleAssignmentRepository.deleteById(id);
    }

    /**
     * Collects all roles assigned to the current login user for the specified entity.
     *
     * @param entityTypeId the type id of the entity, e.g. "customer.Customer", not null
     * @param entityObjectId id of entity instance in given entity type
     * @return a set of all roles assigned to the current login user
     */
    public Set<Role> getEffectiveRoleOfCurrentUser(final String entityTypeId, final long entityObjectId) {
        verify(entityTypeId != null);

        // findByLogin is cached, thus I presume this is faster more specific query which would result in many DB roundtrips
        final Set<Role> roles = SecurityUtils.getCurrentUserLogin()
                .map(
                        login -> userRoleAssignmentRepository.findByLogin(login)
                                .stream()
                                .filter(ura -> matches(entityTypeId, entityObjectId, ura))
                                .map(UserRoleAssignment::getAssignedRole)
                                .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
        return roles;
    }

    private static boolean matches(final String entityTypeId, final long entityObjectId, final UserRoleAssignment ura) {
        return ura.getEntityTypeId().equals(entityTypeId) && ura.getEntityObjectId().equals(entityObjectId);
    }
}
