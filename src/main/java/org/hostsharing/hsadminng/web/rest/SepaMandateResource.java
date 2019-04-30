// Licensed under Apache-2.0
package org.hostsharing.hsadminng.web.rest;

import org.hostsharing.hsadminng.service.SepaMandateQueryService;
import org.hostsharing.hsadminng.service.SepaMandateService;
import org.hostsharing.hsadminng.service.dto.SepaMandateCriteria;
import org.hostsharing.hsadminng.service.dto.SepaMandateDTO;
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
 * REST controller for managing SepaMandate.
 */
@RestController
@RequestMapping("/api")
public class SepaMandateResource {

    private final Logger log = LoggerFactory.getLogger(SepaMandateResource.class);

    private static final String ENTITY_NAME = "sepaMandate";

    private final SepaMandateService sepaMandateService;

    private final SepaMandateQueryService sepaMandateQueryService;

    public SepaMandateResource(SepaMandateService sepaMandateService, SepaMandateQueryService sepaMandateQueryService) {
        this.sepaMandateService = sepaMandateService;
        this.sepaMandateQueryService = sepaMandateQueryService;
    }

    /**
     * POST /sepa-mandates : Create a new sepaMandate.
     *
     * @param sepaMandateDTO the sepaMandateDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new sepaMandateDTO, or with status 400 (Bad
     *         Request) if the sepaMandate has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/sepa-mandates")
    public ResponseEntity<SepaMandateDTO> createSepaMandate(@Valid @RequestBody SepaMandateDTO sepaMandateDTO)
            throws URISyntaxException {
        log.debug("REST request to save SepaMandate : {}", sepaMandateDTO);
        if (sepaMandateDTO.getId() != null) {
            throw new BadRequestAlertException("A new sepaMandate cannot already have an ID", ENTITY_NAME, "idexists");
        }
        SepaMandateDTO result = sepaMandateService.save(sepaMandateDTO);
        return ResponseEntity.created(new URI("/api/sepa-mandates/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
                .body(result);
    }

    /**
     * PUT /sepa-mandates : Updates an existing sepaMandate.
     *
     * @param sepaMandateDTO the sepaMandateDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated sepaMandateDTO,
     *         or with status 400 (Bad Request) if the sepaMandateDTO is not valid,
     *         or with status 500 (Internal Server Error) if the sepaMandateDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/sepa-mandates")
    public ResponseEntity<SepaMandateDTO> updateSepaMandate(@Valid @RequestBody SepaMandateDTO sepaMandateDTO)
            throws URISyntaxException {
        log.debug("REST request to update SepaMandate : {}", sepaMandateDTO);
        if (sepaMandateDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        SepaMandateDTO result = sepaMandateService.save(sepaMandateDTO);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, sepaMandateDTO.getId().toString()))
                .body(result);
    }

    /**
     * GET /sepa-mandates : get all the sepaMandates.
     *
     * @param pageable the pagination information
     * @param criteria the criterias which the requested entities should match
     * @return the ResponseEntity with status 200 (OK) and the list of sepaMandates in body
     */
    @GetMapping("/sepa-mandates")
    public ResponseEntity<List<SepaMandateDTO>> getAllSepaMandates(SepaMandateCriteria criteria, Pageable pageable) {
        log.debug("REST request to get SepaMandates by criteria: {}", criteria);
        Page<SepaMandateDTO> page = sepaMandateQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/sepa-mandates");
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET /sepa-mandates/count : count all the sepaMandates.
     *
     * @param criteria the criterias which the requested entities should match
     * @return the ResponseEntity with status 200 (OK) and the count in body
     */
    @GetMapping("/sepa-mandates/count")
    public ResponseEntity<Long> countSepaMandates(SepaMandateCriteria criteria) {
        log.debug("REST request to count SepaMandates by criteria: {}", criteria);
        return ResponseEntity.ok().body(sepaMandateQueryService.countByCriteria(criteria));
    }

    /**
     * GET /sepa-mandates/:id : get the "id" sepaMandate.
     *
     * @param id the id of the sepaMandateDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the sepaMandateDTO, or with status 404 (Not Found)
     */
    @GetMapping("/sepa-mandates/{id}")
    public ResponseEntity<SepaMandateDTO> getSepaMandate(@PathVariable Long id) {
        log.debug("REST request to get SepaMandate : {}", id);
        Optional<SepaMandateDTO> sepaMandateDTO = sepaMandateService.findOne(id);
        return ResponseUtil.wrapOrNotFound(sepaMandateDTO);
    }

    /**
     * DELETE /sepa-mandates/:id : delete the "id" sepaMandate.
     *
     * @param id the id of the sepaMandateDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/sepa-mandates/{id}")
    public ResponseEntity<Void> deleteSepaMandate(@PathVariable Long id) {
        log.debug("REST request to delete SepaMandate : {}", id);
        sepaMandateService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
