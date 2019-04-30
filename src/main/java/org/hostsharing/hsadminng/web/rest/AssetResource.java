// Licensed under Apache-2.0
package org.hostsharing.hsadminng.web.rest;

import org.hostsharing.hsadminng.service.AssetQueryService;
import org.hostsharing.hsadminng.service.AssetService;
import org.hostsharing.hsadminng.service.dto.AssetCriteria;
import org.hostsharing.hsadminng.service.dto.AssetDTO;
import org.hostsharing.hsadminng.web.rest.errors.BadRequestAlertException;
import org.hostsharing.hsadminng.web.rest.util.HeaderUtil;
import org.hostsharing.hsadminng.web.rest.util.PaginationUtil;

import io.github.jhipster.web.util.ResponseUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

/**
 * REST controller for managing Asset.
 */
@RestController
@RequestMapping("/api")
public class AssetResource {

    private final Logger log = LoggerFactory.getLogger(AssetResource.class);

    private static final String ENTITY_NAME = "asset";

    private final AssetService assetService;

    private final AssetQueryService assetQueryService;

    public AssetResource(AssetService assetService, AssetQueryService assetQueryService) {
        this.assetService = assetService;
        this.assetQueryService = assetQueryService;
    }

    /**
     * POST /assets : Create a new asset.
     *
     * @param assetDTO the assetDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new assetDTO, or with status 400 (Bad Request) if
     *         the asset has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/assets")
    public ResponseEntity<AssetDTO> createAsset(@Valid @RequestBody AssetDTO assetDTO) throws URISyntaxException {
        log.debug("REST request to save Asset : {}", assetDTO);
        if (assetDTO.getId() != null) {
            throw new BadRequestAlertException("A new asset cannot already have an ID", ENTITY_NAME, "idexists");
        }
        AssetDTO result = assetService.save(assetDTO);
        return ResponseEntity.created(new URI("/api/assets/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
                .body(result);
    }

    /**
     * PUT /assets : Updates an existing asset.
     *
     * @param assetDTO the assetDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated assetDTO,
     *         or with status 400 (Bad Request) if the assetDTO is not valid,
     *         or with status 500 (Internal Server Error) if the assetDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/assets")
    public ResponseEntity<AssetDTO> updateAsset(@Valid @RequestBody AssetDTO assetDTO) throws URISyntaxException {
        log.debug("REST request to update Asset : {}", assetDTO);
        if (assetDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        AssetDTO result = assetService.save(assetDTO);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, assetDTO.getId().toString()))
                .body(result);
    }

    /**
     * GET /assets : get all the assets.
     *
     * @param pageable the pagination information
     * @param criteria the criterias which the requested entities should match
     * @return the ResponseEntity with status 200 (OK) and the list of assets in body
     */
    @GetMapping("/assets")
    public ResponseEntity<List<AssetDTO>> getAllAssets(AssetCriteria criteria, Pageable pageable) {
        log.debug("REST request to get Assets by criteria: {}", criteria);
        Page<AssetDTO> page = assetQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/assets");
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET /assets/count : count all the assets.
     *
     * @param criteria the criterias which the requested entities should match
     * @return the ResponseEntity with status 200 (OK) and the count in body
     */
    @GetMapping("/assets/count")
    public ResponseEntity<Long> countAssets(AssetCriteria criteria) {
        log.debug("REST request to count Assets by criteria: {}", criteria);
        return ResponseEntity.ok().body(assetQueryService.countByCriteria(criteria));
    }

    /**
     * GET /assets/:id : get the "id" asset.
     *
     * @param id the id of the assetDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the assetDTO, or with status 404 (Not Found)
     */
    @GetMapping("/assets/{id}")
    public ResponseEntity<AssetDTO> getAsset(@PathVariable Long id) {
        log.debug("REST request to get Asset : {}", id);
        Optional<AssetDTO> assetDTO = assetService.findOne(id);
        return ResponseUtil.wrapOrNotFound(assetDTO);
    }

    /**
     * DELETE /assets/:id : delete the "id" asset.
     *
     * @param id the id of the assetDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/assets/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        log.debug("REST request to delete Asset : {}", id);
        assetService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
