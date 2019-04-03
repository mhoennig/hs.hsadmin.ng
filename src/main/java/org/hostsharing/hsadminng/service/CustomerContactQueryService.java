package org.hostsharing.hsadminng.service;

import java.util.List;

import javax.persistence.criteria.JoinType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.jhipster.service.QueryService;

import org.hostsharing.hsadminng.domain.CustomerContact;
import org.hostsharing.hsadminng.domain.*; // for static metamodels
import org.hostsharing.hsadminng.repository.CustomerContactRepository;
import org.hostsharing.hsadminng.service.dto.CustomerContactCriteria;
import org.hostsharing.hsadminng.service.dto.CustomerContactDTO;
import org.hostsharing.hsadminng.service.mapper.CustomerContactMapper;

/**
 * Service for executing complex queries for CustomerContact entities in the database.
 * The main input is a {@link CustomerContactCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link CustomerContactDTO} or a {@link Page} of {@link CustomerContactDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class CustomerContactQueryService extends QueryService<CustomerContact> {

    private final Logger log = LoggerFactory.getLogger(CustomerContactQueryService.class);

    private final CustomerContactRepository customerContactRepository;

    private final CustomerContactMapper customerContactMapper;

    public CustomerContactQueryService(CustomerContactRepository customerContactRepository, CustomerContactMapper customerContactMapper) {
        this.customerContactRepository = customerContactRepository;
        this.customerContactMapper = customerContactMapper;
    }

    /**
     * Return a {@link List} of {@link CustomerContactDTO} which matches the criteria from the database
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public List<CustomerContactDTO> findByCriteria(CustomerContactCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<CustomerContact> specification = createSpecification(criteria);
        return customerContactMapper.toDto(customerContactRepository.findAll(specification));
    }

    /**
     * Return a {@link Page} of {@link CustomerContactDTO} which matches the criteria from the database
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<CustomerContactDTO> findByCriteria(CustomerContactCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<CustomerContact> specification = createSpecification(criteria);
        return customerContactRepository.findAll(specification, page)
            .map(customerContactMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(CustomerContactCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<CustomerContact> specification = createSpecification(criteria);
        return customerContactRepository.count(specification);
    }

    /**
     * Function to convert CustomerContactCriteria to a {@link Specification}
     */
    private Specification<CustomerContact> createSpecification(CustomerContactCriteria criteria) {
        Specification<CustomerContact> specification = Specification.where(null);
        if (criteria != null) {
            if (criteria.getId() != null) {
                specification = specification.and(buildSpecification(criteria.getId(), CustomerContact_.id));
            }
            if (criteria.getRole() != null) {
                specification = specification.and(buildSpecification(criteria.getRole(), CustomerContact_.role));
            }
            if (criteria.getContactId() != null) {
                specification = specification.and(buildSpecification(criteria.getContactId(),
                    root -> root.join(CustomerContact_.contact, JoinType.LEFT).get(Contact_.id)));
            }
            if (criteria.getCustomerId() != null) {
                specification = specification.and(buildSpecification(criteria.getCustomerId(),
                    root -> root.join(CustomerContact_.customer, JoinType.LEFT).get(Customer_.id)));
            }
        }
        return specification;
    }
}
