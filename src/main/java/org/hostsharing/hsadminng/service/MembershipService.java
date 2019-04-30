// Licensed under Apache-2.0
package org.hostsharing.hsadminng.service;

import org.hostsharing.hsadminng.domain.Membership;
import org.hostsharing.hsadminng.repository.MembershipRepository;
import org.hostsharing.hsadminng.service.dto.MembershipDTO;
import org.hostsharing.hsadminng.service.mapper.MembershipMapper;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import javax.persistence.EntityManager;

/**
 * Service Implementation for managing Membership.
 */
@Service
@Transactional
public class MembershipService implements IdToDtoResolver<MembershipDTO> {

    private final Logger log = LoggerFactory.getLogger(MembershipService.class);

    private final EntityManager em;

    private final MembershipValidator membershipValidator;

    private final MembershipRepository membershipRepository;

    private final MembershipMapper membershipMapper;

    public MembershipService(
            final EntityManager em,
            final MembershipValidator membershipValidator,
            final MembershipRepository membershipRepository,
            final MembershipMapper membershipMapper) {
        this.em = em;
        this.membershipValidator = membershipValidator;
        this.membershipRepository = membershipRepository;
        this.membershipMapper = membershipMapper;
    }

    /**
     * Save a membership.
     *
     * @param membershipDTO the entity to save
     * @return the persisted entity
     */
    public MembershipDTO save(MembershipDTO membershipDTO) {
        log.debug("Request to save Membership : {}", membershipDTO);

        membershipValidator.validate(membershipDTO);

        Membership membership = membershipMapper.toEntity(membershipDTO);
        membership = membershipRepository.save(membership);
        em.flush();
        em.refresh(membership);
        return membershipMapper.toDto(membership);
    }

    /**
     * Get all the memberships.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<MembershipDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Memberships");
        return membershipRepository.findAll(pageable)
                .map(membershipMapper::toDto);
    }

    /**
     * Get one membership by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<MembershipDTO> findOne(Long id) {
        log.debug("Request to get Membership : {}", id);
        return membershipRepository.findById(id)
                .map(membershipMapper::toDto);
    }

    /**
     * Prevent deleting a membership by id via service call.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Membership : {}", id);
        throw new BadRequestAlertException("Membership cannot be deleted", Membership.ENTITY_NAME, "membershipNotDeletable");
    }
}
