// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service;

import org.hostsharing.hsadminng.domain.UserRoleAssignment;
import org.hostsharing.hsadminng.repository.UserRoleAssignmentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service Implementation for managing UserRoleAssignment.
 */
@Service
@Transactional
public class UserRoleAssignmentService {

    private final Logger log = LoggerFactory.getLogger(UserRoleAssignmentService.class);

    private final UserRoleAssignmentRepository userRoleAssignmentRepository;

    public UserRoleAssignmentService(UserRoleAssignmentRepository userRoleAssignmentRepository) {
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
}
