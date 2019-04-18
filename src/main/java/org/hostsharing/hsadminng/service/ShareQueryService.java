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

import org.hostsharing.hsadminng.domain.Share;
import org.hostsharing.hsadminng.domain.*; // for static metamodels
import org.hostsharing.hsadminng.repository.ShareRepository;
import org.hostsharing.hsadminng.service.dto.ShareCriteria;
import org.hostsharing.hsadminng.service.dto.ShareDTO;
import org.hostsharing.hsadminng.service.mapper.ShareMapper;

/**
 * Service for executing complex queries for Share entities in the database.
 * The main input is a {@link ShareCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link ShareDTO} or a {@link Page} of {@link ShareDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ShareQueryService extends QueryService<Share> {

    private final Logger log = LoggerFactory.getLogger(ShareQueryService.class);

    private final ShareRepository shareRepository;

    private final ShareMapper shareMapper;

    public ShareQueryService(ShareRepository shareRepository, ShareMapper shareMapper) {
        this.shareRepository = shareRepository;
        this.shareMapper = shareMapper;
    }

    /**
     * Return a {@link List} of {@link ShareDTO} which matches the criteria from the database
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public List<ShareDTO> findByCriteria(ShareCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<Share> specification = createSpecification(criteria);
        return shareMapper.toDto(shareRepository.findAll(specification));
    }

    /**
     * Return a {@link Page} of {@link ShareDTO} which matches the criteria from the database
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ShareDTO> findByCriteria(ShareCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Share> specification = createSpecification(criteria);
        return shareRepository.findAll(specification, page)
            .map(shareMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ShareCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<Share> specification = createSpecification(criteria);
        return shareRepository.count(specification);
    }

    /**
     * Function to convert ShareCriteria to a {@link Specification}
     */
    private Specification<Share> createSpecification(ShareCriteria criteria) {
        Specification<Share> specification = Specification.where(null);
        if (criteria != null) {
            if (criteria.getId() != null) {
                specification = specification.and(buildSpecification(criteria.getId(), Share_.id));
            }
            if (criteria.getDocumentDate() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getDocumentDate(), Share_.documentDate));
            }
            if (criteria.getValueDate() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getValueDate(), Share_.valueDate));
            }
            if (criteria.getAction() != null) {
                specification = specification.and(buildSpecification(criteria.getAction(), Share_.action));
            }
            if (criteria.getQuantity() != null) {
                specification = specification.and(buildRangeSpecification(criteria.getQuantity(), Share_.quantity));
            }
            if (criteria.getRemark() != null) {
                specification = specification.and(buildStringSpecification(criteria.getRemark(), Share_.remark));
            }
            if (criteria.getMembershipId() != null) {
                specification = specification.and(buildSpecification(criteria.getMembershipId(),
                    root -> root.join(Share_.membership, JoinType.LEFT).get(Membership_.id)));
            }
        }
        return specification;
    }
}
