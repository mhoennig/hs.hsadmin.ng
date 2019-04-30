// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service;

import org.hostsharing.hsadminng.domain.*;
import org.hostsharing.hsadminng.domain.Customer;
import org.hostsharing.hsadminng.repository.CustomerRepository;
import org.hostsharing.hsadminng.service.dto.CustomerCriteria;
import org.hostsharing.hsadminng.service.dto.CustomerDTO;
import org.hostsharing.hsadminng.service.mapper.CustomerMapper;

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
 * Service for executing complex queries for Customer entities in the database.
 * The main input is a {@link CustomerCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link CustomerDTO} or a {@link Page} of {@link CustomerDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class CustomerQueryService extends QueryService<Customer> {

    private final Logger log = LoggerFactory.getLogger(CustomerQueryService.class);

    private final CustomerRepository customerRepository;

    private final CustomerMapper customerMapper;

    public CustomerQueryService(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    /**
     * Return a {@link List} of {@link CustomerDTO} which matches the criteria from the database
     * 
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public List<CustomerDTO> findByCriteria(CustomerCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<Customer> specification = createSpecification(criteria);
        return customerMapper.toDto(customerRepository.findAll(specification));
    }

    /**
     * Return a {@link Page} of {@link CustomerDTO} which matches the criteria from the database
     * 
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<CustomerDTO> findByCriteria(CustomerCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Customer> specification = createSpecification(criteria);
        return customerRepository.findAll(specification, page)
                .map(customerMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database
     * 
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(CustomerCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<Customer> specification = createSpecification(criteria);
        return customerRepository.count(specification);
    }

    /**
     * Function to convert CustomerCriteria to a {@link Specification}
     */
    private Specification<Customer> createSpecification(CustomerCriteria criteria) {
        Specification<Customer> specification = Specification.where(null);
        if (criteria != null) {
            if (criteria.getId() != null) {
                specification = specification.and(buildSpecification(criteria.getId(), Customer_.id));
            }
            if (criteria.getReference() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getReference(), Customer_.reference));
            }
            if (criteria.getPrefix() != null) {
                specification = specification.and(buildStringSpecification(criteria.getPrefix(), Customer_.prefix));
            }
            if (criteria.getName() != null) {
                specification = specification.and(buildStringSpecification(criteria.getName(), Customer_.name));
            }
            if (criteria.getKind() != null) {
                specification = specification.and(buildSpecification(criteria.getKind(), Customer_.kind));
            }
            if (criteria.getBirthDate() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getBirthDate(), Customer_.birthDate));
            }
            if (criteria.getBirthPlace() != null) {
                specification = specification.and(buildStringSpecification(criteria.getBirthPlace(), Customer_.birthPlace));
            }
            if (criteria.getRegistrationCourt() != null) {
                specification = specification
                        .and(buildStringSpecification(criteria.getRegistrationCourt(), Customer_.registrationCourt));
            }
            if (criteria.getRegistrationNumber() != null) {
                specification = specification
                        .and(buildStringSpecification(criteria.getRegistrationNumber(), Customer_.registrationNumber));
            }
            if (criteria.getVatRegion() != null) {
                specification = specification.and(buildSpecification(criteria.getVatRegion(), Customer_.vatRegion));
            }
            if (criteria.getVatNumber() != null) {
                specification = specification.and(buildStringSpecification(criteria.getVatNumber(), Customer_.vatNumber));
            }
            if (criteria.getContractualSalutation() != null) {
                specification = specification
                        .and(buildStringSpecification(criteria.getContractualSalutation(), Customer_.contractualSalutation));
            }
            if (criteria.getContractualAddress() != null) {
                specification = specification
                        .and(buildStringSpecification(criteria.getContractualAddress(), Customer_.contractualAddress));
            }
            if (criteria.getBillingSalutation() != null) {
                specification = specification
                        .and(buildStringSpecification(criteria.getBillingSalutation(), Customer_.billingSalutation));
            }
            if (criteria.getBillingAddress() != null) {
                specification = specification
                        .and(buildStringSpecification(criteria.getBillingAddress(), Customer_.billingAddress));
            }
            if (criteria.getRemark() != null) {
                specification = specification.and(buildStringSpecification(criteria.getRemark(), Customer_.remark));
            }
            if (criteria.getMembershipId() != null) {
                specification = specification.and(
                        buildSpecification(
                                criteria.getMembershipId(),
                                root -> root.join(Customer_.memberships, JoinType.LEFT).get(Membership_.id)));
            }
            if (criteria.getSepamandateId() != null) {
                specification = specification.and(
                        buildSpecification(
                                criteria.getSepamandateId(),
                                root -> root.join(Customer_.sepamandates, JoinType.LEFT).get(SepaMandate_.id)));
            }
        }
        return specification;
    }
}
