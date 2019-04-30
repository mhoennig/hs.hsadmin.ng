// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service;

import org.hostsharing.hsadminng.domain.*;
import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.repository.MembershipRepository;
import org.hostsharing.hsadminng.service.dto.MembershipCriteria;
import org.hostsharing.hsadminng.service.dto.MembershipDTO;
import org.hostsharing.hsadminng.service.mapper.MembershipMapper;

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
 * Service for executing complex queries for Membership entities in the database.
 * The main input is a {@link MembershipCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link MembershipDTO} or a {@link Page} of {@link MembershipDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class MembershipQueryService extends QueryService<Membership> {

    private final Logger log = LoggerFactory.getLogger(MembershipQueryService.class);

    private final MembershipRepository membershipRepository;

    private final MembershipMapper membershipMapper;

    public MembershipQueryService(MembershipRepository membershipRepository, MembershipMapper membershipMapper) {
        this.membershipRepository = membershipRepository;
        this.membershipMapper = membershipMapper;
    }

    /**
     * Return a {@link List} of {@link MembershipDTO} which matches the criteria from the database
     * 
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public List<MembershipDTO> findByCriteria(MembershipCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<Membership> specification = createSpecification(criteria);
        return membershipMapper.toDto(membershipRepository.findAll(specification));
    }

    /**
     * Return a {@link Page} of {@link MembershipDTO} which matches the criteria from the database
     * 
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<MembershipDTO> findByCriteria(MembershipCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Membership> specification = createSpecification(criteria);
        return membershipRepository.findAll(specification, page)
                .map(membershipMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database
     * 
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(MembershipCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<Membership> specification = createSpecification(criteria);
        return membershipRepository.count(specification);
    }

    /**
     * Function to convert MembershipCriteria to a {@link Specification}
     */
    private Specification<Membership> createSpecification(MembershipCriteria criteria) {
        Specification<Membership> specification = Specification.where(null);
        if (criteria != null) {
            if (criteria.getId() != null) {
                specification = specification.and(buildSpecification(criteria.getId(), Membership_.id));
            }
            if (criteria.getAdmissionDocumentDate() != null) {
                specification = specification
                        .and(buildRangeSpecification(criteria.getAdmissionDocumentDate(), Membership_.admissionDocumentDate));
            }
            if (criteria.getCancellationDocumentDate() != null) {
                specification = specification.and(
                        buildRangeSpecification(criteria.getCancellationDocumentDate(), Membership_.cancellationDocumentDate));
            }
            if (criteria.getMemberFromDate() != null) {
                specification = specification
                        .and(buildRangeSpecification(criteria.getMemberFromDate(), Membership_.memberFromDate));
            }
            if (criteria.getMemberUntilDate() != null) {
                specification = specification
                        .and(buildRangeSpecification(criteria.getMemberUntilDate(), Membership_.memberUntilDate));
            }
            if (criteria.getRemark() != null) {
                specification = specification.and(buildStringSpecification(criteria.getRemark(), Membership_.remark));
            }
            if (criteria.getShareId() != null) {
                specification = specification.and(
                        buildSpecification(
                                criteria.getShareId(),
                                root -> root.join(Membership_.shares, JoinType.LEFT).get(Share_.id)));
            }
            if (criteria.getAssetId() != null) {
                specification = specification.and(
                        buildSpecification(
                                criteria.getAssetId(),
                                root -> root.join(Membership_.assets, JoinType.LEFT).get(Asset_.id)));
            }
            if (criteria.getCustomerId() != null) {
                specification = specification.and(
                        buildSpecification(
                                criteria.getCustomerId(),
                                root -> root.join(Membership_.customer, JoinType.LEFT).get(Customer_.id)));
            }
        }
        return specification;
    }
}
