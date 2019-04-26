package org.hostsharing.hsadminng.service;

import org.hostsharing.hsadminng.domain.Asset;
import org.hostsharing.hsadminng.repository.AssetRepository;
import org.hostsharing.hsadminng.service.dto.AssetDTO;
import org.hostsharing.hsadminng.service.mapper.AssetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Optional;

/**
 * Service Implementation for managing Asset.
 */
@Service
@Transactional
public class AssetService implements IdToDtoResolver<AssetDTO> {

    private final Logger log = LoggerFactory.getLogger(AssetService.class);

    private final AssetRepository assetRepository;

    private final AssetMapper assetMapper;
    private final AssetValidator assetValidator;
    private final EntityManager em;

    public AssetService(final EntityManager em, final AssetRepository assetRepository, final AssetMapper assetMapper, final AssetValidator assetValidator) {
        this.em = em;
        this.assetRepository = assetRepository;
        this.assetMapper = assetMapper;
        this.assetValidator = assetValidator;
    }

    /**
     * Save a asset.
     *
     * @param assetDTO the entity to save
     * @return the persisted entity
     */
    public AssetDTO save(AssetDTO assetDTO) {
        log.debug("Request to save Asset : {}", assetDTO);
        assetValidator.validate(assetDTO);
        Asset asset = assetMapper.toEntity(assetDTO);
        asset = assetRepository.save(asset);
        em.flush();
        em.refresh(asset);
        return assetMapper.toDto(asset);
    }

    /**
     * Get all the assets.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<AssetDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Assets");
        return assetRepository.findAll(pageable)
            .map(assetMapper::toDto);
    }


    /**
     * Get one asset by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Optional<AssetDTO> findOne(Long id) {
        log.debug("Request to get Asset : {}", id);
        return assetRepository.findById(id)
            .map(assetMapper::toDto);
    }

    /**
     * Delete the asset by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Asset : {}", id);
        assetRepository.deleteById(id);
    }
}
