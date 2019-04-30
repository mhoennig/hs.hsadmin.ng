// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service;

import org.hostsharing.hsadminng.domain.*;
import org.hostsharing.hsadminng.domain.UserRoleAssignment;
import org.hostsharing.hsadminng.repository.UserRoleAssignmentRepository;
import org.hostsharing.hsadminng.service.dto.UserRoleAssignmentCriteria;

import io.github.jhipster.service.QueryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import javax.persistence.criteria.JoinType;

/**
 * Service for executing complex queries for UserRoleAssignment entities in the database.
 * The main input is a {@link UserRoleAssignmentCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link UserRoleAssignment} or a {@link Page} of {@link UserRoleAssignment} which fulfills the
 * criteria.
 */
@Service
@Transactional(readOnly = true)
public class UserRoleAssignmentQueryService extends QueryService<UserRoleAssignment> {

    private final Logger log = LoggerFactory.getLogger(UserRoleAssignmentQueryService.class);

    private final UserRoleAssignmentRepository userRoleAssignmentRepository;

    public UserRoleAssignmentQueryService(UserRoleAssignmentRepository userRoleAssignmentRepository) {
        this.userRoleAssignmentRepository = userRoleAssignmentRepository;
    }

    /**
     * Return a {@link List} of {@link UserRoleAssignment} which matches the criteria from the database
     * 
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public List<UserRoleAssignment> findByCriteria(UserRoleAssignmentCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<UserRoleAssignment> specification = createSpecification(criteria);
        return userRoleAssignmentRepository.findAll(specification);
    }

    /**
     * Return a {@link Page} of {@link UserRoleAssignment} which matches the criteria from the database
     * 
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<UserRoleAssignment> findByCriteria(UserRoleAssignmentCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<UserRoleAssignment> specification = createSpecification(criteria);
        return userRoleAssignmentRepository.findAll(specification, page);
    }

    /**
     * Return the number of matching entities in the database
     * 
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(UserRoleAssignmentCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<UserRoleAssignment> specification = createSpecification(criteria);
        return userRoleAssignmentRepository.count(specification);
    }

    /**
     * Function to convert UserRoleAssignmentCriteria to a {@link Specification}
     */
    private Specification<UserRoleAssignment> createSpecification(UserRoleAssignmentCriteria criteria) {
        Specification<UserRoleAssignment> specification = Specification.where(null);
        if (criteria != null) {
            if (criteria.getId() != null) {
                specification = specification.and(buildSpecification(criteria.getId(), UserRoleAssignment_.id));
            }
            if (criteria.getEntityTypeId() != null) {
                specification = specification
                        .and(buildStringSpecification(criteria.getEntityTypeId(), UserRoleAssignment_.entityTypeId));
            }
            if (criteria.getEntityObjectId() != null) {
                specification = specification
                        .and(buildRangeSpecification(criteria.getEntityObjectId(), UserRoleAssignment_.entityObjectId));
            }
            if (criteria.getAssignedRole() != null) {
                specification = specification
                        .and(buildSpecification(criteria.getAssignedRole(), UserRoleAssignment_.assignedRole));
            }
            if (criteria.getUserId() != null) {
                specification = specification.and(
                        buildSpecification(
                                criteria.getUserId(),
                                root -> root.join(UserRoleAssignment_.user, JoinType.LEFT).get(User_.id)));
            }
        }
        return specification;
    }
}
