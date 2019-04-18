package org.hostsharing.hsadminng.service;

import org.hostsharing.hsadminng.domain.Share;
import org.hostsharing.hsadminng.repository.ShareRepository;
import org.hostsharing.hsadminng.service.dto.ShareDTO;
import org.hostsharing.hsadminng.service.mapper.ShareMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service Implementation for managing Share.
 */
@Service
@Transactional
public class ShareService {

    private final Logger log = LoggerFactory.getLogger(ShareService.class);

    private final ShareRepository shareRepository;

    private final ShareMapper shareMapper;

    public ShareService(ShareRepository shareRepository, ShareMapper shareMapper) {
        this.shareRepository = shareRepository;
        this.shareMapper = shareMapper;
    }

    /**
     * Save a share.
     *
     * @param shareDTO the entity to save
     * @return the persisted entity
     */
    public ShareDTO save(ShareDTO shareDTO) {
        log.debug("Request to save Share : {}", shareDTO);
        Share share = shareMapper.toEntity(shareDTO);
        share = shareRepository.save(share);
        return shareMapper.toDto(share);
    }

    /**
     * Get all the shares.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<ShareDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Shares");
        return shareRepository.findAll(pageable)
            .map(shareMapper::toDto);
    }


    /**
     * Get one share by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Optional<ShareDTO> findOne(Long id) {
        log.debug("Request to get Share : {}", id);
        return shareRepository.findById(id)
            .map(shareMapper::toDto);
    }

    /**
     * Delete the share by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Share : {}", id);
        shareRepository.deleteById(id);
    }
}
