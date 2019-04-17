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

import org.hostsharing.hsadminng.domain.SepaMandate;
import org.hostsharing.hsadminng.domain.*; // for static metamodels
import org.hostsharing.hsadminng.repository.SepaMandateRepository;
import org.hostsharing.hsadminng.service.dto.SepaMandateCriteria;
import org.hostsharing.hsadminng.service.dto.SepaMandateDTO;
import org.hostsharing.hsadminng.service.mapper.SepaMandateMapper;

/**
 * Service for executing complex queries for SepaMandate entities in the database.
 * The main input is a {@link SepaMandateCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link SepaMandateDTO} or a {@link Page} of {@link SepaMandateDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class SepaMandateQueryService extends QueryService<SepaMandate> {

    private final Logger log = LoggerFactory.getLogger(SepaMandateQueryService.class);

    private final SepaMandateRepository sepaMandateRepository;

    private final SepaMandateMapper sepaMandateMapper;

    public SepaMandateQueryService(SepaMandateRepository sepaMandateRepository, SepaMandateMapper sepaMandateMapper) {
        this.sepaMandateRepository = sepaMandateRepository;
        this.sepaMandateMapper = sepaMandateMapper;
    }

    /**
     * Return a {@link List} of {@link SepaMandateDTO} which matches the criteria from the database
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public List<SepaMandateDTO> findByCriteria(SepaMandateCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<SepaMandate> specification = createSpecification(criteria);
        return sepaMandateMapper.toDto(sepaMandateRepository.findAll(specification));
    }

    /**
     * Return a {@link Page} of {@link SepaMandateDTO} which matches the criteria from the database
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<SepaMandateDTO> findByCriteria(SepaMandateCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<SepaMandate> specification = createSpecification(criteria);
        return sepaMandateRepository.findAll(specification, page)
            .map(sepaMandateMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(SepaMandateCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<SepaMandate> specification = createSpecification(criteria);
        return sepaMandateRepository.count(specification);
    }

    /**
     * Function to convert SepaMandateCriteria to a {@link Specification}
     */
    private Specification<SepaMandate> createSpecification(SepaMandateCriteria criteria) {
        Specification<SepaMandate> specification = Specification.where(null);
        if (criteria != null) {
            if (criteria.getId() != null) {
                specification = specification.and(buildSpecification(criteria.getId(), SepaMandate_.id));
            }
            if (criteria.getReference() != null) {
                specification = specification.and(buildStringSpecification(criteria.getReference(), SepaMandate_.reference));
            }
            if (criteria.getIban() != null) {
                specification = specification.and(buildStringSpecification(criteria.getIban(), SepaMandate_.iban));
            }
            if (criteria.getBic() != null) {
                specification = specification.and(buildStringSpecification(criteria.getBic(), SepaMandate_.bic));
            }
            if (criteria.getCreated() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getCreated(), SepaMandate_.created));
            }
            if (criteria.getValidFrom() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getValidFrom(), SepaMandate_.validFrom));
            }
            if (criteria.getValidTo() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getValidTo(), SepaMandate_.validTo));
            }
            if (criteria.getLastUsed() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getLastUsed(), SepaMandate_.lastUsed));
            }
            if (criteria.getCancelled() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getCancelled(), SepaMandate_.cancelled));
            }
            if (criteria.getComment() != null) {
                specification = specification.and(buildStringSpecification(criteria.getComment(), SepaMandate_.comment));
            }
            if (criteria.getCustomerId() != null) {
                specification = specification.and(buildSpecification(criteria.getCustomerId(),
                    root -> root.join(SepaMandate_.customer, JoinType.LEFT).get(Customer_.id)));
            }
        }
        return specification;
    }
}
