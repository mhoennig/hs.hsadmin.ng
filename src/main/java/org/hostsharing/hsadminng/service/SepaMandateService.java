package org.hostsharing.hsadminng.service;

import org.hostsharing.hsadminng.domain.SepaMandate;
import org.hostsharing.hsadminng.repository.SepaMandateRepository;
import org.hostsharing.hsadminng.service.dto.SepaMandateDTO;
import org.hostsharing.hsadminng.service.mapper.SepaMandateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service Implementation for managing SepaMandate.
 */
@Service
@Transactional
public class SepaMandateService {

    private final Logger log = LoggerFactory.getLogger(SepaMandateService.class);

    private final SepaMandateRepository sepaMandateRepository;

    private final SepaMandateMapper sepaMandateMapper;

    public SepaMandateService(SepaMandateRepository sepaMandateRepository, SepaMandateMapper sepaMandateMapper) {
        this.sepaMandateRepository = sepaMandateRepository;
        this.sepaMandateMapper = sepaMandateMapper;
    }

    /**
     * Save a sepaMandate.
     *
     * @param sepaMandateDTO the entity to save
     * @return the persisted entity
     */
    public SepaMandateDTO save(SepaMandateDTO sepaMandateDTO) {
        log.debug("Request to save SepaMandate : {}", sepaMandateDTO);
        SepaMandate sepaMandate = sepaMandateMapper.toEntity(sepaMandateDTO);
        sepaMandate = sepaMandateRepository.save(sepaMandate);
        return sepaMandateMapper.toDto(sepaMandate);
    }

    /**
     * Get all the sepaMandates.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<SepaMandateDTO> findAll(Pageable pageable) {
        log.debug("Request to get all SepaMandates");
        return sepaMandateRepository.findAll(pageable)
            .map(sepaMandateMapper::toDto);
    }


    /**
     * Get one sepaMandate by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Optional<SepaMandateDTO> findOne(Long id) {
        log.debug("Request to get SepaMandate : {}", id);
        return sepaMandateRepository.findById(id)
            .map(sepaMandateMapper::toDto);
    }

    /**
     * Delete the sepaMandate by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete SepaMandate : {}", id);
        sepaMandateRepository.deleteById(id);
    }
}
